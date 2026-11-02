package hauntedcastle;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

import hauntedcastle.game.Difficulty;
import hauntedcastle.game.Game;
import hauntedcastle.game.GameData;
import hauntedcastle.game.GameLoader;
import hauntedcastle.game.SaveGame;
import hauntedcastle.game.Style;

/** Command-line entry point: options, continue-or-new-game, and the play-again loop. */
public final class Main {

    private static final String USAGE = """
            Usage: java -jar haunted-castle.jar [options] [world-file]
              --difficulty easy|normal|hard   skip the difficulty question
              --seed N                        repeatable dice rolls
              --no-color                      plain text output
              --help                          show this message
            Without a world file the built-in Haunted Castle is played.""";

    private final Scanner in;
    private final PrintStream out;
    private final String worldFile;
    private final Difficulty fixedDifficulty;
    private final Random seeds;
    private final Style style;
    private final Path saveFile;

    private Main(Scanner in, PrintStream out, String worldFile, Difficulty fixedDifficulty, Random seeds,
            Style style) {
        this.in = in;
        this.out = out;
        this.worldFile = worldFile;
        this.fixedDifficulty = fixedDifficulty;
        this.seeds = seeds;
        this.style = style;
        String saveName = worldFile == null ? "save.txt" : "save-" + new File(worldFile).getName();
        this.saveFile = Path.of(System.getProperty("user.home"), ".haunted-castle", saveName);
    }

    public static void main(String[] args) {
        String world = null;
        Difficulty difficulty = null;
        Random seeds = new Random();
        boolean noColor = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--seed" -> seeds = new Random(Long.parseLong(args[++i]));
                    case "--difficulty" -> difficulty = Difficulty.valueOf(args[++i].toUpperCase(Locale.ROOT));
                    case "--no-color" -> noColor = true;
                    case "--help", "-h" -> {
                        System.out.println(USAGE);
                        return;
                    }
                    default -> {
                        if (args[i].startsWith("--")) {
                            throw new IllegalArgumentException("unknown option " + args[i]);
                        }
                        world = args[i];
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Bad arguments: " + e.getMessage() + "\n" + USAGE);
            System.exit(2);
            return;
        }
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        new Main(in, out, world, difficulty, seeds, Style.detect(noColor)).run();
    }

    private void run() {
        while (true) {
            GameData data = loadWorld();
            if (data == null) {
                System.exit(1);
                return;
            }
            Game.Outcome outcome = Files.exists(saveFile) && ask("A saved game was found. Continue it? (y/n) ")
                    ? continueSaved(data)
                    : newGame(data);
            if (outcome == Game.Outcome.WON || outcome == Game.Outcome.DIED) {
                deleteSave();
            }
            if (outcome == Game.Outcome.QUIT || !ask("\nPlay again? (y/n) ")) {
                break;
            }
        }
        out.println("Thanks for playing!");
    }

    private Game.Outcome newGame(GameData data) {
        Difficulty difficulty = fixedDifficulty != null ? fixedDifficulty : askDifficulty();
        Game.Settings settings = new Game.Settings(difficulty, seeds.nextLong(), style, saveFile);
        return new Game(data, in, out, settings).play();
    }

    private Game.Outcome continueSaved(GameData data) {
        try {
            SaveGame save = SaveGame.read(saveFile);
            Game.Settings settings = new Game.Settings(save.difficulty(), save.seed(), style, saveFile);
            return new Game(data, in, out, settings).resume(save);
        } catch (IOException e) {
            out.println("The save could not be read (" + e.getMessage() + "). Starting a new game.");
            deleteSave();
            return newGame(data);
        }
    }

    private Difficulty askDifficulty() {
        Difficulty[] all = Difficulty.values();
        out.println("\nChoose difficulty:");
        for (int i = 0; i < all.length; i++) {
            out.println("  [" + (i + 1) + "] " + all[i].label() + " - " + all[i].blurb());
        }
        out.print("> (press Enter for Normal) ");
        out.flush();
        String answer = in.hasNextLine() ? in.nextLine().replace("﻿", "").strip() : "";
        for (int i = 0; i < all.length; i++) {
            if (answer.equals(String.valueOf(i + 1)) || answer.equalsIgnoreCase(all[i].label())) {
                return all[i];
            }
        }
        return Difficulty.NORMAL;
    }

    private GameData loadWorld() {
        try {
            return worldFile == null
                    ? GameLoader.loadResource(GameLoader.DEFAULT_WORLD)
                    : GameLoader.loadGameData(new File(worldFile));
        } catch (IOException e) {
            System.err.println("Could not load world: " + e.getMessage());
            return null;
        }
    }

    private boolean ask(String question) {
        out.print(question);
        out.flush();
        return in.hasNextLine() && in.nextLine().replace("﻿", "").strip().toLowerCase(Locale.ROOT).startsWith("y");
    }

    private void deleteSave() {
        try {
            Files.deleteIfExists(saveFile);
        } catch (IOException e) {
            out.println("Could not remove the old save file " + saveFile + ": " + e.getMessage());
        }
    }
}
