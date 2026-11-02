package hauntedcastle.item;

import java.util.Locale;

import hauntedcastle.game.Game;
import hauntedcastle.character.DamageType;

/** Adds damage to the wielder's blows and may change their damage type. */
public class Weapon extends Item {
    private final int bonus;
    private final DamageType damageType;

    public Weapon(String name, int bonus, DamageType damageType) {
        super(name, "A weapon: +" + bonus + " damage, " + damageType.name().toLowerCase(Locale.ROOT) + ".");
        this.bonus = bonus;
        this.damageType = damageType;
    }

    public int getBonus() {
        return bonus;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    @Override
    public UseResult use(Game game) {
        if (game.getPlayer().getWeapon() == this) {
            game.say("You are already wielding the " + getName() + ".");
            return UseResult.NOTHING;
        }
        game.getPlayer().equip(this);
        game.say("You wield the " + getName() + ". " + getDescription());
        return UseResult.USED;
    }
}
