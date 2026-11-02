package hauntedcastle.game;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

/** Forgiving name matching for player input: exact name, then prefix, then any word prefix. */
public final class Lookup {

    private Lookup() {
    }

    public static <T extends GameEntity> T find(Collection<? extends T> things, String query) {
        return find(things, GameEntity::getName, query);
    }

    public static <T> T find(Collection<? extends T> things, Function<? super T, String> nameOf, String query) {
        String q = normalize(query);
        if (q.isEmpty()) {
            return null;
        }
        for (T thing : things) {
            if (normalize(nameOf.apply(thing)).equals(q)) {
                return thing;
            }
        }
        for (T thing : things) {
            if (normalize(nameOf.apply(thing)).startsWith(q)) {
                return thing;
            }
        }
        for (T thing : things) {
            for (String word : normalize(nameOf.apply(thing)).split(" ")) {
                if (word.startsWith(q)) {
                    return thing;
                }
            }
        }
        return null;
    }

    private static String normalize(String s) {
        String n = s.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ");
        if (n.startsWith("the ")) {
            n = n.substring(4);
        }
        return n;
    }
}
