describe command('apk info -e bbl') do
  its('exit_status') { should eq 0 }
end

describe command('apk info -a bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/) }
  its('stdout') { should match(/Apache-2\.0/) }
end

describe command('apk info -R bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/gcompat/) }
  its('stdout') { should match(/libgcc/) }
  its('stdout') { should match(/libstdc\+\+/) }
end

describe file('/usr/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'root' }
  its('group') { should eq 'root' }
end

describe file('/usr/lib/bbl/bin/bbl-search-common') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'root' }
  its('group') { should eq 'root' }
end

describe file('/usr/lib/bbl/packs/webus.zip') do
  it { should exist }
  it { should be_file }
  its('owner') { should eq 'root' }
  its('group') { should eq 'root' }
end

describe file('/home/alpine/.bbl/bin/bbl-search-common') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'alpine' }
  its('group') { should eq 'alpine' }
end

describe file('/home/alpine/.bbl/packs/webus.zip') do
  it { should exist }
  it { should be_file }
  its('owner') { should eq 'alpine' }
  its('group') { should eq 'alpine' }
end

describe file('/usr/share/bash-completion/completions/bbl') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe file('/usr/share/zsh/site-functions/_bbl') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe file('/usr/share/fish/vendor_completions.d/bbl.fish') do
  it { should exist }
  it { should be_file }
  its('mode') { should cmp '0644' }
end

describe command('apk info -L bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(%r{(^|\n)usr/bin/bbl(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)usr/lib/bbl/bin/bbl-search-common(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)usr/lib/bbl/packs/webus.zip(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)usr/share/bash-completion/completions/bbl(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)usr/share/zsh/site-functions/_bbl(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)usr/share/fish/vendor_completions.d/bbl.fish(\n|$)}) }
  its('stdout') { should_not include '/home/ubuntu' }
  its('stdout') { should_not include '/home/fedora' }
  its('stdout') { should_not include '/home/arch' }
  its('stdout') { should_not include '/usr/local' }
  its('stdout') { should_not include "/home/alpine/.bbl/" }
end

describe command('file /usr/bin/bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/ELF/) }
end

describe command('ldd /usr/bin/bbl || true') do
  its('stdout') { should_not match(/not found/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl --version'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl list translations'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/WEBUS.*Installed/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl john 3:16'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl search God limit 1'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
