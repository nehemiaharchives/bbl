require 'json'
require 'stringio'
require 'tmpdir'
require 'zip'
require 'base64'

$bbl_windows = os.windows?
$bbl_macos = %w[darwin mac_os_x].include?(os.name.to_s)

$bbl_attrs_file = if $bbl_windows
  temp_dirs = [
    ENV['TEMP'],
    ENV['TMP'],
    os_env('TEMP').content,
    os_env('TMP').content,
    Dir.tmpdir,
  ].compact.uniq
  temp_dirs.map { |dir| File.join(dir, 'bbl-test-attributes.json') }.find { |path| File.file?(path) } ||
    File.join(Dir.tmpdir, 'bbl-test-attributes.json')
else
  '/tmp/bbl-test-attributes.json'
end
$bbl_attrs_content = $bbl_windows ? (File.read($bbl_attrs_file) if File.file?($bbl_attrs_file)) : file($bbl_attrs_file).content
raise "Unable to load bbl_install test attributes from #{$bbl_attrs_file}" if $bbl_attrs_content.nil? || $bbl_attrs_content.empty?

attrs = JSON.parse($bbl_attrs_content)

$bbl_pack_dir = attrs['pack_dir']
$bbl_helper_bin_dir = attrs['helper_bin_dir']
$bbl_bin = attrs['bbl_binary_path']
$bbl_version_file = attrs['version_file_path']
$bbl_installed_pack_codes = attrs['pack_names'].map { |n| n.delete_suffix('.zip') }
$bbl_installed_search_helpers = attrs['helper_bin_names']
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
  $bbl_run = ->(args) { "#{$bbl_bin} #{args}" }
  $bbl_helper_run = ->(path, args) { "#{path} #{args}" }

  $bbl_zip_manifest_version = lambda do |zip_content, manifest_name|
    return nil if zip_content.nil? || zip_content.empty?
    Zip::InputStream.open(StringIO.new(zip_content.b)) do |zip|
      while (entry = zip.get_next_entry)
        next unless entry.name == manifest_name

        manifest = JSON.parse(zip.read)
        version = manifest['version'] || manifest['bblArtifactCompatibilityVersion']
        raise "#{manifest_name} is missing version" if version.nil? || version.empty?

        return version
      end
    end
    nil
  end
end
