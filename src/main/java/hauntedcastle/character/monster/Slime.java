package hauntedcastle.character.monster;

import java.util.Random;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;

/** Hits harder the bigger it is, so it weakens as you hurt it. */
public class Slime extends Monster {

    public Slime(String name) {
        super(name, 40, 2);
    }

    @Override
    public boolean isUndead() {
        return false;
    }

    @Override
    public Attack attack(Random rng) {
        return new Attack(roll(rng, getStrength() + getHealth() / 6), DamageType.PHYSICAL, "engulfs");
    }

    @Override
    public String taunt() {
        return getName() + " oozes across the floor towards you, bubbling hungrily.";
    }
}
