param([Parameter(Mandatory = $true)][ValidateSet('ai', 'backend', 'frontend')][string]$Service)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
switch ($Service) {
    'ai' {
        Set-Location (Join-Path $projectRoot 'ai-model')
        & .\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
    }
    'backend' {
        Set-Location (Join-Path $projectRoot 'backend\mplad-backend')
        & .\mvnw.cmd spring-boot:run
    }
    'frontend' {
        & py -3 -m http.server 5500 --bind 127.0.0.1 --directory (Join-Path $projectRoot 'frontend')
    }
}
