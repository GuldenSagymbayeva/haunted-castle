package hauntedcastle.game;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;
import hauntedcastle.character.GameCharacter;
import hauntedcastle.character.hero.Mage;
import hauntedcastle.character.hero.PlayerCharacter;
import hauntedcastle.character.monster.Monster;
import hauntedcastle.character.monster.Vampire;
import hauntedcastle.item.Armor;
import hauntedcastle.item.Item;
import hauntedcastle.item.Key;
import hauntedcastle.item.UseResult;
import hauntedcastle.item.Weapon;
import hauntedcastle.world.Exit;
import hauntedcastle.world.Room;

/** One play-through: reads commands, runs turns and narrates what happens. */
public class Game {

    public enum Outcome { WON, DIED, QUIT }

    /**
     * How a game is set up. The seed drives every dice roll, which is what makes saving by
     * replay possible. A null save file disables saving.
     */
    public record Settings(Difficulty difficulty, long seed, Style style, Path saveFile) {

        public static Settings forTest(long seed) {
            return new Settings(Difficulty.NORMAL, seed, Style.plain(), null);
        }
    }

    /** How a command affects the turn clock. */
    private enum Turn {
        /** No time passes (looking, help, failed actions). */
        FREE,
        /** Time passes and monsters in the room strike back. */
        TAKEN,
        /** The player changed rooms; monsters already had their parting strike. */
        MOVED
    }

    private static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());

    private final GameData data;
    private final Scanner in;
    private final PrintStream console;
    private final Settings settings;
    private final Style style;
    private final Random rng;

    private PrintStream out;
    private PlayerCharacter player;
    private int heroIndex;
    private Room room;
    private Room previousRoom;
    private final Set<Room> visited = new LinkedHashSet<>();
    private final List<String> history = new ArrayList<>();
    private boolean mapRevealed;
    private boolean quit;
    private int turns;
    private int kills;

    public Game(GameData data, Scanner in, PrintStream out, Settings settings) {
        this.data = data;
        this.in = in;
        this.console = out;
        this.out = out;
        this.settings = settings;
        this.style = settings.style();
        this.rng = new Random(settings.seed());
    }

    public void say(String text) {
        out.println(text);
    }

    public PlayerCharacter getPlayer() {
        return player;
    }

    public Room getCurrentRoom() {
        return room;
    }

    /** Starts a new game: story, hero choice, then the command loop. */
    public Outcome play() {
        intro();
        if (!chooseHero()) {
            return Outcome.QUIT;
        }
        enter(room);
        return loop();
    }

    /** Rebuilds a saved game by silently replaying its commands, then continues the command loop. */
    public Outcome resume(SaveGame save) {
        List<PlayerCharacter> heroes = heroes();
        if (save.hero() < 0 || save.hero() >= heroes.size()) {
            say("The save file does not match this castle. Starting over is the only way.");
            return Outcome.QUIT;
        }
        selectHero(save.hero());
        out = SILENT;
        visited.add(room);
        try {
            for (String command : save.commands()) {
                Outcome outcome = step(command);
                if (outcome != null) {
                    out = console;
                    say("The save file does not match this castle. Starting over is the only way.");
                    return Outcome.QUIT;
                }
            }
        } finally {
            out = console;
        }
        say(style.title("\nWelcome back, " + player.getName() + ". (" + settings.difficulty().label() + ")"));
        describeRoom();
        return loop();
    }

    private Outcome loop() {
        while (true) {
            out.print("\n" + style.bold("> "));
            String line = readLine();
            if (line == null) {
                return Outcome.QUIT;
            }
            if (line.isEmpty()) {
                continue;
            }
            Outcome outcome = step(line);
            if (outcome != null) {
                return outcome;
            }
        }
    }

    /** Runs one command. Returns the outcome if the game ended, otherwise null. */
    private Outcome step(String line) {
        String command = line.toLowerCase(Locale.ROOT);
        if (!isMetaCommand(command)) {
            history.add(command);
        }
        Turn turn = handle(command);
        if (quit) {
            return Outcome.QUIT;
        }
        if (goalReached()) {
            victory();
            return Outcome.WON;
        }
        if (turn != Turn.FREE) {
            endTurn(turn == Turn.TAKEN);
        }
        if (player.isVanquished()) {
            defeat();
            return Outcome.DIED;
        }
        return null;
    }

    /** Commands that are not part of the game world and so are not replayed from a save. */
    private static boolean isMetaCommand(String command) {
        String verb = command.split("\\s+", 2)[0];
        return verb.equals("save") || verb.equals("quit") || verb.equals("exit") || verb.equals("q");
    }

    // ---------------------------------------------------------------- setup

    private void intro() {
        say("");
        say(style.title("============================================="));
        say(style.title("              THE HAUNTED CASTLE"));
        say(style.title("============================================="));
        say("A storm drove you to the castle gates. The doors slammed");
        say("shut behind you, and something in the dark began to laugh.");
        GameCharacter goal = data.getGoal() == null ? null : data.getCharacter(data.getGoal());
        if (goal != null) {
            say("Legend says the curse breaks only when " + style.danger(goal.getName()) + " is destroyed.");
        } else {
            say("Destroy every monster in the castle to break the curse.");
        }
        say(style.dim("Difficulty: " + settings.difficulty().label()
                + ". Type 'help' for commands, 'hint' if you are stuck."));
    }

    private boolean chooseHero() {
        List<PlayerCharacter> heroes = heroes();
        if (heroes.size() == 1) {
            selectHero(0);
        } else {
            say("\nChoose your hero:");
            for (int i = 0; i < heroes.size(); i++) {
                PlayerCharacter hero = heroes.get(i);
                say("  [" + (i + 1) + "] " + style.bold(hero.getName() + " the " + hero.getTitle())
                        + " (HP " + hero.getMaxHealth() + ") - " + hero.getDescription());
            }
            while (player == null) {
                out.print(style.bold("> "));
                String answer = readLine();
                if (answer == null) {
                    return false;
                }
                PlayerCharacter chosen = pick(heroes, answer, PlayerCharacter::getName);
                if (chosen == null) {
                    say("Type a number from 1 to " + heroes.size() + ".");
                } else {
                    selectHero(heroes.indexOf(chosen));
                }
            }
        }
        say("\nYou are " + style.bold(player.getName() + " the " + player.getTitle()) + ".");
        return true;
    }

    private List<PlayerCharacter> heroes() {
        return new ArrayList<>(data.getHeroes().keySet());
    }

    private void selectHero(int index) {
        heroIndex = index;
        player = heroes().get(index);
        room = data.getHeroes().get(player);
    }

    // ---------------------------------------------------------------- command dispatch

    private Turn handle(String line) {
        String[] words = line.split("\\s+", 2);
        String verb = words[0];
        String arg = words.length > 1 ? words[1] : "";
        switch (verb) {
            case "help", "h", "?":
                help();
                return Turn.FREE;
            case "hint":
                hint();
                return Turn.FREE;
            case "look", "l":
                if (arg.isEmpty()) {
                    describeRoom();
                } else {
                    examine(arg);
                }
                return Turn.FREE;
            case "examine", "x", "inspect":
                examine(arg);
                return Turn.FREE;
            case "go", "move", "walk", "enter":
                return go(arg);
            case "back", "b", "flee", "run", "retreat":
                return back();
            case "take", "get", "grab", "pick":
                return take(arg.startsWith("up ") ? arg.substring(3) : arg);
            case "use", "read", "drink", "equip", "wield", "wear", "throw", "unlock":
                return use(arg);
            case "attack", "a", "fight", "hit", "kill", "cast":
                return attack(arg);
            case "inventory", "inv", "i":
                inventory();
                return Turn.FREE;
            case "stats", "status", "s":
                stats();
                return Turn.FREE;
            case "map", "m":
                map();
                return Turn.FREE;
            case "wait", "rest", "z":
                say("You catch your breath.");
                return Turn.TAKEN;
            case "save":
                save();
                return Turn.FREE;
            case "quit", "exit", "q":
                quitGame();
                return Turn.FREE;
            default:
                // Let "library" or "2" work as a shortcut for "go library".
                if (findExit(line) != null) {
                    return go(line);
                }
                say("I don't understand '" + line + "'. Type 'help' for commands.");
                return Turn.FREE;
        }
    }

    private void help() {
        say(style.bold("Commands:"));
        say("  look / l              describe the room");
        say("  look <thing>          examine an item, monster or yourself");
        say("  go <room|number>      walk through an exit (or just type the room name)");
        say("  back                  return to the previous room");
        say("  take <item> / take all");
        say("  use <item>            drink, read, wield, wear, throw or unlock with an item");
        say("  attack [monster]      fight a monster in the room");
        say("  inventory / i         list what you carry");
        say("  stats / s             your health and progress");
        say("  map / m               rooms you know about");
        say("  hint                  a nudge in the right direction");
        say("  wait                  let a turn pass");
        say("  save / quit           save your progress / leave the game");
        say(style.dim("Monsters strike back every turn you stay, may hit you as you leave,"));
        say(style.dim("and guard the items in their room. Names can be shortened: 'go lib'."));
    }

    // ---------------------------------------------------------------- looking

    private void describeRoom() {
        say("\n" + style.title("== " + capitalize(room.getName()) + " =="));
        if (!room.getDescription().isEmpty()) {
            say(room.getDescription());
        }
        List<Item> items = room.getItems();
        if (!items.isEmpty()) {
            say("You see: " + style.item(names(items)) + ".");
        }
        for (Monster monster : room.getMonsters()) {
            if (monster.isVanquished()) {
                say(style.dim("The remains of " + monster.getName() + " lie here."));
            } else {
                say(style.danger("DANGER: " + monster.getName() + " the " + monster.getKind()) + " " + health(monster));
            }
        }
        List<Exit> exits = room.getVisibleExits();
        StringBuilder line = new StringBuilder("Exits:");
        for (int i = 0; i < exits.size(); i++) {
            Exit exit = exits.get(i);
            line.append("  [").append(i + 1).append("] ").append(exit.getTarget().getName());
            if (exit.isLocked()) {
                line.append(" (locked)");
            }
        }
        say(exits.isEmpty() ? "There is no way out." : line.toString());
    }

    private void examine(String arg) {
        if (arg.isEmpty()) {
            say("Look at what?");
            return;
        }
        if (arg.equals("me") || arg.equals("self") || arg.equals("myself")) {
            stats();
            return;
        }
        GameEntity thing = Lookup.find(room.getItems(), arg);
        if (thing == null) {
            thing = Lookup.find(player.getInventory(), arg);
        }
        if (thing == null) {
            thing = Lookup.find(room.getMonsters(), arg);
        }
        if (thing == null) {
            Exit exit = findExit(arg);
            if (exit != null) {
                Room target = exit.getTarget();
                say(visited.contains(target) ? target.getDescription()
                        : "You have not been to the " + target.getName() + " yet.");
                return;
            }
            say("You see no '" + arg + "' here.");
            return;
        }
        say(style.bold(capitalize(thing.getName())) + ": " + thing.getDescription());
        if (thing instanceof Monster monster) {
            say("  " + monster.getKind() + (monster.isUndead() ? ", undead" : "") + " " + health(monster));
        }
    }

    private void inventory() {
        List<Item> items = player.getInventory();
        if (items.isEmpty()) {
            say("You carry nothing.");
            return;
        }
        say("You carry:");
        for (Item item : items) {
            String note = item == player.getWeapon() ? " (wielded)" : item == player.getArmor() ? " (worn)" : "";
            say("  - " + style.item(item.getName()) + note);
        }
    }

    private void stats() {
        say(player.status() + " " + style.bar(player.getHealth(), player.getMaxHealth()));
        say("Turns: " + turns + "   Monsters vanquished: " + kills + "   Rooms explored: "
                + visited.size() + "/" + data.getRooms().size()
                + "   Difficulty: " + settings.difficulty().label());
    }

    private void map() {
        say(style.bold("Castle map:"));
        int unknown = 0;
        for (Room r : data.getRooms()) {
            boolean seen = visited.contains(r);
            if (!seen && !mapRevealed) {
                unknown++;
                continue;
            }
            List<String> exits = new ArrayList<>();
            for (Exit exit : r.getVisibleExits()) {
                exits.add(exit.getTarget().getName() + (exit.isLocked() ? " (locked)" : ""));
            }
            StringBuilder line = new StringBuilder("  ").append(r == room ? "* " : "  ")
                    .append(r.getName()).append(" -> ").append(String.join(", ", exits));
            List<Monster> monsters = r.getLivingMonsters();
            if (!monsters.isEmpty()) {
                line.append("   ").append(style.danger("[" + names(monsters) + "]"));
            }
            if (!seen) {
                line.append(style.dim("   (not visited)"));
            }
            say(line.toString());
        }
        if (unknown > 0) {
            say("  ...and " + unknown + " room(s) you have not found yet.");
        }
        say(style.dim("  (* = you are here)"));
    }

    /** Suggests the most useful next step from what the player has seen and carries. */
    private void hint() {
        // 1. A carried key or tome that opens something the player knows about.
        for (Room r : visited) {
            for (Exit exit : r.getExits()) {
                if (exit.isLocked() && player.getItem(exit.getRequiredKey()) != null) {
                    say("Hint: your " + exit.getRequiredKey() + " opens the way from the " + r.getName()
                            + " to the " + exit.getTarget().getName() + ".");
                    return;
                }
                if (exit.isHidden() && player.getItem(exit.getRevealedBy()) != null) {
                    say("Hint: try to 'use " + exit.getRevealedBy() + "'. It is about the " + r.getName() + ".");
                    return;
                }
            }
        }
        // 2. The final enemy needs silver or magic.
        GameCharacter goal = data.getGoal() == null ? null : data.getCharacter(data.getGoal());
        boolean canFinishVampire = player instanceof Mage
                || (player.getWeapon() != null && player.getWeapon().getDamageType() != DamageType.PHYSICAL);
        if (goal instanceof Vampire && !canFinishVampire) {
            say("Hint: " + goal.getName() + " can only be finished with silver or magic. Find a silver weapon.");
            return;
        }
        // 3. Items left behind in visited rooms, often guarded.
        for (Room r : visited) {
            if (!r.getItems().isEmpty()) {
                List<Monster> guards = r.getLivingMonsters();
                say("Hint: the " + r.getName() + " still holds " + names(r.getItems())
                        + (guards.isEmpty() ? "." : ", guarded by " + names(guards) + "."));
                return;
            }
        }
        // 4. Unexplored places.
        if (visited.size() < data.getRooms().size()) {
            say("Hint: there are rooms you have not found yet. Some ways are locked or hidden.");
            return;
        }
        say("Hint: you have everything you need. Go and break the curse!");
    }

    // ---------------------------------------------------------------- moving

    private Exit findExit(String arg) {
        List<Exit> exits = room.getVisibleExits();
        try {
            int n = Integer.parseInt(arg.strip());
            return n >= 1 && n <= exits.size() ? exits.get(n - 1) : null;
        } catch (NumberFormatException e) {
            return Lookup.find(exits, exit -> exit.getTarget().getName(), arg);
        }
    }

    private Turn go(String arg) {
        if (arg.isEmpty()) {
            say("Go where? Type 'look' to see the exits.");
            return Turn.FREE;
        }
        Exit exit = findExit(arg);
        if (exit == null) {
            say("You can't go '" + arg + "' from here.");
            return Turn.FREE;
        }
        return travel(exit);
    }

    private Turn back() {
        if (previousRoom == null) {
            say("You have nowhere to go back to.");
            return Turn.FREE;
        }
        Exit exit = room.getExitTo(previousRoom);
        if (exit == null || exit.isHidden()) {
            say("There is no way back to the " + previousRoom.getName() + " from here.");
            return Turn.FREE;
        }
        return travel(exit);
    }

    private Turn travel(Exit exit) {
        Room target = exit.getTarget();
        if (exit.isLocked()) {
            Item key = player.getItem(exit.getRequiredKey());
            if (key == null) {
                say("The way to the " + target.getName() + " is locked. You need the " + exit.getRequiredKey() + ".");
                return Turn.FREE;
            }
            unlock(exit);
            say("You unlock the way to the " + target.getName() + " with the " + key.getName() + ".");
        }
        for (Monster monster : room.getLivingMonsters()) {
            if (rng.nextDouble() < settings.difficulty().partingStrikeChance()) {
                say(monster.getName() + " strikes as you flee!");
                monsterAttacks(monster);
                if (player.isVanquished()) {
                    return Turn.MOVED;
                }
            }
        }
        previousRoom = room;
        enter(target);
        return Turn.MOVED;
    }

    private void enter(Room target) {
        room = target;
        visited.add(target);
        describeRoom();
        for (Monster monster : target.getLivingMonsters()) {
            say(style.danger(monster.taunt()));
        }
    }

    // ---------------------------------------------------------------- items

    private Turn take(String arg) {
        if (arg.isEmpty()) {
            say("Take what?");
            return Turn.FREE;
        }
        List<Item> wanted;
        if (arg.equals("all") || arg.equals("everything")) {
            wanted = room.getItems();
            if (wanted.isEmpty()) {
                say("There is nothing here to take.");
                return Turn.FREE;
            }
        } else {
            Item item = Lookup.find(room.getItems(), arg);
            if (item == null) {
                say(Lookup.find(room.getMonsters(), arg) != null
                        ? "You can't put that in your pocket!" : "There is no '" + arg + "' here.");
                return Turn.FREE;
            }
            wanted = List.of(item);
        }
        List<Monster> guards = room.getLivingMonsters();
        if (!guards.isEmpty()) {
            say(guards.get(0).getName() + " blocks your way. Deal with it first!");
            return Turn.FREE;
        }
        for (Item item : wanted) {
            room.removeFromRoom(item);
            player.addItem(item);
            say("You take the " + style.item(item.getName()) + ". " + item.getDescription());
            if (item instanceof Weapon weapon && player.getWeapon() == null) {
                player.equip(weapon);
                say("You wield the " + weapon.getName() + ".");
            }
            if (item instanceof Armor armor && player.getArmor() == null) {
                player.wear(armor);
                say("You put on the " + armor.getName() + ".");
            }
        }
        return Turn.TAKEN;
    }

    private Turn use(String arg) {
        if (arg.isEmpty()) {
            say("Use what?");
            return Turn.FREE;
        }
        Item item = Lookup.find(player.getInventory(), arg);
        if (item == null) {
            say(Lookup.find(room.getItems(), arg) != null
                    ? "You need to pick it up first." : "You don't have '" + arg + "'.");
            return Turn.FREE;
        }
        UseResult result = item.use(this);
        if (result == UseResult.CONSUMED) {
            player.removeItem(item);
        }
        return result == UseResult.NOTHING ? Turn.FREE : Turn.TAKEN;
    }

    /** Unlocks every exit here that this key opens. Returns false if nothing fits. */
    public boolean unlockWith(Key key) {
        boolean any = false;
        for (Exit exit : room.getVisibleExits()) {
            if (exit.isLocked() && exit.getRequiredKey().equals(key.getName())) {
                unlock(exit);
                say("The " + key.getName() + " turns with a screech. The way to the "
                        + exit.getTarget().getName() + " is open.");
                any = true;
            }
        }
        if (!any) {
            say("There is no lock here that the " + key.getName() + " fits.");
        }
        return any;
    }

    private void unlock(Exit exit) {
        String key = exit.getRequiredKey();
        exit.unlock();
        Exit reverse = exit.getTarget().getExitTo(room);
        if (reverse != null && key.equals(reverse.getRequiredKey())) {
            reverse.unlock();
        }
    }

    /** Reveals hidden exits here that this item opens, or hints where they are. */
    public void revealHiddenWith(Item item) {
        boolean found = false;
        for (Exit exit : room.getExits()) {
            if (exit.isHidden() && exit.getRevealedBy().equals(item.getName())) {
                exit.reveal();
                say(style.good("Following the " + item.getName() + ", you find a hidden way to the "
                        + exit.getTarget().getName() + "!"));
                found = true;
            }
        }
        if (found) {
            return;
        }
        for (Room r : data.getRooms()) {
            for (Exit exit : r.getExits()) {
                if (exit.isHidden() && exit.getRevealedBy().equals(item.getName())) {
                    say("The " + item.getName() + " describes a hidden way out of the " + r.getName() + ".");
                    return;
                }
            }
        }
        say("You study the " + item.getName() + " but learn nothing new.");
    }

    public void revealMap() {
        mapRevealed = true;
        say("You peer through the lens at the castle's windows and courtyards...");
        map();
    }

    // ---------------------------------------------------------------- combat

    /** The first monster still standing in the current room, or null. */
    public Monster firstLivingMonster() {
        List<Monster> monsters = room.getLivingMonsters();
        return monsters.isEmpty() ? null : monsters.get(0);
    }

    private Turn attack(String arg) {
        List<Monster> monsters = room.getLivingMonsters();
        if (monsters.isEmpty()) {
            say("There is nothing here to fight.");
            return Turn.FREE;
        }
        Monster target = arg.isEmpty() ? monsters.get(0) : Lookup.find(monsters, arg);
        if (target == null) {
            say("There is no '" + arg + "' here to fight.");
            return Turn.FREE;
        }
        strike(target, player.attack(rng));
        return Turn.TAKEN;
    }

    /** Lands one of the player's blows on a monster and narrates the result. */
    public void strike(Monster target, Attack blow) {
        say("You " + secondPerson(blow.verb()) + " " + target.getName() + "!"
                + (blow.critical() ? style.good(" CRITICAL HIT!") : ""));
        int dealt = target.takeHit(blow, rng, this::say);
        say("  " + target.getName() + " takes " + dealt + " damage " + health(target));
        if (target.isVanquished()) {
            kills++;
            say(style.good(target.getName() + " is vanquished!"));
        }
    }

    private void monsterAttacks(Monster monster) {
        Attack raw = monster.attack(rng);
        Attack blow = new Attack(settings.difficulty().scaleDamageTaken(raw.damage()), raw.type(), raw.verb(),
                raw.critical());
        say(style.danger(monster.getName() + " " + blow.verb() + " you!"));
        int dealt = player.takeHit(blow, rng, this::say);
        monster.afterDealing(dealt, this::say);
        say("  You take " + dealt + " damage " + health(player));
    }

    private void endTurn(boolean monstersAct) {
        turns++;
        if (monstersAct) {
            for (Monster monster : room.getLivingMonsters()) {
                monsterAttacks(monster);
                if (player.isVanquished()) {
                    return;
                }
            }
        }
        player.endTurn();
        if (player.getHealth() * 4 < player.getMaxHealth()) {
            say(style.danger("You are badly hurt. Drink a potion or flee!"));
        }
    }

    // ---------------------------------------------------------------- saving and endings

    private void save() {
        if (settings.saveFile() == null) {
            say("Saving is not available in this game.");
            return;
        }
        try {
            new SaveGame(settings.seed(), settings.difficulty(), heroIndex, history).write(settings.saveFile());
            say(style.good("Game saved.") + " Start the game again and choose to continue.");
        } catch (IOException e) {
            say("Could not save the game: " + e.getMessage());
        }
    }

    private void quitGame() {
        if (settings.saveFile() != null) {
            out.print("Save before you go? (y/n) ");
            String answer = readLine();
            if (answer != null && answer.toLowerCase(Locale.ROOT).startsWith("y")) {
                save();
            }
        }
        quit = true;
        say("You flee into the storm. The castle keeps its secrets... for now.");
    }

    private boolean goalReached() {
        if (data.getGoal() != null) {
            return data.getCharacter(data.getGoal()).isVanquished();
        }
        for (Room r : data.getRooms()) {
            if (!r.getLivingMonsters().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void victory() {
        say("");
        say(style.good("The castle shudders. Moonlight floods every window as the curse lifts."));
        say(style.good("*** YOU WIN! ***"));
        int score = score();
        say("Score: " + style.bold(String.valueOf(score)) + " - rank: " + style.bold(rank(score)));
        stats();
    }

    private void defeat() {
        say("");
        say(style.danger("Your vision fades to black. The castle claims another soul."));
        say(style.danger("*** GAME OVER ***"));
        stats();
        say(style.dim("Tip: type 'hint' when unsure, drink potions early, and pick your fights."));
    }

    /** Rewards kills and remaining health, penalises slow play, scaled by difficulty. */
    int score() {
        int base = Math.max(0, 500 + kills * 100 + player.getHealth() * 5 - turns * 5);
        return settings.difficulty().scaleScore(base);
    }

    static String rank(int score) {
        if (score >= 1000) {
            return "Legend of the Castle";
        }
        if (score >= 750) {
            return "Monster Slayer";
        }
        if (score >= 500) {
            return "Brave Adventurer";
        }
        return "Lucky Survivor";
    }

    // ---------------------------------------------------------------- helpers

    /** Next trimmed input line, or null at end of input. Drops a byte-order mark some shells prepend. */
    private String readLine() {
        out.flush();
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine().replace("﻿", "").strip();
    }

    private String health(GameCharacter c) {
        return style.bar(c.getHealth(), c.getMaxHealth()) + " " + c.getHealth() + "/" + c.getMaxHealth() + " HP";
    }

    /** "hurls a fireball at" becomes "hurl a fireball at". */
    private static String secondPerson(String verb) {
        return verb.replaceFirst("^(\\w+)s\\b", "$1");
    }

    private static String names(List<? extends GameEntity> things) {
        List<String> names = new ArrayList<>();
        for (GameEntity thing : things) {
            names.add(thing.getName());
        }
        return String.join(", ", names);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static <T> T pick(List<T> options, String answer, Function<T, String> nameOf) {
        try {
            int n = Integer.parseInt(answer);
            return n >= 1 && n <= options.size() ? options.get(n - 1) : null;
        } catch (NumberFormatException e) {
            return Lookup.find(options, nameOf, answer);
        }
    }
}
