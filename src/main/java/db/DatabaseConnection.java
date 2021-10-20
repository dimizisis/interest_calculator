package db;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static Wini ini;

    static {
        try {
            ini = new Wini(new File(Objects.requireNonNull(DatabaseConnection.class.getClassLoader().getResource("db.ini")).getFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String databaseDriver = ini.get("config", "driver");
    private static String databaseUrl = ini.get("config", "url");
    private static String databaseUsername = ini.get("creds", "user");
    private static String databasePassword = ini.get("creds", "pass");

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
}
