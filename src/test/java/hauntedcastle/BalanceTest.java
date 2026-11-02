package hauntedcastle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import hauntedcastle.character.Attack;
import hauntedcastle.character.DamageType;
import hauntedcastle.character.hero.Fighter;
import hauntedcastle.character.hero.Mage;
import hauntedcastle.character.hero.PlayerCharacter;
import hauntedcastle.character.monster.Ghost;
import hauntedcastle.character.monster.Monster;
import hauntedcastle.character.monster.Vampire;
import hauntedcastle.character.monster.Werewolf;
import hauntedcastle.game.Difficulty;
import hauntedcastle.item.Armor;
import hauntedcastle.item.Weapon;

/**
 * Monte Carlo balance checks. Plays thousands of simulated fights with the real character
 * classes and a simple strategy (attack; drink a potion when low) to keep the difficulty
 * curve where it was designed: rushing the vampire should usually fail, exploring first
 * should usually win on Normal, and Hard should be clearly harder.
 */
class BalanceTest {
    private static final int RUNS = 3000;

    @Test
    void rushingTheVampireUsuallyFailsOnNormal() {
        double rate = winRate(Fighter::new, Difficulty.NORMAL, false);
        assertTrue(rate < 0.6, "rushing won " + rate);
    }

    @Test
    void exploringFirstUsuallyWinsOnNormal() {
        assertTrue(winRate(Fighter::new, Difficulty.NORMAL, true) > 0.85);
        assertTrue(winRate(Mage::new, Difficulty.NORMAL, true) > 0.7);
    }

    @Test
    void easyIsForgivingAndHardIsHarder() {
        assertTrue(winRate(Fighter::new, Difficulty.EASY, false) > 0.9);
        double normal = winRate(Fighter::new, Difficulty.NORMAL, true);
        double hard = winRate(Fighter::new, Difficulty.HARD, true);
        assertTrue(hard < normal - 0.15, "hard " + hard + " vs normal " + normal);
        assertTrue(hard > 0.3, "hard should still be winnable, got " + hard);
    }

    private interface HeroFactory {
        PlayerCharacter create(String name);
    }

    /**
     * Rush: glowing potion, fight the ghost for the silver dagger, then the vampire.
     * Explore: also collect the chapel and kitchen supplies (potions, holy water) and the
     * werewolf's shield before the final fight.
     */
    private static double winRate(HeroFactory hero, Difficulty difficulty, boolean explore) {
        int wins = 0;
        for (int seed = 0; seed < RUNS; seed++) {
            Random rng = new Random(seed);
            PlayerCharacter player = hero.create("Hero");
            List<Integer> potions = new ArrayList<>(List.of(25));
            int[] holyWater = {0};
            rest(player, 4);
            if (explore) {
                potions.add(15);
                potions.add(30);
                holyWater[0] = 1;
                rest(player, 6);
            }
            boolean alive = fight(player, Ghost::new, rng, difficulty, potions, holyWater);
            player.equip(new Weapon("silver dagger", 6, DamageType.SILVER));
            rest(player, player instanceof Mage ? 12 : 4);
            if (alive && explore) {
                alive = fight(player, Werewolf::new, rng, difficulty, potions, holyWater);
                player.wear(new Armor("steel shield", 3));
                rest(player, player instanceof Mage ? 12 : 3);
            }
            if (alive && fight(player, Vampire::new, rng, difficulty, potions, holyWater)) {
                wins++;
            }
        }
        return (double) wins / RUNS;
    }

    private static boolean fight(PlayerCharacter player, Function<String, Monster> kind,
            Random rng, Difficulty difficulty, List<Integer> potions, int[] holyWater) {
        Monster monster = kind.apply("Monster");
        while (true) {
            if (player.getHealth() <= 15 && !potions.isEmpty()) {
                player.drinkPotion(potions.remove(potions.size() - 1));
            } else if (holyWater[0] > 0 && monster instanceof Vampire) {
                holyWater[0]--;
                monster.takeHit(new Attack(30, DamageType.MAGIC, "throws"), rng, line -> { });
            } else {
                monster.takeHit(player.attack(rng), rng, line -> { });
            }
            if (monster.isVanquished()) {
                player.endTurn();
                return true;
            }
            Attack raw = monster.attack(rng);
            Attack blow = new Attack(difficulty.scaleDamageTaken(raw.damage()), raw.type(), raw.verb());
            monster.afterDealing(player.takeHit(blow, rng, line -> { }), line -> { });
            player.endTurn();
            if (player.isVanquished()) {
                return false;
            }
        }
    }

    private static void rest(PlayerCharacter player, int turns) {
        for (int i = 0; i < turns; i++) {
            player.endTurn();
        }
    }
}
