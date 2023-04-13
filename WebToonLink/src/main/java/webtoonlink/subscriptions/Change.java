package webtoonlink.subscriptions;

import java.io.Serializable;

public class Change implements Serializable {

    private Change() {}

    public static final Change TITLE_CHANGE = new Change();
    public static final Change DESCRIPTION_CHANGE = new Change();
    public static final Change LATEST_EP_CHANGE = new Change();
}
