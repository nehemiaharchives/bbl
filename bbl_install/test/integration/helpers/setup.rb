require 'json'
require 'stringio'
require 'tmpdir'
require 'zip'
require 'base64'

$bbl_windows = os.windows?
$bbl_macos = %w[darwin mac_os_x].include?(os.name.to_s)

def bbl_env_value(name)
  [ENV[name], os_env(name).content].find { |value| value.is_a?(String) && !value.empty? }
end

$bbl_attrs_file = if $bbl_windows
  temp_dirs = [
    bbl_env_value('TEMP'),
    bbl_env_value('TMP'),
    bbl_env_value('RUNNER_TEMP'),
    Dir.tmpdir,
    (File.join(bbl_env_value('USERPROFILE'), '.bbl') if bbl_env_value('USERPROFILE')),
  ].compact.uniq
  candidate_files = temp_dirs.map { |dir| File.join(dir, 'bbl-test-attributes.json') }
  found_file = nil
  20.times do
    found_file = candidate_files.find { |path| File.file?(path) }
    break if found_file

    sleep 0.5
  end
  found_file || candidate_files.first || File.join(Dir.tmpdir, 'bbl-test-attributes.json')
else
  '/tmp/bbl-test-attributes.json'
end
$bbl_attrs_content = $bbl_windows ? (File.read($bbl_attrs_file) if File.file?($bbl_attrs_file)) : file($bbl_attrs_file).content

if $bbl_windows && ($bbl_attrs_content.nil? || $bbl_attrs_content.empty?)
  user_profile = bbl_env_value('USERPROFILE')
  raise "Unable to load bbl_install test attributes from #{$bbl_attrs_file} and USERPROFILE is not set" if user_profile.nil? || user_profile.empty?

  local_app_data = bbl_env_value('LOCALAPPDATA') || File.join(user_profile, 'AppData', 'Local')
  install_root = File.join(user_profile, '.bbl')
  pack_dir = File.join(install_root, 'packs')
  helper_bin_dir = File.join(install_root, 'bin')
  temp_dir = bbl_env_value('TEMP') || bbl_env_value('TMP') || bbl_env_value('RUNNER_TEMP') || Dir.tmpdir

  attrs = {
    'pack_dir' => pack_dir,
    'install_source_dir' => File.join(temp_dir, 'bbl-install-downloads'),
    'helper_bin_dir' => helper_bin_dir,
    'bbl_binary_path' => File.join(local_app_data, 'Programs', 'bbl', 'bbl.exe'),
    'version_file_path' => File.join(install_root, 'version.txt'),
    'pack_names' => Dir.glob(File.join(pack_dir, '*.zip')).map { |path| File.basename(path) }.sort,
    'helper_bin_names' => Dir.glob(File.join(helper_bin_dir, 'bbl-search-*.exe')).map { |path| File.basename(path) }.sort,
  }
else
  raise "Unable to load bbl_install test attributes from #{$bbl_attrs_file}" if $bbl_attrs_content.nil? || $bbl_attrs_content.empty?

  attrs = JSON.parse($bbl_attrs_content)
end

$bbl_pack_dir = attrs['pack_dir']
$bbl_install_source_dir = attrs['install_source_dir']
$bbl_helper_bin_dir = attrs['helper_bin_dir']
$bbl_bin = attrs['bbl_binary_path']
$bbl_version_file = attrs['version_file_path']
$bbl_installed_pack_codes = attrs['pack_names'].map { |n| n.delete_suffix('.zip') }
$bbl_installed_search_helpers = attrs['helper_bin_names']
$bbl_install_user = attrs['install_user']
$bbl_home_dir = attrs['home_dir']
$bbl_sep = $bbl_windows ? '\\' : '/'
$bbl_eol = $bbl_windows ? "\r\n" : "\n"

$bbl_expected_version = file($bbl_version_file).content.to_s.strip

if $bbl_windows
  exec = lambda do |path, args|
    escaped = path.gsub("'", "''")
    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"& '#{escaped}' #{args}\""
  end

  $bbl_run = ->(args) { exec.call($bbl_bin, args) }
  $bbl_helper_run = ->(path, args) { exec.call(path, args) }

  zip_manifest_getter = lambda do |zip_path, manifest_name|
    escaped_path = zip_path.gsub("'", "''")
    escaped_name = manifest_name.gsub("'", "''")
    ps = [
      "$ProgressPreference = 'SilentlyContinue'",
      'Add-Type -AssemblyName System.IO.Compression.FileSystem',
      "$archive = [System.IO.Compression.ZipFile]::OpenRead('#{escaped_path}')",
      'try {',
      "  $entry = $archive.GetEntry('#{escaped_name}')",
      '  if ($null -eq $entry) { exit 2 }',
      '  $reader = [System.IO.StreamReader]::new($entry.Open(), [System.Text.Encoding]::UTF8)',
      '  try {',
      '    $manifest = $reader.ReadToEnd() | ConvertFrom-Json',
      '    Write-Output $manifest.version',
      '  } finally { $reader.Dispose() }',
      '} finally { $archive.Dispose() }',
    ].join("\n")
    encoded = Base64.strict_encode64(ps.encode('UTF-16LE'))
    "powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand #{encoded}"
  end

  $bbl_zip_manifest_version = ->(zip_path, manifest_name) { command(zip_manifest_getter.call(zip_path, manifest_name)).stdout.strip }
else
  shell_escape = lambda { |value| "'#{value.to_s.gsub("'", "'\"'\"'")}'" }
  shell_env = lambda do |env|
    env.map { |key, value| "#{key}=#{shell_escape.call(value)}" }.join(' ')
  end
  posix_command = lambda do |command|
    if !$bbl_macos && $bbl_install_user && !$bbl_install_user.empty?
      home = $bbl_home_dir || File.dirname(File.dirname($bbl_pack_dir))
      "runuser -u #{shell_escape.call($bbl_install_user)} -- env HOME=#{shell_escape.call(home)} #{command}"
    else
      command
    end
  end

  $bbl_run = ->(args) { posix_command.call("#{shell_escape.call($bbl_bin)} #{args}") }
  $bbl_run_with_env = ->(env, args) { posix_command.call("#{shell_env.call(env)} #{shell_escape.call($bbl_bin)} #{args}") }
  $bbl_helper_run = ->(path, args) { posix_command.call("#{shell_escape.call(path)} #{args}") }

  $bbl_zip_manifest_version = lambda do |zip_content, manifest_name|
    return nil if zip_content.nil? || zip_content.empty?
    Zip::File.open_buffer(StringIO.new(zip_content.b)) do |zip|
      entry = zip.find_entry(manifest_name)
      return nil if entry.nil?

      manifest = JSON.parse(entry.get_input_stream.read)
      version = manifest['version']
      raise "#{manifest_name} is missing version" if version.nil? || version.empty?

      return version
    end
  end
end
