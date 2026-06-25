describe command('dpkg -s bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/^Package: bbl$/) }
  its('stdout') { should match(/^Status: install ok installed$/) }
end

describe file('/usr/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

# Prove /usr/bin/bbl is the native ELF executable, not a shell wrapper.
describe command("head -c 4 /usr/bin/bbl | od -An -tx1 | tr -d ' \\n'") do
  its('exit_status') { should eq 0 }
  its('stdout') { should eq '7f454c46' }
end

describe file('/home/ubuntu/.bbl/bin/bbl-search-common') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'ubuntu' }
end

describe file('/home/ubuntu/.bbl/packs/webus.zip') do
  it { should exist }
  it { should be_file }
  its('owner') { should eq 'ubuntu' }
end

describe command('HOME=/home/ubuntu /usr/bin/bbl --version') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/i) }
end

describe command('HOME=/home/ubuntu PATH=/usr/bin:$PATH bbl --version') do
  its('exit_status') { should eq 0 }
end

describe file('/usr/share/bash-completion/completions/bbl') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe file('/usr/share/zsh/vendor-completions/_bbl') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe file('/usr/share/fish/vendor_completions.d/bbl.fish') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe command('HOME=/home/ubuntu /usr/bin/bbl john 3:16') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command('HOME=/home/ubuntu /usr/bin/bbl search God limit 1') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
