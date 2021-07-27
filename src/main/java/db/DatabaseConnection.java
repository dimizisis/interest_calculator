package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static String databaseDriver = "org.postgresql.Driver";
    private static String databaseUrl = "";
    private static String databaseUsername = "";
    private static String databasePassword = "";

    private static Connection connection = null;

    public DatabaseConnection() {
        DatabaseConnection.databaseUrl = "";
    }

    public static Connection getConnection() {
        if (connection != null)
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
        if (connection != null) {
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
}
