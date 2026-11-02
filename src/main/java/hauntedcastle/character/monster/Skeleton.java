package hauntedcastle.character.monster;

import java.util.Random;
import java.util.function.Consumer;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;

/** Pulls itself back together once after being smashed by ordinary weapons. */
public class Skeleton extends Monster {
    private static final int REASSEMBLE_HEALTH = 10;
    private boolean reassembled = false;

    public Skeleton(String name) {
        super(name, 25, 7);
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    @Override
    public Attack attack(Random rng) {
        return new Attack(roll(rng, getStrength()), DamageType.PHYSICAL, "slashes at");
    }

    @Override
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        int dealt = super.takeHit(attack, rng, log);
        if (isVanquished() && !reassembled) {
            if (attack.type() == DamageType.PHYSICAL) {
                reassembled = true;
                increaseHealth(REASSEMBLE_HEALTH);
                log.accept("The bones clatter back together! " + getName() + " rises again.");
            } else {
                log.accept("The bones crumble to dust. They will not rise again.");
            }
        }
        return dealt;
    }

    @Override
    public String taunt() {
        return getName() + " rattles to its feet, a rusty blade in its bony hand.";
    }
}
