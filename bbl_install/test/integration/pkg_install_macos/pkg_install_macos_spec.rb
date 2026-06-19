describe file('/usr/local/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
end

describe command('pkgutil --pkg-info org.gnit.bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/org\.gnit\.bbl/) }
end

describe command('/usr/local/bin/bbl --version') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/i) }
end

describe command('PATH=/usr/local/bin:$PATH bbl --version') do
  its('exit_status') { should eq 0 }
end
