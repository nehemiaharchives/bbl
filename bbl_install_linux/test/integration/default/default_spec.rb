home_dir = '/root'
install_root = "#{home_dir}/.bbl"
pack_dir = "#{install_root}/packs"
bin_dir = "#{install_root}/bin"
install_source_dir = '/tmp/bbl-install-downloads'
install_env = "BBL_PACK_BASE_URL=file://#{install_source_dir} BBL_SEARCH_BINARY_BASE_URL=file://#{install_source_dir}"

RSpec.shared_context 'search helpers' do
  def search_stdout(command_text)
    command(command_text).stdout.force_encoding('UTF-8')
  end

  def search_results(command_text)
    search_stdout(command_text)
      .split("\n\n")
      .map(&:strip)
      .reject(&:empty?)
  end
end

describe file('/usr/local/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe command 'bbl -v' do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl version 4\.0/) }
end

describe file(pack_dir) do
  it { should exist }
  it { should be_directory }
end

describe file("#{pack_dir}/webus.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}/kjv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}/krv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}/cunp.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}/ubg.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{pack_dir}/kttv.zip") do
  it { should exist }
  it { should be_file }
  its('size') { should be > 0 }
end

describe file("#{bin_dir}/bbl-search-common") do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe file("#{bin_dir}/bbl-search-extra") do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe file("#{bin_dir}/bbl-search-morfologik") do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe file("#{bin_dir}/bbl-search-nori") do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe file("#{bin_dir}/bbl-search-smartcn") do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe command('bbl search Jesus Christ') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
end

describe 'bbl search Jesus Christ exact output' do
  include_context 'search helpers'
  subject(:results) { search_results('bbl search Jesus Christ') }

  it 'starts with the expected webus verse text' do
    expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe command('bbl search Jesus Christ in kjv') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/The book of the generation of Jesus Christ/) }
end

describe 'bbl search Jesus Christ in kjv exact output' do
  include_context 'search helpers'
  subject(:results) { search_results('bbl search Jesus Christ in kjv') }

  it 'starts with the expected kjv verse text' do
    expect(results.first).to eq('Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.')
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe command('bbl search Jesus Christ in romans') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Paul, a servant of Jesus Christ/) }
end

describe 'bbl search Jesus Christ in romans exact output' do
  include_context 'search helpers'
  subject(:results) { search_results('bbl search Jesus Christ in romans') }

  it 'starts with the expected romans webus verse text' do
    expect(results.first).to eq('Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,')
  end

  it 'returns multiple Romans hits' do
    expect(results.length).to be > 1
  end
end

describe command('bbl search Jesus Christ in romans 5-12') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Being therefore justified by faith/) }
end

describe 'bbl search Jesus Christ in romans 5-12 exact output' do
  include_context 'search helpers'
  subject(:results) { search_results('bbl search Jesus Christ in romans 5-12') }

  it 'starts with the expected romans chapter-range webus verse text' do
    expect(results.first).to eq('Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;')
  end

  it 'returns multiple hits in the requested chapter range' do
    expect(results.length).to be > 1
  end
end

describe command('bbl search Jesus Christ in romans 5-12 in kjv') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/Therefore being justified by faith/) }
end

describe 'bbl search Jesus Christ in romans 5-12 in kjv exact output' do
  include_context 'search helpers'
  subject(:results) { search_results('bbl search Jesus Christ in romans 5-12 in kjv') }

  it 'starts with the expected romans chapter-range kjv verse text' do
    expect(results.first).to eq('Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:')
  end

  it 'returns multiple hits in the requested kjv chapter range' do
    expect(results.length).to be > 1
  end
end

describe 'bbl install jc deferred dependencies' do
  before(:all) do
    @pack_existed_before = command("test -e #{pack_dir}/jc.zip").exit_status == 0
    @helper_existed_before = command("test -e #{bin_dir}/bbl-search-kuromoji").exit_status == 0
    @install_result = command("#{install_env} bbl install jc")
    @install_exit_status = @install_result.exit_status
    @pack_exists_after = command("test -f #{pack_dir}/jc.zip").exit_status == 0
    @pack_size_after = command("stat -c %s #{pack_dir}/jc.zip").stdout.to_i
    @helper_exists_after = command("test -f #{bin_dir}/bbl-search-kuromoji").exit_status == 0
    @helper_executable_after = command("test -x #{bin_dir}/bbl-search-kuromoji").exit_status == 0
  end

  it 'does not have jc installed before install' do
    expect(@pack_existed_before).to eq(false)
  end

  it 'does not have the kuromoji search helper installed before install' do
    expect(@helper_existed_before).to eq(false)
  end

  it 'installs jc successfully' do
    expect(@install_exit_status).to eq(0)
  end

  it 'installs jc.zip' do
    expect(@pack_exists_after).to eq(true)
    expect(@pack_size_after).to be > 0
  end

  it 'installs the kuromoji search helper' do
    expect(@helper_exists_after).to eq(true)
    expect(@helper_executable_after).to eq(true)
  end
end

describe 'bbl search イエス キリスト in jc exact output' do
  include_context 'search helpers'
  let(:command_text) { 'bbl search イエス キリスト in jc' }
  let(:result) { command(command_text) }
  subject(:results) { search_results('bbl search イエス キリスト in jc') }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end

  it 'includes the expected kuromoji phrase' do
    expect(search_stdout(command_text)).to include('イエス・キリストの系図。')
  end

  it 'starts with the expected kuromoji verse text' do
    expect(results.first).to eq('マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。')
  end
end

describe 'bbl search 예수 그리스도 in krv exact output' do
  include_context 'search helpers'
  let(:command_text) { 'bbl search 예수 그리스도 in krv' }
  let(:result) { command(command_text) }
  subject(:results) { search_results('bbl search 예수 그리스도 in krv') }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end

  it 'includes the expected nori phrase' do
    expect(search_stdout(command_text)).to include('예수 그리스도의 세계라')
  end

  it 'starts with the expected nori verse text' do
    expect(results.first).to eq('마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라')
  end
end

describe 'bbl search 耶稣基督 in cunp exact output' do
  include_context 'search helpers'
  let(:command_text) { 'bbl search 耶稣基督 in cunp' }
  let(:result) { command(command_text) }
  subject(:results) { search_results('bbl search 耶稣基督 in cunp') }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end

  it 'includes the expected smartcn phrase' do
    expect(search_stdout(command_text)).to include('耶稣基督的家谱：')
  end

  it 'starts with the expected smartcn verse text' do
    expect(results.first).to eq('马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：')
  end
end

describe 'bbl search Jezusa Chrystusa in ubg exact output' do
  include_context 'search helpers'
  let(:command_text) { 'bbl search Jezusa Chrystusa in ubg' }
  let(:result) { command(command_text) }
  subject(:results) { search_results('bbl search Jezusa Chrystusa in ubg') }

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

describe 'bbl search Jêsus Christ in kttv exact output' do
  include_context 'search helpers'
  let(:command_text) { 'bbl search Jêsus Christ in kttv' }
  let(:result) { command(command_text) }
  subject(:results) { search_results('bbl search Jêsus Christ in kttv') }

  it 'returns successfully' do
    expect(result.exit_status).to eq(0)
  end

  it 'includes the expected extra phrase' do
    expect(search_stdout(command_text)).to include('Gia-phổ Đức Chúa Jêsus-Christ')
  end

  it 'starts with the expected extra verse text' do
    expect(results.first).to eq('Ma-thi-ơ 1:1 Gia-phổ Đức Chúa Jêsus-Christ, con cháu Đa-vít và con cháu Áp-ra-ham.')
  end
end
