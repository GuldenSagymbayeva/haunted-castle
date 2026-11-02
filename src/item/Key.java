package item;

import game.Game;

/** Opens locked exits that name this key. */
public class Key extends Item {

    public Key(String name) {
        super(name, "A heavy key. It must open something in this castle.");
    }

    @Override
    public UseResult use(Game game) {
        return game.unlockWith(this) ? UseResult.USED : UseResult.NOTHING;
    }
}
