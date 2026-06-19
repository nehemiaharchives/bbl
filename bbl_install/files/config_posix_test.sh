#!/usr/bin/env bash
# bbl config E2E tests for macOS/Linux

set -euo pipefail

BblPath=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -BblPath|--bbl-path)
      if [[ $# -lt 2 ]]; then
        echo "ERROR: $1 requires a value." >&2
        exit 1
      fi
      BblPath="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [-BblPath /path/to/bbl]"
      exit 0
      ;;
    *)
      if [[ -z "$BblPath" ]]; then
        BblPath="$1"
      else
        echo "ERROR: unknown argument: $1" >&2
        exit 1
      fi
      shift
      ;;
  esac
done

if [[ -z "$BblPath" ]]; then
  candidates=(
    "$HOME/.bbl/bin/bbl"
    "$HOME/.bbl/bbl"
    "/usr/local/bin/bbl"
    "/opt/homebrew/bin/bbl"
    "/usr/bin/bbl"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate" || -f "$candidate" ]]; then
      BblPath="$candidate"
      break
    fi
  done

  if [[ -z "$BblPath" ]] && command -v bbl >/dev/null 2>&1; then
    BblPath="$(command -v bbl)"
  fi

  if [[ -z "$BblPath" ]]; then
    echo "ERROR: bbl not found. Provide -BblPath or ensure it is installed." >&2
    exit 1
  fi
fi

if [[ ! -f "$BblPath" && ! -x "$BblPath" ]]; then
  echo "ERROR: bbl not found at: $BblPath" >&2
  exit 1
fi

ORIGINAL_HOME="${HOME:-}"
BblDir="$(cd "$(dirname "$BblPath")" && pwd)"
PACK_SOURCE=""
pack_candidates=(
  "$ORIGINAL_HOME/.bbl/packs"
  "/root/.bbl/packs"
  "$BblDir"
)
HELPER_SOURCE=""
helper_candidates=(
  "$ORIGINAL_HOME/.bbl/bin"
  "/root/.bbl/bin"
  "$BblDir"
)

for candidate in "${pack_candidates[@]}"; do
  if [[ -d "$candidate" && -f "$candidate/webus.zip" && -f "$candidate/kjv.zip" && -f "$candidate/jc.zip" && -f "$candidate/krv.zip" ]]; then
    PACK_SOURCE="$candidate"
    break
  fi
done

if [[ -z "$PACK_SOURCE" ]]; then
  echo "ERROR: installed bbl pack directory with webus.zip, kjv.zip, jc.zip, and krv.zip not found." >&2
  exit 1
fi

for candidate in "${helper_candidates[@]}"; do
  if [[ -d "$candidate" && -f "$candidate/bbl-search-common" ]]; then
    HELPER_SOURCE="$candidate"
    break
  fi
done

if [[ -z "$HELPER_SOURCE" ]]; then
  echo "ERROR: installed bbl helper directory with bbl-search-common not found." >&2
  exit 1
fi

TEST_HOME="$(mktemp -d "${TMPDIR:-/tmp}/bbl-config-posix-test.XXXXXX")"
cleanup() {
  rm -rf "$TEST_HOME"
}
trap cleanup EXIT INT TERM

mkdir -p "$TEST_HOME/.bbl"
ln -s "$PACK_SOURCE" "$TEST_HOME/.bbl/packs"
ln -s "$HELPER_SOURCE" "$TEST_HOME/.bbl/bin"
export HOME="$TEST_HOME"

CONFIG_PATH="$HOME/.bbl/config.json"

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
  if [[ "$output" == Usage:* || "$output" == *$'\nError:'* ]]; then
    echo "FAIL: bbl $* reported an error" >&2
    echo "$output" >&2
    exit 1
  fi
  printf '%s' "$output"
}

assert_equals() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL: $name" >&2
    echo "expected:" >&2
    printf '%s\n' "$expected" >&2
    echo "actual:" >&2
    printf '%s\n' "$actual" >&2
    exit 1
  fi
  echo "[PASS] $name"
}

assert_file_exists() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "FAIL: expected file to exist: $path" >&2
    exit 1
  fi
  echo "[PASS] config file exists"
}

assert_file_contains() {
  local path="$1"
  local text="$2"
  if ! grep -Fq "$text" "$path"; then
    echo "FAIL: expected $path to contain: $text" >&2
    echo "actual file:" >&2
    cat "$path" >&2
    exit 1
  fi
  echo "[PASS] config file contains $text"
}

nonempty_line_count() {
  awk 'NF { count++ } END { print count + 0 }'
}

first_nonempty_line() {
  awk '{ sub(/\r$/, ""); if (NF) { print; exit } }'
}

echo ""
echo "Running bbl config E2E tests"
echo "bbl: $BblPath"
echo "home: $HOME"
echo "packs: $PACK_SOURCE"
echo "helpers: $HELPER_SOURCE"

run_bbl config init >/dev/null
assert_file_exists "$CONFIG_PATH"

webus_2sam="$(run_bbl 2sam 15:30)"
assert_equals "default translation renders WEBUS verse" \
  "30 David went up by the ascent of the Mount of Olives, and wept as he went up; and he had his head covered and went barefoot. All the people who were with him each covered his head, and they went up, weeping as they went up." \
  "$webus_2sam"

set_translation_output="$(run_bbl config translation kjv)"
assert_equals "config set mode reports translation update" \
  "translation set to kjv" \
  "$set_translation_output"
assert_file_contains "$CONFIG_PATH" "kjv"

show_translation_output="$(run_bbl config translation)"
assert_equals "config show mode prints translation" \
  "kjv" \
  "$show_translation_output"

kjv_2sam="$(run_bbl 2sam 15:30)"
assert_equals "configured translation renders KJV verse" \
  "30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up." \
  "$kjv_2sam"

search_olivet="$(run_bbl search olivet)"
search_olivet_first="$(printf '%s\n' "$search_olivet" | first_nonempty_line)"
search_olivet_count="$(printf '%s\n' "$search_olivet" | nonempty_line_count)"
assert_equals "configured translation search first result" \
  "2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up." \
  "$search_olivet_first"
assert_equals "default configured search result count" "2" "$search_olivet_count"

run_bbl config searchResult 1 >/dev/null
search_olivet_one="$(run_bbl search olivet)"
search_olivet_one_count="$(printf '%s\n' "$search_olivet_one" | nonempty_line_count)"
assert_equals "configured searchResult limits result count" "1" "$search_olivet_one_count"

rand_verse="$(run_bbl rand)"
rand_verse_count="$(printf '%s\n' "$rand_verse" | nonempty_line_count)"
assert_equals "default random output is one verse" "1" "$rand_verse_count"

run_bbl config randomlyShow chapter >/dev/null
rand_chapter="$(run_bbl rand)"
rand_chapter_count="$(printf '%s\n' "$rand_chapter" | awk 'END { print NR + 0 }')"
if [[ "$rand_chapter_count" -le 2 ]]; then
  echo "FAIL: expected chapter random output to contain more than 2 lines (including blank), got $rand_chapter_count" >&2
  printf '%s\n' "$rand_chapter" >&2
  exit 1
fi
echo "[PASS] configured randomlyShow chapter returns multiple lines"

run_bbl config translation webus >/dev/null
john_no_header="$(run_bbl john 3:16)"
assert_equals "default header false omits header" \
  "16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life." \
  "$john_no_header"

run_bbl config header true >/dev/null
john_with_header="$(run_bbl john 3:16)"
assert_equals "configured header true shows header" \
  $'John 3:16\n16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.' \
  "$john_with_header"

run_bbl config header false >/dev/null
default_block_matthew="$(run_bbl matthew 28:18-20 in kjv jc krv)"
assert_equals "default compareBy block prints translation blocks" \
  $'18 And Jesus came and spake unto them, saying, All power is given unto me in heaven and in earth.\n19 Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:\n20 Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, [even] unto the end of the world. Amen.\n18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。\n19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、\n20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。\n18 예수께서 나아와 일러 가라사대 하늘과 땅의 모든 권세를 내게 주셨으니\n19 그러므로 너희는 가서 모든 족속으로 제자를 삼아 아버지와 아들과 성령의 이름으로 세례를 주고\n20 내가 너희에게 분부한 모든 것을 가르쳐 지키게 하라 볼찌어다 내가 세상 끝날까지 너희와 항상 함께 있으리라 하시니라' \
  "$default_block_matthew"

set_compare_by_output="$(run_bbl config compareBy verse)"
assert_equals "config set mode reports compareBy update" \
  "compareBy set to verse" \
  "$set_compare_by_output"
assert_file_contains "$CONFIG_PATH" "verse"

verse_compare_matthew="$(run_bbl matthew 28:18-20 in kjv jc krv)"
assert_equals "configured compareBy verse prints verse by verse" \
  $'18 And Jesus came and spake unto them, saying, All power is given unto me in heaven and in earth.\n18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。\n18 예수께서 나아와 일러 가라사대 하늘과 땅의 모든 권세를 내게 주셨으니\n19 Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:\n19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、\n19 그러므로 너희는 가서 모든 족속으로 제자를 삼아 아버지와 아들과 성령의 이름으로 세례를 주고\n20 Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, [even] unto the end of the world. Amen.\n20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。\n20 내가 너희에게 분부한 모든 것을 가르쳐 지키게 하라 볼찌어다 내가 세상 끝날까지 너희와 항상 함께 있으리라 하시니라' \
  "$verse_compare_matthew"

verse_compare_genesis="$(run_bbl genesis 1 in kjv jc krv)"
verse_compare_genesis_first_three="$(printf '%s\n' "$verse_compare_genesis" | sed -n '1,3p')"
assert_equals "configured compareBy verse interleaves whole chapters" \
  $'1 In the beginning God created the heaven and the earth.\n1 はじめに神は天と地とを創造された。\n1 태초에 하나님이 천지를 창조하시니라' \
  "$verse_compare_genesis_first_three"

# Test config key aliases
run_bbl config tr webus >/dev/null
show_tr="$(run_bbl config tr)"
assert_equals "alias tr writes and reads translation" "webus" "$show_tr"

run_bbl config sr 5 >/dev/null
show_sr="$(run_bbl config sr)"
assert_equals "alias sr writes and reads searchResult" "5" "$show_sr"

run_bbl config he false >/dev/null
show_he="$(run_bbl config he)"
assert_equals "alias he writes and reads historyEnabled" "false" "$show_he"

run_bbl config he true >/dev/null

run_bbl config hd true >/dev/null
show_hd="$(run_bbl config hd)"
assert_equals "alias hd writes and reads header" "true" "$show_hd"

run_bbl config hd false >/dev/null

run_bbl config rs chapter >/dev/null
show_rs="$(run_bbl config rs)"
assert_equals "alias rs writes and reads randomlyShow" "chapter" "$show_rs"

run_bbl config rs verse >/dev/null

run_bbl config cb verse >/dev/null
show_cb="$(run_bbl config cb)"
assert_equals "alias cb writes and reads compareBy" "verse" "$show_cb"

run_bbl config cb block >/dev/null

run_bbl config hf datetimeTimezoneCommand >/dev/null
show_hf="$(run_bbl config hf)"
assert_equals "alias hf writes and reads historyFormat" "datetimeTimezoneCommand" "$show_hf"

run_bbl config hf command >/dev/null

echo ""
echo "Test Summary: config E2E successful"
