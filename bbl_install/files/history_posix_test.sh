#!/usr/bin/env bash
set -euo pipefail

BblPath="${BblPath:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -BblPath|--bbl-path)
      BblPath="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

find_bbl() {
  local candidate
  for candidate in "$HOME/.bbl/bin/bbl" "$HOME/.bbl/bbl" "/usr/local/bin/bbl" "/opt/homebrew/bin/bbl" "/usr/bin/bbl"; do
    if [[ -x "$candidate" ]]; then
      BblPath="$candidate"
      return
    fi
  done
  if [[ -z "$BblPath" ]] && command -v bbl >/dev/null 2>&1; then
    BblPath="$(command -v bbl)"
  fi
}

find_bbl
if [[ -z "$BblPath" || ! -x "$BblPath" ]]; then
  echo "ERROR: bbl not found. Provide -BblPath or ensure it is installed." >&2
  exit 1
fi

ORIGINAL_HOME="${HOME:-/root}"
BblDir="$(cd "$(dirname "$BblPath")" && pwd)"

find_pack_source() {
  local candidate
  for candidate in "$ORIGINAL_HOME/.bbl/packs" "/root/.bbl/packs" "$BblDir/packs"; do
    if [[ -f "$candidate/webus.zip" && -f "$candidate/kjv.zip" && -f "$candidate/jc.zip" ]]; then
      printf '%s\n' "$candidate"
      return
    fi
  done
  return 1
}

find_helper_source() {
  local candidate
  for candidate in "$ORIGINAL_HOME/.bbl/bin" "/root/.bbl/bin" "$BblDir"; do
    if [[ -f "$candidate/bbl-search-common" ]]; then
      printf '%s\n' "$candidate"
      return
    fi
  done
  return 1
}

PACK_SOURCE="$(find_pack_source)" || {
  echo "ERROR: installed bbl pack directory with webus.zip, kjv.zip, and jc.zip not found." >&2
  exit 1
}
HELPER_SOURCE="$(find_helper_source)" || {
  echo "ERROR: installed bbl helper directory with bbl-search-common not found." >&2
  exit 1
}

TEST_HOME="$(mktemp -d "${TMPDIR:-/tmp}/bbl-history-posix-test.XXXXXX")"
cleanup() {
  rm -rf "$TEST_HOME"
}
trap cleanup EXIT

mkdir -p "$TEST_HOME/.bbl/packs" "$TEST_HOME/.bbl/bin"
cp "$PACK_SOURCE/webus.zip" "$TEST_HOME/.bbl/packs/"
cp "$PACK_SOURCE/kjv.zip" "$TEST_HOME/.bbl/packs/"
cp "$PACK_SOURCE/jc.zip" "$TEST_HOME/.bbl/packs/"
cp "$HELPER_SOURCE/bbl-search-common" "$TEST_HOME/.bbl/bin/"
chmod +x "$TEST_HOME/.bbl/bin/bbl-search-common" || true
export HOME="$TEST_HOME"

HISTORY_PATH="$HOME/.bbl/packs/history.json"

run_bbl() {
  local output status
  set +e
  output="$("$BblPath" "$@" 2>&1)"
  status=$?
  set -e
  if [[ $status -ne 0 ]]; then
    echo "FAIL: bbl $* exited with $status" >&2
    echo "$output" >&2
    exit 1
  fi
  printf '%s' "$output"
}

assert_contains() {
  local name="$1"
  local haystack="$2"
  local needle="$3"
  if [[ "$haystack" != *"$needle"* ]]; then
    echo "FAIL: $name" >&2
    echo "Expected to contain: $needle" >&2
    echo "Actual:" >&2
    echo "$haystack" >&2
    exit 1
  fi
}

assert_not_contains() {
  local name="$1"
  local haystack="$2"
  local needle="$3"
  if [[ "$haystack" == *"$needle"* ]]; then
    echo "FAIL: $name" >&2
    echo "Did not expect to contain: $needle" >&2
    echo "Actual:" >&2
    echo "$haystack" >&2
    exit 1
  fi
}

assert_equals() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$expected" != "$actual" ]]; then
    echo "FAIL: $name" >&2
    echo "expected:" >&2
    printf '%s\n' "$expected" >&2
    echo "actual:" >&2
    printf '%s\n' "$actual" >&2
    exit 1
  fi
}

echo "Running bbl history E2E tests"
echo "bbl: $BblPath"
echo "home: $HOME"

run_bbl config init >/dev/null
run_bbl gen 1 >/dev/null
run_bbl search Jesus Christ limit 1 >/dev/null
run_bbl config searchResult 10 >/dev/null

if [[ ! -f "$HISTORY_PATH" ]]; then
  echo "FAIL: expected history file to exist: $HISTORY_PATH" >&2
  exit 1
fi

history_all="$(run_bbl history)"
assert_contains "history all contains config init" "$history_all" "bbl config init"
assert_contains "history all contains read command" "$history_all" "bbl genesis 1"
assert_contains "history all contains search command" "$history_all" "bbl search Jesus Christ limit 1"
assert_contains "history all contains config command" "$history_all" "bbl config searchResult 10"

history_read="$(run_bbl history read)"
assert_contains "history read includes read command" "$history_read" "bbl genesis 1"
assert_not_contains "history read excludes search command" "$history_read" "bbl search"
assert_not_contains "history read excludes config command" "$history_read" "bbl config"

history_search="$(run_bbl history s)"
assert_contains "history search includes search command" "$history_search" "bbl search Jesus Christ limit 1"
assert_not_contains "history search excludes read command" "$history_search" "bbl genesis 1"

history_config="$(run_bbl history c)"
assert_contains "history config includes config command" "$history_config" "bbl config searchResult 10"
assert_not_contains "history config excludes search command" "$history_config" "bbl search"

# Test open-ended verse range is recorded correctly
run_bbl john 3:16- in jc >/dev/null

history_open_ended="$(run_bbl history)"
assert_contains "history records open-ended verse range correctly" "$history_open_ended" "bbl john 3:16- in jc"
assert_not_contains "history does not contain --1 in open-ended verse" "$history_open_ended" "bbl john 3:16--1 in jc"

# Test book name normalization in history
run_bbl gn 4 >/dev/null
run_bbl 2john 1 >/dev/null
run_bbl rev 21:1-4 >/dev/null

history_normalized="$(run_bbl history)"
assert_contains "history normalizes gn to genesis" "$history_normalized" "bbl genesis 4"
assert_not_contains "history does not contain raw gn" "$history_normalized" "bbl gn 4"
assert_contains "history normalizes 2john to 2 john" "$history_normalized" "bbl 2 john 1"
assert_not_contains "history does not contain raw 2john" "$history_normalized" "bbl 2john 1"
assert_contains "history normalizes rev to revelation" "$history_normalized" "bbl revelation 21:1-4"
assert_not_contains "history does not contain raw rev" "$history_normalized" "bbl rev 21:1-4"

run_bbl config historyFormat datetimeCommand >/dev/null
history_datetime="$(run_bbl history)"
if [[ ! "$history_datetime" =~ [[:space:]]1[[:space:]]+[0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}[[:space:]]bbl[[:space:]]config[[:space:]]init ]]; then
  echo "FAIL: history datetimeCommand format" >&2
  printf '%s\n' "$history_datetime" >&2
  exit 1
fi

run_bbl config historyFormat datetimeTimezoneCommand >/dev/null
history_datetime_timezone="$(run_bbl history)"
if [[ ! "$history_datetime_timezone" =~ [[:space:]]1[[:space:]]+[0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}[[:space:]]+[^[:space:]]+[[:space:]]bbl[[:space:]]config[[:space:]]init ]]; then
  echo "FAIL: history datetimeTimezoneCommand format" >&2
  printf '%s\n' "$history_datetime_timezone" >&2
  exit 1
fi

run_bbl config historyEnabled false >/dev/null
history_disabled_baseline="$(run_bbl history)"
run_bbl gen 1 >/dev/null
history_disabled_after="$(run_bbl history)"
assert_equals "history disabled skips later command recording" "$history_disabled_baseline" "$history_disabled_after"

run_bbl config historyEnabled true >/dev/null
run_bbl gen 1 >/dev/null
history_reenabled="$(run_bbl history)"
assert_contains "history re-enabled records later command" "$history_reenabled" "bbl config historyEnabled true"
assert_contains "history re-enabled records read command" "$history_reenabled" "bbl genesis 1"

echo "Test Summary: history E2E successful"
