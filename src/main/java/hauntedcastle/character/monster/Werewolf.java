package hauntedcastle.character.monster;

import java.util.Random;
import java.util.function.Consumer;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;

/** Flies into a rage below half health (+50% damage). Silver burns it for double damage. */
public class Werewolf extends Monster {

    public Werewolf(String name) {
        super(name, 45, 8);
    }

    @Override
    public boolean isUndead() {
        return false;
    }

    public boolean isEnraged() {
        return getHealth() * 2 < getMaxHealth();
    }

    @Override
    public Attack attack(Random rng) {
        int damage = roll(rng, getStrength());
        if (isEnraged()) {
            return new Attack(damage * 3 / 2, DamageType.PHYSICAL, "savagely mauls");
        }
        return new Attack(damage, DamageType.PHYSICAL, "claws at");
    }

    @Override
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        boolean wasEnraged = isEnraged();
        Attack hit = attack;
        if (attack.type() == DamageType.SILVER) {
            hit = new Attack(attack.damage() * 2, attack.type(), attack.verb(), attack.critical());
            log.accept("The silver sears " + getName() + "'s hide!");
        }
        int dealt = super.takeHit(hit, rng, log);
        if (!wasEnraged && isEnraged() && !isVanquished()) {
            log.accept(getName() + " throws back its head and howls. Its eyes burn red with rage!");
        }
        return dealt;
    }

    @Override
    public String taunt() {
        return getName() + " drops to all fours, snarling, hackles raised.";
    }
}
