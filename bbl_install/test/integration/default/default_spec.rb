describe file($bbl_bin) do
  it { should exist }
  it { should be_file }
  it { should be_executable } unless $bbl_windows
end

describe file($bbl_version_file) do
  it { should exist }
  it { should be_file }
  its('content') { should match(/\A\d+\.\d+\.\d+\s*\z/) }
end

describe command($bbl_run.call('-v')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should include("bbl version #{$bbl_expected_version}") }
end

describe file($bbl_pack_dir) do
  it { should exist }
  it { should be_directory }
end

$bbl_installed_pack_codes.each do |code|
  describe file("#{$bbl_pack_dir}#{$bbl_sep}#{code}.zip") do
    it { should exist }
    it { should be_file }
    its('size') { should be > 0 }
  end
end

$bbl_installed_search_helpers.each do |name|
  describe file("#{$bbl_helper_bin_dir}#{$bbl_sep}#{name}") do
    it { should exist }
    it { should be_file }
    it { should be_executable } unless $bbl_windows
  end
end

$bbl_installed_search_helpers.each do |name|
  path = "#{$bbl_helper_bin_dir}#{$bbl_sep}#{name}"
  expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
  expected_version_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"

  describe command($bbl_helper_run.call(path, '--version')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should eq(expected_version_stdout) }
  end

  describe command($bbl_helper_run.call(path, '-v')) do
    its('exit_status') { should eq 0 }
    its('stdout') { should eq(expected_version_stdout) }
  end
end

$bbl_installed_pack_codes.each do |code|
  pack_file = "#{$bbl_pack_dir}#{$bbl_sep}#{code}.zip"
  manifest = "#{code}.0.manifest.json"

  if $bbl_windows
    describe "#{code}.zip manifest version" do
      subject(:bbl_version) { $bbl_zip_manifest_version.call(pack_file, manifest) }
      it { should eq($bbl_expected_version) }
    end
  else
    describe "#{code}.zip manifest version" do
      subject(:bbl_version) { $bbl_zip_manifest_version.call(file(pack_file).content, manifest) }
      it { should eq($bbl_expected_version) }
    end
  end
end
