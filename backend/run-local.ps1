$ErrorActionPreference = 'Stop'
$managedVariables = @('HELLODOC_DB_HOST','HELLODOC_DB_PORT','HELLODOC_DB_NAME','HELLODOC_DB_URL','HELLODOC_DB_USERNAME','HELLODOC_DB_PASSWORD','HELLODOC_ADMIN_INITIAL_PASSWORD','HELLODOC_EMAIL_ENABLED','HELLODOC_SMTP_HOST','HELLODOC_SMTP_PORT','HELLODOC_SMTP_USERNAME','HELLODOC_SMTP_PASSWORD','HELLODOC_EMAIL_FROM','SERVER_PORT','LARAGON_ROOT')
$originalEnvironment = @{}
foreach ($name in $managedVariables) {
    $item = Get-Item "Env:$name" -ErrorAction SilentlyContinue
    if ($null -ne $item) { $originalEnvironment[$name] = $item.Value }
}

function Import-LocalEnvironment([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        if ($trimmed -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            throw "Invalid environment entry in $Path. Use KEY=VALUE lines."
        }
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        # Existing process environment variables take precedence over the local file.
        if (-not (Test-Path "Env:$key")) { Set-Item "Env:$key" $value }
    }
}

function Read-SecretText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
}

function Restore-LocalEnvironment {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    foreach ($name in $managedVariables) {
        if ($originalEnvironment.ContainsKey($name)) { Set-Item "Env:$name" $originalEnvironment[$name] }
        else { Remove-Item "Env:$name" -ErrorAction SilentlyContinue }
    }
}

trap { Restore-LocalEnvironment; throw }

function Read-RequiredValue([string]$Name, [string]$Prompt) {
    $current = (Get-Item "Env:$Name" -ErrorAction SilentlyContinue).Value
    if ([string]::IsNullOrWhiteSpace($current)) { $current = (Read-Host $Prompt).Trim() }
    if ([string]::IsNullOrWhiteSpace($current)) { throw "$Name is required." }
    Set-Item "Env:$Name" $current
}

$localEnvPath = Join-Path $PSScriptRoot '.env.local'
Import-LocalEnvironment $localEnvPath

Read-RequiredValue 'HELLODOC_DB_HOST' 'MySQL host (for example 127.0.0.1)'
Read-RequiredValue 'HELLODOC_DB_PORT' 'MySQL port for your computer'
Read-RequiredValue 'HELLODOC_DB_NAME' 'MySQL database/schema name'
Read-RequiredValue 'HELLODOC_DB_USERNAME' 'MySQL username'
if (-not (Test-Path Env:HELLODOC_DB_PASSWORD)) {
    $env:HELLODOC_DB_PASSWORD = Read-SecretText 'MySQL password (press Enter if the account has no password)'
}
if ($env:HELLODOC_DB_PORT -notmatch '^\d{1,5}$' -or [int]$env:HELLODOC_DB_PORT -lt 1 -or [int]$env:HELLODOC_DB_PORT -gt 65535) {
    throw 'HELLODOC_DB_PORT must be a valid TCP port.'
}
if ($env:HELLODOC_DB_NAME -notmatch '^[A-Za-z0-9_]+$') { throw 'Database name may contain only letters, digits, and underscores.' }

$jdbcOptions = 'useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:HELLODOC_DB_URL = "jdbc:mysql://$($env:HELLODOC_DB_HOST):$($env:HELLODOC_DB_PORT)/$($env:HELLODOC_DB_NAME)?$jdbcOptions"

$mysqlCandidates = @()
if ($env:LARAGON_ROOT) { $mysqlCandidates += Join-Path $env:LARAGON_ROOT 'bin\mysql\*\bin\mysql.exe' }
$mysqlExe = $mysqlCandidates | ForEach-Object { Get-ChildItem -Path $_ -ErrorAction SilentlyContinue } | Select-Object -First 1 -ExpandProperty FullName
if (-not $mysqlExe) { $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue; if ($mysqlCommand) { $mysqlExe = $mysqlCommand.Source } }
if (-not $mysqlExe) { throw 'Could not find mysql.exe. Set LARAGON_ROOT or install a MySQL client.' }

# Create or verify only the configured schema. No other database is modified.
if ([string]::IsNullOrEmpty($env:HELLODOC_DB_PASSWORD)) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
else { $env:MYSQL_PWD = $env:HELLODOC_DB_PASSWORD }
try {
    $schema = $env:HELLODOC_DB_NAME
    $sql = "CREATE DATABASE IF NOT EXISTS $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$schema';"
    & $mysqlExe --protocol=TCP --host=$env:HELLODOC_DB_HOST --port=$env:HELLODOC_DB_PORT --user=$env:HELLODOC_DB_USERNAME --batch --skip-column-names --execute $sql
    if ($LASTEXITCODE -ne 0) { throw 'Could not connect to MySQL or create/verify the configured schema.' }
}
finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

if (-not (Test-Path Env:HELLODOC_ADMIN_INITIAL_PASSWORD) -or [string]::IsNullOrWhiteSpace($env:HELLODOC_ADMIN_INITIAL_PASSWORD)) {
    $env:HELLODOC_ADMIN_INITIAL_PASSWORD = Read-SecretText 'Initial Admin password (required; separate from MySQL password)'
}
if ([string]::IsNullOrWhiteSpace($env:HELLODOC_ADMIN_INITIAL_PASSWORD)) { throw 'The initial Admin password is required.' }

if ($env:HELLODOC_EMAIL_ENABLED -ne 'true') {
    $configureEmail = Read-Host 'Configure SMTP email delivery now? [y/N]'
    if ($configureEmail -match '^(y|yes|o|oui)$') {
        $env:HELLODOC_SMTP_HOST = (Read-Host 'SMTP host').Trim()
        $env:HELLODOC_SMTP_PORT = (Read-Host 'SMTP port').Trim()
        $env:HELLODOC_SMTP_USERNAME = (Read-Host 'SMTP username').Trim()
        $env:HELLODOC_SMTP_PASSWORD = Read-SecretText 'SMTP password/app password'
        $env:HELLODOC_EMAIL_FROM = (Read-Host 'Sender email address').Trim()
        $env:HELLODOC_EMAIL_ENABLED = 'true'
    }
    else { $env:HELLODOC_EMAIL_ENABLED = 'false' }
}

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenPath = if ($mavenCommand) { $mavenCommand.Source } else { $null }
if (-not $mavenPath) {
    $mavenRoot = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
    $mavenPath = Get-ChildItem $mavenRoot -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mavenPath) { throw 'Maven is not installed. Install Maven 3.9+ and retry.' }

try { & $mavenPath -f (Join-Path $PSScriptRoot 'pom.xml') spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=$PSScriptRoot\target" }
finally {
    Restore-LocalEnvironment
}
