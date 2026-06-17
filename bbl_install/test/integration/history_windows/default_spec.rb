describe 'aggregated history tests' do
  script_path = File.join(__dir__, 'test_history.ps1')

  subject(:result) do
    command("pwsh -NoProfile -ExecutionPolicy Bypass -File \"#{script_path}\" -BblPath \"#{$bbl_bin}\"")
  end

  it 'all tests should pass' do
    puts "\n--- history test stdout ---"
    puts result.stdout

    unless result.stderr.nil? || result.stderr.empty?
      puts "\n--- history test stderr ---"
      puts result.stderr
    end

    expect(result.exit_status).to eq 0
  end
end
describe 'aggregated history tests' do
  script_path = File.join(__dir__, 'test_history.ps1')

  it 'passes the PowerShell history verifier' do
    result = command("pwsh -NoProfile -ExecutionPolicy Bypass -File \"#{script_path}\" -BblPath \"#{$bbl_bin}\"")
    expect(result.exit_status).to eq(0), "stdout:\n#{result.stdout}\nstderr:\n#{result.stderr}"
  end
end
