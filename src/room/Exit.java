package room;

/** A one-way path to another room. It may be locked by a key or hidden until an item reveals it. */
public class Exit {
    private final Room target;
    private String requiredKey;
    private String revealedBy;

    public Exit(Room target) {
        this.target = target;
    }

    public Room getTarget() {
        return target;
    }

    public boolean isLocked() {
        return requiredKey != null;
    }

    /** Name of the key that opens this exit, or null when unlocked. */
    public String getRequiredKey() {
        return requiredKey;
    }

    public void lock(String keyName) {
        requiredKey = keyName;
    }

    public void unlock() {
        requiredKey = null;
    }

    public boolean isHidden() {
        return revealedBy != null;
    }

    /** Name of the item that reveals this exit, or null when visible. */
    public String getRevealedBy() {
        return revealedBy;
    }

    public void hideUntil(String itemName) {
        revealedBy = itemName;
    }

    public void reveal() {
        revealedBy = null;
    }
}
