import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by macbookpro on 15/10/7.
 */
public class DataSync {
    public static void main(String[] args) throws SQLException {
        Connection connection = ProxyGrabber.getConnection();

        Connection targetConnection = DriverManager.getConnection("jdbc:mysql://172.30.30.12/test", "test", "password");

        PreparedStatement preparedStatement = targetConnection.prepareStatement("INSERT INTO proxy(ip, port, status) VALUES (?, ?, ?)");

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT ip, port, status FROM proxy");
        while (resultSet.next()) {
            preparedStatement.setString(1, resultSet.getString("ip"));
            preparedStatement.setInt(2, resultSet.getInt("port"));
            preparedStatement.setInt(3, resultSet.getInt("status"));
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();

        targetConnection.close();
    }
}
