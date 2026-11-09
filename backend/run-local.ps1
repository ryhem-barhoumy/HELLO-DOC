$ErrorActionPreference = 'Stop'

function Read-SecretText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
}

$mysqlCandidates = @()
if ($env:LARAGON_ROOT) { $mysqlCandidates += Join-Path $env:LARAGON_ROOT 'bin\mysql\*\bin\mysql.exe' }
$mysqlCandidates += 'C:\laragon\bin\mysql\*\bin\mysql.exe'
$mysqlExe = $mysqlCandidates | ForEach-Object { Get-ChildItem -Path $_ -ErrorAction SilentlyContinue } | Select-Object -First 1 -ExpandProperty FullName
if (-not $mysqlExe) { throw 'Could not find Laragon mysql.exe. Set LARAGON_ROOT to the Laragon directory.' }

$env:HELLODOC_DB_URL = 'jdbc:mysql://127.0.0.1:3308/hello_doc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:HELLODOC_DB_USERNAME = 'root'
if ([string]::IsNullOrWhiteSpace($env:HELLODOC_DB_PASSWORD)) { $env:HELLODOC_DB_PASSWORD = Read-SecretText 'Laragon MySQL root password (press Enter if it is empty)' }

# Create or verify only the requested schema; this statement leaves all other databases untouched.
if ([string]::IsNullOrEmpty($env:HELLODOC_DB_PASSWORD)) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
else { $env:MYSQL_PWD = $env:HELLODOC_DB_PASSWORD }
try {
    & $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3308 --user=root --batch --skip-column-names --execute "CREATE DATABASE IF NOT EXISTS hello_doc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='hello_doc';"
    if ($LASTEXITCODE -ne 0) { throw 'Could not connect to Laragon MySQL or create/verify hello_doc.' }
}
finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

if ([string]::IsNullOrWhiteSpace($env:HELLODOC_ADMIN_INITIAL_PASSWORD)) { $env:HELLODOC_ADMIN_INITIAL_PASSWORD = Read-SecretText 'Initial Admin password (required; separate from MySQL password)' }
if ([string]::IsNullOrWhiteSpace($env:HELLODOC_ADMIN_INITIAL_PASSWORD)) { throw 'The initial Admin password is required.' }

if ($env:HELLODOC_EMAIL_ENABLED -ne 'true') {
    $configureGmail = Read-Host 'Configure Gmail SMTP for doctor approval emails now? [y/N]'
    if ($configureGmail -match '^(y|yes|o|oui)$') {
        $gmailAddress = (Read-Host 'Gmail sender address').Trim()
        if ($gmailAddress -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') { throw 'Enter a valid Gmail sender address.' }
        $gmailAppPassword = Read-SecretText 'Gmail App Password (not your normal Google password)'
        if ([string]::IsNullOrWhiteSpace($gmailAppPassword)) { throw 'A Gmail App Password is required to enable email delivery.' }
        $env:HELLODOC_EMAIL_ENABLED = 'true'
        $env:HELLODOC_SMTP_HOST = 'smtp.gmail.com'
        $env:HELLODOC_SMTP_PORT = '587'
        $env:HELLODOC_SMTP_USERNAME = $gmailAddress
        $env:HELLODOC_SMTP_PASSWORD = $gmailAppPassword -replace '\s',''
        $env:HELLODOC_SMTP_AUTH = 'true'
        $env:HELLODOC_SMTP_STARTTLS = 'true'
        $env:HELLODOC_EMAIL_FROM = $gmailAddress
        $gmailAppPassword = $null
        $gmailAddress = $null
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

try { & $mavenPath -f (Join-Path $PSScriptRoot 'pom.xml') spring-boot:run }
finally {
    Remove-Item Env:HELLODOC_DB_URL,Env:HELLODOC_DB_USERNAME,Env:HELLODOC_DB_PASSWORD,Env:HELLODOC_ADMIN_INITIAL_PASSWORD,Env:HELLODOC_EMAIL_ENABLED,Env:HELLODOC_SMTP_HOST,Env:HELLODOC_SMTP_PORT,Env:HELLODOC_SMTP_USERNAME,Env:HELLODOC_SMTP_PASSWORD,Env:HELLODOC_SMTP_AUTH,Env:HELLODOC_SMTP_STARTTLS,Env:HELLODOC_EMAIL_FROM -ErrorAction SilentlyContinue
}
