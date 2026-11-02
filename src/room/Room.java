package room;

import java.util.ArrayList;
import java.util.List;

import game.GameEntity;
import game_character.monster.Monster;
import item.Item;

public class Room {
    private final String name;
    private String description = "";
    private final List<Exit> exits = new ArrayList<>();
    private final List<GameEntity> gameEntities = new ArrayList<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void placeInRoom(GameEntity entity) {
        gameEntities.add(entity);
    }

    public void removeFromRoom(GameEntity entity) {
        gameEntities.remove(entity);
    }

    public List<Item> getItems() {
        List<Item> items = new ArrayList<>();
        for (GameEntity entity : gameEntities) {
            if (entity instanceof Item item) {
                items.add(item);
            }
        }
        return items;
    }

    /** All monsters here, vanquished ones included. */
    public List<Monster> getMonsters() {
        List<Monster> monsters = new ArrayList<>();
        for (GameEntity entity : gameEntities) {
            if (entity instanceof Monster monster) {
                monsters.add(monster);
            }
        }
        return monsters;
    }

    public List<Monster> getLivingMonsters() {
        List<Monster> living = new ArrayList<>();
        for (Monster monster : getMonsters()) {
            if (!monster.isVanquished()) {
                living.add(monster);
            }
        }
        return living;
    }

    public Exit addExit(Room target) {
        Exit exit = new Exit(target);
        exits.add(exit);
        return exit;
    }

    public List<Exit> getExits() {
        return new ArrayList<>(exits);
    }

    public List<Exit> getVisibleExits() {
        List<Exit> visible = new ArrayList<>();
        for (Exit exit : exits) {
            if (!exit.isHidden()) {
                visible.add(exit);
            }
        }
        return visible;
    }

    /** The exit leading to {@code target}, or null. */
    public Exit getExitTo(Room target) {
        for (Exit exit : exits) {
            if (exit.getTarget() == target) {
                return exit;
            }
        }
        return null;
    }

    /** Creates a two-way connection and returns the exit from room1 to room2. */
    public static Exit connectRooms(Room room1, Room room2) {
        room2.addExit(room1);
        return room1.addExit(room2);
    }
}
