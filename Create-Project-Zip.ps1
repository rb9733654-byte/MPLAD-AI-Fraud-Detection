$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$parent = Split-Path -Parent $projectRoot
$zipPath = Join-Path $projectRoot ('MPLAD-AI-Fraud-Detection-shareable-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
$stage = Join-Path ([IO.Path]::GetTempPath()) ('MPLAD-AI-Fraud-Detection-' + [guid]::NewGuid().ToString('N'))
$excludedDirectories = @('.git', '.venv', 'venv', 'target', '.maven-home', 'mysql-data', '.local-home', 'uploads', '__pycache__', 'node_modules')

function Copy-ShareableFiles([string]$sourceDirectory, [string]$destinationDirectory) {
    foreach ($entry in (Get-ChildItem -LiteralPath $sourceDirectory -Force -ErrorAction SilentlyContinue)) {
        if ($entry.PSIsContainer) {
            if ($excludedDirectories -contains $entry.Name) { continue }
            $childDestination = Join-Path $destinationDirectory $entry.Name
            New-Item -ItemType Directory -Path $childDestination -Force | Out-Null
            Copy-ShareableFiles $entry.FullName $childDestination
            continue
        }
        if ($entry.Extension -in @('.log', '.tmp', '.temp', '.bak', '.pyc', '.zip')) { continue }
        if ($entry.Name -eq 'my.ini') { continue }
        $relative = $entry.FullName.Substring($projectRoot.Length).TrimStart('\')
        if ($relative -match '^backend\\mplad-backend\\src\\main\\resources\\application(-local)?\.(properties|yml|yaml)$') { continue }
        $destination = Join-Path $destinationDirectory $entry.Name
        Copy-Item -LiteralPath $entry.FullName -Destination $destination
    }
}

try {
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    Copy-ShareableFiles $projectRoot $stage
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($stage, $zipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false)
    Write-Host "Created shareable project archive: $zipPath" -ForegroundColor Green
    Write-Host 'The archive excludes local databases, credentials, build outputs, uploads, and virtual environments.'
} finally {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
