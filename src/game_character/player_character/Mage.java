package game_character.player_character;

import java.util.Random;

import game_character.Attack;
import game_character.DamageType;

/** Fragile, but fireballs hit hard and are magic. Mana refills slowly every turn. */
public class Mage extends PlayerCharacter {
    public static final int MAX_MAGIC = 30;
    public static final int SPELL_COST = 6;
    private static final int SPELL_POWER = 17;
    private static final int MAGIC_REGEN = 2;

    private int magic = MAX_MAGIC;

    public Mage(String name) {
        super(name, 45, 3, "A sharp-eyed spellcaster. Fragile, but casts magic fireballs.");
    }

    public int getMagic() {
        return magic;
    }

    @Override
    public Attack attack(Random rng) {
        if (magic >= SPELL_COST) {
            magic -= SPELL_COST;
            return new Attack(roll(rng, SPELL_POWER), DamageType.MAGIC, "hurls a fireball at");
        }
        int damage = roll(rng, getStrength()) + weaponBonus();
        String verb = getWeapon() == null ? "swings a staff at" : "stabs with the " + getWeapon().getName() + " at";
        return new Attack(damage, weaponType(), verb);
    }

    @Override
    public void endTurn() {
        magic = Math.min(MAX_MAGIC, magic + MAGIC_REGEN);
    }

    @Override
    public String status() {
        return super.status() + ", magic " + magic + "/" + MAX_MAGIC + " (fireball costs " + SPELL_COST + ")";
    }
}
