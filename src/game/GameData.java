package game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import game_character.GameCharacter;
import game_character.player_character.PlayerCharacter;
import item.Item;
import room.Room;

/** Everything loaded from a world file: rooms, characters, items, playable heroes and the goal. */
public class GameData {
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final List<GameCharacter> characters = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Map<PlayerCharacter, Room> heroes = new LinkedHashMap<>();
    private String goal;

    public void addRoom(Room room) {
        rooms.put(room.getName(), room);
    }

    public Room getRoom(String name) {
        return rooms.get(name);
    }

    public Collection<Room> getRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public List<String> getRoomNames() {
        return new ArrayList<>(rooms.keySet());
    }

    public void addCharacter(GameCharacter character) {
        characters.add(character);
    }

    public GameCharacter getCharacter(String name) {
        for (GameCharacter character : characters) {
            if (character.getName().equals(name)) {
                return character;
            }
        }
        return null;
    }

    public List<String> getCharacterNames() {
        List<String> names = new ArrayList<>();
        for (GameCharacter character : characters) {
            names.add(character.getName());
        }
        return names;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Item getItem(String name) {
        for (Item item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public List<String> getItemNames() {
        List<String> names = new ArrayList<>();
        for (Item item : items) {
            names.add(item.getName());
        }
        return names;
    }

    /** Registers a playable hero and the room they start in. */
    public void addHero(PlayerCharacter hero, Room start) {
        characters.add(hero);
        heroes.put(hero, start);
    }

    public Map<PlayerCharacter, Room> getHeroes() {
        return Collections.unmodifiableMap(heroes);
    }

    /** Name of the character whose defeat wins the game, or null to win by clearing every monster. */
    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
