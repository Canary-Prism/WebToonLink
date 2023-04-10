package webtoonlink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.text.StringEscapeUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.UserStatus;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

import webtoonlink.subscriptions.Webtoon;
import webtoonlink.subscriptions.Subscriber;

public class Bot implements Runnable {

    private Thread t;

    private String token;

    private WebClient client;
    
    private DiscordApi api;

    private volatile ArrayList<Webtoon> webtoons = new ArrayList<>();
    private volatile ArrayList<Subscriber> subscribers = new ArrayList<>();

    public Bot(String token) {
        this.token = token;
        api = new DiscordApiBuilder().setToken(token).login().join();
        api.updateStatus(UserStatus.DO_NOT_DISTURB);
        
        client = new WebClient(BrowserVersion.BEST_SUPPORTED);
    }

    @Override
    public void run() {

        ((ScheduledExecutorService)Executors.newFixedThreadPool(1)).scheduleAtFixedRate(this::checkWebtoons, 0, 30, TimeUnit.MINUTES);
        
        api.updateStatus(UserStatus.ONLINE);
    }

    private boolean error;
    private String
        title,
        desc,
        eptitle,
        eplink,
        epdate;

    private synchronized void checkWebtoons() {
        error = false;
        webtoons.removeIf((toon) -> {
            try {
                XmlPage page = client.getPage(toon.getUrl());
                page.getFirstByXPath("/rss/channel/title");
                title = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/title")).asNormalizedText());
                desc = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/description")).asNormalizedText());
                eptitle = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/item[1]/title")).asNormalizedText());
                eplink = ((DomElement)page.getFirstByXPath("/rss/channel/item[1]/link")).asNormalizedText();
                epdate = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/item[1]/pubDate")).asNormalizedText());

                toon.checkForChanges(eptitle, desc, eptitle, eplink, epdate);
            } catch (FailingHttpStatusCodeException | IOException e) {
                e.printStackTrace();
                toon.getChannels().forEach((subscriber) -> {
                    subscriber.getChannel().sendMessage("The Webtoon with the name of " + toon.getName() + " is unavailable, the subscription will be removed");
                    subscriber.getSubbed().remove(toon);
                    
                });
            }
            return error;//delete all references to this webtoon if something weird happens
        });
    }
    

    public void start() {
        if (t == null) {
            t = new Thread(this, "bot");
            t.start();
        }
    }
}
