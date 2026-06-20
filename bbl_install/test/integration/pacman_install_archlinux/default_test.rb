describe command('pacman -Q bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/^bbl\s+/) }
end

describe command('pacman -Qi bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/^Name\s+:\s+bbl$/) }
  its('stdout') { should match(/^Architecture\s+:\s+x86_64$/) }
  its('stdout') { should match(/Apache-2\.0/) }
end

describe file('/usr/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'root' }
  its('group') { should eq 'root' }
end

describe file('/home/arch/.bbl/bin/bbl-search-common') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'arch' }
  its('group') { should eq 'arch' }
end

describe file('/home/arch/.bbl/packs/webus.zip') do
  it { should exist }
  it { should be_file }
  its('owner') { should eq 'arch' }
  its('group') { should eq 'arch' }
end

describe command('pacman -Ql bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should include "bbl /usr/bin/bbl\n" }
  its('stdout') { should include "bbl /home/arch/.bbl/bin/bbl-search-common\n" }
  its('stdout') { should include "bbl /home/arch/.bbl/packs/webus.zip\n" }
  its('stdout') { should_not include '/home/ubuntu' }
  its('stdout') { should_not include '/home/fedora' }
  its('stdout') { should_not include '/usr/local' }
  its('stdout') { should_not include "bbl /home/arch/.bbl/webus.zip\n" }
end

describe command('runuser -u arch -- env HOME=/home/arch /usr/bin/bbl --version') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
end

describe command('runuser -u arch -- env HOME=/home/arch /usr/bin/bbl list translations') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/WEBUS.*Installed/i) }
end

describe command('runuser -u arch -- env HOME=/home/arch /usr/bin/bbl john 3:16') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command('runuser -u arch -- env HOME=/home/arch /usr/bin/bbl search God limit 1') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
