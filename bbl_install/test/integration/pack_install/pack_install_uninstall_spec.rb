describe 'bbl pack install and uninstall' do
  let(:shell_single_quote) { ->(value) { "'#{value.to_s.gsub("'", "'\"'\"'")}'" } }

  let(:bbl_with_env) do
    lambda do |args, env|
      if $bbl_windows
        assignments = env.map do |key, value|
          "$env:#{key} = #{shell_single_quote.call(value)}"
        end.join('; ')
        escaped = $bbl_bin.gsub("'", "''")
        "powershell -NoProfile -ExecutionPolicy Bypass -Command \"#{assignments}; & '#{escaped}' #{args}\""
      else
        assignments = env.map do |key, value|
          "#{key}=#{shell_single_quote.call(value)}"
        end.join(' ')
        "#{assignments} #{$bbl_run.call(args)}"
      end
    end
  end

  it 'uninstalls and reinstalls kjv' do
    uninstall = command($bbl_run.call('uninstall kjv'))
    expect(uninstall.exit_status).to eq(0)
    expect(uninstall.stdout).to include("Uninstalled kjv#{$bbl_eol}")

    after_uninstall_list = command(bbl_with_env.call('list translations', 'BBL_TEST_STEP' => 'after_uninstall'))
    expect(after_uninstall_list.exit_status).to eq(0)
    kjv_available = after_uninstall_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_available).not_to be_nil
    expect(kjv_available).to match(/\|\s+Available\s+\|/)

    install = command(bbl_with_env.call('install kjv', 'BBL_PACK_BASE_URL' => "file://#{$bbl_install_source_dir}"))
    expect(install.exit_status).to eq(0)
    expect(install.stdout).to include("Installed kjv#{$bbl_eol}")

    after_install_list = command(bbl_with_env.call('list translations', 'BBL_TEST_STEP' => 'after_install'))
    expect(after_install_list.exit_status).to eq(0)
    kjv_installed = after_install_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_installed).not_to be_nil
    expect(kjv_installed).to match(/\|\s+Installed\s+\|/)
  end
end
