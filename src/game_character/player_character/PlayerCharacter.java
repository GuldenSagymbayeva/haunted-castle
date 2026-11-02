package game_character.player_character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game_character.DamageType;
import game_character.GameCharacter;
import item.Item;
import item.Weapon;

public abstract class PlayerCharacter extends GameCharacter {
    private final List<Item> inventory = new ArrayList<>();
    private Weapon weapon;

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
        return s;
    }

    /** Called once at the end of every turn the player spends. */
    public void endTurn() {
    }
}
