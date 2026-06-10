['Jesus Christ', 'Jesus', 'Christ'].each do |term|
  describe "bbl search #{term} in kjv" do
    subject(:result) { command($bbl_run.call("search #{term} in kjv")) }
    it 'succeeds' do
      expect(result.exit_status).to eq 0
    end
    it 'returns Matthew 1:1 as first hit' do
      expect(result.stdout).to include 'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.'
    end
  end
end

describe 'bbl search Jesus Christ --translation kjv' do
  subject(:result) { command($bbl_run.call('search Jesus Christ --translation kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Matthew 1:1 as first hit' do
    expect(result.stdout).to include 'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.'
  end
end

describe 'bbl search "Jesus wept" in kjv exact' do
  subject(:result) { command($bbl_run.call('search "Jesus wept" in kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns John 11:35 as first hit' do
    expect(result.stdout).to include 'John 11:35 Jesus wept.'
  end
end

describe 'bbl search Jesus wept in kjv' do
  subject(:result) { command($bbl_run.call('search Jesus wept in kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Matthew 26:75 as first hit' do
    expect(result.stdout).to include 'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.'
  end
end

describe 'bbl search Jesus Christ --book romans in kjv' do
  subject(:result) { command($bbl_run.call('search Jesus Christ --book romans in kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Romans 1:1 as first hit' do
    expect(result.stdout).to include 'Romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,'
  end
end

describe 'bbl search Jesus Christ in "johns letters" in kjv' do
  subject(:result) { command($bbl_run.call('search Jesus Christ in "johns letters" in kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns 1 John 1:3 as first hit' do
    expect(result.stdout).to include '1 John 1:3 That which we have seen and heard declare we unto you, that ye also may have fellowship with us: and truly our fellowship [is] with the Father, and with his Son Jesus Christ.'
  end
end

describe 'bbl search Jesus weep in kjv stemming' do
  subject(:result) { command($bbl_run.call('search Jesus weep in kjv')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Matthew 26:75 as first hit' do
    expect(result.stdout).to include 'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.'
  end
end
