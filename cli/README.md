# bbl CLI
Here is the readme file of bbl CLI command line tool to read (and hopefully search) Holy Bible
Currently this software is work in progress and this readme will serve for development purposes.

# Overview
Previous version of tool bbl existed as Kotlin/JVM project. Now bbl is on the way to be integrated with bbl-kmp which is parent project of this project.
bbl-kmp has sub projects/modules such as ../composeApp (Android/iOS/Desktop JVM) and this cli and ../shared is the shared code between them, test-framework is code shared in tests.
project has 2 phases, "migrating bible reading features kotlin/jvm to kmp" and "lucene-kmp integration to develop search features"

# Current status of bbl CLI: 

## Phase 1: Kotlin/Jvm to KMP migration 
This phase focus on porting core reading features with features additional translation management

### 1. Translation management and list
  * 1.1. bbl list translation : DONE
  * 1.2. bbl list book : DONE
  * 1.3. bbl list category (NEW) : DONE
  * 1.4. bbl install $translation (NEW) : DONE
  * 1.5. bbl uninstall $translation (NEW) : DONE
  * 1.4. bbl install $translation1 $translation2 $translation3 ... (NEW) : DONE
  * 1.5. bbl uninstall $translation1 $translation2 $translation3 ... (NEW) : TODO

### 2. Bible reading
  * 2.1. bbl $bookName:$chapter  : DONE
  * 2.2. bbl $bookName:$chapter$startVerse : DONE
  * 2.3. bbl $bookName:$chapter$startVerse:$endVerse : DONE

### 3. Bible reading specifying translation
  * 2.1. bbl $bookName:$chapter in $translation : DONE
  * 2.2. bbl $bookName:$chapter$startVerse in $translation : DONE
  * 2.3. bbl $bookName:$chapter$startVerse:$endVerse in $translation : DONE
  * 2.4. bbl $bookName:$chapter in $translation1 $translation2 $translation3 ... : TODO
  * 2.5. bbl $bookName:$chapter$startVerse in $translation1 $translation2 $translation3 ... : TODO
  * 2.6. bbl $bookName:$chapter$startVerse:$endVerse in $translation1 $translation2 $translation3 ... : TODO

### 4. Random bible
  * 4.1. bbl rand : TODO
  * 4.2. bbl rand g (random verse within 4 gospels) : DONE
  * 4.3. bbl rand nt (random verse within New Testament) : DONE
  * 4.4. bbl rand ot (random verse within Old Testament) : DONE
  * 4.5. bbl rand $category (NEW) : DONE

### 5. Configuration
  * 5.1. bbl init (NEW): DONE
  * 5.2. bbl config key (NEW) : DONE
  * 5.3. bbl config key value (NEW) : DONE
  * 5.4. option to change default translation (NEW) : DONE
  * 5.5. option to show a whole chapter or a verse in bbl rand (NEW) : DONE
  * 5.6. option to show/hide header (NEW) : DONE

## Phase 2: Search/Index 
This phase focus on finish porting lucene-kmp needed in bbl-kmp while debugging lucene-kmp to enable bbl pack to create search index, then achieving bbl search in single binary Kotlin/Native

### 6. Search related features 
  * 6.1. bbl pack $translationSourceDir (build lucene index, expected a lot of debugging for lucene-kmp, port many language specific lucene Analyzers into kmp) : TODO
  * 6.2. bbl search $query (expected a lot of debugging for lucene-kmp) : TODO
  * 6.3. bbl search $query in $translation : TODO
  * 6.4. bbl search $query in $book : TODO
  * 6.5. bbl search $query in $book in $translation : TODO
  * 6.6. bbl search $query in $book:$chapter : TODO
  * 6.7. bbl search $query in $book:$chapter in $translation : TODO
  * 6.8. bbl search $query in $book:$chapter:$startVerse-$endVerse : TODO
  * 6.9. bbl search $query in $book:$chapter:$startVerse-$endVerse in $translation : TODO
