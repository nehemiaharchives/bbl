# bbl config E2E tests for Windows

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

$testHome = Join-Path ([System.IO.Path]::GetTempPath()) ("bbl-config-windows-test.{0}" -f ([System.Guid]::NewGuid().ToString('N')))
$testBblDir = Join-Path $testHome '.bbl'
$testPackDir = Join-Path $testBblDir 'packs'
$testHelperDir = Join-Path $testBblDir 'bin'
$configPath = Join-Path $testBblDir 'config.json'

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

function Assert-FileExists {
  param([string]$Path)

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    Write-Host "FAIL: expected file to exist: $Path"
    exit 1
  }

  Write-Host "[PASS] config file exists"
}

function Assert-FileContains {
  param(
    [string]$Path,
    [string]$Text
  )

  $content = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
  if (-not $content.Contains($Text)) {
    Write-Host "FAIL: expected $Path to contain: $Text"
    Write-Host "actual file:"
    Write-Host $content
    exit 1
  }

  Write-Host "[PASS] config file contains $Text"
}

function Get-NonEmptyLineCount {
  param([string]$Text)

  return @(($Text -split "`r?`n") | Where-Object { $_.Trim() -ne "" }).Count
}

function Get-FirstNonEmptyLine {
  param([string]$Text)

  return ($Text -split "`r?`n" | Where-Object { $_.Trim() -ne "" } | Select-Object -First 1)
}

try {
  New-Item -ItemType Directory -Path $testPackDir -Force | Out-Null
  New-Item -ItemType Directory -Path $testHelperDir -Force | Out-Null
  Copy-Item -LiteralPath (Join-Path $packSource 'webus.zip') -Destination $testPackDir -Force
  Copy-Item -LiteralPath (Join-Path $packSource 'kjv.zip') -Destination $testPackDir -Force
  Copy-Item -LiteralPath (Join-Path $helperSource 'bbl-search-common.exe') -Destination $testHelperDir -Force

  $env:USERPROFILE = $testHome

  Write-Host ""
  Write-Host "Running bbl config E2E tests"
  Write-Host "bbl: $BblPath"
  Write-Host "home: $env:USERPROFILE"
  Write-Host "packs: $packSource"
  Write-Host "helpers: $helperSource"

  Run-Bbl @('config', 'init') | Out-Null
  Assert-FileExists $configPath

  $webus2Sam = Run-Bbl @('2sam', '15:30')
  Assert-Equals `
    'default translation renders WEBUS verse' `
    '30 David went up by the ascent of the Mount of Olives, and wept as he went up; and he had his head covered and went barefoot. All the people who were with him each covered his head, and they went up, weeping as they went up.' `
    $webus2Sam

  $setTranslationOutput = Run-Bbl @('config', 'translation', 'kjv')
  Assert-Equals `
    'config set mode reports translation update' `
    'translation set to kjv' `
    $setTranslationOutput
  Assert-FileContains $configPath 'kjv'

  $showTranslationOutput = Run-Bbl @('config', 'translation')
  Assert-Equals `
    'config show mode prints translation' `
    'kjv' `
    $showTranslationOutput

  $kjv2Sam = Run-Bbl @('2sam', '15:30')
  Assert-Equals `
    'configured translation renders KJV verse' `
    '30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.' `
    $kjv2Sam

  $searchOlivet = Run-Bbl @('search', 'olivet')
  $searchOlivetFirst = Get-FirstNonEmptyLine $searchOlivet
  $searchOlivetCount = Get-NonEmptyLineCount $searchOlivet
  Assert-Equals `
    'configured translation search first result' `
    '2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.' `
    $searchOlivetFirst
  Assert-Equals 'default configured search result count' '2' ([string]$searchOlivetCount)

  Run-Bbl @('config', 'searchResult', '1') | Out-Null
  $searchOlivetOne = Run-Bbl @('search', 'olivet')
  $searchOlivetOneCount = Get-NonEmptyLineCount $searchOlivetOne
  Assert-Equals 'configured searchResult limits result count' '1' ([string]$searchOlivetOneCount)

  $randVerse = Run-Bbl @('rand')
  $randVerseCount = Get-NonEmptyLineCount $randVerse
  Assert-Equals 'default random output is one verse' '1' ([string]$randVerseCount)

  Run-Bbl @('config', 'randomlyShow', 'chapter') | Out-Null
  $randChapter = Run-Bbl @('rand')
  $randChapterCount = Get-NonEmptyLineCount $randChapter
  if ($randChapterCount -le 2) {
    Write-Host "FAIL: expected chapter random output to contain more than 2 non-empty lines, got $randChapterCount"
    Write-Host $randChapter
    exit 1
  }
  Write-Host "[PASS] configured randomlyShow chapter returns multiple lines"

  Run-Bbl @('config', 'translation', 'webus') | Out-Null
  $johnNoHeader = Run-Bbl @('john', '3:16')
  Assert-Equals `
    'default header false omits header' `
    '16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.' `
    $johnNoHeader

  Run-Bbl @('config', 'header', 'true') | Out-Null
  $johnWithHeader = Run-Bbl @('john', '3:16')
  Assert-Equals `
    'configured header true shows header' `
    "John 3:16`n16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life." `
    $johnWithHeader

  Write-Host ""
  Write-Host "Test Summary: 13 successful, 0 failures"
  exit 0
} finally {
  $env:USERPROFILE = $originalUserProfile
  Remove-TestHome
}
