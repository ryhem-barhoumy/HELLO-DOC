$ErrorActionPreference = 'Stop'

function Read-SecretText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
}

$base = [Uri]'http://localhost:8080/api/'
$handler = [Net.Http.HttpClientHandler]::new()
$handler.CookieContainer = [Net.CookieContainer]::new()
$client = [Net.Http.HttpClient]::new($handler)
$originalPassword = Read-SecretText 'Current admin password'
$temporaryPassword = Read-SecretText 'Temporary password for the change/restore check (remember it in case restore fails)'
if ($temporaryPassword -eq $originalPassword -or $temporaryPassword.Length -lt 12) { throw 'Choose a different temporary password with at least 12 characters.' }
$passwordChanged = $false

function Send-Request([string]$Method,[string]$Path,[object]$Body=$null,[int[]]$Expected=@(200)) {
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($Method),[Uri]::new($base,$Path))
    if ($null -ne $Body) { $json = ConvertTo-Json -InputObject $Body -Depth 10 -Compress; $request.Content = [Net.Http.StringContent]::new($json,[Text.Encoding]::UTF8,'application/json') }
    if ($Method -notin @('GET','HEAD','OPTIONS')) {
        $cookie = $handler.CookieContainer.GetCookies($base)['XSRF-TOKEN']
        if (-not $cookie) { throw 'XSRF-TOKEN cookie is missing. Call GET /auth/csrf before unsafe requests.' }
        $request.Headers.Add('X-XSRF-TOKEN',[Uri]::UnescapeDataString($cookie.Value))
    }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -notin $Expected) { throw "$Method $Path returned HTTP $([int]$response.StatusCode); expected $($Expected -join ', '). Response: $content" }
    [pscustomobject]@{ Status=[int]$response.StatusCode; Content=$content }
}

function Invoke-AdminLogin([string]$Password) {
    Send-Request 'GET' 'auth/csrf' $null @(200) | Out-Null
    $result = Send-Request 'POST' 'auth/login' @{email='admin@hellodoc.com';password=$Password} @(200)
    $user = $result.Content | ConvertFrom-Json
    if ($user.role -ne 'ADMIN' -or $user.email -ne 'admin@hellodoc.com') { throw 'Login response did not contain the expected database-backed ADMIN account.' }
}

try {
    Invoke-AdminLogin $originalPassword
    Send-Request 'GET' 'auth/me' $null @(200) | Out-Null
    $dashboard = Send-Request 'GET' 'admin/dashboard' $null @(200)
    $dashboardData = $dashboard.Content | ConvertFrom-Json
    if ($null -eq $dashboardData.patientsCount -or $null -eq $dashboardData.doctorsCount) { throw 'Admin dashboard did not return persisted account counts.' }

    Send-Request 'POST' 'auth/password' @{currentPassword=$originalPassword;newPassword=$temporaryPassword} @(204) | Out-Null
    $passwordChanged = $true
    Send-Request 'POST' 'auth/password' @{currentPassword=$temporaryPassword;newPassword=$originalPassword} @(204) | Out-Null
    $passwordChanged = $false

    Send-Request 'POST' 'auth/logout' @{} @(204) | Out-Null
    Send-Request 'GET' 'auth/me' $null @(401) | Out-Null
    Invoke-AdminLogin $originalPassword
    Send-Request 'GET' 'admin/dashboard' $null @(200) | Out-Null
    Send-Request 'POST' 'auth/logout' @{} @(204) | Out-Null
    'PASS: real admin login, session restore, dashboard, password change and restore, logout, and session invalidation.'
}
finally {
    if ($passwordChanged) {
        try { Send-Request 'POST' 'auth/password' @{currentPassword=$temporaryPassword;newPassword=$originalPassword} @(204) | Out-Null }
        catch { Write-Error 'Could not restore the prior administrator password; sign in with the temporary password you entered.' }
    }
    $client.Dispose(); $handler.Dispose(); $originalPassword=$null; $temporaryPassword=$null
}
