package webtoonlink.subscriptions;

import java.io.Serializable;
import java.util.ArrayList;

public class Webtoon implements Serializable {
    /**
     * Must be the full url to the rss of the Webtoon
     */
    private String url;
    private ArrayList<Subscriber> subscribers = new ArrayList<>();

    private transient boolean isNew = true;

    private transient String
        title = "",
        desc = "",
        eptitle = "",
        eplink = "",
        epdate = "";
    
    public Webtoon(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }
    public String getDesc() {
        return desc;
    }
    public String getEptitle() {
        return eptitle;
    }
    public String getEplink() {
        return eplink;
    }
    public String getEpdate() {
        return epdate;
    }
    
    public boolean sub(Subscriber subscriber) {
        if (!subscribers.contains(subscriber))
            return subscribers.add(subscriber);
        else
            return false;
    }
    public boolean unsub(Subscriber subscriber) {
        return subscribers.remove(subscriber);
    }

    public ArrayList<Subscriber> getSubscribers() {
        return subscribers;
    }

    private ArrayList<Change> changes = new ArrayList<>();

    public void checkForChanges(String title, String desc, String eptitle, String eplink, String epdate) {
        if (isNew) {
            isNew = false;
            this.title = title; this.desc = desc; this.eptitle = eptitle; this.eplink = eplink; this.epdate = epdate;
            return;
        }

        changes.clear();
        if (!this.title.equals(title))
            changes.add(Change.TITLE_CHANGE);
        if (!this.desc.equals(desc))
            changes.add(Change.DESCRIPTION_CHANGE);
        if (!this.epdate.equals(epdate))
            changes.add(Change.LATEST_EP_CHANGE);

        this.title = title; this.desc = desc; this.eptitle = eptitle; this.eplink = eplink; this.epdate = epdate;

        subscribers.forEach((e) -> {
            e.broadcast(this, changes);
        });
    }
}
