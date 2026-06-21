control 'bbl-winget-manifest-and-package' do
  impact 1.0
  title 'bbl WinGet local manifest and package fixture are present'

  describe file(File.join(ENV['TEMP'] || ENV['TMP'], 'bbl-winget-install', 'bbl-winget.zip')) do
    it { should exist }
    it { should be_file }
  end

  describe file(File.join(ENV['TEMP'] || ENV['TMP'], 'bbl-winget-install', 'manifests')) do
    it { should exist }
    it { should be_directory }
  end

  describe command('powershell -NoProfile -Command "Get-ChildItem -Recurse $env:TEMP\\bbl-winget-install\\manifests | Select-Object -ExpandProperty FullName"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Gnit\.Bbl\.yaml/) }
    its('stdout') { should match(/Gnit\.Bbl\.locale\.en-US\.yaml/) }
    its('stdout') { should match(/Gnit\.Bbl\.installer\.yaml/) }
  end
end

control 'bbl-winget-installed' do
  impact 1.0
  title 'bbl is installed by WinGet from local manifest'

  describe command('winget list --id Gnit.Bbl --exact --disable-interactivity') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Gnit\.Bbl/) }
  end

  describe command('winget validate --manifest "%TEMP%\\bbl-winget-install\\manifests" --disable-interactivity') do
    its('exit_status') { should eq 0 }
  end
end

control 'bbl-winget-runtime' do
  impact 1.0
  title 'bbl installed by WinGet runs from PATH and can read/search WEBUS'

  describe command('powershell -NoProfile -Command "$env:USERPROFILE = $env:USERPROFILE; & bbl --version"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
  end

  describe command('powershell -NoProfile -Command "$env:USERPROFILE = $env:USERPROFILE; & bbl list translations"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/WEBUS/i) }
  end

  describe command('powershell -NoProfile -Command "$env:USERPROFILE = $env:USERPROFILE; & bbl john 3:16"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end

  describe command('powershell -NoProfile -Command "$env:USERPROFILE = $env:USERPROFILE; & bbl search God limit 1"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end
end

control 'bbl-winget-no-manual-install-contamination' do
  impact 1.0
  title 'WinGet install does not accidentally rely on old manual install locations'

  describe command('powershell -NoProfile -Command "if (Test-Path $env:LOCALAPPDATA\\Programs\\bbl\\bbl.exe) { exit 1 } else { exit 0 }"') do
    its('exit_status') { should eq 0 }
  end
end
