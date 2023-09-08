package webtoonlink.subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.TextChannel;

import webtoonlink.Bot;

public class Subscriber {
    private transient TextChannel channel;
    private long channelID;
    private long user_id;
    private ArrayList<Webtoon> webtoons = new ArrayList<>();
    
    private boolean isDM;

    public final ArrayList<Change> listens = new ArrayList<>();

    public Subscriber(TextChannel channel, long user_id) {
        this.channel = channel;
        this.channelID = channel.getId();
        this.listens.add(Change.LATEST_EP_CHANGE);
        this.isDM = channel.asServerChannel().isEmpty();
        if (isDM) {
            this.user_id = user_id;
        }
    }

    public Subscriber(long channel_id, String message, int listens, List<Webtoon> webtoons, boolean isDM, long user_id) {
        this.channelID = channel_id;
        this.listens.addAll(Change.decode(listens));
        this.message.in(message);
        this.webtoons.addAll(webtoons);
        this.isDM = isDM;
        if (isDM) {
            this.user_id = user_id;
        }
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
    
    public TextChannel getChannel(Bot bot) throws NoSuchElementException {
        if (channel == null) {
            if (isDM)
                try {
                    channel = bot.getApi().getUserById(user_id).get().sendMessage("Bot Online, this message is required for the bot to work.").join().getChannel().asTextChannel().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new NoSuchElementException("User not found", e);
                }
            else
                channel = bot.getApi().getServerTextChannelById(channelID).get();
        }
        return channel;
    }

    public long getChannelId() {
        return channelID;
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

    public boolean isDM() {
        return isDM;
    }
    public long getUserId() {
        return user_id;
    }
}
