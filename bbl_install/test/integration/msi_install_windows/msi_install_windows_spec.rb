userProfile = ENV.fetch('USERPROFILE')
localAppData = ENV.fetch('LOCALAPPDATA', File.join(userProfile, 'AppData', 'Local'))

bbl_exe = File.join(localAppData, 'bbl', 'bbl.exe')
webus_pack = File.join(userProfile, '.bbl', 'packs', 'webus.zip')
helper_exe = File.join(userProfile, '.bbl', 'bin', 'bbl-search-common.exe')

describe file(bbl_exe) do
  it { should exist }
  it { should be_file }
end

describe file(webus_pack) do
  it { should exist }
  it { should be_file }
end

describe file(helper_exe) do
  it { should exist }
  it { should be_file }
end

describe powershell("$env:USERPROFILE = $env:USERPROFILE; & '#{bbl_exe.tr("'", "''")}' --version") do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/i) }
end

describe powershell("$env:USERPROFILE = $env:USERPROFILE; & '#{bbl_exe.tr("'", "''")}' john 3:16") do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe powershell("$env:USERPROFILE = $env:USERPROFILE; & '#{bbl_exe.tr("'", "''")}' search God limit 1") do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
