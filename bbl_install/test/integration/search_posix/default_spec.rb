require 'base64'
require 'shellwords'

describe 'aggregated linux search tests' do
  script_path = File.join(__dir__, 'test_search.sh')
  script_base64 = Base64.strict_encode64(File.binread(script_path))

  subject(:result) do
    command(<<~CMD, timeout: 1200)
      tmp_script="$(mktemp)"
      tmp_log="$(mktemp)"

      cleanup() {
        rm -f "$tmp_script" "$tmp_log"
      }

      trap cleanup EXIT INT TERM

      printf '%s' #{Shellwords.escape(script_base64)} | {
        if base64 --help 2>&1 | grep -q -- '-d'; then
          base64 -d
        else
          base64 -D
        fi
      } > "$tmp_script"

      chmod +x "$tmp_script"

      echo "Starting aggregated linux search tests..."
      echo "bbl: #{Shellwords.escape($bbl_bin)}"
      echo "throttle: 4"
      echo ""

      "$tmp_script" -BblPath #{Shellwords.escape($bbl_bin)} -ThrottleLimit 4 > "$tmp_log" 2>&1 &
      pid="$!"

      elapsed=0

      while kill -0 "$pid" 2>/dev/null; do
        sleep 15
        elapsed=$((elapsed + 15))
        echo "search tests still running... ${elapsed}s" >&2
      done

      wait "$pid"
      status="$?"

      cat "$tmp_log"

      exit "$status"
    CMD
  end

  it 'all tests should pass' do
    puts "\n--- search test stdout ---"
    puts result.stdout

    unless result.exit_status == 0 || result.stderr.nil? || result.stderr.empty?
      puts "\n--- search test stderr ---"
      puts result.stderr
    end

    expect(result.exit_status).to eq 0
  end
end
