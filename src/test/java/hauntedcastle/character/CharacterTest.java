package hauntedcastle.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import hauntedcastle.character.hero.Fighter;
import hauntedcastle.character.hero.Mage;
import hauntedcastle.character.monster.Ghost;
import hauntedcastle.character.monster.Skeleton;
import hauntedcastle.character.monster.Slime;
import hauntedcastle.character.monster.Vampire;
import hauntedcastle.character.monster.Werewolf;
import hauntedcastle.item.Armor;

class CharacterTest {
    private final Random rng = new Random(1);
    private final List<String> log = new ArrayList<>();

    private int hit(GameCharacter target, int damage, DamageType type) {
        return target.takeHit(new Attack(damage, type, "hits"), rng, log::add);
    }

    @Test
    void vampireSurvivesSteelButNotSilverOrMagic() {
        Vampire v = new Vampire("V");
        hit(v, 500, DamageType.PHYSICAL);
        assertEquals(1, v.getHealth());
        hit(v, 5, DamageType.SILVER);
        assertTrue(v.isVanquished());

        Vampire w = new Vampire("W");
        hit(w, 500, DamageType.MAGIC);
        assertTrue(w.isVanquished());
    }

    @Test
    void vampireHealsByHalfTheDamageItDeals() {
        Vampire v = new Vampire("V");
        hit(v, 30, DamageType.SILVER);
        v.afterDealing(10, log::add);
        assertEquals(v.getMaxHealth() - 25, v.getHealth());
    }

    @Test
    void skeletonReassemblesOnceAfterPhysicalBlows() {
        Skeleton k = new Skeleton("K");
        hit(k, 100, DamageType.PHYSICAL);
        assertFalse(k.isVanquished());
        hit(k, 100, DamageType.PHYSICAL);
        assertTrue(k.isVanquished());
    }

    @Test
    void magicStopsTheSkeletonReassembling() {
        Skeleton k = new Skeleton("K");
        hit(k, 100, DamageType.MAGIC);
        assertTrue(k.isVanquished());
    }

    @Test
    void ghostNeverDodgesSilver() {
        Ghost g = new Ghost("G");
        for (int i = 0; i < 4; i++) {
            assertEquals(5, hit(g, 5, DamageType.SILVER));
        }
        assertTrue(g.isVanquished());
    }

    @Test
    void ghostDodgesSomePhysicalBlows() {
        int dodged = 0;
        for (int i = 0; i < 100; i++) {
            if (hit(new Ghost("G"), 1, DamageType.PHYSICAL) == 0) {
                dodged++;
            }
        }
        assertTrue(dodged > 20 && dodged < 80, "about half should pass through, got " + dodged);
    }

    @Test
    void werewolfTakesDoubleSilverAndEnragesBelowHalf() {
        Werewolf w = new Werewolf("W");
        assertEquals(20, hit(w, 10, DamageType.SILVER));
        assertFalse(w.isEnraged());
        hit(w, 5, DamageType.PHYSICAL);
        assertTrue(w.isEnraged());
        assertTrue(log.stream().anyMatch(line -> line.contains("howls")));
    }

    @Test
    void slimeHitsSofterWhenHurt() {
        Slime s = new Slime("S");
        int fresh = s.attack(new Random(3)).damage();
        hit(s, 36, DamageType.PHYSICAL);
        int hurt = s.attack(new Random(3)).damage();
        assertTrue(hurt < fresh, fresh + " then " + hurt);
    }

    @Test
    void armorReducesBlowsButNeverBelowOne() {
        Fighter f = new Fighter("F");
        f.wear(new Armor("shield", 3));
        assertEquals(7, hit(f, 10, DamageType.PHYSICAL));
        assertEquals(1, hit(f, 2, DamageType.PHYSICAL));
    }

    @Test
    void mageSpendsManaOnFireballsAndRegains() {
        Mage m = new Mage("M");
        Attack first = m.attack(rng);
        assertEquals(DamageType.MAGIC, first.type());
        assertEquals(Mage.MAX_MAGIC - Mage.SPELL_COST, m.getMagic());
        while (m.getMagic() >= Mage.SPELL_COST) {
            m.attack(rng);
        }
        assertEquals(DamageType.PHYSICAL, m.attack(rng).type(), "out of mana: staff");
        int before = m.getMagic();
        m.endTurn();
        assertEquals(before + 2, m.getMagic());
    }

    @Test
    void potionsHealTheLivingAndHurtTheUndead() {
        Fighter f = new Fighter("F");
        f.decreaseHealth(30);
        assertEquals(20, f.drinkPotion(20));
        Skeleton k = new Skeleton("K");
        assertEquals(-20, k.drinkPotion(20));
    }
}
