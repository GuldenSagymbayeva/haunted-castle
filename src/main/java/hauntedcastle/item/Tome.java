package hauntedcastle.item;

import hauntedcastle.game.Game;

/** Reading it reveals the hidden exits that name this tome. */
public class Tome extends Item {

    public Tome(String name) {
        super(name, "Crumbling pages full of floor plans and scribbled notes.");
    }

    @Override
    public UseResult use(Game game) {
        game.revealHiddenWith(this);
        return UseResult.USED;
    }
}
