import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;

/**
 * Created by macbookpro on 15/10/7.
 */
public class Grabber2 {
    private static final String PROXY_URL = "http://www.cz88.net/proxy/http_10.shtml";
    public static void main(String[] args) throws IOException {
        URL url = new URL(PROXY_URL);
        Document document = Jsoup.parse(url, 30000);
        Elements elements = document.select("div.box694 > ul > li");
        if (!elements.isEmpty()) {
            for (Element element : elements) {
                String ip = element.select("div.ip").text();
                String port = element.select("div.port").text();
                if (port.equals("端口")) {
                    continue;
                }
                ProxyGrabber.saveProxy(ip, Integer.parseInt(port));
            }
        }
    }
}
