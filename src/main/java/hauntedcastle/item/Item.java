package hauntedcastle.item;

import hauntedcastle.game.Game;
import hauntedcastle.game.GameEntity;

/** A plain item. Subclasses give items an effect when used. */
public class Item implements GameEntity {
    private final String name;
    private String description;

    public Item(String name) {
        this(name, "Nothing special about it.");
    }

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UseResult use(Game game) {
        game.say("You turn the " + name + " over in your hands. Nothing happens.");
        return UseResult.NOTHING;
    }
}
