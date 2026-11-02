package hauntedcastle.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Plays scripted games against the built-in world and checks the narration. */
class GameTest {

    @TempDir
    Path tempDir;

    private String output;

    private Game.Outcome play(long seed, String... commands) throws IOException {
        return play(Game.Settings.forTest(seed), commands);
    }

    private Game.Outcome play(Game.Settings settings, String... commands) throws IOException {
        GameData data = GameLoader.loadResource(GameLoader.DEFAULT_WORLD);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Scanner in = new Scanner(String.join("\n", commands) + "\n");
        Game.Outcome outcome = new Game(data, in, new PrintStream(buffer, true, StandardCharsets.UTF_8), settings)
                .play();
        output = buffer.toString(StandardCharsets.UTF_8);
        return outcome;
    }

    @Test
    void lockedGateNeedsTheKey() throws IOException {
        play(1, "1", "go laboratory", "go dungeon", "go crypt", "quit");
        assertTrue(output.contains("is locked. You need the iron key"));
    }

    @Test
    void carriedKeyUnlocksAutomatically() throws IOException {
        play(1, "1", "go great hall", "go kitchen", "take all", "back", "go chapel", "quit");
        assertTrue(output.contains("You unlock the way to the chapel with the chapel key."));
        assertTrue(output.contains("== Chapel =="));
    }

    @Test
    void tomeRevealsTheSecretPassage() throws IOException {
        play(1, "2", "take tome", "go secret", "read tome", "go secret passage", "quit");
        assertTrue(output.contains("You can't go 'secret' from here."), "hidden before reading");
        assertTrue(output.contains("you find a hidden way to the secret passage"));
        assertTrue(output.contains("== Secret passage =="));
    }

    @Test
    void monstersGuardTheirItems() throws IOException {
        play(1, "1", "go laboratory", "take key", "quit");
        assertTrue(output.contains("Ooze blocks your way. Deal with it first!"));
    }

    @Test
    void holyWaterIsThrownAndUsedUp() throws IOException {
        play(1, "1", "go great hall", "go kitchen", "take all", "back", "go chapel", "take all",
                "throw holy water", "back", "go armory", "throw holy water", "throw holy water", "quit");
        assertTrue(output.contains("There is nothing here to throw the holy water at."));
        assertTrue(output.contains("You throw the holy water at Grimfang!"));
        assertTrue(output.contains("You don't have 'holy water'."), "consumed after one throw");
    }

    @Test
    void numbersAndRoomNamesWorkAsShortcuts() throws IOException {
        play(1, "1", "3", "kitchen", "quit");
        assertTrue(output.contains("== Great hall =="));
        assertTrue(output.contains("== Kitchen =="));
    }

    @Test
    void unknownCommandsAreExplained() throws IOException {
        play(1, "1", "dance", "quit");
        assertTrue(output.contains("I don't understand 'dance'."));
    }

    @Test
    void hintPointsAtCarriedKey() throws IOException {
        play(1, "1", "go great hall", "go kitchen", "take all", "hint", "quit");
        assertTrue(output.contains("Hint: your chapel key opens the way from the great hall to the chapel."));
    }

    @Test
    void sameSeedPlaysTheSameGame() throws IOException {
        String[] script = {"1", "go laboratory", "attack", "attack", "attack", "attack", "stats", "quit"};
        play(42, script);
        String first = output;
        play(42, script);
        assertEquals(first, output);
    }

    @Test
    void savedGameResumesInTheSameState() throws IOException {
        Path save = tempDir.resolve("save.txt");
        Game.Settings settings = new Game.Settings(Difficulty.NORMAL, 7, Style.plain(), save);
        play(settings, "1", "go laboratory", "attack", "attack", "attack", "save", "stats", "quit", "n");
        assertTrue(Files.exists(save));
        String before = output.substring(output.lastIndexOf("Sir Roderick the Fighter - HP"));
        before = before.substring(0, before.indexOf("Difficulty"));

        SaveGame loaded = SaveGame.read(save);
        assertEquals(7, loaded.seed());
        assertFalse(loaded.commands().contains("save"), "meta commands are not replayed");

        GameData data = GameLoader.loadResource(GameLoader.DEFAULT_WORLD);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Scanner in = new Scanner("stats\nquit\nn\n");
        new Game(data, in, new PrintStream(buffer, true, StandardCharsets.UTF_8), settings).resume(loaded);
        String resumed = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(resumed.contains("Welcome back, Sir Roderick."));
        assertTrue(resumed.contains(before), "expected stats line: " + before);
    }

    @Test
    void damagedSaveFilesAreRejected() throws IOException {
        Path save = tempDir.resolve("bad.txt");
        Files.writeString(save, "hello\n");
        assertThrows(IOException.class, () -> SaveGame.read(save));
    }

    @Test
    void fighterCanWinOnEasy() throws IOException {
        Game.Settings easy = new Game.Settings(Difficulty.EASY, 11, Style.plain(), null);
        Game.Outcome outcome = play(easy, "1", "take potion", "go library", "take tome", "read tome",
                "go tower", "attack", "attack", "attack", "attack", "attack", "attack", "attack", "attack",
                "take all", "back", "go secret passage", "go crypt",
                "attack", "attack", "attack", "attack", "drink potion", "attack", "attack", "attack", "attack",
                "attack", "attack", "attack", "attack", "attack", "attack", "attack", "attack");
        assertEquals(Game.Outcome.WON, outcome, output);
        assertTrue(output.contains("*** YOU WIN! ***"));
    }

    @Test
    void ranksFollowScore() {
        assertEquals("Legend of the Castle", Game.rank(1200));
        assertEquals("Monster Slayer", Game.rank(800));
        assertEquals("Brave Adventurer", Game.rank(500));
        assertEquals("Lucky Survivor", Game.rank(10));
    }
}
