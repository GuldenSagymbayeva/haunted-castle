package hauntedcastle.game;

/** Optional ANSI colours for terminal output. With colours off every method returns plain text. */
public final class Style {
    private static final String RESET = "\u001B[0m";

    private final boolean enabled;

    public Style(boolean enabled) {
        this.enabled = enabled;
    }

    public static Style plain() {
        return new Style(false);
    }

    /** Colours on only for an interactive terminal, unless disabled by flag or the NO_COLOR convention. */
    public static Style detect(boolean disabledByFlag) {
        return new Style(!disabledByFlag && System.console() != null && System.getenv("NO_COLOR") == null);
    }

    public String title(String s) {
        return paint("1;36", s);
    }

    public String danger(String s) {
        return paint("1;31", s);
    }

    public String good(String s) {
        return paint("1;32", s);
    }

    public String item(String s) {
        return paint("33", s);
    }

    public String dim(String s) {
        return paint("2", s);
    }

    public String bold(String s) {
        return paint("1", s);
    }

    /** A ten-segment health bar, e.g. [######----], coloured by how full it is. */
    public String bar(int current, int max) {
        int filled = max <= 0 ? 0 : (int) Math.ceil(10.0 * current / max);
        String bar = "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
        if (current * 4 < max) {
            return danger(bar);
        }
        return current * 2 < max ? paint("33", bar) : good(bar);
    }

    private String paint(String code, String s) {
        return enabled ? "\u001B[" + code + "m" + s + RESET : s;
    }
}
