package hauntedcastle.item;

import hauntedcastle.game.Game;

/** Worn armour: every blow you take is reduced by a fixed amount (to a minimum of 1). */
public class Armor extends Item {
    private final int reduction;

    public Armor(String name, int reduction) {
        super(name, "Armour: blocks " + reduction + " damage from every blow.");
        this.reduction = reduction;
    }

    public int getReduction() {
        return reduction;
    }

    @Override
    public UseResult use(Game game) {
        if (game.getPlayer().getArmor() == this) {
            game.say("You are already wearing the " + getName() + ".");
            return UseResult.NOTHING;
        }
        game.getPlayer().wear(this);
        game.say("You strap on the " + getName() + ". " + getDescription());
        return UseResult.USED;
    }
}
