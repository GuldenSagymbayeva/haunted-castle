package hauntedcastle.character.monster;

import java.util.Random;
import java.util.function.Consumer;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;

/** Ordinary blows pass through it half the time. Silver and magic always land. */
public class Ghost extends Monster {

    public Ghost(String name) {
        super(name, 20, 5);
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    @Override
    public Attack attack(Random rng) {
        return new Attack(roll(rng, getStrength()), DamageType.MAGIC, "chills");
    }

    @Override
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        if (attack.type() == DamageType.PHYSICAL && rng.nextBoolean()) {
            log.accept("The blow passes straight through " + getName() + "!");
            return 0;
        }
        return super.takeHit(attack, rng, log);
    }

    @Override
    public String taunt() {
        return "The air turns to frost. " + getName() + " drifts out of the wall, wailing.";
    }
}
