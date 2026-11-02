package hauntedcastle.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import hauntedcastle.character.DamageType;
import hauntedcastle.character.GameCharacter;
import hauntedcastle.character.monster.Ghost;
import hauntedcastle.character.monster.Monster;
import hauntedcastle.character.monster.Skeleton;
import hauntedcastle.character.monster.Slime;
import hauntedcastle.character.monster.Vampire;
import hauntedcastle.character.monster.Werewolf;
import hauntedcastle.character.hero.Fighter;
import hauntedcastle.character.hero.Mage;
import hauntedcastle.character.hero.PlayerCharacter;
import hauntedcastle.item.Armor;
import hauntedcastle.item.Item;
import hauntedcastle.item.Key;
import hauntedcastle.item.Potion;
import hauntedcastle.item.Spyglass;
import hauntedcastle.item.ThrownItem;
import hauntedcastle.item.Tome;
import hauntedcastle.item.Weapon;
import hauntedcastle.world.Exit;
import hauntedcastle.world.Room;

/**
 * Reads a world file. One command per line; underscores in names become spaces; lines starting
 * with '#' are comments. See README.md for the full format.
 */
public final class GameLoader {

    private static final Map<String, Function<String, Monster>> MONSTERS = Map.of(
            "VAMPIRE", Vampire::new,
            "SKELETON", Skeleton::new,
            "SLIME", Slime::new,
            "GHOST", Ghost::new,
            "WEREWOLF", Werewolf::new);

    private static final Map<String, Function<String, PlayerCharacter>> HEROES = Map.of(
            "FIGHTER", Fighter::new,
            "MAGE", Mage::new);

    private GameLoader() {
    }

    /** The world bundled inside the program (works from a jar). */
    public static final String DEFAULT_WORLD = "/worlds/hauntedcastle.txt";

    public static GameData loadGameData(File file) throws IOException {
        return loadGameData(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8), file.getName());
    }

    /** Loads a world from the classpath, e.g. {@link #DEFAULT_WORLD}. */
    public static GameData loadResource(String path) throws IOException {
        try (InputStream stream = GameLoader.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing built-in world " + path);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return loadGameData(reader.lines().toList(), path.substring(path.lastIndexOf('/') + 1));
        }
    }

    public static GameData loadGameData(List<String> lines, String source) throws IOException {
        GameData data = new GameData();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                parseLine(data, line.split("\\s+"));
            } catch (IOException e) {
                throw new IOException(source + " line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        if (data.getHeroes().isEmpty()) {
            throw new IOException(source + ": no FIGHTER or MAGE defined");
        }
        if (data.getGoal() != null && data.getCharacter(data.getGoal()) == null) {
            throw new IOException(source + ": GOAL names unknown character " + data.getGoal());
        }
        return data;
    }

    private static void parseLine(GameData data, String[] parts) throws IOException {
        String command = parts[0].toUpperCase(Locale.ROOT);
        switch (command) {
            case "ROOM" -> {
                expect(parts, 2, 2);
                data.addRoom(new Room(name(parts[1])));
            }
            case "DESC" -> {
                expect(parts, 3, Integer.MAX_VALUE);
                describe(data, name(parts[1]), String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
            }
            case "CONNECT" -> {
                expect(parts, 3, 3);
                Room.connectRooms(room(data, parts[1]), room(data, parts[2]));
            }
            case "LEADS_TO" -> {
                expect(parts, 3, 3);
                room(data, parts[1]).addExit(room(data, parts[2]));
            }
            case "LOCKED" -> {
                expect(parts, 4, 4);
                Room from = room(data, parts[1]);
                Room to = room(data, parts[2]);
                Room.connectRooms(from, to).lock(name(parts[3]));
                to.getExitTo(from).lock(name(parts[3]));
            }
            case "HIDDEN" -> {
                expect(parts, 4, 4);
                Exit exit = room(data, parts[1]).addExit(room(data, parts[2]));
                exit.hideUntil(name(parts[3]));
            }
            case "GOAL" -> {
                expect(parts, 2, 2);
                data.setGoal(name(parts[1]));
            }
            case "ITEM" -> {
                expect(parts, 3, 6);
                Item item = makeItem(name(parts[1]), Arrays.copyOfRange(parts, 3, parts.length));
                data.addItem(item);
                room(data, parts[2]).placeInRoom(item);
            }
            default -> {
                if (MONSTERS.containsKey(command)) {
                    expect(parts, 3, 3);
                    Monster monster = MONSTERS.get(command).apply(name(parts[1]));
                    data.addCharacter(monster);
                    room(data, parts[2]).placeInRoom(monster);
                } else if (HEROES.containsKey(command)) {
                    expect(parts, 3, 3);
                    data.addHero(HEROES.get(command).apply(name(parts[1])), room(data, parts[2]));
                } else {
                    throw new IOException("unknown command " + parts[0]);
                }
            }
        }
    }

    /**
     * ITEM name room [KEY | TOME | SPYGLASS | POTION amount | ARMOR reduction | THROWN damage
     * | WEAPON bonus [PHYSICAL|SILVER|MAGIC]]
     */
    private static Item makeItem(String name, String[] kind) throws IOException {
        if (kind.length == 0) {
            return new Item(name);
        }
        String type = kind[0].toUpperCase(Locale.ROOT);
        switch (type) {
            case "KEY":
                return new Key(name);
            case "TOME":
                return new Tome(name);
            case "SPYGLASS":
                return new Spyglass(name);
            case "POTION":
                if (kind.length != 2) {
                    throw new IOException("POTION needs an amount");
                }
                return new Potion(name, number(kind[1]));
            case "ARMOR":
                if (kind.length != 2) {
                    throw new IOException("ARMOR needs a damage reduction");
                }
                return new Armor(name, number(kind[1]));
            case "THROWN":
                if (kind.length != 2) {
                    throw new IOException("THROWN needs a damage amount");
                }
                return new ThrownItem(name, number(kind[1]));
            case "WEAPON":
                if (kind.length < 2 || kind.length > 3) {
                    throw new IOException("WEAPON needs a bonus and an optional damage type");
                }
                DamageType damageType = DamageType.PHYSICAL;
                if (kind.length == 3) {
                    try {
                        damageType = DamageType.valueOf(kind[2].toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new IOException("unknown damage type " + kind[2]);
                    }
                }
                return new Weapon(name, number(kind[1]), damageType);
            default:
                throw new IOException("unknown item type " + kind[0]);
        }
    }

    private static void describe(GameData data, String target, String text) throws IOException {
        Room room = data.getRoom(target);
        Item item = data.getItem(target);
        GameCharacter character = data.getCharacter(target);
        if (room != null) {
            room.setDescription(text);
        } else if (item != null) {
            item.setDescription(text);
        } else if (character != null) {
            character.setDescription(text);
        } else {
            throw new IOException("DESC for unknown room, item or character " + target);
        }
    }

    private static String name(String token) {
        return token.replace('_', ' ');
    }

    private static Room room(GameData data, String token) throws IOException {
        Room room = data.getRoom(name(token));
        if (room == null) {
            throw new IOException("unknown room " + token);
        }
        return room;
    }

    private static int number(String token) throws IOException {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IOException("expected a number, got " + token);
        }
    }

    private static void expect(String[] parts, int min, int max) throws IOException {
        if (parts.length < min || parts.length > max) {
            throw new IOException("wrong number of arguments for " + parts[0]);
        }
    }
}
