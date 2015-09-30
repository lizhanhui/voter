import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Voter {

    private static final String VOTE_URL = "http://adonotify.meirixue.com/jinpai/api.php";

    private static final String SIGN_URL = "http://adonotify.meirixue.com/jinpai/wap/index2.php?no=2296&from=groupmessage&isappinstalled=0";

    private static Pattern TIME_PATTERN = Pattern.compile("var timesp = \\'(\\d{10})\\'");

    //var sign = '70c140f9bdd95e11312c995c663708a8';
    private static Pattern PATTERN = Pattern.compile("var sign = \\'(\\w{32})\\'");

    private static CloseableHttpClient httpClient = HttpClients.createDefault();
    public static void main(String[] args) {

        int max = 10000;

        if (args.length == 1) {
            try {
                max = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("You need to specify an optional integer as argument");
            }
        }

        HttpPost post = new HttpPost(VOTE_URL);
        CloseableHttpResponse response = null;
        for (int i = 0; i < max; i++) {
            try {
                post.setEntity(buildEntity());
                response = httpClient.execute(post);
                switch (response.getStatusLine().getStatusCode()) {
                    case 200:
                        System.out.println("OK");
                        break;

                    default:
                        System.err.println("Yuck!");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != response) {
                    try {
                        response.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static UrlEncodedFormEntity buildEntity() throws IOException {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("ids", "2296"));
        String[] data = getData();
        if (null == data) {
            throw new IOException("Unable to get timesp and sign");
        }
        nameValuePairs.add(new BasicNameValuePair("timesp", data[0]));
        nameValuePairs.add(new BasicNameValuePair("sign", data[1]));
        return new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
    }

    private static String[] getData() throws IOException {
        HttpGet get = new HttpGet(SIGN_URL);
        CloseableHttpResponse response = httpClient.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            String html = EntityUtils.toString(response.getEntity());
            String[] result = new String[2];
            Matcher timeMatcher = TIME_PATTERN.matcher(html);
            Matcher signMatcher = PATTERN.matcher(html);

            if (timeMatcher.find() && signMatcher.find()) {
                result[0] = timeMatcher.group(1);
                result[1] = signMatcher.group(1);
                return result;
            }
        }
        return null;
    }
}
