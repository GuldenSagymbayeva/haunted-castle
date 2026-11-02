package hauntedcastle.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved game. Every dice roll comes from one seeded Random, so the seed, the difficulty,
 * the chosen hero and the list of commands typed so far are enough to rebuild the exact state
 * by replaying them.
 */
public record SaveGame(long seed, Difficulty difficulty, int hero, List<String> commands) {

    private static final String HEADER = "haunted-castle-save 1";

    public SaveGame {
        commands = List.copyOf(commands);
    }

    public void write(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("seed " + seed);
        lines.add("difficulty " + difficulty.name());
        lines.add("hero " + hero);
        for (String command : commands) {
            lines.add("command " + command);
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public static SaveGame read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() < 4 || !lines.get(0).equals(HEADER)) {
            throw new IOException("not a Haunted Castle save file: " + file);
        }
        try {
            long seed = Long.parseLong(value(lines.get(1), "seed"));
            Difficulty difficulty = Difficulty.valueOf(value(lines.get(2), "difficulty"));
            int hero = Integer.parseInt(value(lines.get(3), "hero"));
            List<String> commands = new ArrayList<>();
            for (String line : lines.subList(4, lines.size())) {
                commands.add(value(line, "command"));
            }
            return new SaveGame(seed, difficulty, hero, commands);
        } catch (IllegalArgumentException e) {
            throw new IOException("damaged save file " + file + ": " + e.getMessage(), e);
        }
    }

    private static String value(String line, String key) {
        if (!line.startsWith(key + " ")) {
            throw new IllegalArgumentException("expected '" + key + "' but found '" + line + "'");
        }
        return line.substring(key.length() + 1);
    }
}
