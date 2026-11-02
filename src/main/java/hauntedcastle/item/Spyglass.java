package hauntedcastle.item;

import hauntedcastle.game.Game;

/** Reveals every room of the castle, and who lurks there, on the map. */
public class Spyglass extends Item {

    public Spyglass(String name) {
        super(name, "A brass instrument. From up high it could show you the whole castle.");
    }

    @Override
    public UseResult use(Game game) {
        game.revealMap();
        return UseResult.USED;
    }
}
