* bbl is moved from Kotlin/JVM to Kotlin/Native to produce **single binary**
* bbl reading commands will be single binary executable + bbl pack zip files contains text and lucene index, and bbl search command will be dispatched to search specific single binary produced for each language(s) according to lucene language analyzer library boundary
* installs only one default bible translation `webus` (World English Bible) with one default search binary `bbl-search-common`, then users run `bbl list` to see what's available for download, then installs additional bbl packs by `bbl install kjv` just like package manager, `bbl uninstall kjv` also supported. This is because we increased number of languages/bible translations significantly, and having single fat installation over 300 MB with many unused languages does not make sense.
* bbl packs will be stored at `$HOME/.bbl/packs/webus.zip` and search binaries will be stored at `$HOME/.bbl/bin/bbl-search-common`
* search binaries are installed automatically as dependency of bbl packs, and executed as background process, so users will not see/use them, they are split into multiple just for reducing disk space.
* search engine library switched from [Apache Lucene](https://github.com/apache/lucene) to [lucene-kmp](https://github.com/nehemiaharchives/lucene-kmp) which was ported from Java to KMP **just for bbl**. bbl will **dogfood** lucene-kmp and both will be improved together

## New features
- `bbl john 3:16 in es` - specify translation by iso 2 letter language code e.g.  `es`, `de`, `fr`  or language names e.g. `Spanish`, `German`, `French`
- `bbl john 3:16 in es de fr` - compare multiple translation by space separated list of translation code, language code, language names, the display layout can be changed by verse or by translation via `bbl config compareBy verse` and `bbl config compareBy block`
- `bbl search Jesus Christ in en es de fr` - compare search results of `en` bible with other languages
- `bbl list categories` and `bbl search [term] in [category code]` to narrow down search result by e.g. `nt`, `ot`, `prophets`, `gospels`, `paul`, `johns writings`
- `bbl history` to see command history. can be filtered only reading/search/config history by `bbl history read`, `bbl history search`, `bbl history config`, history file is in `$HOME/.bbl/history.json`, `bbl config historyEnabled false` to disable recording history, `bbl config historyFormat [format]` to format in `command`, `commandDatetime`, `commandDatetimeTimezone`
- `bbl config header true` to show book name and chapter name on top of currently reading text output
- `bbl config [key] to show current config value. `bbl config [key] [value]` to change config value of a key
- `bbl s Jesus` as shortcut of `bbl search Jesus`, in other way `r` for `rand`, `h` for `history`, `ls` for `list`, `c` for `config`

## Languages supported:
```
WEBUS  | World English Bible               | World English Bible              | English    | 2000 | Available | Public Domain
KJV    | King James Version                | King James Version               | English    | 1611 | Available | Public Domain
RVR09  | Reina-Valera                      | Reina-Valera                     | Spanish    | 1909 | Available | Public Domain
TB     | Brazilian Translation             | Tradução Brasileira              | Portuguese | 1917 | Available | Public Domain
DELUT  | Luther Bible                      | Lutherbibel                      | German     | 1912 | Available | Public Domain
LSG    | Louis Segond                      | Bible Segond                     | French     | 1910 | Available | Public Domain
SINOD  | Russian Synodal Bible             | Синодальный перевод              | Russian    | 1876 | Available | Public Domain
SVRJ   | Statenvertaling Jongbloed edition | Statenvertaling Jongbloed-editie | Dutch      | 1888 | Available | Public Domain
RDV24  | Revised Diodati Version           | Versione Diodati Riveduta        | Italian    | 1924 | Available | Public Domain
UBG    | Updated Gdansk Bible              | Uwspółcześniona Biblia gdańska   | Polish     | 2017 | Available | © 2017 Fundacja Wrota Nadziei (Non-commercial)
UBIO   | Ukrainian Bible, Ivan Ogienko     | Біблія в пер. Івана Огієнка      | Ukrainian  | 1962 | Available | CC BY-SA 4.0 © 1962 Українське Біблійне Товариство
SVEN   | Svenska 1917                      | 1917 års kyrkobibel              | Swedish    | 1917 | Available | Public Domain
CUNP   | Chinese Union Version             | 新標點和合本                     | Chinese    | 1919 | Available | Public Domain
KRV    | Korean Revised Version            | 개역한글                         | Korean     | 1961 | Available | Public Domain
JC     | Japanese Colloquial Bible         | 口語訳                           | Japanese   | 1955 | Available | Public Domain
ABTAG  | Ang Biblia                        | Ang Biblia                       | Tagalog    | 1905 | Available | Public Domain
AYT    | The Opened Bible                  | Alkitab Yang Terbuka             | Indonesian | 2024 | Available | CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT
KTTV   | Vietnamese Bible 1925             | Kinh Thánh Tiếng Việt            | Vietnamese | 1925 | Available | Public Domain
TH1971 | Thai Bible 1925                   | พระคริสตธรรมคัมภีร์ ฉบับ1971          | Thai       | 1971 | Available | Public Domain
IRVHIN | Indian Revised Version - Hindi    | इंडियन रिवाइज्ड वर्जन - हिंदी             | Hindi      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVBEN | Indian Revised Version - Bengali  | ইন্ডিয়ান রিভাইজড ভার্সন - বেঙ্গলী         | Bengali    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVMAR | Indian Revised Version - Marathi  | इंडियन रीवाइज्ड वर्जन - मराठी            | Marathi    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVTEL | Indian Revised Version - Telugu   | ఇండియన్ రివైజ్డ్ వెర్షన్ - తెలుగు         | Telugu     | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVTAM | Indian Revised Version - Tamil    | இண்டியன் ரிவைஸ்டு வெர்ஸன் - தமிழ்    | Tamil      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVGUJ | Indian Revised Version - Gujarati | ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી           | Gujarati   | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
IRVURD | Indian Revised Version - Urdu     | इंडियन रिवाइज्ड वर्जन - उर्दू              | Urdu       | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions
NPIULB | Nepali Unlocked Literal Bible     | पवित्र बाइबल                        | Nepali     | 2019 | Available | CC BY-SA 4.0 © 2019 Door43 World Missions Community
```

## Supported platforms
- Ubuntu/Debian family: [deb](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-amd64.deb) x64
- Debian arm64, Raspberry Pi, Nvidia DGX Spark: [deb](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-arm64.deb) arm64
- Fedora and other RHEL family: [rpm](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-x86_64.rpm) x64
- Arch Linux: [pkg.tar.zst](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-x86_64.pkg.tar.zst) x64
- NixOS: [flake](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-x64-nix-flake.tar.gz) x64
- Alpine Linux: [apk](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-x86_64.apk) x64, [apk](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-linux-aarch64.apk) arm64
- Apple Silicon: [pkg](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-macos-arm64.pkg) arm64
- Intel Mac: [pkg](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-v2.0-macos-x64.pkg) x64
- Windows: [msi](https://github.com/nehemiaharchives/bbl/releases/download/v2.0/bbl-2.0-windows-x64.msi) x64