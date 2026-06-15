describe 'bbl pack install and uninstall' do
  it 'uninstalls and reinstalls kjv' do
    uninstall = command($bbl_run.call('uninstall kjv'))
    expect(uninstall.exit_status).to eq(0)
    expect(uninstall.stdout).to include("Uninstalled kjv#{$bbl_eol}")

    after_uninstall_list = command("BBL_INSPEC_STEP=after_uninstall #{$bbl_run.call('list translations')}")
    expect(after_uninstall_list.exit_status).to eq(0)
    kjv_available = after_uninstall_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_available).not_to be_nil
    expect(kjv_available).to match(/\|\s+Available\s+\|/)

    install = command($bbl_run_with_env.call({ 'BBL_PACK_BASE_URL' => "file://#{$bbl_install_source_dir}" }, 'install kjv'))
    expect(install.exit_status).to eq(0)
    expect(install.stdout).to include("Installed kjv#{$bbl_eol}")
    unless $bbl_windows || $bbl_macos
      expect(file("#{$bbl_pack_dir}#{$bbl_sep}kjv.zip").owner).to eq($bbl_install_user)
    end

    after_install_list = command("BBL_INSPEC_STEP=after_install #{$bbl_run.call('list translations')}")
    expect(after_install_list.exit_status).to eq(0)
    kjv_installed = after_install_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_installed).not_to be_nil
    expect(kjv_installed).to match(/\|\s+Installed\s+\|/)
  end
end
