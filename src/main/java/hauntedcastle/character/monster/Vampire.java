package hauntedcastle.character.monster;

import java.util.Random;
import java.util.function.Consumer;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;

/** Drains blood to heal itself. Only silver or magic can deliver the final blow. */
public class Vampire extends Monster {

    public Vampire(String name) {
        super(name, 90, 11);
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    @Override
    public Attack attack(Random rng) {
        return new Attack(roll(rng, getStrength()), DamageType.PHYSICAL, "bites");
    }

    @Override
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        if (attack.type() == DamageType.PHYSICAL && attack.damage() >= getHealth()) {
            int dealt = getHealth() - 1;
            decreaseHealth(dealt);
            log.accept(getName() + "'s wounds knit before your eyes. Ordinary steel cannot finish this creature"
                    + " - you need silver or magic.");
            return dealt;
        }
        return super.takeHit(attack, rng, log);
    }

    @Override
    public void afterDealing(int dealt, Consumer<String> log) {
        int healed = dealt / 2;
        if (healed > 0 && getHealth() < getMaxHealth()) {
            increaseHealth(healed);
            log.accept(getName() + " drinks your blood and recovers " + healed + " health.");
        }
    }

    @Override
    public String taunt() {
        return getName() + " rises from the coffin. \"At last... a guest for dinner.\"";
    }
}
