local_app_data = os_env('LOCALAPPDATA').content
install_root = "#{local_app_data}\\.bbl"
pack_dir = "#{install_root}\\packs"
bin_dir = "#{install_root}\\bin"
BBL_BIN = "#{bin_dir}\\bbl.exe"
RSpec.shared_context 'search helpers' do
  def bbl_command(args)
    escaped_bbl = BBL_BIN.gsub("'", "''")
    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"& '#{escaped_bbl}' #{args}\""
  end

  def search_stdout(command_text)
    command(command_text).stdout.force_encoding('UTF-8')
  end

  def search_results(command_text)
    search_stdout(command_text)
      .split(/\r?\n\r?\n/)
      .map(&:strip)
      .reject(&:empty?)
  end
end

def bbl_command(args)
  escaped_bbl = BBL_BIN.gsub("'", "''")
  "powershell -NoProfile -ExecutionPolicy Bypass -Command \"& '#{escaped_bbl}' #{args}\""
end

describe file(BBL_BIN) do
  it { should exist }
  it { should be_file }
end

describe command(bbl_command('-v')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl version 4\.0/) }
end

describe file(pack_dir) do
  it { should exist }
  it { should be_directory }
end

describe file("#{pack_dir}\\webus.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\kjv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\jc.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\krv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\cunp.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\ubg.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}\\kttv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{bin_dir}\\bbl-search-common.exe") do
  it { should exist }
  it { should be_file }
end

describe file("#{bin_dir}\\bbl-search-extra.exe") do
  it { should exist }
  it { should be_file }
end

describe file("#{bin_dir}\\bbl-search-kuromoji.exe") do
  it { should exist }
  it { should be_file }
end

describe file("#{bin_dir}\\bbl-search-morfologik.exe") do
  it { should exist }
  it { should be_file }
end

describe file("#{bin_dir}\\bbl-search-nori.exe") do
  it { should exist }
  it { should be_file }
end

describe file("#{bin_dir}\\bbl-search-smartcn.exe") do
  it { should exist }
  it { should be_file }
end

describe command(bbl_command('search Jesus Christ')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
end

describe 'bbl search Jesus Christ exact output' do
  include_context 'search helpers'
  subject(:results) { search_results(bbl_command('search Jesus Christ')) }

  it 'starts with the expected webus verse text' do
    expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe command(bbl_command('search Jesus Christ in kjv')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/The book of the generation of Jesus Christ/) }
end

describe 'bbl search Jesus Christ in kjv exact output' do
  include_context 'search helpers'
  subject(:results) { search_results(bbl_command('search Jesus Christ in kjv')) }

  it 'starts with the expected kjv verse text' do
    expect(results.first).to eq('Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.')
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe command(bbl_command('search Jesus Christ in romans')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Paul, a servant of Jesus Christ/) }
end

describe 'bbl search Jesus Christ in romans exact output' do
  include_context 'search helpers'
  subject(:results) { search_results(bbl_command('search Jesus Christ in romans')) }

  it 'starts with the expected romans webus verse text' do
    expect(results.first).to eq('Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,')
  end

  it 'returns multiple Romans hits' do
    expect(results.length).to be > 1
  end
end

describe command(bbl_command('search Jesus Christ in romans 5-12')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Being therefore justified by faith/) }
end

describe 'bbl search Jesus Christ in romans 5-12 exact output' do
  include_context 'search helpers'
  subject(:results) { search_results(bbl_command('search Jesus Christ in romans 5-12')) }

  it 'starts with the expected romans chapter-range webus verse text' do
    expect(results.first).to eq('Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;')
  end

  it 'returns multiple hits in the requested chapter range' do
    expect(results.length).to be > 1
  end
end

describe command(bbl_command('search Jesus Christ in romans 5-12 in kjv')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Therefore being justified by faith/) }
end

describe 'bbl search Jesus Christ in romans 5-12 in kjv exact output' do
  include_context 'search helpers'
  subject(:results) { search_results(bbl_command('search Jesus Christ in romans 5-12 in kjv')) }

  it 'starts with the expected romans chapter-range kjv verse text' do
    expect(results.first).to eq('Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:')
  end

  it 'returns multiple hits in the requested kjv chapter range' do
    expect(results.length).to be > 1
  end
end

describe 'bbl search Japanese term in jc exact output' do
  include_context 'search helpers'
  let(:query) { "\u{30A4}\u{30A8}\u{30B9} \u{30AD}\u{30EA}\u{30B9}\u{30C8}" }
  let(:command_text) { bbl_command("search #{query} in jc") }
  let(:result) { command(command_text) }
  subject(:results) { search_results(command_text) }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end
end

describe 'bbl search Korean term in krv exact output' do
  include_context 'search helpers'
  let(:query) { "\u{C608}\u{C218} \u{ADF8}\u{B9AC}\u{C2A4}\u{B3C4}" }
  let(:command_text) { bbl_command("search #{query} in krv") }
  let(:result) { command(command_text) }
  subject(:results) { search_results(command_text) }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end
end


describe 'bbl search Chinese term in cunp exact output' do
  include_context 'search helpers'
  let(:query) { "\u{8036}\u{7A23}\u{57FA}\u{7763}" }
  let(:command_text) { bbl_command("search #{query} in cunp") }
  let(:result) { command(command_text) }
  subject(:results) { search_results(command_text) }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end
end

describe 'bbl search Jezusa Chrystusa in ubg exact output' do
  include_context 'search helpers'
  let(:command_text) { bbl_command('search Jezusa Chrystusa in ubg') }
  let(:result) { command(command_text) }
  subject(:results) { search_results(command_text) }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end

  it 'includes the expected morfologik phrase' do
    expect(search_stdout(command_text)).to include('Księga rodu Jezusa Chrystusa')
  end

  it 'starts with the expected morfologik verse text' do
    expect(results.first).to eq('Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.')
  end
end

describe 'bbl search Vietnamese term in kttv exact output' do
  include_context 'search helpers'
  let(:query) { "J\u{00EA}sus Christ" }
  let(:command_text) { bbl_command("search #{query} in kttv") }
  let(:result) { command(command_text) }
  subject(:results) { search_results(command_text) }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end
end

