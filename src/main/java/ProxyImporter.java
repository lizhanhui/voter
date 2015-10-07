import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ProxyImporter {
    public static void main(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("/Users/macbookpro/proxy.txt")));
        String line = null;
        while (null != (line = bufferedReader.readLine())) {
            String[] ipPortPair = line.split("\\s");
            if (ipPortPair.length == 2) {
                ProxyGrabber.saveProxy(ipPortPair[0], Integer.parseInt(ipPortPair[1]));
            }
        }
        bufferedReader.close();
    }
}
