Recommended Path

1. First add GitHub Actions .deb generation to publish.yml.
   Reuse the existing Linux release build from build-cli-assets. It already builds bbl, all bbl-search-* helpers, and pack zips. Add a Linux-only packaging job that creates
   bbl-kmp_<version>_amd64.deb and uploads it to the GitHub Release.
2. Decide install layout before packaging.
   I’d use:

   /usr/bin/bbl
   $HOME/.bbl/bin/bbl-search-*
   $HOME/.bbl/packs/*.zip

   The .deb should not install into /usr/local or /root. It should install only the core bbl binary into /usr/bin and let bbl manage user-scoped helpers and packs under
   $HOME/.bbl.

3. Add a Debian packaging directory.
   Add something like:

   packaging/debian/
   packaging/debian/control
   packaging/debian/changelog
   packaging/debian/rules
   packaging/debian/install
   packaging/debian/copyright
   packaging/debian/source/format

   For the first .deb, this can be a simple package that installs already-built Linux binaries from the workflow artifact.

4. Add a local .deb build job to publish.yml.
   After Linux CLI assets are built, run a job on ubuntu-latest that installs debhelper, devscripts, and lintian, stages the Linux binaries into a package source tree, runs:

   dpkg-buildpackage -us -uc -b

   Then upload the resulting .deb as another GitHub Release asset.

5. Test the .deb in CI before publishing.
   Add a job that installs the generated .deb into ubuntu:24.04 or the GitHub runner and runs the same smoke tests as bbl_install_linux/Kitchen:

   sudo apt install ./bbl-kmp_*_amd64.deb
   bbl ...
   bbl search ...

6. For PPA, choose between “proper” and “pragmatic”.
   Proper PPA packaging means Launchpad builds Kotlin/Native from source. That is likely hard because Launchpad builds are source-package builds with apt dependencies, and this
   project needs Gradle, Kotlin/Native, Konan downloads, JDK 24, and likely network-hostile build behavior.

   Pragmatic PPA packaging means creating a signed source package that contains the prebuilt Linux payload and whose debian/rules only installs those files into a .deb.
   Launchpad still builds the .deb, but it is not compiling Kotlin from source. This is much easier and matches your current release pipeline.
7. Add a publish-ppa job only after the .deb layout is stable.
   It should run only on tags or manual dispatch, import a GPG private key from GitHub Secrets, build a signed source package, then upload with:

   debuild -S -sa -k"$GPG_KEY_ID"
   dput ppa:<launchpad-id>/<ppa-name> ../bbl-kmp_*_source.changes

8. Required GitHub Secrets.
   You will need:

   LAUNCHPAD_PPA
   GPG_PRIVATE_KEY
   GPG_PASSPHRASE
   GPG_KEY_ID
   DEBEMAIL
   DEBFULLNAME

   The GPG public key must be registered in Launchpad, and the account/team must have upload rights.

9. Versioning rule.
   Convert tags like v0.3.0 into Debian versions like:

   0.3.0-1~ppa1~ubuntu24.04.1

   Launchpad requires versions to be unique per package/distribution, and multi-Ubuntu-series uploads need distinct versions.

10. Start with one Ubuntu target.
    Use noble / Ubuntu 24.04 first because your Kitchen Linux target is already ubuntu-24.04. Add more series later only after one PPA build is reliable.

My suggested implementation order is: GitHub Release .deb first, CI install test second, PPA upload third. That keeps the packaging/debug loop short before adding Launchpad
signing and upload complexity.


› I want to also test .deb file install with E2E test


• I’ll trace the Linux E2E and install cookbook tests, then propose where the .deb install test should sit so it validates the same CLI behavior without duplicating too much CI
time.

• Explored
└ Read default_spec.rb, ci.yml

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• Yes. I would add .deb install E2E as a separate Linux job, not inside the existing Dokken cookbook job.

The existing E2E (linux/cli) validates the Chef cookbook path: bbl_install_linux::default copies staged binaries into /usr/bin and /root/.bbl. A .deb E2E should validate
the Debian package contract instead: package metadata, apt/dpkg installability, installed file layout, uninstall behavior, and then the same real CLI/search behavior.

Step by step:

1. Add a .deb package build job in publish.yml first.
   It should consume the Linux release binaries or run the same Linux build, then output something like:

   dist/bbl-kmp_<version>_amd64.deb

2. Upload the .deb as a workflow artifact.
   Name it clearly:

   bbl-kmp-linux-deb

3. Add an E2E job that downloads that .deb.
   The job should run on ubuntu-latest and use a clean Ubuntu container or Docker image. I’d prefer Docker so the runner machine does not get polluted:

   docker run --rm -v "$PWD:/work" ubuntu:24.04 ...

4. In the container, install the .deb using apt, not only dpkg.
   This catches dependency metadata problems:

   apt-get update
   apt-get install -y /work/dist/bbl-kmp_*_amd64.deb

5. Run package-level assertions.
   Example checks:

   dpkg -s bbl-kmp
   command -v bbl
   bbl -v
   dpkg -L bbl-kmp

6. Run CLI behavior E2E.
   Reuse the same important commands from bbl_install_linux/test/integration/default/default_spec.rb:

   bbl search Jesus Christ
   bbl search Jesus Christ in kjv
   bbl search Jesus Christ in romans 5-12
   bbl search イエス キリスト in jc
   bbl search 예수 그리스도 in krv
   bbl search 耶稣基督 in cunp
   bbl search Jezusa Chrystusa in ubg

7. Test uninstall.
   This is useful for .deb specifically:

   apt-get remove -y bbl-kmp
   ! command -v bbl

8. Only after this passes, add PPA publishing.
   PPA upload should build/upload a signed source package, but this .deb E2E still remains valuable because it validates the package behavior before release or before upload.

The CI shape I’d aim for is:

build-linux-cli-fixtures
-> e2e-linux-cli-cookbook

build-linux-deb
-> e2e-linux-cli-deb
-> publish-github-release-deb
-> publish-ppa-source

One important design decision: the .deb install layout should be stable before writing the E2E. If .deb installs packs under /usr/share/bbl/packs, then the CLI must know to
read from there. The current cookbook tests assume /root/.bbl/packs, so the .deb tests should be separate rather than reusing those assertions unchanged.
