# bbl history E2E tests for Windows

param(
  [string]$BblPath = ""
)

$ErrorActionPreference = 'Stop'

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $BblPath) {
  $candidates = @(
    "$env:LOCALAPPDATA\Programs\bbl\bbl.exe",
    "$env:USERPROFILE\.bbl\bin\bbl.exe",
    "$env:USERPROFILE\.bbl\bbl.exe"
  )

  $BblPath = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

  if (-not $BblPath) {
    Write-Host "ERROR: bbl.exe not found. Provide -BblPath or ensure it is installed."
    exit 1
  }
}

if (-not (Test-Path -LiteralPath $BblPath)) {
  Write-Host "ERROR: bbl.exe not found at: $BblPath"
  exit 1
}

$originalUserProfile = $env:USERPROFILE
$bblDir = Split-Path -Parent $BblPath

$packCandidates = @(
  (Join-Path $originalUserProfile '.bbl\packs'),
  (Join-Path $bblDir 'packs'),
  $bblDir
) | Where-Object { $_ }

$helperCandidates = @(
  (Join-Path $originalUserProfile '.bbl\bin'),
  $bblDir
) | Where-Object { $_ }

$packSource = $packCandidates | Where-Object {
  (Test-Path -LiteralPath $_ -PathType Container) -and
    (Test-Path -LiteralPath (Join-Path $_ 'webus.zip')) -and
    (Test-Path -LiteralPath (Join-Path $_ 'kjv.zip'))
} | Select-Object -First 1

if (-not $packSource) {
  Write-Host "ERROR: installed bbl pack directory with webus.zip and kjv.zip not found."
  exit 1
}

$helperSource = $helperCandidates | Where-Object {
  (Test-Path -LiteralPath $_ -PathType Container) -and
    (Test-Path -LiteralPath (Join-Path $_ 'bbl-search-common.exe'))
} | Select-Object -First 1

if (-not $helperSource) {
  Write-Host "ERROR: installed bbl helper directory with bbl-search-common.exe not found."
  exit 1
}

$testHome = Join-Path ([System.IO.Path]::GetTempPath()) ("bbl-history-windows-test.{0}" -f ([System.Guid]::NewGuid().ToString('N')))
$testBblDir = Join-Path $testHome '.bbl'
$testPackDir = Join-Path $testBblDir 'packs'
$testHelperDir = Join-Path $testBblDir 'bin'
$historyPath = Join-Path $testBblDir 'history.json'

function Remove-TestHome {
  if (Test-Path -LiteralPath $testHome) {
    Remove-Item -LiteralPath $testHome -Recurse -Force
  }
}

function Normalize-Output {
  param([object[]]$Output)

  if ($null -eq $Output) {
    return ""
  }

  return (($Output | ForEach-Object { $_.ToString() }) -join "`n") -replace "(`r?`n)+$", ""
}

function Run-Bbl {
  param([string[]]$CliArgs)

  $output = & $BblPath @CliArgs 2>&1
  $status = $LASTEXITCODE
  $text = Normalize-Output $output

  if ($status -ne 0) {
    Write-Host "FAIL: bbl $($CliArgs -join ' ') exited with $status"
    Write-Host $text
    exit 1
  }

  if ($text.StartsWith('Usage:') -or $text.Contains("`nError:")) {
    Write-Host "FAIL: bbl $($CliArgs -join ' ') reported an error"
    Write-Host $text
    exit 1
  }

  return $text
}

function Assert-Contains {
  param(
    [string]$Name,
    [string]$Haystack,
    [string]$Needle
  )

  if (-not $Haystack.Contains($Needle)) {
    Write-Host "FAIL: $Name"
    Write-Host "expected to contain: $Needle"
    Write-Host "actual:"
    Write-Host $Haystack
    exit 1
  }

  Write-Host "[PASS] $Name"
}

function Assert-NotContains {
  param(
    [string]$Name,
    [string]$Haystack,
    [string]$Needle
  )

  if ($Haystack.Contains($Needle)) {
    Write-Host "FAIL: $Name"
    Write-Host "expected not to contain: $Needle"
    Write-Host "actual:"
    Write-Host $Haystack
    exit 1
  }

  Write-Host "[PASS] $Name"
}

function Assert-Equals {
  param(
    [string]$Name,
    [string]$Expected,
    [string]$Actual
  )

  if ($Actual -ne $Expected) {
    Write-Host "FAIL: $Name"
    Write-Host "expected:"
    Write-Host $Expected
    Write-Host "actual:"
    Write-Host $Actual
    exit 1
  }

  Write-Host "[PASS] $Name"
}

try {
  New-Item -ItemType Directory -Path $testPackDir -Force | Out-Null
  New-Item -ItemType Directory -Path $testHelperDir -Force | Out-Null
  Copy-Item -LiteralPath (Join-Path $packSource 'webus.zip') -Destination $testPackDir -Force
  Copy-Item -LiteralPath (Join-Path $packSource 'kjv.zip') -Destination $testPackDir -Force
  Copy-Item -LiteralPath (Join-Path $helperSource 'bbl-search-common.exe') -Destination $testHelperDir -Force

  $env:USERPROFILE = $testHome

  Write-Host ""
  Write-Host "Running bbl history E2E tests"
  Write-Host "bbl: $BblPath"
  Write-Host "home: $env:USERPROFILE"
  Write-Host "packs: $packSource"
  Write-Host "helpers: $helperSource"

  Run-Bbl @('config', 'init') | Out-Null
  Run-Bbl @('gen', '1') | Out-Null
  Run-Bbl @('search', 'Jesus', 'Christ', 'limit', '1') | Out-Null
  Run-Bbl @('config', 'searchResult', '10') | Out-Null

  if (-not (Test-Path -LiteralPath $historyPath -PathType Leaf)) {
    Write-Host "FAIL: expected history file to exist: $historyPath"
    exit 1
  }
  Write-Host "[PASS] history file exists"

  $historyAll = Run-Bbl @('history')
  Assert-Contains 'history all contains config init' $historyAll 'bbl config init'
  Assert-Contains 'history all contains read command' $historyAll 'bbl genesis 1'
  Assert-Contains 'history all contains search command' $historyAll 'bbl search Jesus Christ limit 1'
  Assert-Contains 'history all contains config command' $historyAll 'bbl config searchResult 10'

  $historyRead = Run-Bbl @('history', 'read')
  Assert-Contains 'history read includes read command' $historyRead 'bbl genesis 1'
  Assert-NotContains 'history read excludes search command' $historyRead 'bbl search'
  Assert-NotContains 'history read excludes config command' $historyRead 'bbl config'

  $historySearch = Run-Bbl @('history', 's')
  Assert-Contains 'history search includes search command' $historySearch 'bbl search Jesus Christ limit 1'
  Assert-NotContains 'history search excludes read command' $historySearch 'bbl genesis 1'

  $historyConfig = Run-Bbl @('history', 'c')
  Assert-Contains 'history config includes config command' $historyConfig 'bbl config searchResult 10'
  Assert-NotContains 'history config excludes search command' $historyConfig 'bbl search'

  Run-Bbl @('config', 'historyFormat', 'datetimeCommand') | Out-Null
  $historyDatetime = Run-Bbl @('history')
  if ($historyDatetime -notmatch '\s1\s+\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\s+bbl\s+config\s+init') {
    Write-Host "FAIL: history datetimeCommand format"
    Write-Host $historyDatetime
    exit 1
  }
  Write-Host "[PASS] history datetimeCommand format"

  Run-Bbl @('config', 'historyFormat', 'datetimeTimezoneCommand') | Out-Null
  $historyDatetimeTimezone = Run-Bbl @('history')
  if ($historyDatetimeTimezone -notmatch '\s1\s+\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\s+\S+\s+bbl\s+config\s+init') {
    Write-Host "FAIL: history datetimeTimezoneCommand format"
    Write-Host $historyDatetimeTimezone
    exit 1
  }
  Write-Host "[PASS] history datetimeTimezoneCommand format"

  Run-Bbl @('config', 'historyEnabled', 'false') | Out-Null
  $historyDisabledBaseline = Run-Bbl @('history')
  Run-Bbl @('gen', '1') | Out-Null
  $historyDisabledAfter = Run-Bbl @('history')
  Assert-Equals 'history disabled skips later command recording' $historyDisabledBaseline $historyDisabledAfter

  Run-Bbl @('config', 'historyEnabled', 'true') | Out-Null
  Run-Bbl @('gen', '1') | Out-Null
  $historyReenabled = Run-Bbl @('history')
  Assert-Contains 'history re-enabled records later command' $historyReenabled 'bbl config historyEnabled true'
  Assert-Contains 'history re-enabled records read command' $historyReenabled 'bbl genesis 1'

  # Test open-ended verse range is recorded correctly
  Run-Bbl @('john', '3:16-', 'in', 'jc') | Out-Null

  $historyOpenEnded = Run-Bbl @('history')
  Assert-Contains 'history records open-ended verse range correctly' $historyOpenEnded 'bbl john 3:16- in jc'
  Assert-NotContains 'history does not contain --1 in open-ended verse' $historyOpenEnded 'bbl john 3:16--1 in jc'

  # Test book name normalization in history
  Run-Bbl @('gn', '4') | Out-Null
  Run-Bbl @('2john', '1') | Out-Null
  Run-Bbl @('rev', '21:1-4') | Out-Null

  $historyNormalized = Run-Bbl @('history')
  Assert-Contains 'history normalizes gn to genesis' $historyNormalized 'bbl genesis 4'
  Assert-NotContains 'history does not contain raw gn' $historyNormalized 'bbl gn 4'
  Assert-Contains 'history normalizes 2john to 2 john' $historyNormalized 'bbl 2 john 1'
  Assert-NotContains 'history does not contain raw 2john' $historyNormalized 'bbl 2john 1'
  Assert-Contains 'history normalizes rev to revelation' $historyNormalized 'bbl revelation 21:1-4'
  Assert-NotContains 'history does not contain raw rev' $historyNormalized 'bbl rev 21:1-4'

  Write-Host ""
  Write-Host "Test Summary: history E2E successful"
  exit 0
} finally {
  $env:USERPROFILE = $originalUserProfile
  Remove-TestHome
}
