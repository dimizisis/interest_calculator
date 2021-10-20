package db;

import org.ini4j.Wini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Properties properties;

    static {
        properties = new Properties();
        try {
            Path temp = Files.createTempFile("db", ".properties");
            Files.copy(Objects.requireNonNull(DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")), temp, StandardCopyOption.REPLACE_EXISTING);
            FileInputStream input = new FileInputStream(temp.toFile());
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String databaseDriver = properties.getProperty("driver");
    private static String databaseUrl = properties.getProperty("url");
    private static String databaseUsername = properties.getProperty("user");
    private static String databasePassword = properties.getProperty("pass");

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
