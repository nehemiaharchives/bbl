describe 'bbl help' do
  subject(:cmd) { command($bbl_run.call('help')) }

  it 'prints usage help and exits with status 0' do
    expect(cmd.exit_status).to eq 0
    expect($bbl_normalized_stdout.call(cmd)).to include('Read, search Holy Bible')
  end
end

describe 'bbl help search' do
  subject(:cmd) { command($bbl_run.call('help search')) }

  it 'prints search help and exits with status 0' do
    expect(cmd.exit_status).to eq 0
    expect($bbl_normalized_stdout.call(cmd)).to include('Search Bible text')
  end
end

describe 'bbl help unknowncommand' do
  subject(:cmd) { command($bbl_run.call('help unknowncommand')) }

  it 'prints error message' do
    expect($bbl_normalized_stdout.call(cmd)).to include('Unknown command')
  end
end
