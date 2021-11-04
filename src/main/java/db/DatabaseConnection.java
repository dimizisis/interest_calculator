package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static String databaseDriver = "";
    private static String databaseUrl = "";
    private static String databaseUsername = "";
    private static String databasePassword = "";

    private static Connection connection = null;

    public DatabaseConnection() {
        DatabaseConnection.databaseUrl = "";
    }

    public static Connection getConnection() {
        if (Objects.nonNull(connection))
            return connection;
        return createConnection();
    }

    private static Connection createConnection() {
        try {
            Class.forName(databaseDriver);
            connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    public static void closeConnection(boolean success) {
        if (Objects.nonNull(connection)) {
            try {
                if (success)
                    connection.commit();
                else
                    connection.rollback();
                connection.close();
            } catch (SQLException e) {
                Logger logger = Logger.getAnonymousLogger();
                logger.log(Level.SEVERE, "Exception was thrown: ", e);
            }
        }
    }

    public static String getDatabaseDriver() {
        return databaseDriver;
    }

    public static void setDatabaseDriver(String databaseDriver) {
        DatabaseConnection.databaseDriver = databaseDriver;
    }

    public static String getDatabaseUrl() {
        return databaseUrl;
    }

    public static void setDatabaseUrl(String databaseUrl) {
        DatabaseConnection.databaseUrl = databaseUrl;
    }

    public static String getDatabaseUsername() {
        return databaseUsername;
    }

    public static void setDatabaseUsername(String databaseUsername) {
        DatabaseConnection.databaseUsername = databaseUsername;
    }

    public static String getDatabasePassword() {
        return databasePassword;
    }

    public static void setDatabasePassword(String databasePassword) {
        DatabaseConnection.databasePassword = databasePassword;
    }
}
