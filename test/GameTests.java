import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

import game.Game;
import game.GameData;
import game.GameLoader;
import game_character.Attack;
import game_character.DamageType;
import game_character.monster.Ghost;
import game_character.monster.Skeleton;
import game_character.monster.Vampire;
import room.Exit;

/** Dependency-free tests. Run: java -cp out GameTests (from the project root). */
public class GameTests {
    private static final File WORLD = new File("worlds/hauntedcastle.txt");
    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        loaderReadsWorld();
        vampireSurvivesSteelButNotSilver();
        skeletonReassemblesOnce();
        ghostAlwaysTakesSilver();
        lockedDoorNeedsKey();
        tomeRevealsSecretPassage();
        mageCanWin();
        if (failures > 0) {
            System.out.println(failures + " check(s) FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
    }

    private static void loaderReadsWorld() throws IOException {
        GameData data = GameLoader.loadGameData(WORLD);
        check(data.getRoomNames().size() == 7, "world has 7 rooms");
        check(data.getHeroes().size() == 2, "world has 2 heroes");
        check("Count Orlok".equals(data.getGoal()), "goal is Count Orlok");
        Exit gate = data.getRoom("dungeon").getExitTo(data.getRoom("crypt"));
        check(gate.isLocked() && gate.getRequiredKey().equals("iron key"), "crypt gate locked by iron key");
    }

    private static void vampireSurvivesSteelButNotSilver() {
        Vampire v = new Vampire("V");
        Random rng = new Random(1);
        v.takeHit(new Attack(500, DamageType.PHYSICAL, "hits"), rng, s -> { });
        check(v.getHealth() == 1, "steel leaves the vampire at 1 HP");
        v.takeHit(new Attack(5, DamageType.SILVER, "stabs"), rng, s -> { });
        check(v.isVanquished(), "silver finishes the vampire");
    }

    private static void skeletonReassemblesOnce() {
        Skeleton k = new Skeleton("K");
        Random rng = new Random(1);
        k.takeHit(new Attack(100, DamageType.PHYSICAL, "hits"), rng, s -> { });
        check(!k.isVanquished(), "skeleton rises after first defeat");
        k.takeHit(new Attack(100, DamageType.PHYSICAL, "hits"), rng, s -> { });
        check(k.isVanquished(), "skeleton stays down the second time");
    }

    private static void ghostAlwaysTakesSilver() {
        Ghost g = new Ghost("G");
        Random rng = new Random(1);
        for (int i = 0; i < 4; i++) {
            g.takeHit(new Attack(5, DamageType.SILVER, "stabs"), rng, s -> { });
        }
        check(g.isVanquished(), "four silver hits of 5 destroy a 20 HP ghost");
    }

    private static void lockedDoorNeedsKey() throws IOException {
        // Fighter walks to the dungeon past the slime without the key and cannot open the gate.
        String out = run(1, "1", "go laboratory", "go dungeon", "go crypt", "quit");
        check(out.contains("is locked. You need the iron key"), "locked gate refuses without key");
    }

    private static void tomeRevealsSecretPassage() throws IOException {
        String out = run(1, "2", "look", "take tome", "read tome", "go secret passage", "quit");
        check(out.contains("you find a hidden way to the secret passage"), "tome reveals passage");
        check(out.contains("== Secret passage =="), "player can enter the passage");
    }

    private static void mageCanWin() throws IOException {
        String[] script = {
            "2", "take tome", "read tome",
            "go tower", "attack", "attack", "take all",
            "back", "go entrance hall", "take potion", "go library",
            "wait", "wait", "wait", "wait", "wait", "wait", "wait", "wait", "wait", "wait", "wait", "wait",
            "go secret passage", "go crypt",
            "attack", "attack", "attack", "attack", "drink potion", "attack", "attack", "attack",
            "attack", "attack", "attack", "attack", "attack", "attack", "attack", "attack",
        };
        String out = run(4, script);
        check(out.contains("*** YOU WIN! ***"), "scripted mage run (seed 4) defeats Count Orlok");
    }

    private static String run(long seed, String... commands) throws IOException {
        GameData data = GameLoader.loadGameData(WORLD);
        Scanner in = new Scanner(String.join("\n", commands) + "\n");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new Game(data, in, out, new Random(seed)).play();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void check(boolean ok, String what) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
