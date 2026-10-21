#!/usr/bin/env python3
"""
Builds app/src/main/res/raw/words_en.txt, the small English word list behind the
word suggestions on the keyboard.

  python3 -I tools/build-wordlist.py FREQ_FILE AFF_FILE DIC_FILE BAD_WORDS_FILE OUT_FILE [COUNT]

  FREQ_FILE       en_50k.txt from github.com/hermitdave/FrequencyWords (content/2018/en).
                  Lines look like "word 12345", most frequent first.
  AFF_FILE / DIC_FILE
                  A Hunspell English dictionary (en_US.aff / en_US.dic). Used only to throw
                  out names, typos and subtitle noise: a word is kept if the dictionary
                  knows it.
  BAD_WORDS_FILE  One word or phrase per line, e.g. the "en" file from
                  github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words.
                  Only used to leave words out. Nothing from it is copied into the result.
  COUNT           How many words to keep (default 20000).

The result is the words in order of how often people say them, one per line. Lines starting
with # are comments. The frequency data is CC BY-SA 4.0, so the file keeps its credit header,
see licenses/WORD_LIST.md.
"""
import re
import sys

HEADER = """\
# English word list for Inlay's word suggestions. One word per line, most common first.
# Order comes from word frequencies in the FrequencyWords project by Hermit Dave
# (https://github.com/hermitdave/FrequencyWords, built from OpenSubtitles 2018,
# https://www.opensubtitles.org), licensed CC BY-SA 4.0
# (https://creativecommons.org/licenses/by-sa/4.0/).
# Changes: kept the most frequent words, removed anything a standard English dictionary does
# not know (names, typos, noise), removed offensive words and put common contractions back
# (the source splits "don't" into "don" + "'t"). Made by tools/build-wordlist.py.
# This file is shared under the same CC BY-SA 4.0 licence.
"""


def read_affixes(path):
    """Hunspell prefix / suffix rules: {flag: (cross_product, [(strip, add, condition_regex)])}."""
    pfx, sfx = {}, {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            parts = line.split()
            if len(parts) < 4 or parts[0] not in ("PFX", "SFX"):
                continue
            table = pfx if parts[0] == "PFX" else sfx
            flag = parts[1]
            if len(parts) == 4 and parts[3].isdigit():          # header: PFX A Y 3
                table[flag] = (parts[2] == "Y", [])
                continue
            strip = "" if parts[2] == "0" else parts[2]
            add = parts[3].split("/")[0]
            add = "" if add == "0" else add
            cond = parts[4] if len(parts) > 4 else "."
            if flag not in table:
                table[flag] = (True, [])
            regex = re.compile((cond + "$") if table is sfx else ("^" + cond))
            table[flag][1].append((strip, add, regex))
    return pfx, sfx


def apply_rules(word, rules, suffix):
    out = []
    for strip, add, regex in rules:
        if not regex.search(word):
            continue
        if suffix:
            if strip and not word.endswith(strip):
                continue
            out.append(word[: len(word) - len(strip)] + add)
        else:
            if strip and not word.startswith(strip):
                continue
            out.append(add + word[len(strip):])
    return out


def known_words(aff_path, dic_path):
    """Every lower-case word form the dictionary can produce."""
    pfx, sfx = read_affixes(aff_path)
    words = {"i", "a"}
    with open(dic_path, encoding="utf-8", errors="replace") as f:
        next(f, None)                                           # first line is a count
        for line in f:
            entry = line.strip().split()[0] if line.strip() else ""
            if not entry:
                continue
            root, _, flags = entry.partition("/")
            if not root or any(c.isupper() for c in root):      # skip names and abbreviations
                continue
            forms = {root}
            suffixed = []
            for flag in flags:
                if flag in sfx:
                    cross, rules = sfx[flag]
                    for form in apply_rules(root, rules, True):
                        forms.add(form)
                        if cross:
                            suffixed.append(form)
            for flag in flags:
                if flag in pfx:
                    cross, rules = pfx[flag]
                    for form in apply_rules(root, rules, False):
                        forms.add(form)
                    if cross:
                        for s in suffixed:
                            forms.update(apply_rules(s, rules, False))
            words.update(w for w in forms if w.isalpha() or "'" in w)
    return words


CONTRACTION = re.compile(r"^([a-z]+?)(n't|'s|'m|'re|'ve|'ll|'d)$")
ODD_CONTRACTIONS = {"can't", "won't", "shan't", "ain't", "y'all"}
STEMS = ("fuck", "shit", "bitch", "cunt", "whore", "slut")

# The subtitle data splits "didn't" into "didn" + "'t". These stems stand for their contraction.
NOT_STEMS = {"don", "didn", "doesn", "isn", "wasn", "aren", "weren", "couldn", "wouldn",
             "shouldn", "hasn", "haven", "hadn", "mustn"}

# Contractions that follow the word they are built on, so they rank next to it.
AFTER = {
    "i": ["i'm", "i'll", "i've", "i'd"],
    "it": ["it's", "it'll"],
    "that": ["that's", "that'll"],
    "he": ["he's", "he'll", "he'd"],
    "she": ["she's", "she'll", "she'd"],
    "there": ["there's"],
    "what": ["what's"],
    "who": ["who's"],
    "here": ["here's"],
    "let": ["let's"],
    "you": ["you're", "you'll", "you've", "you'd"],
    "we": ["we're", "we'll", "we've", "we'd"],
    "they": ["they're", "they'll", "they've", "they'd"],
    "can": ["can't"],
    "will": ["won't"],
}


def acceptable(word, known, bad):
    if word in bad or word.startswith(STEMS):
        return False
    if len(word) == 1:
        return word in ("a", "i")
    if "'" in word:
        if word in ODD_CONTRACTIONS:
            return True
        m = CONTRACTION.match(word)
        return bool(m) and (m.group(1) in known or m.group(1) in ("do", "does", "did", "is", "are", "was", "were",
                                                                    "has", "have", "had", "would", "could", "should",
                                                                    "must", "might", "need"))
    return word in known


def main():
    if len(sys.argv) < 6:
        sys.exit(__doc__)
    freq, aff, dic, bad_path, out = sys.argv[1:6]
    count = int(sys.argv[6]) if len(sys.argv) > 6 else 20000

    known = known_words(aff, dic)
    with open(bad_path, encoding="utf-8", errors="replace") as f:
        bad = {line.strip().lower() for line in f if line.strip()}

    kept, seen = [], set()
    word_re = re.compile(r"^[a-z]+(?:'[a-z]+)?$")
    with open(freq, encoding="utf-8", errors="replace") as f:
        for line in f:
            word = line.split(" ")[0].strip()
            if not word_re.match(word) or word in seen:
                continue
            seen.add(word)
            if word in NOT_STEMS:
                kept.append(word + "'t")
            elif acceptable(word, known, bad):
                kept.append(word)
                for extra in AFTER.get(word, []):
                    if extra not in seen:
                        seen.add(extra)
                        kept.append(extra)
            if len(kept) >= count:
                break

    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADER)
        f.write("\n".join(kept) + "\n")
    print(f"{len(known)} dictionary forms, kept {len(kept)} words -> {out}")


if __name__ == "__main__":
    main()
