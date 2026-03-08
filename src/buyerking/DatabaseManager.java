package buyerking;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseManager {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    private static synchronized void loadConfig() {
        if (initialized)
            return;
        try {
            Properties props = new Properties();
            InputStream input = DatabaseManager.class.getClassLoader()
                    .getResourceAsStream("config.properties");

            if (input == null) {
                try {
                    input = new java.io.FileInputStream("src/resources/config.properties");
                } catch (java.io.FileNotFoundException e) {
                    try {
                        input = new java.io.FileInputStream("config.properties");
                    } catch (java.io.FileNotFoundException ex) {
                        System.err.println("Config file not found!");
                    }
                }
            }

            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("Could not find config.properties");
            }

            dbUrl = props.getProperty("db.url");
            dbUser = props.getProperty("db.username", "");
            dbPassword = props.getProperty("db.password", "");
            String driver = props.getProperty("db.driver", "org.sqlite.JDBC");
            Class.forName(driver);

            // For SQLite, initialize schema once
            if (dbUrl.contains("sqlite")) {
                String path = dbUrl.replace("jdbc:sqlite:", "");
                java.io.File dbFile = new java.io.File(path);
                boolean needsInit = !dbFile.exists() || dbFile.length() == 0;
                if (needsInit) {
                    try (Connection conn = DriverManager.getConnection(dbUrl)) {
                        initializeSchema(conn);
                    }
                }
            }

            initialized = true;
            System.out.println("Database configured: " + dbUrl);

        } catch (Exception e) {
            System.err.println("Database config failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        loadConfig();
        if (dbUrl == null)
            throw new SQLException("Database not configured");
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static void initializeSchema(Connection conn) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("database", "buyerking_schema.sql");
            if (!java.nio.file.Files.exists(path)) {
                System.err.println("Schema file not found at: " + path.toAbsolutePath());
                return;
            }

            String script = java.nio.file.Files.readString(path);
            String[] statements = script.split(";");

            Statement stmt = conn.createStatement();
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty())
                    continue;
                try {
                    stmt.execute(trimmed);
                } catch (SQLException ex) {
                    System.err.println("Warning executing SQL: " + ex.getMessage());
                }
            }

            System.out.println("Schema initialized!");

        } catch (Exception e) {
            System.err.println("Failed to initialize schema: " + e.getMessage());
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}