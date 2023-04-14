/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package webtoonlink;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import webtoonlink.subscriptions.Subscriber;
import webtoonlink.subscriptions.Webtoon;

import java.awt.BorderLayout;
import java.awt.Taskbar;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Main {

    public static final int READ_BUFFER_SIZE = 200;

    private Bot bot;
    {{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}

    private String workingDirectory;

    private final static String[] CHANGELOG = {
        "remembered that Changelogs are a thing",
        "updated the icon to have the \"LINK\" word be transparent"
    };
    private final static String VERSION = "1.1";
    
    private JFrame frame = new JFrame("WebToonLink");
    private final static int ROOM = 20;
    
    private JLabel dud = new JLabel();
    
    public static void main(String args[]) {
        new Main().load();
    }
    
    public Main() {
        //here, we assign the name of the OS, according to Java, to a variable...
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            workingDirectory = System.getenv("AppData");
        }
        else {
            //in either case, we would start in the user's home directory
            workingDirectory = System.getProperty("user.home");
            //if we are on a Mac, we are not done, we look for "Application Support"
            workingDirectory += "/Library/Application Support";
        }

        workingDirectory += "/WebToonLink";
        //we are now free to set the workingDirectory to the subdirectory that is our 
        //folder.

        frame.setIconImage(new ImageIcon(this.getClass().getClassLoader().getResource("icon/WebToonLink.png")).getImage());


        final Taskbar tb = Taskbar.getTaskbar();
        if (System.getProperty("os.name").contains("Mac"))
            tb.setIconImage(new ImageIcon(Main.class.getClassLoader().getResource("icon/WebToonLink.png")).getImage());

        folder = new File(workingDirectory);
        save_bot = new File(workingDirectory + "/bot.json");
        save_webtoons = new File(workingDirectory + "/webtoons");
        save_subscribers = new File(workingDirectory + "/subscribers");

        
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        main_label.setBounds(Main.ROOM + 10, Main.ROOM + 0, 200, 100);
        main_label.setVerticalAlignment(SwingConstants.TOP);
        
        main_reload_button.setBounds(Main.ROOM + 0, Main.ROOM + 110, 200, 30);
        main_reload_button.addActionListener((e) -> bot.reloadAllSlashCommands());
        
        main_about_button.setBounds(Main.ROOM + 0, Main.ROOM + 140, 200, 30);
        main_about_button.addActionListener(this::aboutMenu);
        
        main_token_button.setBounds(Main.ROOM + 0, Main.ROOM + 170, 200, 30);
        main_token_button.addActionListener(this::tokenMenu);
        
        about_label.setBounds(Main.ROOM + 0, Main.ROOM + 0, 450, 250);
        about_label.setVerticalAlignment(SwingConstants.TOP);
        
        about_changelog_button.setBounds(Main.ROOM + 0, Main.ROOM + 200, 100, 30);
        about_changelog_button.addActionListener(this::changelogMenu);
        
        about_version_label.setBounds(Main.ROOM + 100, Main.ROOM + 200, 100, 30);
        
        about_back_button.setBounds(Main.ROOM + 350, Main.ROOM + 200, 100, 30);
        about_back_button.addActionListener(this::mainMenu);
        

        String temp = "<html><h1>Changelog</h1>" + Main.VERSION;
        for (int i = 0; i < Main.CHANGELOG.length; i++) {
            temp += "<br />•" + Main.CHANGELOG[i];
        }
        temp += "</html>";
        
        changelog_label.setText(temp);
        changelog_label.setVerticalAlignment(SwingConstants.TOP);
        
        changelog_back_button.addActionListener((e) -> {frame.setResizable(false);mainMenu(e);});
        

        
        token_field.setBounds(Main.ROOM + 0, Main.ROOM + 0, 500, 30);
        
        token_cancel_button.setBounds(Main.ROOM + 200, Main.ROOM + 30, 100, 30);
        token_cancel_button.addActionListener((e) -> {
            token_field.setText(token);
        });
        
        token_save_button.setBounds(Main.ROOM + 300, Main.ROOM + 30, 200, 30);
        token_save_button.addActionListener((e) -> {
            token = token_field.getText();
            save(token);
            start();
        });

        token_back_button.setBounds(Main.ROOM + 0, Main.ROOM + 30, 100, 30);
        token_back_button.addActionListener(this::mainMenu);

        loading_label.setBounds(Main.ROOM + 0, Main.ROOM + 0, 190, 100);
        loading_label.setVerticalAlignment(SwingConstants.TOP);

        frame.setVisible(true);
    }

    private void load() {
        loadingMenu(null);

        loadSaves();

        if (token == null) {
            tokenMenu(null);
        } else
            start();
    }

    private void start() {
        loadingMenu(null);
        bot = new Bot(token, this);
        if (subscribers != null && webtoons != null)
            bot.fromSave(subscribers, webtoons);

        bot.start();
        
        mainMenu(null);
    }

    private File folder, save_bot, save_webtoons, save_subscribers;

    private String token;

    private ArrayList<Subscriber> subscribers;
    private ArrayList<Webtoon> webtoons;

    @SuppressWarnings("unchecked")
    private void loadSaves() {
        try {
            if (!folder.exists())
                folder.mkdirs();
            else {
                if (save_bot.exists()) {
                    try (FileReader fr = new FileReader(save_bot)) {
                        final char[] cbuff = new char[READ_BUFFER_SIZE];
                        fr.read(cbuff);
                        JSONObject contents = new JSONObject(String.valueOf(cbuff));
                        token = contents.getString("token");
                        token_field.setText(token);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    save_bot.createNewFile();
                }

                if (save_webtoons.exists()) {
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(save_webtoons));) {
                        webtoons = ((ArrayList<Webtoon>)in.readObject());
                    } catch (InvalidClassException | ClassNotFoundException e) {}
                } else {
                    save_webtoons.createNewFile();
                }

                if (save_subscribers.exists()) {
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(save_subscribers));) {
                        subscribers = ((ArrayList<Subscriber>)in.readObject());
                    } catch (InvalidClassException | ClassNotFoundException e) {}
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot Access Save Data", "sadness", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    protected void save(String token) {
        try (FileWriter fw = new FileWriter(save_bot)) {
            new JSONWriter(fw)
            .object()
                .key("token")
                .value(token)
            .endObject();

            fw.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot Access Save Data", "sadness", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    protected void save(ArrayList<Subscriber> subscribers, ArrayList<Webtoon> webtoons) {
        try (
            ObjectOutputStream subscribers_out = new ObjectOutputStream(new FileOutputStream(save_subscribers));
            ObjectOutputStream webtoons_out = new ObjectOutputStream(new FileOutputStream(save_webtoons));
        ) {
            subscribers_out.writeObject(subscribers);
            webtoons_out.writeObject(webtoons);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot Access Save Data", "sadness", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }
    
    private JLabel main_label = new JLabel("<html><h1>WebToonLink</h1>control panel</html>");

    private JButton main_reload_button = new JButton("Reload Slash Commands");
    private JButton main_about_button = new JButton("About");
    private JButton main_token_button = new JButton("Bot Token");
    
    private void mainMenu(ActionEvent e) {
        frame.getContentPane().removeAll();
        
        frame.getContentPane().add(main_label);

        frame.getContentPane().add(main_reload_button);
        frame.getContentPane().add(main_about_button);
        frame.getContentPane().add(main_token_button);
        
        frame.getContentPane().add(dud);
        
        frame.setSize(200 + Main.ROOM * 2, 220 + Main.ROOM * 2);
    }
    

    
    

    private JLabel about_label = new JLabel("<html><h1>About</h1>Hi, i'm Canary Prism, and this is a little bot i made to make webtoon notifications better. now obviously, Webtoon notifications suck, more than discord, so this bot will send a message when a subscribed webtoon has an update, and sends a message to all channels that subscribed to it, with a customisable message<br>this bot checks every webtoon every 5 mins<br>this UI is also mostly for monitoring, most of the interacting with the bot is through Slash Commands</html>");
    private JButton about_changelog_button = new JButton("Changelog");
    private JLabel about_version_label = new JLabel(Main.VERSION);
    private JButton about_back_button = new JButton("Back");
    
    private void aboutMenu(ActionEvent e) {
        frame.getContentPane().removeAll();
        
        frame.getContentPane().add(about_label);
        frame.getContentPane().add(about_changelog_button);
        frame.getContentPane().add(about_version_label);
        frame.getContentPane().add(about_back_button);
        
        frame.getContentPane().add(dud);
        
        frame.setSize(450 + Main.ROOM * 2, 250 + Main.ROOM * 2);
    }
    

    private JLabel changelog_label = new JLabel();
    private JButton changelog_back_button = new JButton("back");
    
    private void changelogMenu(ActionEvent e) {
        frame.getContentPane().removeAll();
        
        frame.getContentPane().add(changelog_back_button, BorderLayout.PAGE_END);
        frame.getContentPane().add(changelog_label);
        
        frame.setResizable(true);
        frame.setSize(400 + Main.ROOM * 2, Main.CHANGELOG.length * 30 + 140 + Main.ROOM * 2);
    }
    

    private JTextField token_field = new JTextField();
    private JButton token_cancel_button = new JButton("Cancel");
    private JButton token_save_button = new JButton("Save and Reload");
    private JButton token_back_button = new JButton("Back");
    
    private void tokenMenu(ActionEvent e) {
        frame.getContentPane().removeAll();
        
        frame.getContentPane().add(token_field);
        frame.getContentPane().add(token_cancel_button);
        frame.getContentPane().add(token_save_button);
        frame.getContentPane().add(token_back_button);
        
        frame.getContentPane().add(dud);
        
        frame.setSize(500 + Main.ROOM * 2, 80 + Main.ROOM * 2);
    }
    
    private JLabel loading_label = new JLabel("<html><h1>Loading Bot...</h1>please wait</html>");
    
    private void loadingMenu(ActionEvent e) {
        frame.getContentPane().removeAll();
        
        frame.getContentPane().add(loading_label);
        
        frame.getContentPane().add(dud);
        
        frame.setSize(190 + Main.ROOM * 2, 100 + Main.ROOM * 2);
    }
}
