# Impostata

A retro pixel-style **impostor / undercover party game** for one shared phone.
Everyone gets a secret word, except the impostors. Talk about your word without
giving it away, find out who doesn't know it, and vote them out.

**[▶ Play in the browser](https://icedpotat.github.io/projects/impostata/)** ·
**[⬇ Download the Android APK](https://github.com/icedpotat/Impostata/releases/latest/download/Impostata.apk)** ·
[All releases](https://github.com/icedpotat/Impostata/releases)

> The game is in German and ships with more than 1,400 German word pairs. You can import your own word list in any language.

---

## How to play

1. **Set up:** pick 3–20 players (or a saved group) and choose how many impostors and undercovers to include.
2. **Pass the phone:** each player looks at their card in secret, then passes the phone on.
3. **Talk:** a random player starts, and the round goes clockwise or counter-clockwise. Everyone describes their word in one hint.
4. **Vote:** vote out whoever you suspect. Their role is revealed.
5. **Last chance:** a voted-out impostor can guess the crew word. If they get it right, the impostors win.

### Roles

| Role | Sees | Goal |
|---|---|---|
| **Crew** | The word | Find and vote out the impostors |
| **Undercover** | A *similar* word, and doesn't know they're undercover | Survive; usually they think they're crew |
| **Impostor** | Nothing but "Du bist der Impostor." | Blend in, or guess the crew word |
| **Jester** | The crew word plus their role | Get voted out **first**, which wins alone |

### Chaos modes

In the settings you can give each game a small chance to turn into a special round. Nobody is warned.

- **No impostors:** everyone is crew. How long until someone gets suspicious?
- **All impostors:** nobody has a word.
- **One crew:** a single crew member against everyone else.
- **Jester:** one crew member becomes the jester.

### Reveal modes

- **Klick:** tap the card to flip it.
- **Halten:** the word is visible only while you hold the card.
- **Wählen:** each player picks a face-down card from a grid.

## Features

- Pixel-art look with the *Press Start 2P* font
- Saved player groups, so you don't retype names every game
- Word pairs don't repeat until all of them have been played
- Import your own words from `.txt` / `.csv`, one pair per line:
  ```
  Pizza,Burger
  Strand,See
  ```
  The first word goes to the crew, the second to the undercovers. `;` also works as a separator. See [`docs/example-words.csv`](docs/example-words.csv).
- Works fully offline, with no ads, tracking or account

## Install on Android

1. Download [`Impostata.apk`](https://github.com/icedpotat/Impostata/releases/latest/download/Impostata.apk) on your phone.
2. Open it and allow installing from your browser or file manager when Android asks.
3. Requires Android 11 (API 30) or newer.

## Web version

The [browser version](https://icedpotat.github.io/projects/impostata/) is a JavaScript port with the same rules and settings. It runs on any phone or computer and stores everything locally. For offline use, download it from the [downloads page](https://icedpotat.github.io/downloads/).

## Building from source

Requirements: Android Studio (or JDK 17 + Android SDK 35).

```bash
git clone https://github.com/icedpotat/Impostata.git
cd Impostata
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### Releases

Pushing a tag like `v1.1` runs [`.github/workflows/apk.yml`](.github/workflows/apk.yml). It builds the APK and attaches it to a GitHub release as `Impostata.apk`. You can also start the workflow manually from the *Actions* tab.

### Project layout

```
app/src/main/
├── assets/word_pairs.txt        built-in word list (crew,undercover)
├── java/com/freeze/impostata/
│   ├── GameLogic.kt             roles, chaos modes, win conditions, word pool
│   ├── GameFragment.kt          setup → reveal → vote → result screens
│   ├── RevealManager.kt         click / hold reveal
│   ├── GroupActivity.kt         saved player groups
│   └── model/                   Player, Role, Group, WordPair
└── res/                         pixel layouts, drawables, font
```

Contributions are welcome. New word pairs (one `crew,undercover` per line in `word_pairs.txt`) are the easiest place to start.

## License

[MIT](LICENSE). The *Press Start 2P* font is licensed separately under the SIL Open Font License 1.1.
