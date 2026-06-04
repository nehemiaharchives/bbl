require 'json'
require 'stringio'
require 'zip'

macos = %w[darwin mac_os_x].include?(os.name.to_s)
home_dir = macos ? os_env('HOME').content : '/root'
install_root = "#{home_dir}/.bbl"
pack_dir = "#{install_root}/packs"
bin_dir = "#{install_root}/bin"
version_file_path = "#{install_root}/version.txt"
artifact_compatibility_version_file_path = "#{install_root}/artifact_compatibility_version.txt"
expected_bbl_version = file(version_file_path).content.to_s.strip
expected_artifact_compatibility_version = file(artifact_compatibility_version_file_path).content.to_s.strip
bbl_bin_path = macos ? "#{bin_dir}/bbl" : '/usr/bin/bbl'
bbl_command = lambda { |args| "#{bbl_bin_path} #{args}" }
install_source_dir = '/tmp/bbl-install-downloads'
install_env = "BBL_PACK_BASE_URL=file://#{install_source_dir} BBL_SEARCH_BINARY_BASE_URL=file://#{install_source_dir}"
stat_size_command = macos ? 'stat -f %z' : 'stat -c %s'
installed_pack_codes = %w[
  abtag
  ayt
  cunp
  delut
  irvben
  irvguj
  irvhin
  irvmar
  irvtam
  irvtel
  irvurd
  jc
  kjv
  krv
  kttv
  lsg
  npiulb
  rdv24
  rvr09
  sinod
  sven
  svrj
  tb
  th1971
  ubg
  ubio
  webus
]
installed_search_helpers = %w[
  bbl-search-common
  bbl-search-extra
  bbl-search-kuromoji
  bbl-search-morfologik
  bbl-search-nori
  bbl-search-smartcn
]

zip_manifest_bbl_version = lambda do |zip_content, manifest_name|
  return nil if zip_content.nil? || zip_content.empty?

  Zip::InputStream.open(StringIO.new(zip_content.b)) do |zip|
    while (entry = zip.get_next_entry)
      return JSON.parse(zip.read)['bblArtifactCompatibilityVersion'] if entry.name == manifest_name
    end
  end

  nil
end

RSpec.shared_context 'search helpers' do
  def search_stdout(command_text)
    command(command_text).stdout.force_encoding('UTF-8')
  end

  def translation_list_lines(command_text)
    search_stdout(command_text)
      .lines
      .map(&:strip)
      .reject(&:empty?)
      .select { |line| line.match?(/^[A-Z0-9]+\s+\|/) }
  end

  def search_results(command_text)
    results = []
    current = []

    search_stdout(command_text).each_line do |line|
      stripped = line.strip
      next if stripped.empty?

      if stripped.match?(/^\S.*\d+:\d+\s+/)
        results << current.join("\n").strip unless current.empty?
        current = [stripped]
      else
        current << stripped
      end
    end

    results << current.join("\n").strip unless current.empty?
    results.reject(&:empty?)
  end
end

unless os.windows?

  describe file(bbl_bin_path) do
    it { should exist }
    it { should be_file }
    it { should be_executable }
  end

  describe file(version_file_path) do
    it { should exist }
    it { should be_file }
    its('content') { should match(/\A\d+\.\d+\.\d+\s*\z/) }
  end

  describe file(artifact_compatibility_version_file_path) do
    it { should exist }
    it { should be_file }
    its('content') { should match(/\A\d+\.\d+\.\d+\s*\z/) }
  end

  describe command(bbl_command.call('-v')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should include("bbl version #{expected_bbl_version}") }
  end

  describe file(pack_dir) do
    it { should exist }
    it { should be_directory }
  end

  installed_pack_codes.each do |pack_code|
    describe file("#{pack_dir}/#{pack_code}.zip") do
      it { should exist }
      it { should be_file }
      its('size') { should be > 0 }
    end
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

  installed_search_helpers.each do |helper_name|
    describe command("#{bin_dir}/#{helper_name} --version") do
      its('exit_status') { should eq 0 }
      its('stdout') { should include("#{helper_name} version #{expected_bbl_version}") }
    end

    describe command("#{bin_dir}/#{helper_name} --artifact-compat-version") do
      its('exit_status') { should eq 0 }
      its('stdout') { should eq("#{expected_artifact_compatibility_version}\n") }
    end
  end

  installed_pack_codes.each do |pack_code|
    describe "#{pack_code}.zip manifest bblArtifactCompatibilityVersion" do
      subject(:bbl_version) do
        zip_manifest_bbl_version.call(file("#{pack_dir}/#{pack_code}.zip").content, "#{pack_code}.0.manifest.json")
      end

      it { should eq(expected_artifact_compatibility_version) }
    end
  end

  describe command(bbl_command.call('search Christ')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
  end

  describe command(bbl_command.call('search Jesus')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
  end

  describe 'bbl list translations output' do
    include_context 'search helpers'
    subject(:translations) { translation_list_lines(bbl_command.call('list translations')) }

    it 'lists the full translation catalog' do
      expect(translations.length).to eq(27)
    end
  end

  describe 'bbl search Christ exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Christ')) }

    it 'starts with the expected webus verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe 'bbl search Jesus exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Jesus')) }

    it 'starts with the expected webus verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe 'bbl search Jesus wept exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Jesus wept')) }

    it 'starts with the expected plain-search verse text' do
      expect(results.first).to eq('Matthew 26:75 Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” Then he went out and wept bitterly.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe 'bbl search Jesus weep exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Jesus weep')) }

    it 'starts with the expected normalized-search verse text' do
      expect(results.first).to eq('Matthew 26:75 Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” Then he went out and wept bitterly.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe 'bbl search quoted Jesus wept exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search "Jesus wept"')) }

    it 'starts with the expected exact-search verse text' do
      expect(results.first).to eq('John 11:35 Jesus wept.')
    end

    it 'returns only the exact phrase result' do
      expect(results.length).to eq(1)
    end
  end

  describe command(bbl_command.call('search Christ in kjv')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the generation of Jesus Christ/) }
  end

  describe command(bbl_command.call('search Jesus in kjv')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the generation of Jesus Christ/) }
  end

  describe 'bbl search Christ in kjv exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Christ in kjv')) }

    it 'starts with the expected kjv verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe 'bbl search Jesus in kjv exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Jesus in kjv')) }

    it 'starts with the expected kjv verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe command(bbl_command.call('search Christ in romans')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Paul, a servant of Jesus Christ/) }
  end

  describe 'bbl search Christ in romans exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Christ in romans')) }

    it 'starts with the expected romans webus verse text' do
      expect(results.first).to eq('Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,')
    end

    it 'returns multiple Romans hits' do
      expect(results.length).to be > 1
    end
  end

  describe command(bbl_command.call('search Christ in romans 5-12')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Being therefore justified by faith/) }
  end

  describe 'bbl search Christ in romans 5-12 exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Christ in romans 5-12')) }

    it 'starts with the expected romans chapter-range webus verse text' do
      expect(results.first).to eq('Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;')
    end

    it 'returns multiple hits in the requested chapter range' do
      expect(results.length).to be > 1
    end
  end

  describe command(bbl_command.call('search Christ in romans 5-12 in kjv')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Therefore being justified by faith/) }
  end

  describe 'bbl search Christ in romans 5-12 in kjv exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call('search Christ in romans 5-12 in kjv')) }

    it 'starts with the expected romans chapter-range kjv verse text' do
      expect(results.first).to eq('Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:')
    end

    it 'returns multiple hits in the requested kjv chapter range' do
      expect(results.length).to be > 1
    end
  end

  describe command(bbl_command.call("search Christ in johns letters")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/1 John 1:3/) }
  end

  describe 'bbl search Christ in johns letters exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call("search Christ in johns letters")) }

    it 'starts with the expected Johns letters verse text' do
      expect(results.first).to match(/\A1 John 1:3 /)
    end

    it 'returns multiple hits from the requested category' do
      expect(results.length).to be > 1
    end
  end

  describe command(bbl_command.call("search Christ in johns letters in kjv")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/1 John 1:3/) }
  end

  describe 'bbl search Christ --category johns letters exact output' do
    include_context 'search helpers'
    subject(:results) { search_results(bbl_command.call("search Christ --category 'johns letters'")) }

    it 'starts with the expected Johns letters verse text' do
      expect(results.first).to match(/\A1 John 1:3 /)
    end

    it 'returns multiple hits from the requested category' do
      expect(results.length).to be > 1
    end
  end

  # describe 'bbl preinstalled jc dependencies' do
  #  before(:all) do
  #    @list_result = command("sh -lc '#{bbl_command.call('list translations')} | grep \"^JC\"' # preinstalled")
  #    @list_stdout = @list_result.stdout.force_encoding('UTF-8')
  #    @jc_line = @list_stdout.lines.find { |line| line.start_with?('JC') }
  #    @pack_exists = command("test -f #{pack_dir}/jc.zip").exit_status == 0
  #    @pack_size = command("#{stat_size_command} #{pack_dir}/jc.zip").stdout.to_i
  #    @pack_version = zip_manifest_bbl_version.call(file("#{pack_dir}/jc.zip").content, 'jc.0.manifest.json')
  #    @helper_exists = command("test -f #{bin_dir}/bbl-search-kuromoji").exit_status == 0
  #    @helper_executable = command("test -x #{bin_dir}/bbl-search-kuromoji").exit_status == 0
  #    @helper_version = command("#{bin_dir}/bbl-search-kuromoji --version").stdout.force_encoding('UTF-8')
  #    @helper_artifact_compatibility_version =
  #      command("#{bin_dir}/bbl-search-kuromoji --artifact-compat-version").stdout.force_encoding('UTF-8')
  #    @search_result = command(bbl_command.call('search イエス キリスト in jc'))
  #    @search_stdout = @search_result.stdout.force_encoding('UTF-8')
  #    @search_results = @search_stdout
  #      .split("\n\n")
  #      .map(&:strip)
  #      .reject(&:empty?)
  #  end
  #
  #  it 'installs jc.zip' do
  #    expect(@pack_exists).to eq(true)
  #    expect(@pack_size).to be > 0
  #  end
  #
  #  it 'installs jc.zip with the expected bblArtifactCompatibilityVersion' do
  #    expect(@pack_version).to eq(expected_artifact_compatibility_version)
  #  end
  #
  #  it 'installs the kuromoji search helper' do
  #    expect(@helper_exists).to eq(true)
  #    expect(@helper_executable).to eq(true)
  #  end
  #
  #  it 'installs the kuromoji search helper with the expected version' do
  #    expect(@helper_version).to include("bbl-search-kuromoji version #{expected_bbl_version}")
  #  end
  #
  #  it 'installs the kuromoji search helper with the expected artifact compatibility version' do
  #    expect(@helper_artifact_compatibility_version).to eq("#{expected_artifact_compatibility_version}\n")
  #  end
  #
  #  it 'bbl list command shows jc as installed' do
  #    expect(@jc_line).to include('JC')
  #    expect(@jc_line).to include('| Installed |')
  #  end
  #
  #  it 'returns successfully' do
  #    expect(@search_result.exit_status).to eq(0)
  #  end
  #
  #  it 'includes the expected kuromoji phrase' do
  #    expect(@search_stdout).to include('イエス・キリストの系図。')
  #  end
  #
  #  it 'starts with the expected kuromoji verse text' do
  #    expect(@search_results.first).to eq('マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。')
  #  end
  # end

  describe 'bbl search 예수 그리스도 in krv exact output' do
    include_context 'search helpers'
    let(:command_text) { bbl_command.call('search 예수 그리스도 in krv') }
    let(:result) { command(command_text) }
    subject(:results) { search_results(bbl_command.call('search 예수 그리스도 in krv')) }

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
    let(:command_text) { bbl_command.call('search 耶稣基督 in cunp') }
    let(:result) { command(command_text) }
    subject(:results) { search_results(bbl_command.call('search 耶稣基督 in cunp')) }

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
    let(:command_text) { bbl_command.call('search Jezusa Chrystusa in ubg') }
    let(:result) { command(command_text) }
    subject(:results) { search_results(bbl_command.call('search Jezusa Chrystusa in ubg')) }

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
    let(:command_text) { bbl_command.call('search Jêsus Christ in kttv') }
    let(:result) { command(command_text) }
    subject(:results) { search_results(bbl_command.call('search Jêsus Christ in kttv')) }

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

end
