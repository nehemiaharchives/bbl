$ErrorActionPreference = 'Stop'

$localAppData = if ($env:LOCALAPPDATA) {
    $env:LOCALAPPDATA
} elseif ($env:USERPROFILE) {
    Join-Path $env:USERPROFILE 'AppData\Local'
} else {
    throw 'LOCALAPPDATA and USERPROFILE are not set; cannot resolve bbl install root.'
}

$userProfile = if ($env:USERPROFILE) {
    $env:USERPROFILE
} else {
    Split-Path -Parent $localAppData
}

$installRoot = Join-Path $userProfile '.bbl'
$binDir = Join-Path $localAppData 'Programs\bbl'
$helperBinDir = Join-Path $installRoot 'bin'
$legacyInstallRoot = Join-Path $localAppData 'bbl'
$legacyBinDir = Join-Path $legacyInstallRoot 'bin'

function Normalize-PathEntry {
    param([string]$PathEntry)

    try {
        return [System.IO.Path]::GetFullPath($PathEntry).TrimEnd('\')
    } catch {
        return $PathEntry.TrimEnd('\')
    }
}

foreach ($path in @($installRoot, $binDir, $legacyInstallRoot)) {
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
        Write-Host "Removed $path"
    } else {
        Write-Host "$path does not exist"
    }
}

$msiInstallDir = Join-Path ${env:ProgramFiles} 'bbl'
$msiPerUserInstallDir = Join-Path $localAppData 'bbl'

$bblMsiProduct = Get-ChildItem 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*', 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
    Where-Object { $_.GetValue('DisplayName') -eq 'bbl' } |
    Select-Object -First 1

if ($bblMsiProduct) {
    $productCode = $bblMsiProduct.PSChildName
    Write-Host "Uninstalling bbl MSI product $productCode"
    Start-Process msiexec -ArgumentList @('/x', $productCode, '/qn', '/norestart') -Wait -NoNewWindow
}

foreach ($dir in @($msiInstallDir, $msiPerUserInstallDir)) {
    if (Test-Path -LiteralPath $dir) {
        Remove-Item -LiteralPath $dir -Recurse -Force
        Write-Host "Removed $dir"
    }
}

$msiWorkRoot = Join-Path $env:TEMP 'bbl-msi-install'
if (Test-Path -LiteralPath $msiWorkRoot) {
    Remove-Item -LiteralPath $msiWorkRoot -Recurse -Force
    Write-Host "Removed $msiWorkRoot"
}

$wingetWorkRoot = if ($env:TEMP) { $env:TEMP } elseif ($env:TMP) { $env:TMP } else { [System.IO.Path]::GetTempPath() }
$wingetWorkRoot = Join-Path $wingetWorkRoot 'bbl-winget-install'
$serverPidFile = Join-Path $wingetWorkRoot 'server.pid'

if (Test-Path -LiteralPath $serverPidFile) {
    $pidText = Get-Content -LiteralPath $serverPidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pidText -match '^\d+$') {
        Stop-Process -Id ([int]$pidText) -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped bbl winget fixture HTTP server process $pidText"
    }
}

winget uninstall --id Gnit.Bbl --exact --disable-interactivity --silent --force 2>$null | Out-Host

if (Test-Path -LiteralPath $wingetWorkRoot) {
    Remove-Item -LiteralPath $wingetWorkRoot -Recurse -Force
    Write-Host "Removed $wingetWorkRoot"
}

$environmentKey = 'HKCU:\Environment'
$pathValue = (Get-ItemProperty -Path $environmentKey -Name Path -ErrorAction SilentlyContinue).Path
if ($null -ne $pathValue) {
    $normalizedBinDirs = @(
        Normalize-PathEntry $binDir
        Normalize-PathEntry $helperBinDir
        Normalize-PathEntry $legacyBinDir
    )
    $pathEntries = $pathValue -split ';' | Where-Object {
        $_ -and ((Normalize-PathEntry $_) -notin $normalizedBinDirs)
    }
    $newPathValue = $pathEntries -join ';'

    if ($newPathValue -ne $pathValue) {
        Set-ItemProperty -Path $environmentKey -Name Path -Value $newPathValue
        Write-Host "Removed bbl install directories from user PATH"
    }
}
