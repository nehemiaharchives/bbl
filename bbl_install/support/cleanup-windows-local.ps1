$ErrorActionPreference = 'Stop'

$localAppData = if ($env:LOCALAPPDATA) {
    $env:LOCALAPPDATA
} elseif ($env:USERPROFILE) {
    Join-Path $env:USERPROFILE 'AppData\Local'
} else {
    throw 'LOCALAPPDATA and USERPROFILE are not set; cannot resolve bbl install root.'
}

$installRoot = Join-Path $localAppData '.bbl'
$binDir = Join-Path $installRoot 'bin'

function Normalize-PathEntry {
    param([string]$PathEntry)

    try {
        return [System.IO.Path]::GetFullPath($PathEntry).TrimEnd('\')
    } catch {
        return $PathEntry.TrimEnd('\')
    }
}

if (Test-Path -LiteralPath $installRoot) {
    Remove-Item -LiteralPath $installRoot -Recurse -Force
    Write-Host "Removed $installRoot"
} else {
    Write-Host "$installRoot does not exist"
}

$environmentKey = 'HKCU:\Environment'
$pathValue = (Get-ItemProperty -Path $environmentKey -Name Path -ErrorAction SilentlyContinue).Path
if ($null -ne $pathValue) {
    $normalizedBinDir = Normalize-PathEntry $binDir
    $pathEntries = $pathValue -split ';' | Where-Object {
        $_ -and ((Normalize-PathEntry $_) -ine $normalizedBinDir)
    }
    $newPathValue = $pathEntries -join ';'

    if ($newPathValue -ne $pathValue) {
        Set-ItemProperty -Path $environmentKey -Name Path -Value $newPathValue
        Write-Host "Removed $binDir from user PATH"
    }
}
