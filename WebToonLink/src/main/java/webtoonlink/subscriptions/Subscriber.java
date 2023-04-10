package webtoonlink.subscriptions;

import java.util.ArrayList;

import org.javacord.api.entity.channel.TextChannel;

public class Subscriber {
    private TextChannel channel;
    private ArrayList<Webtoon> webtoons;

    private ArrayList<Change> listens;

    public ArrayList<Webtoon> getSubbed() {
        return webtoons;
    }
    
    public TextChannel getChannel() {
        return channel;
    }

    private boolean cares;
    public void broadcast(Webtoon toon, ArrayList<Change> changes) {
        cares = false;
        for (var change : changes)
            if (listens.contains(change)) {
                cares = true;
                break;
            }
        if (cares) {
            
        }
    }
}
