package buyerking;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Load configuration
                Properties props = new Properties();
                InputStream input = DatabaseManager.class.getClassLoader()
                        .getResourceAsStream("config.properties");

                // Fallback: Try loading from file system if classpath fails
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

                String url = props.getProperty("db.url");
                String user = props.getProperty("db.username");
                String password = props.getProperty("db.password");

                // Load driver and create connection
                Class.forName("com.mysql.cj.jdbc.Driver");
                try {
                    connection = DriverManager.getConnection(url, user, password);
                    System.out.println("Database connected successfully!");
                } catch (SQLException e) {
                    if (e.getMessage().contains("Unknown database")) {
                        System.out.println("Database not found. Attempting to create...");
                        initializeDatabase(url, user, password);
                        // Retry connection
                        connection = DriverManager.getConnection(url, user, password);
                        System.out.println("Database created and connected successfully!");
                    } else {
                        throw e;
                    }
                }

            } catch (Exception e) {
                System.err.println("Database connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }

    private static void initializeDatabase(String dbUrl, String user, String password) {
        // Extract root URL (remove database name)
        String rootUrl = dbUrl.substring(0, dbUrl.lastIndexOf("/"));

        try (Connection conn = DriverManager.getConnection(rootUrl, user, password);
                Statement stmt = conn.createStatement()) {

            // Read schema file
            java.nio.file.Path path = java.nio.file.Paths.get("database", "buyerking_schema.sql");
            if (!java.nio.file.Files.exists(path)) {
                System.err.println("Schema file not found at: " + path.toAbsolutePath());
                return;
            }

            String script = java.nio.file.Files.readString(path);
            String[] statements = script.split(";");

            for (String sql : statements) {
                if (sql.trim().isEmpty())
                    continue;
                try {
                    stmt.execute(sql);
                } catch (SQLException ex) {
                    // Ignore "database exists" errors, etc.
                    System.err.println("Warning executing SQL: " + ex.getMessage());
                }
            }

            System.out.println("Schema initialized!");

        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method untuk eksekusi query sederhana
    public static ResultSet executeQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }

    public static int executeUpdate(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeUpdate(query);
    }
}