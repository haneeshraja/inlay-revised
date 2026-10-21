# Word list (word suggestions)

`app/src/main/res/raw/words_en.txt` is the English word list behind the keyboard's word
suggestions. It is rebuilt by `tools/build-wordlist.py`.

## Source and licence

- **Order of words:** word frequencies from [FrequencyWords](https://github.com/hermitdave/FrequencyWords)
  by Hermit Dave (the 2018 English list), which is built from
  [OpenSubtitles 2018](https://opus.nlpl.eu/OpenSubtitles.php) (data from
  [opensubtitles.org](https://www.opensubtitles.org)).
  The content is licensed **CC BY-SA 4.0**: <https://creativecommons.org/licenses/by-sa/4.0/>
- **Changes made:** kept the 20,000 most frequent words; removed words that a standard English
  dictionary does not know (names, typos, noise); removed offensive words; put common
  contractions back (the source splits "don't" into "don" and "'t").
- **Licence of the word list file:** CC BY-SA 4.0, same as the source. The rest of the app is
  under the licence in `LICENSE`. CC BY-SA 4.0 material can be used in a GPL-3.0 project.

## Used while building only (nothing from these is in the app)

- A Hunspell English dictionary (`en_US`), to check that a word is a real word.
- The "en" list from [LDNOOBW](https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words)
  (CC BY 4.0), to leave out offensive words.
