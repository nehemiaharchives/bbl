describe command('brew list bbl') do
  its('exit_status') { should eq 0 }
end

describe command('realpath "$(brew --prefix bbl)"') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(%r{/Cellar/bbl/}) }
end

describe command('test -f "$(brew --prefix bbl)/etc/bash_completion.d/bbl"') do
  its('exit_status') { should eq 0 }
end

describe command('test -f "$(brew --prefix bbl)/share/zsh/site-functions/_bbl"') do
  its('exit_status') { should eq 0 }
end

describe command('test -f "$(brew --prefix bbl)/share/fish/vendor_completions.d/bbl.fish"') do
  its('exit_status') { should eq 0 }
end

describe command('brew test bbl-kmp-e2e/local/bbl') do
  its('exit_status') { should eq 0 }
end

describe command('bbl --version') do
  its('exit_status') { should eq 0 }
end

describe file(File.join(Dir.home, '.bbl/bin/bbl-search-common')) do
  it { should be_file }
  it { should be_executable }
end

describe file(File.join(Dir.home, '.bbl/packs/webus.zip')) do
  it { should be_file }
end

describe command('bbl john 3:16') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command('bbl search God limit 1') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
