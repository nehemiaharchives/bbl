# Windows Integration Test & Build Considerations

This document captures pitfalls encountered when building, testing, and deploying
the bbl CLI on Windows (mingwX64 / local transport). Read this before making
changes to `bbl_install`, `core/src/mingwX64Main`, or CI Windows jobs.

## Chef Cookbook on Windows

### a. `cookbook_file` Requires Admin Privileges

**Pitfall:** The `cookbook_file` resource tries to set `owner`, `group`, and
`mode`. On Windows these require admin privileges and fail in non-admin
convergence.

**Fix:** Use `ruby_block` with `FileUtils.cp` for file operations on Windows:
```ruby
ruby_block "copy #{source_name} to #{path}" do
  block do
    source_path = ::File.join(::File.expand_path('../files', __dir__), source_name)
    ::FileUtils.mkdir_p(::File.dirname(path))
    ::FileUtils.cp(source_path, path)
  end
end
```

### b. `directory` Resource on Windows

**Pitfall:** The `directory` resource on Windows does not support `owner` /
`group` / `mode` properties.

**Fix:** Only use `recursive true` for Windows directories. Guard POSIX-only
properties behind `unless windows`.

### c. Split Recipes by Platform

**Practice:** Keep Windows logic in `recipes/install_windows.rb` and POSIX logic
in `recipes/default.rb`. Use `include_recipe` with a platform guard:
```ruby
include_recipe 'bbl_install::install_windows' if windows
return if windows
```

---

## InSpec / Test Kitchen on Windows

### a. `powershell` Resource Prepends `$ProgressPreference`

**Pitfall:** The InSpec `powershell` resource automatically prepends
`$ProgressPreference='SilentlyContinue';` to every command. This **breaks**
`-File` usage:
```powershell
# What InSpec actually executes:
$ProgressPreference='SilentlyContinue';-File C:\path\script.ps1
# ERROR: -File is not recognized
```

**Workaround:** Never pass `-File` to the `powershell` resource. Pass the script
content directly, or use the `command()` resource with `powershell -Command`.

### b. `command()` Resource Expands Variables Before PowerShell

**Pitfall:** The `command()` resource on Windows runs through cmd.exe, which
expands environment variables before PowerShell sees them:
```ruby
command("$env:USERPROFILE = $env:USERPROFILE; & 'bbl.exe'")
# cmd.exe expands this to:
# C:\Users\joel = C:\Users\joel; & 'bbl.exe'  ← SyntaxError
```

**Fix:** For PowerShell-specific commands, use the `powershell` resource directly.
For commands that need full shell expansion control, use `powershell -Command`.

### c. USERPROFILE Not Propagated to BBL Tool

**Pitfall:** The `bbl` binary determines its pack directory from `%USERPROFILE%`.
In InSpec's `powershell` resource, USERPROFILE is set correctly, but the bbl tool
may not inherit it for all sub-processes.

**Fix:** Explicitly set USERPROFILE in every PowerShell command:
```ruby
powershell("$env:USERPROFILE = $env:USERPROFILE; & 'bbl.exe' list translations")
```

### d. BBL Pack Directory Resolution

**Pitfall:** The bbl tool computes `packDir` as `%USERPROFILE%\.bbl\packs`. If
USERPROFILE is wrong or missing, the tool will not find installed packs and will
show translations as "Available" instead of "Installed".

**Verification:** After install, run `list translations` to confirm the pack shows
as "Installed", not just "Available".

### e. Kitchen Suite Naming Convention

Kitchen instance names are `<suite>-<platform>`. When splitting suites:
- `pack_install_windows` + `windows-local` → `pack-install-windows-windows-local`

Update both `kitchen.yml` and `.github/workflows/ci.yml` with the new names.

---

## 4. Build & Staging Pipeline

**After any Kotlin code change** affecting the CLI or core module:

```bash
./gradlew stageBblInstall<Platform>CliAllFixture
# e.g. for Windows:
./gradlew stageBblInstallWindowsCliAllFixture
```

This copies the freshly built binaries into `bbl_install/files/`. The `kitchen
converge` step then copies from `bbl_install/files/` to the test machine. Without
this step, kitchen tests run against stale binaries.

**For cross-compilation debugging:**
```bash
# Compile only (fast feedback):
./gradlew :core:compileKotlinMingwX64 --no-daemon

# Force recompile (clear cache):
./gradlew :core:compileKotlinMingwX64 --rerun-tasks --no-daemon

# Link executable:
./gradlew :cli:core:linkReleaseExecutableMingwX64 --no-daemon
```

Do NOT use `jetbrains_build_project` for Kotlin/Native targets — it times out.
Use `./gradlew` directly.

---

## 5. Mixed Forward/Backslash Paths

Chef attributes on Windows may produce mixed-slash paths:
```json
"pack_dir": "C:\\Users\\joel/.bbl/packs"
```

The okio `Path` library used by the bbl tool handles both separators correctly.
However, be aware of this when writing PowerShell scripts or InSpec checks that
do string comparisons on paths.

---

## 6. PowerShell Quoting Pitfalls

- Backslashes in paths inside single-quoted strings are literal (good).
- Backslashes in double-quoted strings may be interpreted as escape characters.
- Use the `&` call operator with single-quoted paths for reliability:
  ```powershell
  & 'C:\Users\...\bbl.exe' install kjv
  ```
- Do **not** use:
  ```powershell
  $env:LOCALAPPDATA\Programs\bbl\bbl.exe  # ← SyntaxError (backslash after env var)
  ```
  Use:
  ```powershell
  & "$env:LOCALAPPDATA\Programs\bbl\bbl.exe"  # ← Correct
  ```

---

## 7. Test File Organization

- When windows specific test logic need to be implemented, POSIX and Windows tests should be in separate directories e.g.:
  - `test/integration/pack_install_posix/`
  - `test/integration/pack_install_windows/`
- But when test logic is simple enough to cover both platform, only one test directory is needed e.g.:
  - `test/integration/search/`
- Never hardcode user-specific paths (e.g., `C:\Users\joel`). Use environment
  variables (`$env:LOCALAPPDATA`, `$env:USERPROFILE`) in test commands.
