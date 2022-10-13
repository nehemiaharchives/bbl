package_name = "bbl_#{node['bbl_install']['version']}_amd64.deb"

cookbook_file "/opt/#{package_name}" do
	source package_name
	owner "root"
	group "root"
	mode "0755"
end

dpkg_package 'bbl' do
	source "/opt/#{package_name}"
end
