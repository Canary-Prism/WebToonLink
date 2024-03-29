package webtoonlink.subscriptions;

import java.util.ArrayList;

public class Webtoon {
    /**
     * Must be the full url to the rss of the Webtoon
     */
    private String url;
    private ArrayList<Subscriber> subscribers = new ArrayList<>();

    private boolean isNew = true;

    private String
        title = "",
        desc = "",
        eptitle = "",
        eplink = "",
        epdate = "";
    
    public Webtoon(String url) {
        this.url = url;
    }
    public Webtoon(String url, String title, String desc, String eptitle, String eplink, String epdate) {
        this.url = url;
        this.title = title;
        this.desc = desc;
        this.eptitle = eptitle;
        this.eplink = eplink;
        this.epdate = epdate;
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
        if (this.title == null || !this.title.equals(title))
            changes.add(Change.TITLE_CHANGE);
        if (this.desc == null || !this.desc.equals(desc))
            changes.add(Change.DESCRIPTION_CHANGE);
        if (this.epdate == null || !this.epdate.equals(epdate))
            changes.add(Change.LATEST_EP_CHANGE);

        this.title = title; this.desc = desc; this.eptitle = eptitle; this.eplink = eplink; this.epdate = epdate;

        subscribers.forEach((e) -> {
            e.broadcast(this, changes);
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Webtoon) {
            return ((Webtoon) obj).getUrl().equals(url);
        }
        return false;
    }
}
