package webtoonlink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.text.StringEscapeUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

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

    private Main main;

    private volatile ArrayList<Webtoon> webtoons = new ArrayList<>();
    private volatile ArrayList<Subscriber> subscribers = new ArrayList<>();

    public Bot(String token, Main main) {
        this.main = main;
        this.token = token;
        api = new DiscordApiBuilder().setToken(this.token).login().join();
        api.updateStatus(UserStatus.DO_NOT_DISTURB);
        
        client = new WebClient(BrowserVersion.BEST_SUPPORTED);
    }

    public DiscordApi getApi() {
        return api;
    }

    protected void fromSave(ArrayList<Subscriber> subscribers, ArrayList<Webtoon> webtoons) {
        this.subscribers = subscribers;
        this.webtoons = webtoons;
    }


    private ArrayList<Subscriber> tbd_subscribers = new ArrayList<>();
    private ArrayList<Webtoon> tbd_webtoons = new ArrayList<>();

    private Subscriber subscriber;
    private Webtoon webtoon;
    private String url;
    @Override
    public void run() {

        api.addSlashCommandCreateListener((event) -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();

            if (interaction.getCommandName().equals("webtoons")) {
                if (interaction.getOptionByIndex(0).get().getName().equals("add")) {
                    url = interaction.getOptionByIndex(0).get().getOptionByIndex(0).get().getStringValue().get();
                    url = "https://www." + url.replaceAll("https://", "").replaceAll("www.", "").replaceAll("list\\?", "rss?");


                    subscriber = null;
                    webtoon = null;
                    
                    subscribers.forEach((e) -> {
                        if (e.getChannel(this).equals(interaction.getChannel().get())) {
                            subscriber = e;
                            return;
                        }
                    });
                    webtoons.forEach((e) -> {
                        if (e.getUrl().equals(url)) {
                            webtoon = e;
                            return;
                        }
                    });

                    if (subscriber == null) {
                        subscriber = new Subscriber(interaction.getChannel().get());
                        subscribers.add(subscriber);
                    }
                    if (webtoon == null) {
                        webtoon = new Webtoon(url);
                        webtoons.add(webtoon);
                    }

                    if (subscriber.getSubbed().contains(webtoon)) {
                        interaction.createImmediateResponder().setContent("Already subscribed to webtoon").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                    
                    subscriber.sub(webtoon);
                    webtoon.sub(subscriber);

                    System.out.println(url);
                    XmlPage page;
                    try {
                        page = client.getPage(url);
                        page.getFirstByXPath("/rss/channel/title");
                        title = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/title")).asNormalizedText());    
                    } catch (FailingHttpStatusCodeException | IOException e1) {
                        e1.printStackTrace();
                    }
                    
                    interaction.createImmediateResponder().setContent("Added Webtoon \"" + title + "\" to subscriptions").setFlags(MessageFlag.EPHEMERAL).respond().join();

                    checkWebtoons();
                } else if (interaction.getOptionByIndex(0).get().getName().equals("list")) {
                    subscriber = null;
                    subscribers.forEach((e) -> {
                        if (e.getChannel(this).equals(interaction.getChannel().get())) {
                            subscriber = e;
                            return;
                        }
                    });
                    if (subscriber == null) {
                        interaction.createImmediateResponder().setContent("This channel doesn't have any subscribed webtoons").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                    String temp = "Subscribed Webtoons:";
                    for (int i = 0; i < subscriber.getSubbed().size(); i++) {
                        temp += "\n" + i + ": " + subscriber.getSubbed().get(i).getTitle();
                    }

                    interaction.createImmediateResponder().setContent(temp).setFlags(MessageFlag.EPHEMERAL).respond().join();
                } else if (interaction.getOptionByIndex(0).get().getName().equals("remove")) {
                    final int index = interaction.getOptionByIndex(0).get().getOptionByIndex(0).get().getLongValue().get().intValue();

                    subscriber = null;
                    subscribers.forEach((e) -> {
                        if (e.getChannel(this).equals(interaction.getChannel().get())) {
                            subscriber = e;
                            return;
                        }
                    });
                    if (subscriber == null) {
                        interaction.createImmediateResponder().setContent("This channel doesn't have any subscribed Webtoons");
                        return;
                    }
                    try {
                        tbd_subscribers.add(subscriber);
                        tbd_webtoons.add(subscriber.getSubbed().get(index));
                        interaction.createImmediateResponder().setContent("Are you sure you want to unsubscribe from \"" + subscriber.getSubbed().get(index).getTitle() + "\"?").addComponents(ActionRow.of(
                            Button.danger("confirm_delete", "Yes"),
                            Button.secondary("cancel_delete", "No")
                        )).respond().join();

                        main.save(subscribers, webtoons);
                    } catch (IndexOutOfBoundsException e) {
                        interaction.createImmediateResponder().setContent("Not the index of a subscribed Webtoon").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                }
            } else if (interaction.getCommandName().equals("ping")) {
                interaction.createImmediateResponder().setContent("pinged").setFlags(MessageFlag.EPHEMERAL).respond().join();
            }
        });

        api.addButtonClickListener((event) -> {
            ButtonInteraction interaction = event.getButtonInteraction();
            for (int i = 0; i < tbd_subscribers.size(); i++) {
                if (tbd_subscribers.get(i).getChannel(this).equals(interaction.getChannel().get())) {

                    Subscriber subscriber = tbd_subscribers.remove(i);
                    Webtoon toon = tbd_webtoons.remove(i);

                    if (interaction.getCustomId().equals("confirm_delete")) {
                        subscriber.unsub(toon);
                        if (subscriber.getSubbed().size() < 1)
                            subscribers.remove(subscriber);
                        
                        toon.unsub(subscriber);
                        if (toon.getSubscribers().size() < 1)
                            webtoons.remove(toon);


                        interaction.createImmediateResponder().setContent("Unsubscribed from \"" + toon.getTitle() + "\"").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        interaction.acknowledge();

                    } else if (interaction.getCustomId().equals("cancel_delete")) {
                        interaction.createImmediateResponder().setContent("Cancelled").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        interaction.acknowledge();
                    }
                    return;
                }
            }
        });

        ((ScheduledExecutorService)Executors.newScheduledThreadPool(1)).scheduleAtFixedRate(this::checkWebtoons, 0, 5, TimeUnit.MINUTES);
        
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
                title = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/title")).asNormalizedText());
                desc = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/description")).asNormalizedText());
                eptitle = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/item[1]/title")).asNormalizedText());
                eplink = ((DomElement)page.getFirstByXPath("/rss/channel/item[1]/link")).asNormalizedText();
                epdate = StringEscapeUtils.unescapeHtml4(((DomElement)page.getFirstByXPath("/rss/channel/item[1]/pubDate")).asNormalizedText());

                toon.checkForChanges(title, desc, eptitle, eplink, epdate);
            } catch (FailingHttpStatusCodeException | IOException e) {
                e.printStackTrace();
                toon.getSubscribers().forEach((subscriber) -> {
                    subscriber.getChannel(this).sendMessage("The Webtoon with the name of " + toon.getTitle() + " is unavailable, the subscription will be removed");
                    subscriber.getSubbed().remove(toon);
                    
                });
            }
            return error;//delete all references to this webtoon if something weird happens
        });
        main.save(subscribers, webtoons);
    }
    

    public void start() {
        if (t == null) {
            t = new Thread(this, "bot");
            t.start();
        }
    }

    public void reloadAllSlashCommands() {
        removeAllSlashCommands();
        createPingCommand();
        createWebtoonsCommand();
    }

    protected void removeAllSlashCommands() {
        api.getGlobalSlashCommands().join().forEach(command -> command.delete().join());

        api.getServers().forEach(server -> api.getServerSlashCommands(server).join().forEach(command -> command.delete()));
    }

    protected SlashCommand createPingCommand() {
        return SlashCommand.with("ping", "Pings the bot").createGlobal(api).join();
    }

    protected SlashCommand createWebtoonsCommand() {
        return SlashCommand.with("webtoons", "Manage Webtoon subscriptions", Arrays.asList(
            SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "subscribe to a webtoon", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.STRING, "url", "The url of the Webtoon", true)
            )),
            SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "Lists all subscribed Webtoons with their index number"),
            SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "remove", "Removes a Webtoon from your subscriptions", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.LONG, "index", "The index of the Webtoon to remove from subscriptions", true)
            ))
        )).createGlobal(api).join();
    }
}
