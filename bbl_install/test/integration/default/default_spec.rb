describe package 'bbl' do
    it { should be_installed }
    its('version') { should eq '1.3-1' }
end

describe command 'bbl -v' do
    its('exit_status') { should eq 0 }
    its('stdout') { should match /bbl version 1.3/ }
end

describe command 'bbl' do
    its('exit_status') { should eq 0 }
    its('stdout') { should match /1 In the beginning, God created the heavens and the earth./ }
end
