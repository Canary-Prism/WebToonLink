package webtoonlink.subscriptions;

import java.util.ArrayList;

public class Webtoon {
    /**
     * Must be the full url to the rss of the Webtoon
     */
    private String url;
    private ArrayList<Subscriber> subscribers = new ArrayList<>();

    private String
        title,
        desc,
        eptitle,
        eplink,
        epdate;
    
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
    
    public void sub(Subscriber subscriber) {
        subscribers.add(subscriber);
    }
    public boolean unsub(Subscriber subscriber) {
        return subscribers.remove(subscriber);
    }

    public ArrayList<Subscriber> getChannels() {
        return subscribers;
    }

    private ArrayList<Change> changes;

    public void checkForChanges(String title, String desc, String eptitle, String eplink, String epdate) {
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
