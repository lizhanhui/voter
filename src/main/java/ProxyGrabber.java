import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ProxyGrabber {

    private static final String BASE_URL = "http://proxy-list.org/english/index.php";

    public static void main(String[] args) {
        int i = 1;
        boolean stopped = false;
        while (!stopped) {
            String urlString = BASE_URL + "?p=" + i;
            System.out.println("Processing " + urlString);
            try {
                URL url = new URL(urlString);
                Document doc = Jsoup.parse(url, 30 * 1000);
                Elements elements = doc.select("li.proxy");
                if (null != elements && !elements.isEmpty()) {
                    for (Element element : elements) {
                        if (element.parent().hasClass("header-row")) {
                            continue;
                        }
                        persistProxy(element.text());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                stopped = true;
            }
            i++;
            try {
                System.out.println("Wait 5 seconds.");
                Thread.sleep(5000);
                System.out.println("Resume");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private static void persistProxy(String proxy) {
        if (null == proxy || !proxy.contains(":")) {
            return;
        }

        String[] ipPortPair = proxy.split(":");
        if (ipPortPair.length != 2) {
            return;
        }
        saveProxy(ipPortPair[0], Integer.parseInt(ipPortPair[1].trim()));
    }

    private static final String SQL_INSERT_PROXY = "INSERT INTO proxy(ip, port) VALUES (?, ?)";

    private static Connection connection;

    private static final String SQL_GET_PROXY = "SELECT ip, port FROM proxy WHERE status = 0 AND active IS TRUE LIMIT 0, 1";

    private static final String SQL_MARK_PROXY_USED = "UPDATE proxy SET status = 1 WHERE ip = ? AND port = ?";

    private static final String SQL_MARK_PROXY_INACTIVE = "UPDATE proxy SET active = false WHERE ip = ? AND port = ?";


    public static Pair<String, Integer> getProxyFromDB() {
        Connection connection = getConnection();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SQL_GET_PROXY);
            if (resultSet.first()) {
                return new Pair<String, Integer>(resultSet.getString("ip"), resultSet.getInt("port"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(statement);
        }
        return null;
    }

    public static void markProxyUsed(String ip, int port) {
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(SQL_MARK_PROXY_USED);
            preparedStatement.setString(1, ip);
            preparedStatement.setInt(2, port);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(preparedStatement);
        }
    }

    public static void markProxyInactive(String ip, int port) {
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(SQL_MARK_PROXY_INACTIVE);
            preparedStatement.setString(1, ip);
            preparedStatement.setInt(2, port);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(preparedStatement);
        }
    }

    public static void saveProxy(String ip, int port) {
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(SQL_INSERT_PROXY);
            preparedStatement.setString(1, ip);
            preparedStatement.setInt(2, port);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(preparedStatement);
        }
    }

    private static void close(PreparedStatement preparedStatement) {
        if (null != preparedStatement) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void close(Statement statement) {
        if (null != statement) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection getConnection() {
        if (null == connection) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "password");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }


}
