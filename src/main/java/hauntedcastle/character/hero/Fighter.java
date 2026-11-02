package hauntedcastle.character.hero;

import java.util.Random;

import hauntedcastle.character.Attack;

/** Tough and steady, with a chance to land a double-damage critical hit. */
public class Fighter extends PlayerCharacter {
    private static final double CRITICAL_CHANCE = 0.2;

    public Fighter(String name) {
        super(name, 60, 10, "A battle-scarred knight. Tough, strong, and lands critical hits.");
    }

    @Override
    public Attack attack(Random rng) {
        int damage = roll(rng, getStrength()) + weaponBonus();
        boolean critical = rng.nextDouble() < CRITICAL_CHANCE;
        if (critical) {
            damage *= 2;
        }
        String verb = getWeapon() == null ? "swings a sword at" : "strikes with the " + getWeapon().getName() + " at";
        return new Attack(damage, weaponType(), verb, critical);
    }
}
