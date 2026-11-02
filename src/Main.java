import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

import game.Game;
import game.GameData;
import game.GameLoader;

/**
 * Usage: java -cp out Main [world-file] [--seed N]
 * The default world is worlds/hauntedcastle.txt. A seed makes every dice roll repeatable.
 */
public class Main {

    public static void main(String[] args) {
        String world = "worlds/hauntedcastle.txt";
        Random rng = new Random();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--seed") && i + 1 < args.length) {
                rng = new Random(Long.parseLong(args[++i]));
            } else {
                world = args[i];
            }
        }

        Scanner in = new Scanner(System.in);
        while (true) {
            GameData data;
            try {
                data = GameLoader.loadGameData(new File(world));
            } catch (IOException e) {
                System.err.println("Could not load world: " + e.getMessage());
                System.exit(1);
                return;
            }
            Game.Outcome outcome = new Game(data, in, System.out, rng).play();
            if (outcome == Game.Outcome.QUIT || !playAgain(in)) {
                break;
            }
        }
        System.out.println("Thanks for playing!");
    }

    private static boolean playAgain(Scanner in) {
        System.out.print("\nPlay again? (y/n) ");
        System.out.flush();
        return in.hasNextLine() && in.nextLine().strip().toLowerCase(Locale.ROOT).startsWith("y");
    }
}
