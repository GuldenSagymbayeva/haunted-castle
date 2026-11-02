package game_character;

import java.util.Random;
import java.util.function.Consumer;

import game.GameEntity;

public abstract class GameCharacter implements GameEntity {
    private final String name;
    private final int maxHealth;
    private final int strength;
    private int health;
    private String description;

    protected GameCharacter(String name, int maxHealth, int strength) {
        this(name, maxHealth, strength, "");
    }

    protected GameCharacter(String name, int maxHealth, int strength, String description) {
        this.name = name;
        this.description = description;
        this.maxHealth = maxHealth;
        this.strength = strength;
        this.health = maxHealth;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getStrength() {
        return strength;
    }

    public boolean isVanquished() {
        return health == 0;
    }

    public void decreaseHealth(int amount) {
        health = Math.max(0, health - amount);
    }

    public void increaseHealth(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public abstract boolean isUndead();

    /** Rolls this character's next blow. */
    public Attack attack(Random rng) {
        return new Attack(roll(rng, strength), DamageType.PHYSICAL, "punches");
    }

    /**
     * Applies an incoming blow and returns the damage actually taken.
     * Subclasses add resistances; extra narration goes to {@code log}.
     */
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        int before = health;
        decreaseHealth(attack.damage());
        return before - health;
    }

    /** Called after one of this character's blows dealt {@code dealt} damage. */
    public void afterDealing(int dealt, Consumer<String> log) {
    }

    /** Potions heal the living and burn the undead. Returns the change in health. */
    public int drinkPotion(int amount) {
        int before = health;
        if (isUndead()) {
            decreaseHealth(amount);
        } else {
            increaseHealth(amount);
        }
        return health - before;
    }

    /** Uniform roll in [base - base/4, base + base/4], never below 1. */
    protected static int roll(Random rng, int base) {
        int spread = base / 4;
        return Math.max(1, base - spread + rng.nextInt(2 * spread + 1));
    }
}
