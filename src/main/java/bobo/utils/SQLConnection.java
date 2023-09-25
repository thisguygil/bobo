package bobo.utils;

import bobo.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnection {
    private static final String url = Config.get("MYSQL_URL");
    private static final String username = Config.get("MYSQL_USERNAME");
    private static final String password = Config.get("MYSQL_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}