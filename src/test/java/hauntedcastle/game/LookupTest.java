package hauntedcastle.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import hauntedcastle.item.Item;

class LookupTest {
    private final Item key = new Item("iron key");
    private final Item keyRing = new Item("iron key ring");
    private final Item tome = new Item("ancient tome");
    private final List<Item> items = List.of(keyRing, key, tome);

    @Test
    void exactNameWinsOverPrefix() {
        assertEquals(key, Lookup.find(items, "iron key"));
    }

    @Test
    void matchesPrefixAndAnyWordPrefix() {
        assertEquals(tome, Lookup.find(items, "anc"));
        assertEquals(tome, Lookup.find(items, "tome"));
    }

    @Test
    void ignoresCaseUnderscoresAndLeadingThe() {
        assertEquals(tome, Lookup.find(items, "The Ancient_Tome"));
    }

    @Test
    void returnsNullForNoMatchOrBlank() {
        assertNull(Lookup.find(items, "sword"));
        assertNull(Lookup.find(items, "  "));
    }
}
