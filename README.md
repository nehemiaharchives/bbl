# bbl
A command line tool to read Holy Bible

## Usage
In the command line, type ```bbl genesis 1```. Then you start to read Genesis chapter 1 in King James Version.
The text is hosted at [data](https://github.com/nehemiaharchives/bbl/tree/master/data) directory of this repository.
The program is internally using [ktor](https://github.com/ktorio/ktor) to fetch verses of a chapter of the Bible.
Command functionality is powered by [Clikt](https://github.com/ajalt/clikt).
It validates the input of the number of chapters of a book, emits error when you request more chapter than the book has.

Following abbreviation are accepted as command argument for book:
```
    genesis, gen, ge, gn
    exodus, ex, exod, exo
    leviticus, lev, le, lv
    numbers, num, nu, nm, nb
    deuteronomy, deut, de, dt
    joshua, josh, jos, jsh
    judges, judg, jdg, jg, jdgs
    ruth, rth, ru
    1st samuel, 1 sam, 1sam, 1sm, 1sa, 1s, 1 samuel, 1samuel, 1st sam, first samuel, first sam
    2nd samuel, 2 sam, 2sam, 2sm, 2sa, 2s, 2 samuel, 2ndsam, 2nd sam, second samuel, second sam
    1st kings, 1kings, 1 kings, 1kgs, 1 kgs, 1ki, 1k, 1stkgs, first kings, first kgs
    2nd kings, 2kings, 2 kings, 2kgs, 2 kgs, 2ki, 2k, 2ndkgs, second kings, second kgs
    1st chronicles, 1chronicles, 1 chronicles, 1chr, 1 chr, 1ch, 1stchr, 1st chr, first chronicles, first chr
    2nd chronicles, 2chronicles, 2 chronicles, 2chr, 2 chr, 2ch, 2ndchr, 2nd chr, second chronicles, second chr
    ezra, ezr, ez
    nehemiah, neh, ne
    esther, est, esth, es
    job, jb
    psalms, ps, psalm, pslm, psa, psm, pss
    proverbs, prov, pro, prv, pr
    ecclesiastes, eccles, eccle, ecc, ec, qoh
    song of solomon, song, song of songs, sos, so, canticle of canticles, canticles, cant
    isaiah, isa, is
    jeremiah, jer, je, jr
    lamentations, lam, la
    ezekiel, ezek, eze, ezk
    daniel, dan, da, dn
    hosea, hos, ho
    joel, jl
    amos, am
    obadiah, obad, ob
    jonah, jnh, jon
    micah, mic, mc
    nahum, nah, na
    habakkuk, hab, hb
    zephaniah, zeph, zep, zp
    haggai, hag, hg
    zechariah, zech, zec, zc
    malachi, mal, ml
    matthew, matt, mt
    mark, mrk, mar, mk, mr
    luke, luk, lk
    john, joh, jhn, jn
    acts, act, ac
    romans, rom, ro, rm
    1 corinthians, 1corinthians, 1 cor, 1cor, 1 co, 1co, 1st corinthians, first corinthians
    2 corinthians, 2corinthians, 2 cor, 2cor, 2 co, 2co, 2nd corinthians, second corinthians
    galatians, gal, ga
    ephesians, eph, ephes
    philippians, phil, php, pp
    colossians, col, co
    1 thessalonians, 1thessalonians, 1 thess, 1thess, 1 thes, 1thes, 1 th, 1th, 1st thessalonians, 1st thess, first thessalonians, first thess
    2 thessalonians, 2thessalonians, 2 thess, 2thess, 2 thes, 2thes, 2 th, 2th, 2nd thessalonians, 2nd thess, second thessalonians, second thess
    1 timothy, 1timothy, 1 tim, 1tim, 1 ti, 1ti, 1st timothy, 1st tim, first timothy, first tim
    2 timothy, 2timothy, 2 tim, 2tim, 2 ti, 2ti, 2nd timothy, 2nd tim, second timothy, second tim
    titus, tit, ti
    philemon, philem, phm, pm
    hebrews, heb
    james, jas, jm
    1 peter, 1peter, 1 pet, 1pet, 1 pe, 1pe, 1 pt, 1pt, 1p, 1st peter, first peter
    2 peter, 2peter, 2 pet, 2pet, 2 pe, 2pe, 2 pt, 2pt, 2p, 2nd peter, second peter
    1 john, 1john, 1 jhn, 1jhn, 1 jn, 1jn, 1j, 1st john, first john
    2 john, 2john, 2 jhn, 2jhn, 2 jn, 2jn, 2j, 2nd john, second john
    3 john, 3john, 3 jhn, 3jhn, 3 jn, 3jn, 3j, 3rd  john, third john
    jude, jud, jd
    revelation, rev, re, the revelation
```

## Installation
Linux Installation (Debian based only): 
1. Download bbl_1.0-1_amd64.deb from below
2. Run ```sudo apt install bbl_1.0-1_amd64.deb -y``` then 

Windows Installation:
1. Download installer from below and click Install button on ```bbl-1.0.msi```
2. Add folder where you put ```bbl.exe``` to your PATH by editing environmental variable setting.
