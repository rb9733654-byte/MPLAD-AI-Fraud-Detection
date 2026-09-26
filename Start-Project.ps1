$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Join-Path $projectRoot 'backend\mplad-backend'
$pythonRoot = Join-Path $projectRoot 'ai-model'

function Stop-WithMessage([string]$message) {
    Write-Host "`n$message" -ForegroundColor Red
    exit 1
}

function Invoke-MySql([string]$sql, [int]$port) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $script:mysqlClient
    $startInfo.Arguments = "--protocol=TCP --host=127.0.0.1 --port=$port --user=$script:dbUser --batch --skip-column-names"
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $process.StandardInput.Write($sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "MySQL error: $stderr" }
    return $stdout.Trim()
}

function Start-ServiceWindow([string]$serviceName) {
    $runner = Join-Path $projectRoot 'Run-Service.ps1'
    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$runner`"", $serviceName
    ) -WorkingDirectory $projectRoot | Out-Null
}

function Test-Service([string]$serviceName) {
    try {
        switch ($serviceName) {
            'frontend' {
                $client = New-Object Net.WebClient
                $served = $client.DownloadData('http://127.0.0.1:5500/Access.html')
                $source = [IO.File]::ReadAllBytes((Join-Path $projectRoot 'frontend\Access.html'))
                return [Convert]::ToBase64String($source) -eq [Convert]::ToBase64String($served)
            }
            'backend' {
                $response = Invoke-WebRequest 'http://127.0.0.1:8080/api/public/projects' -UseBasicParsing -TimeoutSec 4
                return $response.StatusCode -eq 200 -and $response.Content -match '^\s*\['
            }
            'ai' {
                $response = Invoke-WebRequest 'http://127.0.0.1:8000/health' -UseBasicParsing -TimeoutSec 4
                return $response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"ok"'
            }
        }
    } catch { return $false }
    return $false
}

Write-Host 'MPLAD Project Monitoring - safe startup' -ForegroundColor Cyan
Write-Host 'This verifies the existing database without modifying schema or records, then starts or reuses the app services.'

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) { Stop-WithMessage 'Java 17 or later is required. Install a JDK, reopen this window, and run Start-Project.bat again.' }
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { Stop-WithMessage 'A Java Development Kit (JDK) 17 or later is required. Install a JDK, reopen this window, and try again.' }
$javaVersion = (& java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "(?<version>\d+)' -and $javaVersion -notmatch 'openjdk (?<version>\d+)') {
    Stop-WithMessage 'Could not read the Java version. Install Java 17 or later and try again.'
}
if ([int]$Matches.version -lt 17) { Stop-WithMessage 'Java 17 or later is required. Install a JDK, reopen this window, and try again.' }

$pythonLauncher = Get-Command py -ErrorAction SilentlyContinue
if (-not $pythonLauncher) { Stop-WithMessage 'Python 3 is required. Install Python with the Python Launcher (py), reopen this window, and try again.' }
& py -3 --version | Out-Null
if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'Python 3 was not found by the Python Launcher. Install Python 3 and try again.' }

$mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
if ($mysqlCommand) { $script:mysqlClient = $mysqlCommand.Source }
else {
    $mysqlClientCandidate = Get-ChildItem 'C:\Program Files\MySQL' -Filter mysql.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\bin\\mysql\.exe$' } | Select-Object -First 1
    if (-not $mysqlClientCandidate) { Stop-WithMessage 'MySQL 8 is required. Install MySQL Server and its command-line client, then run Start-Project.bat again.' }
    $script:mysqlClient = $mysqlClientCandidate.FullName
}

$script:dbUser = Read-Host 'MySQL username [root]'
if ([string]::IsNullOrWhiteSpace($script:dbUser)) { $script:dbUser = 'root' }
$securePassword = Read-Host 'MySQL password (press Enter if none)' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try { $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }

$dbPort = 3306
try { $databaseReady = (Invoke-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mplad_db' AND table_name='projects';" $dbPort) -eq '1' }
catch { $databaseReady = $false }
if (-not $databaseReady) {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Stop-WithMessage 'Could not verify the existing mplad_db.projects table on MySQL port 3306. No schema or data was changed.'
}

$requiredTables = @('projects', 'project_history', 'progress_evidence', 'authority_reviews', 'contractor_notifications', 'public_users', 'public_otp_verifications', 'public_feedback', 'workspace_users')
try {
    $existingTables = (Invoke-MySql "SELECT table_name FROM information_schema.tables WHERE table_schema='mplad_db';" $dbPort) -split "`r?`n"
    $missingTables = @($requiredTables | Where-Object { $_ -notin $existingTables })
    if ($missingTables.Count -gt 0) { throw "Required existing tables are missing: $($missingTables -join ', ')" }
} catch {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Stop-WithMessage "Read-only database verification failed. No schema or data was changed.`n$($_.Exception.Message)"
}

$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:$dbPort/mplad_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME = $script:dbUser
$env:SPRING_DATASOURCE_PASSWORD = $env:MYSQL_PWD
$env:DEMO_OTP_MODE = 'true'
$profilePath = [Environment]::GetFolderPath('UserProfile')
if ($profilePath) { $env:JAVA_TOOL_OPTIONS = "-Duser.home=`"$profilePath`"" }
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

$pythonEnv = Join-Path $pythonRoot '.venv\Scripts\python.exe'
if (-not (Test-Path $pythonEnv)) {
    Write-Host 'Creating the Python environment...'
    & py -3 -m venv (Join-Path $pythonRoot '.venv')
    if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'Could not create the Python environment.' }
}
if (-not (Test-Path (Join-Path $pythonRoot '.venv\Scripts\uvicorn.exe'))) {
    Write-Host 'Installing the AI service packages (internet is needed the first time)...'
    & $pythonEnv -m pip install -r (Join-Path $pythonRoot 'requirements.txt')
    if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'Could not install AI service packages. Check the internet connection and run this file again.' }
}

foreach ($service in @('ai', 'backend', 'frontend')) {
    $port = @{ ai = 8000; backend = 8080; frontend = 5500 }[$service]
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        if (Test-Service $service) { Write-Host "Reusing verified $service service on port $port." -ForegroundColor Green }
        else { Stop-WithMessage "Port $port is occupied, but its $service health check failed. The listener was left untouched; inspect it before retrying." }
    } else {
        Write-Host "Starting $service service on port $port..."
        Start-ServiceWindow $service
    }
}

$ready = $false
Write-Host 'Waiting for the services to become ready. First startup may take a few minutes while Maven downloads its packages.'
for ($attempt = 0; $attempt -lt 180; $attempt++) {
    try {
        if (-not (Test-Service 'ai') -or -not (Test-Service 'backend') -or -not (Test-Service 'frontend')) { throw 'A service health check failed.' }
        $ready = $true
        break
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ready) { Stop-WithMessage 'The services started but did not all become ready in time. Check the AI, backend, and frontend windows for errors.' }

Write-Host "`nProject is ready: http://localhost:5500/Access.html" -ForegroundColor Green
Write-Host 'Keep the three service windows open while using the project. Close them to stop the services.'
try { Start-Process 'http://localhost:5500/Access.html' } catch { Write-Host 'Open this address in your browser: http://localhost:5500/Access.html' }
