describe file('/usr/local/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe command('pkgutil --pkg-info org.gnit.bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/org\.gnit\.bbl/) }
end

describe file('/usr/local/share/bash-completion/completions/bbl') do
  it { should exist }
  it { should be_file }
end

describe file('/usr/local/share/zsh/site-functions/_bbl') do
  it { should exist }
  it { should be_file }
end

describe file('/usr/local/share/fish/vendor_completions.d/bbl.fish') do
  it { should exist }
  it { should be_file }
end

describe command('/usr/local/bin/bbl --version') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/i) }
end

describe command('PATH=/usr/local/bin:$PATH bbl --version') do
  its('exit_status') { should eq 0 }
end

describe file(File.join(Dir.home, '.bbl/bin/bbl-search-common')) do
  it { should be_file }
  it { should be_executable }
end

describe file(File.join(Dir.home, '.bbl/packs/webus.zip')) do
  it { should be_file }
end

describe command('/usr/local/bin/bbl john 3:16') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command('/usr/local/bin/bbl search God limit 1') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
