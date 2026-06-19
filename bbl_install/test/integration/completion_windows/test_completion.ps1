# bbl PowerShell completion E2E tests for Windows

param(
  [string]$BblPath = ""
)

if (-not $BblPath) {
  $candidates = @(
    "$env:LOCALAPPDATA\Programs\bbl\bbl.exe",
    "$env:USERPROFILE\.bbl\bin\bbl.exe",
    "$env:USERPROFILE\.bbl\bbl.exe"
  )

  $BblPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1

  if (-not $BblPath) {
    Write-Host "ERROR: bbl.exe not found. Provide -BblPath or ensure it is installed."
    exit 1
  }
}

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$passed = 0
$failed = 0

function Assert-True {
  param([string]$Name, [scriptblock]$Condition)
  $result = & $Condition
  if ($result) {
    Write-Host "[PASS] $Name"
    $script:passed++
  } else {
    Write-Host "FAIL: $Name"
    $script:failed++
  }
}

# === Verify generate-completion powershell output ===
$output = & $BblPath generate-completion powershell
Assert-True "generate-completion powershell output is non-empty" { ($output -and $output.Length -gt 0) }
Assert-True "generate-completion powershell output contains bbl" { $output -match 'bbl' }
Assert-True "generate-completion powershell contains Register-ArgumentCompleter" {
  $output -match 'Register-ArgumentCompleter'
}

$outputAlias = & $BblPath completion powershell
Assert-True "completion powershell alias output is non-empty" { ($outputAlias -and $outputAlias.Length -gt 0) }
Assert-True "completion powershell alias output contains bbl" { $outputAlias -match 'bbl' }
Assert-True "completion powershell alias matches generate-completion output" {
  ($output -join "`n") -ceq ($outputAlias -join "`n")
}

# === Dot-source the completion script and test via TabExpansion2 ===
$tempDir = Join-Path $env:TEMP "bbl-completion-test"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

$scriptPath = Join-Path $tempDir "_bbl.ps1"
& $BblPath generate-completion powershell | Out-File -FilePath $scriptPath -Encoding utf8

. $scriptPath

# Helper: get completions for a given command line prefix
function Get-Completions {
  param([string]$Line)
  $cursor = $Line.Length
  $completion = TabExpansion2 $Line $cursor $null
  return $completion.CompletionMatches.CompletionText
}

# Helper: assert a completion list contains expected values
function Assert-Completions-Contain {
  param([string]$Name, [string[]]$Line, [string[]]$Expected)
  $completions = Get-Completions -Line $Line
  $missing = $Expected | Where-Object { $_ -notin $completions }
  if ($missing) {
    Write-Host "FAIL: $Name - missing completions: $($missing -join ', ')"
    Write-Host "  actual completions: $($completions -join ', ')"
    $script:failed++
  } else {
    Write-Host "[PASS] $Name"
    $script:passed++
  }
}

# Root-level completions: subcommands and book names
Assert-Completions-Contain "root completions include subcommands" 'bbl ' @(
  'search', 'rand', 'list', 'install', 'uninstall', 'config', 'history', 'help', 'generate-completion'
)

Assert-Completions-Contain "root completions include book names" 'bbl ' @(
  'genesis', '1john'
)

# Prefix completion
$seCompletions = Get-Completions -Line 'bbl se'
Assert-True "prefix 'se' completes to 'search'" { 'search' -in $seCompletions }

# Subcommand completions: list
Assert-Completions-Contain "bbl list completions" 'bbl list ' @(
  'translations', 'books', 'categories'
)

Assert-True "bbl list prefix 'tr' completes to 'translations'" {
  $comps = Get-Completions -Line 'bbl list tr'
  'translations' -in $comps
}

# Config key completions
Assert-Completions-Contain "bbl config key completions" 'bbl config ' @(
  'translation', 'searchResult', 'randomlyShow', 'header', 'compareBy', 'historyEnabled', 'historyFormat'
)

Assert-True "bbl config prefix 'tr' completes to 'translation'" {
  $comps = Get-Completions -Line 'bbl config tr'
  'translation' -in $comps
}

# Config value completions
Assert-Completions-Contain "bbl config randomlyShow values" 'bbl config randomlyShow ' @(
  'verse', 'chapter'
)

Assert-Completions-Contain "bbl config header values" 'bbl config header ' @(
  'true', 'false'
)

Assert-Completions-Contain "bbl config compareBy values" 'bbl config compareBy ' @(
  'block', 'verse'
)

Assert-True "bbl config historyFormat completes to datetimeCommand" {
  $comps = Get-Completions -Line 'bbl config historyFormat '
  'datetimeCommand' -in $comps
}

Assert-True "bbl config historyFormat prefix 'date' completes to datetimeCommand" {
  $comps = Get-Completions -Line 'bbl config historyFormat date'
  'datetimeCommand' -in $comps
}

# History filter completions
Assert-Completions-Contain "bbl history filter completions" 'bbl history ' @(
  'read', 'search', 'config'
)

Assert-True "bbl history prefix 'se' completes to 'search'" {
  $comps = Get-Completions -Line 'bbl history se'
  'search' -in $comps
}

# Help subcommand completions
Assert-Completions-Contain "bbl help completions" 'bbl help ' @(
  'search', 'rand', 'list', 'install', 'uninstall', 'config', 'history', 'help', 'generate-completion'
)

Assert-True "bbl help prefix 'se' completes to 'search'" {
  $comps = Get-Completions -Line 'bbl help se'
  'search' -in $comps
}

Write-Host "---"
Write-Host "Results: $passed passed, $failed failed"

if ($failed -gt 0) {
  exit 1
}

Write-Host "[PASS] bbl PowerShell completion tests"
