describe 'bbl binary' do
  subject { file($bbl_bin) }
  it { should exist }
  it { should be_file }
  it 'is executable' do
    skip 'N/A on Windows' if $bbl_windows
    expect(subject).to be_executable
  end
end

describe 'bbl version file' do
  subject { file($bbl_version_file) }
  it { should exist }
  it { should be_file }
  its('content') { should match(/\Av\d+\.\d+\s*\z/) }
end

describe 'bbl -v' do
  subject(:cmd) { command($bbl_run.call('-v')) }
  its('exit_status') { should eq 0 }
  its('stdout') { should include("bbl version #{$bbl_expected_version}") }
end

describe 'bbl pack dir' do
  subject { file($bbl_pack_dir) }
  it { should exist }
  it { should be_directory }
end

describe 'bbl pack files' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are non-empty' do
    pack_codes.each do |code|
      pack_file = file("#{pack_dir}#{sep}#{code}.zip")
      expect(pack_file).to exist
      expect(pack_file).to be_file
      expect(pack_file.size).to be > 0
    end
  end
end

describe 'bbl helper binaries' do
  let(:helper_names) { $bbl_installed_search_helpers }
  let(:helper_bin_dir) { $bbl_helper_bin_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are executable' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      helper_file = file(path)
      expect(helper_file).to exist
      expect(helper_file).to be_file
      unless $bbl_windows
        expect(helper_file).to be_executable
      end
    end
  end

  it 'report correct version via --version' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '--version'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end

  it 'report correct version via -v' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '-v'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end
end

describe 'bbl pack manifest versions' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }
  let(:expected_version) { $bbl_expected_version }

  it 'match expected version' do
    pack_codes.each do |code|
      pack_file = "#{pack_dir}#{sep}#{code}.zip"
      manifest = "#{code}.0.manifest.json"

      version = if $bbl_windows
        $bbl_zip_manifest_version.call(pack_file, manifest)
      else
        $bbl_zip_manifest_version.call(file(pack_file).content, manifest)
      end

      expect(version).to eq(expected_version)
    end
  end
end
