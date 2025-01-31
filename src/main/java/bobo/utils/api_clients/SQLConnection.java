package bobo.utils.api_clients;

import bobo.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLConnection {
    private static final String url = "jdbc:mysql://" + Config.get("MYSQL_HOST") + ":" + Config.get("MYSQL_PORT") + "/" + Config.get("MYSQL_DATABASE");
    private static final String username = Config.get("MYSQL_USER");
    private static final String password = Config.get("MYSQL_PASSWORD");

    private SQLConnection() {} // Prevent instantiation

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}