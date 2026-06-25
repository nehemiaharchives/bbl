control 'bbl-rpm-package-installed' do
  impact 1.0
  title 'bbl RPM package is installed on Fedora'

  describe package('bbl') do
    it { should be_installed }
  end

  describe command('rpm -q bbl') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/^bbl-/) }
  end

  describe command("rpm -qi bbl") do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/^Name\s*:\s*bbl$/) }
    its('stdout') { should match(/^Version\s*:\s*\d+\.\d+\.\d+/) }
    its('stdout') { should match(/^License\s*:\s*Apache-2\.0$/) }
  end
end

control 'bbl-rpm-files' do
  impact 1.0
  title 'bbl RPM installs files to expected locations'

  describe file('/usr/bin/bbl') do
    it { should exist }
    it { should be_file }
    it { should be_executable }
    its('owner') { should eq 'root' }
    its('group') { should eq 'root' }
  end

  describe file('/home/fedora/.bbl/bin/bbl-search-common') do
    it { should exist }
    it { should be_file }
    it { should be_executable }
    its('owner') { should eq 'fedora' }
    its('group') { should eq 'fedora' }
  end

  describe file('/home/fedora/.bbl/packs/webus.zip') do
    it { should exist }
    it { should be_file }
    its('owner') { should eq 'fedora' }
    its('group') { should eq 'fedora' }
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

  describe command('rpm -ql bbl') do
    its('exit_status') { should eq 0 }
    its('stdout') { should include "/usr/bin/bbl\n" }
    its('stdout') { should include "/home/fedora/.bbl/bin/bbl-search-common\n" }
    its('stdout') { should include "/home/fedora/.bbl/packs/webus.zip\n" }
    its('stdout') { should include "/usr/share/bash-completion/completions/bbl\n" }
    its('stdout') { should include "/usr/share/zsh/site-functions/_bbl\n" }
    its('stdout') { should include "/usr/share/fish/vendor_completions.d/bbl.fish\n" }
    its('stdout') { should_not include '/home/ubuntu' }
    its('stdout') { should_not include '/usr/local' }
    its('stdout') { should_not include "/home/fedora/.bbl/webus.zip\n" }
  end
end

control 'bbl-rpm-runtime' do
  impact 1.0
  title 'bbl installed from RPM runs as Fedora user'

  describe command('runuser -u fedora -- env HOME=/home/fedora /usr/bin/bbl --version') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
  end

  describe command('runuser -u fedora -- env HOME=/home/fedora /usr/bin/bbl john 3:16') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end

  describe command('runuser -u fedora -- env HOME=/home/fedora /usr/bin/bbl search God limit 1') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end
end
