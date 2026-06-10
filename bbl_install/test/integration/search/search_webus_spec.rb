# bbl expects WEBUS as the default translation when none is specified
['Jesus Christ', 'Jesus', 'Christ'].each do |term|
  describe "bbl search #{term}" do
    subject(:result) { command($bbl_run.call("search #{term}")) }
    it 'succeeds' do
      expect(result.exit_status).to eq 0
    end
    it 'returns Matthew 1:1 as first hit' do
      expect(result.stdout.force_encoding('UTF-8')).to include 'Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.'
    end
  end
end

describe 'bbl search "Jesus wept" exact' do
  subject(:result) { command($bbl_run.call('search "Jesus wept"')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns John 11:35 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'John 11:35 Jesus wept.'
  end
end

describe 'bbl search Jesus wept unquoted' do
  subject(:result) { command($bbl_run.call('search Jesus wept')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Matthew 26:75 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Matthew 26:75 Peter remembered the word which Jesus had said to him'
  end
  it 'contains wept' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'wept bitterly'
  end
end

describe 'bbl search Jesus Christ --book romans' do
  subject(:result) { command($bbl_run.call('search Jesus Christ --book romans')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Romans 1:1 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,'
  end
end

describe 'bbl search Jesus Christ in "johns letters"' do
  subject(:result) { command($bbl_run.call('search Jesus Christ in "johns letters"')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns 1 John 1:3 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include '1 John 1:3 that which we have seen and heard we declare to you, that you also may have fellowship with us. Yes, and our fellowship is with the Father and with his Son, Jesus Christ.'
  end
end

describe 'bbl search Jesus weep stemming' do
  subject(:result) { command($bbl_run.call('search Jesus weep')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Matthew 26:75 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Matthew 26:75 Peter remembered the word which Jesus had said to him'
  end
  it 'contains wept' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'wept bitterly'
  end
end
