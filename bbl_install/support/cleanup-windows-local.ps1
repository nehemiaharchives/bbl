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
$legacyInstallRoot = Join-Path $localAppData '.bbl'
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
