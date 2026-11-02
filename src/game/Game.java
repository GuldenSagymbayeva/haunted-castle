package game;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import game_character.Attack;
import game_character.GameCharacter;
import game_character.monster.Monster;
import game_character.player_character.PlayerCharacter;
import item.Item;
import item.Key;
import item.UseResult;
import item.Weapon;
import room.Exit;
import room.Room;

/** One play-through: reads commands, runs turns and narrates what happens. */
public class Game {

    public enum Outcome { WON, DIED, QUIT }

    /** How a command affects the turn clock. */
    private enum Turn {
        /** No time passes (looking, help, failed actions). */
        FREE,
        /** Time passes and monsters in the room strike back. */
        TAKEN,
        /** The player changed rooms; monsters already had their parting strike. */
        MOVED
    }

    private static final double PARTING_STRIKE_CHANCE = 0.5;

    private final GameData data;
    private final Scanner in;
    private final PrintStream out;
    private final Random rng;

    private PlayerCharacter player;
    private Room room;
    private Room previousRoom;
    private final Set<Room> visited = new LinkedHashSet<>();
    private boolean mapRevealed;
    private boolean quit;
    private int turns;
    private int kills;

    public Game(GameData data, Scanner in, PrintStream out, Random rng) {
        this.data = data;
        this.in = in;
        this.out = out;
        this.rng = rng;
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

    public Outcome play() {
        intro();
        if (!chooseHero()) {
            return Outcome.QUIT;
        }
        enter(room);
        while (true) {
            out.print("\n> ");
            String line = readLine();
            if (line == null) {
                return Outcome.QUIT;
            }
            line = line.toLowerCase(Locale.ROOT);
            if (line.isEmpty()) {
                continue;
            }
            Turn turn = handle(line);
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
        }
    }

    // ---------------------------------------------------------------- setup

    private void intro() {
        say("");
        say("=============================================");
        say("            THE HAUNTED CASTLE");
        say("=============================================");
        say("A storm drove you to the castle gates. The doors slammed");
        say("shut behind you, and something in the dark began to laugh.");
        GameCharacter goal = data.getGoal() == null ? null : data.getCharacter(data.getGoal());
        if (goal != null) {
            say("Legend says the curse breaks only when " + goal.getName() + " is destroyed.");
        } else {
            say("Destroy every monster in the castle to break the curse.");
        }
        say("Type 'help' at any time for a list of commands.");
    }

    private boolean chooseHero() {
        List<PlayerCharacter> heroes = new ArrayList<>(data.getHeroes().keySet());
        if (heroes.size() == 1) {
            player = heroes.get(0);
        } else {
            say("\nChoose your hero:");
            for (int i = 0; i < heroes.size(); i++) {
                PlayerCharacter hero = heroes.get(i);
                say("  [" + (i + 1) + "] " + hero.getName() + " the " + hero.getTitle()
                        + " (HP " + hero.getMaxHealth() + ") - " + hero.getDescription());
            }
            while (player == null) {
                out.print("> ");
                String answer = readLine();
                if (answer == null) {
                    return false;
                }
                player = pick(heroes, answer, PlayerCharacter::getName);
                if (player == null) {
                    say("Type a number from 1 to " + heroes.size() + ".");
                }
            }
        }
        room = data.getHeroes().get(player);
        say("\nYou are " + player.getName() + " the " + player.getTitle() + ".");
        return true;
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
            case "use", "read", "drink", "equip", "wield", "unlock":
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
            case "quit", "exit", "q":
                quit = true;
                say("You flee into the storm. The castle keeps its secrets.");
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
        say("Commands:");
        say("  look / l              describe the room");
        say("  look <thing>          examine an item, monster or yourself");
        say("  go <room|number>      walk through an exit (or just type the room name)");
        say("  back                  return to the previous room");
        say("  take <item> / take all");
        say("  use <item>            drink, read, wield or unlock with an item");
        say("  attack [monster]      fight a monster in the room");
        say("  inventory / i         list what you carry");
        say("  stats / s             your health and progress");
        say("  map / m               rooms you know about");
        say("  wait                  let a turn pass");
        say("  quit                  give up");
        say("Tips: monsters strike back every turn you stay, and may hit you as you leave.");
        say("      Monsters guard the items in their room.");
    }

    // ---------------------------------------------------------------- looking

    private void describeRoom() {
        say("\n== " + capitalize(room.getName()) + " ==");
        if (!room.getDescription().isEmpty()) {
            say(room.getDescription());
        }
        List<Item> items = room.getItems();
        if (!items.isEmpty()) {
            say("You see: " + names(items) + ".");
        }
        for (Monster monster : room.getMonsters()) {
            if (monster.isVanquished()) {
                say("The remains of " + monster.getName() + " lie here.");
            } else {
                say("DANGER: " + monster.getName() + " the " + monster.getKind() + " (" + health(monster) + ")");
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
        say(capitalize(thing.getName()) + ": " + thing.getDescription());
        if (thing instanceof Monster monster) {
            say("  " + monster.getKind() + (monster.isUndead() ? ", undead" : "") + ", " + health(monster));
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
            say("  - " + item.getName() + (item == player.getWeapon() ? " (wielded)" : ""));
        }
    }

    private void stats() {
        say(player.status());
        say("Turns: " + turns + "   Monsters vanquished: " + kills + "   Rooms explored: "
                + visited.size() + "/" + data.getRooms().size());
    }

    private void map() {
        say("Castle map:");
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
                line.append("   [").append(names(monsters)).append("]");
            }
            if (!seen) {
                line.append("   (not visited)");
            }
            say(line.toString());
        }
        if (unknown > 0) {
            say("  ...and " + unknown + " room(s) you have not found yet.");
        }
        say("  (* = you are here)");
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
            if (rng.nextDouble() < PARTING_STRIKE_CHANCE) {
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
            say(monster.taunt());
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
            say("You take the " + item.getName() + ". " + item.getDescription());
            if (item instanceof Weapon weapon && player.getWeapon() == null) {
                player.equip(weapon);
                say("You wield the " + weapon.getName() + ".");
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
                say("Following the " + item.getName() + ", you find a hidden way to the "
                        + exit.getTarget().getName() + "!");
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
        Attack blow = player.attack(rng);
        say("You " + blow.verb().replaceFirst("^(\\w+)s\\b", "$1") + " " + target.getName() + "!"
                + (blow.critical() ? " CRITICAL HIT!" : ""));
        int dealt = target.takeHit(blow, rng, this::say);
        say("  " + target.getName() + " takes " + dealt + " damage (" + health(target) + ").");
        if (target.isVanquished()) {
            kills++;
            say(target.getName() + " is vanquished!");
        }
        return Turn.TAKEN;
    }

    private void monsterAttacks(Monster monster) {
        Attack blow = monster.attack(rng);
        say(monster.getName() + " " + blow.verb() + " you!");
        int dealt = player.takeHit(blow, rng, this::say);
        monster.afterDealing(dealt, this::say);
        say("  You take " + dealt + " damage (" + health(player) + ").");
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
            say("You are badly hurt. Drink a potion or flee!");
        }
    }

    // ---------------------------------------------------------------- endings

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
        say("\nThe castle shudders. Moonlight floods every window as the curse lifts.");
        say("*** YOU WIN! ***");
        say("Score: " + score());
        stats();
    }

    private void defeat() {
        say("\nYour vision fades to black. The castle claims another soul.");
        say("*** GAME OVER ***");
        stats();
    }

    /** Rewards kills and remaining health, penalises slow play. */
    int score() {
        return Math.max(0, 500 + kills * 100 + player.getHealth() * 5 - turns * 5);
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

    private static String health(GameCharacter c) {
        return c.getHealth() + "/" + c.getMaxHealth() + " HP";
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
