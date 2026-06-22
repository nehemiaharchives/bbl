describe 'bbl pack install and uninstall' do
  it 'downloads the platform-qualified common helper and installs it with its local name' do
    shell_quote = ->(value) { "'#{value.to_s.gsub("'", %q('\\''))}'" }
    bbl_cmd = shell_quote.call($bbl_bin)
    source_dir = shell_quote.call($bbl_install_source_dir)

    script = <<~SH
      set -eu
      test_home="$(mktemp -d)"
      test_release="$test_home/release"
      mkdir -p "$test_release"
      trap 'rm -rf "$test_home"' EXIT

      case "$(uname -s):$(uname -m)" in
        Linux:x86_64|Linux:amd64) release_target='linux-x64' ;;
        Darwin:arm64|Darwin:aarch64) release_target='macos-arm64' ;;
        Darwin:x86_64|Darwin:amd64) release_target='macos-x64' ;;
        *) echo "Unsupported release target: $(uname -s):$(uname -m)" >&2; exit 1 ;;
      esac

      cp #{source_dir}/webus.zip "$test_release/webus.zip"
      cp #{source_dir}/bbl-search-common "$test_release/bbl-search-common-$release_target"

      HOME="$test_home" BBL_RELEASE_DOWNLOAD_URL="file://$test_release" #{bbl_cmd} install webus
      local_binary="$test_home/.bbl/bin/bbl-search-common"
      release_binary="$test_home/.bbl/bin/bbl-search-common-$release_target"
      test -f "$local_binary"
      test ! -e "$release_binary"

      HOME="$test_home" #{bbl_cmd} uninstall webus
      test ! -e "$local_binary"
    SH

    result = command(script)
    expect(result.exit_status).to eq(0), result.stderr
    expect(result.stdout).to include("Installed bbl-search-common#{$bbl_eol}")
    expect(result.stdout).to include("Uninstalled bbl-search-common#{$bbl_eol}")
  end

  it 'uninstalls and reinstalls kjv' do
    uninstall = command($bbl_run.call('uninstall kjv'))
    expect(uninstall.exit_status).to eq(0)
    expect(uninstall.stdout).to include("Uninstalled kjv#{$bbl_eol}")

    after_uninstall_list = command("BBL_INSPEC_STEP=after_uninstall #{$bbl_run.call('list translations')}")
    expect(after_uninstall_list.exit_status).to eq(0)
    kjv_available = after_uninstall_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_available).not_to be_nil
    expect(kjv_available).to match(/\|\s+Available\s+\|/)

    install = command($bbl_run_with_env.call({ 'BBL_RELEASE_DOWNLOAD_URL' => "file://#{$bbl_install_source_dir}" }, 'install kjv'))
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
