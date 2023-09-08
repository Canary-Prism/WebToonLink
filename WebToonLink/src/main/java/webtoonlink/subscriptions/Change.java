package webtoonlink.subscriptions;

import java.util.ArrayList;

public enum Change {
    TITLE_CHANGE, //1
    DESCRIPTION_CHANGE, //2
    LATEST_EP_CHANGE; //4

    public int encode() {
        return 1 << ordinal();
    }
    public static ArrayList<Change> decode(int bit) {
        ArrayList<Change> changes = new ArrayList<>();
        for (Change change : Change.values()) {
            if ((bit & change.encode()) != 0)
                changes.add(change);
        }
        return changes;
    }
}
