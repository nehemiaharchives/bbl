describe 'aggregated completion tests' do
  script_path = File.join(__dir__, 'test_completion.ps1')

  subject(:result) do
    command("pwsh -NoProfile -ExecutionPolicy Bypass -File \"#{script_path}\" -BblPath \"#{$bbl_bin}\"")
  end

  it 'all tests should pass' do
    puts "\n--- completion test stdout ---"
    puts result.stdout

    unless result.stderr.nil? || result.stderr.empty?
      puts "\n--- completion test stderr ---"
      puts result.stderr
    end

    expect(result.exit_status).to eq 0
  end
end
