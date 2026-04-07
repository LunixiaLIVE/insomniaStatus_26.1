package net.lunix.nixstats;

public class StatEntry {

    // stat types: phantom | block_mined | item_used | item_crafted | item_broken
    //             item_picked_up | item_dropped | entity_killed | entity_killed_by | custom
    public String statType = "phantom";
    public String targetId = null;   // registry ID or custom stat ID; null for phantom
    public String label    = "Phantom";

    public StatEntry() {}

    public StatEntry(String statType, String targetId, String label) {
        this.statType = statType;
        this.targetId = targetId;
        this.label    = label;
    }

    public static StatEntry phantom() {
        return new StatEntry("phantom", null, "Phantom");
    }
}
