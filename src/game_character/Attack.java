package game_character;

/**
 * One blow: how much damage, of what kind, and how to narrate it.
 * The verb reads as "attacker VERB target", e.g. "bites" or "hurls a fireball at".
 */
public record Attack(int damage, DamageType type, String verb, boolean critical) {

    public Attack(int damage, DamageType type, String verb) {
        this(damage, type, verb, false);
    }
}
