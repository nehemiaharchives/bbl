#!/usr/bin/env bash
# bbl shell completion E2E tests for macOS/Linux

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

find_first_file() {
  local candidate
  for candidate in "$@"; do
    if [[ -f "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

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

BashCompletionPath="$(find_first_file \
  "$HOME/.bbl/completions/bbl.bash" \
  "/usr/share/bash-completion/completions/bbl.bash" \
  "/usr/local/share/bash-completion/completions/bbl.bash" \
  "/opt/homebrew/share/bash-completion/completions/bbl.bash")" || {
  echo "ERROR: bbl bash completion file not found." >&2
  exit 1
}

ZshCompletionPath="$(find_first_file \
  "$HOME/.bbl/completions/_bbl" \
  "/usr/share/zsh/site-functions/_bbl" \
  "/usr/local/share/zsh/site-functions/_bbl" \
  "/opt/homebrew/share/zsh/site-functions/_bbl")" || {
  echo "ERROR: bbl zsh completion file not found." >&2
  exit 1
}

FishCompletionPath="$(find_first_file \
  "$HOME/.bbl/completions/bbl.fish" \
  "/usr/share/fish/vendor_completions.d/bbl.fish" \
  "/usr/local/share/fish/vendor_completions.d/bbl.fish" \
  "/opt/homebrew/share/fish/vendor_completions.d/bbl.fish")" || {
  echo "ERROR: bbl fish completion file not found." >&2
  exit 1
}

echo "uname: $(uname -a)"
echo "bash: $(bash --version | sed -n '1p')"
if command -v zsh >/dev/null 2>&1; then
  echo "zsh: $(zsh --version)"
else
  echo "zsh: SKIP zsh not installed"
fi
if command -v fish >/dev/null 2>&1; then
  echo "fish: $(fish --version)"
else
  echo "fish: SKIP fish not installed"
fi
echo "bbl: $BblPath"
echo "bash completion: $BashCompletionPath"
echo "zsh completion: $ZshCompletionPath"
echo "fish completion: $FishCompletionPath"

assert_nonempty_file() {
  local path="$1"
  if [[ ! -s "$path" ]]; then
    echo "FAIL: expected non-empty file: $path" >&2
    exit 1
  fi
  echo "[PASS] non-empty file: $path"
}

assert_output_contains_bbl() {
  local shell="$1"
  local output="$2"
  if [[ -z "$output" || "$output" != *bbl* ]]; then
    echo "FAIL: generated $shell completion output was empty or did not contain bbl" >&2
    printf '%s\n' "$output" >&2
    exit 1
  fi
  echo "[PASS] bbl generate-completion $shell"
}

assert_nonempty_file "$BashCompletionPath"
assert_nonempty_file "$ZshCompletionPath"
assert_nonempty_file "$FishCompletionPath"

assert_output_contains_bbl "bash" "$("$BblPath" generate-completion bash)"
assert_output_contains_bbl "zsh" "$("$BblPath" generate-completion zsh)"
assert_output_contains_bbl "fish" "$("$BblPath" generate-completion fish)"

assert_output_contains_bbl "bash alias" "$("$BblPath" completion bash)"
assert_output_contains_bbl "zsh alias" "$("$BblPath" completion zsh)"
assert_output_contains_bbl "fish alias" "$("$BblPath" completion fish)"

TestHome="$(mktemp -d "${TMPDIR:-/tmp}/bbl-completion-posix-test.XXXXXX")"
cleanup() {
  rm -rf "$TestHome"
}
trap cleanup EXIT INT TERM

mkdir -p "$TestHome/bin"
ln -s "$BblPath" "$TestHome/bin/bbl"

HOME="$TestHome" PATH="$TestHome/bin:$PATH" bash --noprofile --norc -c '
set -eo pipefail

completion_file="$1"
source "$completion_file"

completion_spec="$(complete -p bbl)"
completion_function=""
read -r -a completion_tokens <<< "$completion_spec"
for ((i = 0; i < ${#completion_tokens[@]}; i++)); do
  if [[ "${completion_tokens[$i]}" == "-F" && $((i + 1)) -lt ${#completion_tokens[@]} ]]; then
    completion_function="${completion_tokens[$((i + 1))]}"
    break
  fi
done

if [[ -z "$completion_function" ]]; then
  echo "FAIL: no bash completion function registered for bbl" >&2
  echo "$completion_spec" >&2
  exit 1
fi

run_completion() {
  local line="$1"
  shift
  COMP_WORDS=("$@")
  COMP_CWORD=$((${#COMP_WORDS[@]} - 1))
  COMP_LINE="$line"
  COMP_POINT=${#COMP_LINE}
  COMPREPLY=()
  "$completion_function"
  printf "%s\n" "${COMPREPLY[@]}"
}

contains_completion() {
  local output="$1"
  local expected="$2"
  printf "%s\n" "$output" | sed "s/[[:space:]]*$//" | grep -Fx -- "$expected" >/dev/null
}

root_output="$(run_completion "bbl " bbl "")"
for expected in search rand list install uninstall config history; do
  if ! contains_completion "$root_output" "$expected"; then
    echo "FAIL: root bash completions did not include $expected" >&2
    printf "%s\n" "$root_output" >&2
    exit 1
  fi
done

prefix_output="$(run_completion "bbl se" bbl se)"
if ! contains_completion "$prefix_output" "search"; then
  echo "FAIL: prefix bash completions did not include search" >&2
  printf "%s\n" "$prefix_output" >&2
  exit 1
fi

echo "[PASS] bash completion behavior"
' _ "$BashCompletionPath"

if command -v zsh >/dev/null 2>&1; then
  zsh -n "$ZshCompletionPath"
  zsh -f -c '
    set -e
    fpath=("$1" $fpath)
    autoload -Uz compinit
    compinit -D
    autoload -Uz _bbl
    whence -w _bbl >/dev/null
  ' _ "$(dirname "$ZshCompletionPath")"
  echo "[PASS] zsh completion syntax/load"
else
  echo "[SKIP] zsh completion load check: zsh not installed"
fi

if command -v fish >/dev/null 2>&1; then
  fish -n "$FishCompletionPath"
  fish -c '
    set -l dir $argv[1]
    set fish_complete_path $dir $fish_complete_path
    complete -C "bbl " | string match -q "*search*"
  ' "$(dirname "$FishCompletionPath")"
  echo "[PASS] fish completion syntax/load"
else
  echo "[SKIP] fish completion load check: fish not installed"
fi

echo "[PASS] bbl POSIX completion tests"
