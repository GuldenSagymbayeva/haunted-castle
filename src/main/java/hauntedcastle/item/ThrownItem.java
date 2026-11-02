package hauntedcastle.item;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;
import hauntedcastle.character.monster.Monster;
import hauntedcastle.game.Game;

/** Thrown once at a monster for magic damage, doubled against the undead. */
public class ThrownItem extends Item {
    private final int damage;

    public ThrownItem(String name, int damage) {
        super(name, "Throw it at a monster: " + damage + " magic damage, double against the undead.");
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public UseResult use(Game game) {
        Monster target = game.firstLivingMonster();
        if (target == null) {
            game.say("There is nothing here to throw the " + getName() + " at. Save it for a fight.");
            return UseResult.NOTHING;
        }
        int dealt = target.isUndead() ? damage * 2 : damage;
        game.strike(target, new Attack(dealt, DamageType.MAGIC, "throws the " + getName() + " at"));
        return UseResult.CONSUMED;
    }
}
