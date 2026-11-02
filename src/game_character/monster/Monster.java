package game_character.monster;

import game_character.GameCharacter;

public abstract class Monster extends GameCharacter {

    protected Monster(String name, int maxHealth, int strength) {
        super(name, maxHealth, strength);
    }

    /** Monster kind shown to the player, e.g. "Vampire". */
    public String getKind() {
        return getClass().getSimpleName();
    }

    /** Line shown when the player walks into this monster's room. */
    public abstract String taunt();
}
