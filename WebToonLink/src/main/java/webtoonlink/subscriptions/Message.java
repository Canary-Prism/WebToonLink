package webtoonlink.subscriptions;

import java.util.ArrayList;

import org.apache.commons.text.StringEscapeUtils;

public class Message {
    
    private ArrayList<Part> parts = new ArrayList<>();

    private String temp;
    private char state;

    public Message() {
        parts.add(new Text().in("The Webtoon "));
        parts.add(new Component().in("title"));
        parts.add(new Text().in(" has a new episode!"));
        parts.add(new Component().in("br"));
        parts.add(new Component().in("epLink"));
    }

    public void in(String value) {
        parts.clear();
        temp = "";
        state = 'l';
        for (char i : value.toCharArray()) {
            if (i == '<') {
                parts.add(new Text().in(temp));
                temp = "";
                continue;
            }
            if (i == '>') {
                parts.add(new Component().in(temp));
                temp = "";
                continue;
            }
            temp += i;
        }
    }

    public String out(Webtoon toon) {
        String output = "";
        
        for (Part part : parts) 
            output += part.out(toon);
        
        return output;
    }

    protected interface Part {
        public Part in(String string);
        public String out(Webtoon toon);
    }

    class Text implements Part {
        private String text;

        @Override
        public Text in(String string) {
            text = string;
            return this;
        }

        @Override
        public String out(Webtoon toon) {
            return text;
        }

    }

    private class Component implements Part {
        /**
         * this is the type of component<ul>
         * <li>1: the webtoon title</li>
         * <li>2: the webtoon description</li>
         * <li>3: the latest episode title</li>
         * <li>4: the latest episode link</li>
         * <li>5: the latest episode publish date</li>
         * <li>6: line break</li>
         * <li>7: text hider
         * <ul><li>this hides all text after it but keeps them functional. useful for hiding the urls if you just want embeds</li></ul>
         * </li>
         * <li>0: invalid tag</li>
         * </ul>
         */
        private int type;

        private String text;

        @Override
        public Component in(String string) {
            type = switch (string) {
                case "title" -> 1;
                case "desc" -> 2;
                case "epTitle" -> 3;
                case "epLink" -> 4;
                case "epPubDate" -> 5;
                case "br" -> 6;
                case "hide" -> 7;
                default -> 0;
            };
            return this;
        }

        @Override
        public String out(Webtoon toon) {
            return switch (type) {
                case 0 -> "";
                case 1 -> toon.getTitle();
                case 2 -> toon.getDesc();
                case 3 -> toon.getEptitle();
                case 4 -> toon.getEplink();
                case 5 -> toon.getEpdate();
                case 6 -> "\n";
                case 7 -> "||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​||||​|| _ _ _ _ _ _";
                default -> throw new RuntimeException("This shouldn't even be possible");
            };
        }
    }
}