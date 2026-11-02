package hauntedcastle.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import hauntedcastle.character.monster.Werewolf;
import hauntedcastle.item.Armor;
import hauntedcastle.item.ThrownItem;
import hauntedcastle.world.Exit;

class GameLoaderTest {

    @Test
    void loadsBuiltInWorld() throws IOException {
        GameData data = GameLoader.loadResource(GameLoader.DEFAULT_WORLD);
        assertEquals(11, data.getRoomNames().size());
        assertEquals(2, data.getHeroes().size());
        assertEquals("Count Orlok", data.getGoal());
        assertInstanceOf(Werewolf.class, data.getCharacter("Grimfang"));
        assertInstanceOf(Armor.class, data.getItem("steel shield"));
        assertInstanceOf(ThrownItem.class, data.getItem("holy water"));
    }

    @Test
    void lockedAndHiddenExits() throws IOException {
        GameData data = GameLoader.loadResource(GameLoader.DEFAULT_WORLD);
        Exit gate = data.getRoom("dungeon").getExitTo(data.getRoom("crypt"));
        assertTrue(gate.isLocked());
        assertEquals("iron key", gate.getRequiredKey());
        assertTrue(data.getRoom("crypt").getExitTo(data.getRoom("dungeon")).isLocked(), "lock works both ways");
        Exit passage = data.getRoom("library").getExitTo(data.getRoom("secret passage"));
        assertTrue(passage.isHidden());
        assertEquals("ancient tome", passage.getRevealedBy());
    }

    @Test
    void descriptionsApplyToRoomsItemsAndCharacters() throws IOException {
        GameData data = load("ROOM hall", "FIGHTER Bob hall", "ITEM cup hall",
                "DESC hall A big hall.", "DESC cup A tin cup.", "DESC Bob A knight.");
        assertEquals("A big hall.", data.getRoom("hall").getDescription());
        assertEquals("A tin cup.", data.getItem("cup").getDescription());
        assertEquals("A knight.", data.getCharacter("Bob").getDescription());
    }

    @Test
    void errorsNameTheLine() {
        IOException e = assertThrows(IOException.class, () -> load("ROOM hall", "", "DRAGON Smaug hall"));
        assertEquals("test line 3: unknown command DRAGON", e.getMessage());
    }

    @Test
    void rejectsUnknownRoomsAndBadItems() {
        assertThrows(IOException.class, () -> load("ROOM hall", "FIGHTER Bob attic"));
        assertThrows(IOException.class, () -> load("ROOM hall", "FIGHTER Bob hall", "ITEM vial hall POTION"));
        assertThrows(IOException.class, () -> load("ROOM hall", "FIGHTER Bob hall", "ITEM vial hall POTION lots"));
        assertThrows(IOException.class, () -> load("ROOM hall", "FIGHTER Bob hall", "ITEM axe hall WEAPON 3 FIRE"));
    }

    @Test
    void requiresAHeroAndAKnownGoal() {
        assertThrows(IOException.class, () -> load("ROOM hall"));
        assertThrows(IOException.class, () -> load("ROOM hall", "FIGHTER Bob hall", "GOAL Nobody"));
    }

    private static GameData load(String... lines) throws IOException {
        return GameLoader.loadGameData(List.of(lines), "test");
    }
}
