package webtoonlink.subscriptions;

import java.io.Serializable;
import java.util.ArrayList;

import org.javacord.api.entity.channel.TextChannel;

import webtoonlink.Bot;

public class Subscriber implements Serializable {
    private transient TextChannel channel;
    private long channelID;
    private ArrayList<Webtoon> webtoons = new ArrayList<>();

    public final ArrayList<Change> listens = new ArrayList<>();

    public Subscriber(TextChannel channel) {
        this.channel = channel;
        this.channelID = channel.getId();
        this.listens.add(Change.LATEST_EP_CHANGE);
    }

    private Message message = new Message();

    public boolean sub(Webtoon toon) {
        if (!webtoons.contains(toon))
            return webtoons.add(toon);
        else
            return false;
    }

    public boolean unsub(Webtoon toon) {
        return webtoons.remove(toon);
    }

    public ArrayList<Webtoon> getSubbed() {
        return webtoons;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
    public TextChannel getChannel(Bot bot) {
        if (channel == null)
            channel = bot.getApi().getTextChannelById(channelID).get();
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
            channel.sendMessage(message.out(toon));
        }
    }
}
