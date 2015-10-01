import com.alibaba.fastjson.JSON;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Voter {

    private static final String VOTE_URL = "http://adonotify.meirixue.com/jinpai/api.php";

    private static final String SIGN_URL = "http://adonotify.meirixue.com/jinpai/wap/index2.php?no=2296&from=groupmessage&isappinstalled=0";

    //var timesp = '';
    private static Pattern TIME_PATTERN = Pattern.compile("var timesp = \\'(\\d{10})\\'");

    //var sign = '70c140f9bdd95e11312c995c663708a8';
    private static Pattern PATTERN = Pattern.compile("var sign = \\'(\\w{32})\\'");

    private static String[] USER_AGENTS = {
            //Android N1
            "Mozilla/5.0 (Linux; U; Android 2.3.7; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1",
            //Android QQ浏览器 For android
            "MQQBrowser/26 Mozilla/5.0 (Linux; U; Android 2.3.7; zh-cn; MB200 Build/GRJ22; CyanogenMod-7) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1",
            // Android UC For android
            "JUC (Linux; U; 2.3.7; zh-cn; MB200; 320*480) UCWEB7.9.3.103/139/999", //备注: 320*480 是设备的分辨率,可以修改.
            // Android Firefox手机版Fennec
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:7.0a1) Gecko/20110623 Firefox/7.0a1 Fennec/7.0a1",
            // Android Opera Mobile
            "Opera/9.80 (Android 2.3.4; Linux; Opera Mobi/build-1107180945; U; en-GB) Presto/2.8.149 Version/11.10",
            // Android Pad Moto Xoom
            "Mozilla/5.0 (Linux; U; Android 3.0; en-us; Xoom Build/HRI39) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13",
            // iPhone3
            "Mozilla/5.0 (iPhone; U; CPU iPhone OS 3_0 like Mac OS X; en-us) AppleWebKit/420.1 (KHTML, like Gecko) Version/3.0 Mobile/1A542a Safari/419.3",
            // iPhone4
            "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7",
            // iPad
            "Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B334b Safari/531.21.10"
    };

    private static CloseableHttpClient httpClient = HttpClients.createDefault();

    public static void main(String[] args) {
        launchVote(args);
    }


    public static void launchVote(String[] args) {
        int max = 10000;
        if (args.length == 1) {
            try {
                max = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("You need to specify an optional integer as argument");
            }
        }

        final AtomicInteger count = new AtomicInteger(0);
        final int MAX_COUNT = max;
        ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1),
                new ThreadPoolExecutor.CallerRunsPolicy());

        while (count.intValue() < max) {
            executorService.submit(new Runnable() {
                public void run() {
                    if (count.intValue() > MAX_COUNT) {
                        return;
                    }
                    HttpPost post = new HttpPost(VOTE_URL);
                    post.setHeader(new BasicHeader("User-Agent", USER_AGENTS[count.intValue() % USER_AGENTS.length]));
                    CloseableHttpResponse response = null;
                    try {
                        post.setEntity(buildEntity());
                        response = httpClient.execute(post);
                        switch (response.getStatusLine().getStatusCode()) {
                            case 200:
                                System.out.println("OK");
                                count.incrementAndGet();
                                String responseText = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                                ResponseText responseTextObject = JSON.parseObject(responseText, ResponseText.class);
                                System.out.println(responseTextObject.getMsg());
                                if (responseTextObject.getMsg().contains("投票超限")) {
                                    System.exit(1);
                                }
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
            });
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
                System.out.println("TimeSP:" + result[0] + ", sign: " + result[1]);
                return result;
            }
        }
        return null;
    }
}

class ResponseText {
    int code;

    String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
