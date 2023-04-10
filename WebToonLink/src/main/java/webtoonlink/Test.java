package webtoonlink;

import java.io.IOException;

import org.apache.commons.text.StringEscapeUtils;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

public class Test {
    public static void main(String[] args) {
        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setAppletEnabled(false);
        try {
            XmlPage page = client.getPage("https://www.webtoons.com/en/supernatural/lalins-curse/rss?title_no=1601");
            System.out.println(((DomElement)page.getFirstByXPath("/rss/channel/title")).asNormalizedText());
        } catch (FailingHttpStatusCodeException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
