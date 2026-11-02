package item;

/** Outcome of using an item: whether it cost a turn and whether it was used up. */
public enum UseResult {
    /** Nothing happened; no turn spent. */
    NOTHING,
    /** The item worked; a turn is spent. */
    USED,
    /** The item worked and is gone; a turn is spent. */
    CONSUMED
}
