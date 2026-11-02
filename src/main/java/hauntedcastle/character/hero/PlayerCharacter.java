package hauntedcastle.character.hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;
import hauntedcastle.character.GameCharacter;
import hauntedcastle.item.Armor;
import hauntedcastle.item.Item;
import hauntedcastle.item.Weapon;

public abstract class PlayerCharacter extends GameCharacter {
    private final List<Item> inventory = new ArrayList<>();
    private Weapon weapon;
    private Armor armor;

    protected PlayerCharacter(String name, int maxHealth, int strength, String description) {
        super(name, maxHealth, strength, description);
    }

    @Override
    public boolean isUndead() {
        return false;
    }

    /** Class name shown to the player, e.g. "Fighter". */
    public String getTitle() {
        return getClass().getSimpleName();
    }

    public List<Item> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public void removeItem(Item item) {
        inventory.remove(item);
        if (item == weapon) {
            weapon = null;
        }
        if (item == armor) {
            armor = null;
        }
    }

    /** Returns the carried item with exactly this name, or null. */
    public Item getItem(String name) {
        for (Item item : inventory) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public void equip(Weapon weapon) {
        this.weapon = weapon;
    }

    public Armor getArmor() {
        return armor;
    }

    public void wear(Armor armor) {
        this.armor = armor;
    }

    /** Armour softens every landed blow, but never below 1 damage. */
    @Override
    public int takeHit(Attack attack, Random rng, Consumer<String> log) {
        if (armor == null || attack.damage() <= 0) {
            return super.takeHit(attack, rng, log);
        }
        int reduced = Math.max(1, attack.damage() - armor.getReduction());
        return super.takeHit(new Attack(reduced, attack.type(), attack.verb(), attack.critical()), rng, log);
    }

    protected int weaponBonus() {
        return weapon == null ? 0 : weapon.getBonus();
    }

    protected DamageType weaponType() {
        return weapon == null ? DamageType.PHYSICAL : weapon.getDamageType();
    }

    /** One-line summary for the stats command. */
    public String status() {
        String s = getName() + " the " + getTitle() + " - HP " + getHealth() + "/" + getMaxHealth();
        if (weapon != null) {
            s += ", wielding the " + weapon.getName();
        }
        if (armor != null) {
            s += ", wearing the " + armor.getName();
        }
        return s;
    }

    /** Called once at the end of every turn the player spends. */
    public void endTurn() {
    }
}
