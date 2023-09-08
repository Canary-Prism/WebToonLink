package webtoonlink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.text.StringEscapeUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

import webtoonlink.subscriptions.Webtoon;
import webtoonlink.subscriptions.Change;
import webtoonlink.subscriptions.Message;
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
        api = new DiscordApiBuilder().setToken(this.token).addIntents(Intent.DIRECT_MESSAGES, Intent.GUILDS).login().join();
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

        subscribers.forEach((e) -> {
            e.getChannel(this);
        });

        subscribers.removeIf((e) -> {
            try {
                e.getChannel(this);
            } catch (NoSuchElementException nsee) {
                webtoons.forEach((toon) -> {
                    toon.unsub(e);
                });
                return true;
            }
            return false;
        });



        api.addSlashCommandCreateListener((event) -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();

            if (interaction.getCommandName().equals("webtoons")) {
                if (interaction.getOptionByIndex(0).get().getName().equals("add")) {
                    url = interaction.getOptionByIndex(0).get().getOptionByIndex(0).get().getStringValue().get();
                    url = "https://www." + url.replaceAll("https://", "").replaceAll("www.", "").replaceAll("list\\?", "rss?").replaceAll("canvas", "challenge");


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
                        subscriber = new Subscriber(interaction.getChannel().get(), interaction.getUser().getId());
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
                        interaction.createImmediateResponder().setContent("Invalid Webtoon URL").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
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
                        )).setFlags(MessageFlag.EPHEMERAL).respond().join();

                        main.save(subscribers, webtoons);
                    } catch (IndexOutOfBoundsException e) {
                        interaction.createImmediateResponder().setContent("Not the index of a subscribed Webtoon").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                }
            } else if (interaction.getCommandName().equals("message")) {
                if (interaction.getOptionByIndex(0).get().getName().equals("set")) {
                    String str = interaction.getOptionByIndex(0).get().getOptionByIndex(0).get().getStringValue().get();
                    subscriber = null;
                    subscribers.forEach((e) -> {
                        if (e.getChannel(this).equals(interaction.getChannel().get())) {
                            subscriber = e;
                            return;
                        }
                    });
                    if (subscriber == null) {
                        interaction.createImmediateResponder().setContent("Subscribe to a Webtoon first").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                    Message message = new Message().in(str);
                    subscriber.setMessage(message);
                    interaction.createImmediateResponder().setContent("Done. here's an example of what that might look like: \n" + message.out(subscriber.getSubbed().get(0))).setFlags(MessageFlag.EPHEMERAL).respond().join();
                } else if (interaction.getOptionByIndex(0).get().getName().equals("get")) {
                    subscriber = null;
                    subscribers.forEach((e) -> {
                        if (e.getChannel(this).equals(interaction.getChannel().get())) {
                            subscriber = e;
                            return;
                        }
                    });
                    if (subscriber == null) {
                        interaction.createImmediateResponder().setContent("Subscribe to a Webtoon first").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        return;
                    }
                    interaction.createImmediateResponder().setContent("The current message that is set is: \n" + subscriber.getMessage().rawOut() + "\nwhich would look something like this: \n" + subscriber.getMessage().out(subscriber.getSubbed().get(0))).setFlags(MessageFlag.EPHEMERAL).respond().join();
                } else if ((interaction.getOptionByIndex(0).get().getName().equals("help"))) {
                    interaction.createImmediateResponder().setContent("To set a new Message, do `/message set [Message]`\nYou can type literal words, or use some tags for attributes of the Webtoon\n`<title>` the webtoon title\n`<desc>` the webtoon description\n`<epTitle>` the latest episode title\n`<epLink>` the latest episode link\n`<epPubDate>` the latest episode publish date\n`<br>` line break\n`<hide>` text hider (this hides all text after it but keeps them functional. useful for hiding the urls if you just want embeds)").setFlags(MessageFlag.EPHEMERAL).respond().join();
                }
            } else if (interaction.getCommandName().equals("notify")) {
                subscriber = null;
                subscribers.forEach((e) -> {
                    if (e.getChannel(this).equals(interaction.getChannel().get())) {
                        subscriber = e;
                        return;
                    }
                });
                if (subscriber == null) {
                    interaction.createImmediateResponder().setContent("Subscribe to a Webtoon first").setFlags(MessageFlag.EPHEMERAL).respond().join();
                    return;
                }
                switch (interaction.getOptionByIndex(0).get().getLongValue().get().intValue()) {
                    case 0 -> {
                        if (subscriber.listens.contains(Change.TITLE_CHANGE))
                            interaction.createImmediateResponder().setContent("Title Change notifications are **On**").addComponents(ActionRow.of(
                                Button.danger("0notif_off", "Turn Off")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                        else
                            interaction.createImmediateResponder().setContent("Title Change notifications are **Off**").addComponents(ActionRow.of(
                                Button.success("0notif_on", "Turn On")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                    }
                    case 1 -> {
                        if (subscriber.listens.contains(Change.DESCRIPTION_CHANGE))
                            interaction.createImmediateResponder().setContent("Description Change notifications are **On**").addComponents(ActionRow.of(
                                Button.danger("1notif_off", "Turn Off")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                        else
                            interaction.createImmediateResponder().setContent("Description Change notifications are **Off**").addComponents(ActionRow.of(
                                Button.success("1notif_on", "Turn On")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                    }
                    case 2 -> {
                        if (subscriber.listens.contains(Change.LATEST_EP_CHANGE))
                            interaction.createImmediateResponder().setContent("Latest Episode Change notifications are **On**").addComponents(ActionRow.of(
                                Button.danger("2notif_off", "Turn Off")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                        else
                            interaction.createImmediateResponder().setContent("Latest Episode Change notifications are **Off**").addComponents(ActionRow.of(
                                Button.success("2notif_on", "Turn On")
                            )).setFlags(MessageFlag.EPHEMERAL).respond().join();
                    }
                }
            } else if (interaction.getCommandName().equals("ping")) {
                interaction.createImmediateResponder().setContent("pinged").setFlags(MessageFlag.EPHEMERAL).respond().join();
            }
        });

        api.addButtonClickListener((event) -> {
            ButtonInteraction interaction = event.getButtonInteraction();
            if (interaction.getCustomId().contains("delete"))
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
            else if (interaction.getCustomId().contains("notif")) {
                subscriber = null;
                subscribers.forEach((e) -> {
                    if (e.getChannel(this).equals(interaction.getChannel().get())) {
                        subscriber = e;
                        return;
                    }
                });
                if (subscriber == null) {
                    interaction.createImmediateResponder().setContent("Subscribe to a Webtoon first").setFlags(MessageFlag.EPHEMERAL).respond().join();
                    return;
                }
                switch (interaction.getCustomId().charAt(0)) {
                    case '0' -> {
                        if (interaction.getCustomId().contains("on")) {
                            if (subscriber.listens.contains(Change.TITLE_CHANGE))
                                return;
                            subscriber.listens.add(Change.TITLE_CHANGE);
                            interaction.createImmediateResponder().setContent("Now being notified of Title Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        } else {
                            if (!subscriber.listens.contains(Change.TITLE_CHANGE))
                                return;
                            subscriber.listens.remove(Change.TITLE_CHANGE);
                            interaction.createImmediateResponder().setContent("No longer being notified of Title Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();

                        }
                    }
                    case '1' -> {
                        if (interaction.getCustomId().contains("on")) {
                            if (subscriber.listens.contains(Change.DESCRIPTION_CHANGE))
                                return;
                            subscriber.listens.add(Change.DESCRIPTION_CHANGE);
                            interaction.createImmediateResponder().setContent("Now being notified of Description Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        } else {
                            if (!subscriber.listens.contains(Change.DESCRIPTION_CHANGE))
                                return;
                            subscriber.listens.remove(Change.DESCRIPTION_CHANGE);
                            interaction.createImmediateResponder().setContent("No longer being notified of Description Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();

                        }
                    }
                    case '2' -> {
                        if (interaction.getCustomId().contains("on")) {
                            if (subscriber.listens.contains(Change.LATEST_EP_CHANGE))
                                return;
                            subscriber.listens.add(Change.LATEST_EP_CHANGE);
                            interaction.createImmediateResponder().setContent("Now being notified of Latest Episode Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();
                        } else {
                            if (!subscriber.listens.contains(Change.LATEST_EP_CHANGE))
                                return;
                            subscriber.listens.remove(Change.LATEST_EP_CHANGE);
                            interaction.createImmediateResponder().setContent("No longer being notified of Latests Episode Changes").setFlags(MessageFlag.EPHEMERAL).respond().join();

                        }
                    }
                }
                interaction.acknowledge();
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
        subscribers.removeIf((e) -> {
            try {
                e.getChannel(this);
            } catch (NoSuchElementException nsee) {
                webtoons.forEach((toon) -> {
                    toon.unsub(e);
                });
                return true;
            }
            return false;
        });
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
            } catch (FailingHttpStatusCodeException | IOException | ClassCastException e) {
                error = true;
                toon.getSubscribers().forEach((subscriber) -> {
                    subscriber.getChannel(this).sendMessage("The Webtoon with the url of " + toon.getUrl() + " is unavailable, the subscription will be removed");
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
        createMessageCommand();
        createNotifyCommand();
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

    protected SlashCommand createMessageCommand() {
        return SlashCommand.with("message", "Manage the messages that get sent when a Webtoon updates", Arrays.asList(
            SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Set a the message that will be sent", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.STRING, "message", "The message to set to", true)
            )),
            SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "get", "Get the current message that will be sent"),
            SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "help", "Get help about this thing")
        )).createGlobal(api).join();
    }

    protected SlashCommand createNotifyCommand() {
        return SlashCommand.with("notify", "Manage what changes to the Webtoon this channel will listen to", Arrays.asList(
            SlashCommandOption.createWithChoices(SlashCommandOptionType.LONG, "of", "Choosing a change", true,  Arrays.asList(
                SlashCommandOptionChoice.create("Title Change", 0),
                SlashCommandOptionChoice.create("Description Change", 1),
                SlashCommandOptionChoice.create("Latest Episode Change", 2)                
            ))
        )).createGlobal(api).join();
    }
}
