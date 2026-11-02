package item;

import game.Game;
import game_character.player_character.PlayerCharacter;

/** Restores health when drunk. */
public class Potion extends Item {
    private final int amount;

    public Potion(String name, int amount) {
        super(name, "A healing draught. It restores " + amount + " health.");
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public UseResult use(Game game) {
        PlayerCharacter player = game.getPlayer();
        if (player.getHealth() == player.getMaxHealth()) {
            game.say("You are already at full health. Better save the " + getName() + ".");
            return UseResult.NOTHING;
        }
        int healed = player.drinkPotion(amount);
        game.say("You drink the " + getName() + " and recover " + healed + " health.");
        return UseResult.CONSUMED;
    }
}
