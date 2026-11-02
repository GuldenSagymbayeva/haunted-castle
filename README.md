# The Haunted Castle

A turn-based text adventure in plain Java. Explore a cursed castle, fight monsters with
their own tricks, find keys, secret passages and a silver dagger, and destroy the vampire
Count Orlok to break the curse.

Started as a university OOP lab (rooms, characters and items loaded from a text file) and
grown into a complete, replayable game.

```
== Crypt ==
Rows of stone coffins. The largest one stands open... and empty.
DANGER: Count Orlok the Vampire (70/70 HP)
Exits:  [1] dungeon (locked)  [2] secret passage
Count Orlok rises from the coffin. "At last... a guest for dinner."

> attack
You strike with the silver dagger at Count Orlok! CRITICAL HIT!
  Count Orlok takes 34 damage (36/70 HP).
Count Orlok bites you!
Count Orlok drinks your blood and recovers 4 health.
  You take 9 damage (41/60 HP).
```

## Run it

Needs Java 17 or newer (JDK).

```sh
./run.sh            # macOS / Linux / Git Bash
run.bat             # Windows
```

Or by hand:

```sh
javac -d out $(find src -name '*.java')
java -cp out Main                     # default world
java -cp out Main --seed 42           # repeatable dice rolls
java -cp out Main worlds/my_world.txt # your own world
```

## How to play

Pick a hero:

| Hero | HP | Style |
|------|----|-------|
| Sir Roderick the Fighter | 60 | Sword blows with a 20% chance of a double-damage critical hit. |
| Elowen the Mage | 45 | Magic fireballs (6 mana each, mana refills 2 per turn); weak staff when out of mana. |

Commands (names can be shortened: `go lib`, `take tome`, or just type `2` or `library`):

| Command | Effect |
|---------|--------|
| `look`, `look <thing>` | Describe the room, or examine an item, monster or `me` |
| `go <room or number>`, `back` | Move through an exit / return the way you came |
| `take <item>`, `take all` | Pick things up |
| `use <item>` (`read`, `drink`, `wield`) | Use an item |
| `attack [monster]` | Fight |
| `inventory`, `stats`, `map` | Check yourself and the castle |
| `wait`, `help`, `quit` | |

Rules that matter:

- Every turn you stay in a room with a monster, it strikes back. Leaving gives each monster a 50% chance of a parting blow.
- Monsters guard the items in their room.
- **Skeleton** pulls itself back together once after ordinary blows. Silver or magic stops that.
- **Slime** hits harder the bigger it is.
- **Ghost**: ordinary blows pass through half the time.
- **Vampire** heals by drinking your blood, and only silver or magic can deliver the final blow.
- Score = 500 + 100 per monster vanquished + 5 per HP left - 5 per turn.

There is more than one way into the crypt. Some are safer than others.

## Make your own world

Worlds are plain text, one command per line. Underscores become spaces; `#` starts a comment.

```
ROOM <room>
DESC <room|item|character> <free text...>
CONNECT <room> <room>                # two-way path
LEADS_TO <from> <to>                 # one-way path
LOCKED <room> <room> <key item>      # two-way path opened by a key
HIDDEN <from> <to> <item>            # one-way path revealed by using an item
FIGHTER <name> <start room>          # playable heroes (at least one)
MAGE <name> <start room>
VAMPIRE | SKELETON | SLIME | GHOST <name> <room>
GOAL <character>                     # win when it is vanquished (default: clear every monster)
ITEM <name> <room> [KEY | TOME | SPYGLASS | POTION <amount> | WEAPON <bonus> [PHYSICAL|SILVER|MAGIC]]
```

See [`worlds/hauntedcastle.txt`](worlds/hauntedcastle.txt). Load errors report the file and line number.

## Code tour

```
src/
  Main.java                    command-line entry, play-again loop
  game/        Game            turn loop, commands, combat, narration
               GameLoader      world-file parser
               GameData        loaded rooms, characters, items, heroes, goal
               Lookup          forgiving name matching for player input
  room/        Room, Exit      rooms; exits can be locked or hidden
  item/        Item + Key, Potion, Weapon, Tome, Spyglass   (polymorphic use())
  game_character/
               GameCharacter, Attack, DamageType
               monster/            Monster + Vampire, Skeleton, Slime, Ghost
               player_character/   PlayerCharacter + Fighter, Mage
test/GameTests.java            dependency-free tests
```

Each monster's special rule lives in its own `takeHit` / `attack` / `afterDealing` override, so a
new monster is one small class plus one line in `GameLoader`.

## Tests

```sh
javac -d out $(find src test -name '*.java') && java -cp out GameTests
```
