require 'chefspec'

describe 'bbl_install' do
	
	context 'installs bbl' do
		it 'copy deb file from local to container' do
			expect(chef_run).to create_cookbook_file("/opt/bbl_1.2_amd64.deb").with(
				source: "bbl_1.2_amd64.deb",
				owner: "root",
				group: "root",
				mode: "0755"
			)
		end

		it 'installs bbl from deb file' do
			expect(chef_run).to install_dpkg_package('bbl').with(
				source: "/opt/bbl_1.2_amd64.deb"
			)
		end
	end
end