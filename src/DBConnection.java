import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection provides JDBC database connectivity for the
 * International Port Container Terminal & Yard Engine (Project Assignment Set 19).
 * 
 * Supports both local deployment (XAMPP/Local MySQL) and remote OCI VM architecture
 * (Student 1 Application Host connecting to Student 2 Database Host on port 3306/5432).
 */
public final class DBConnection {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB   = "port_terminal_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Mysql@12345";

    private static String host = System.getenv("PORT_DB_HOST") != null ? System.getenv("PORT_DB_HOST") : DEFAULT_HOST;
    private static String port = System.getenv("PORT_DB_PORT") != null ? System.getenv("PORT_DB_PORT") : DEFAULT_PORT;
    private static String dbName = System.getenv("PORT_DB_NAME") != null ? System.getenv("PORT_DB_NAME") : DEFAULT_DB;
    private static String user = System.getenv("PORT_DB_USER") != null ? System.getenv("PORT_DB_USER") : DEFAULT_USER;
    private static String password = System.getenv("PORT_DB_PASS") != null ? System.getenv("PORT_DB_PASS") : DEFAULT_PASS;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL Driver not found: " + e.getMessage());
        }
    }

    private DBConnection() {
        // Utility class
    }

    /**
     * Configures database credentials dynamically (useful for OCI remote host switching).
     */
    public static void configure(String customHost, String customPort, String customDb, String customUser, String customPass) {
        host = customHost;
        port = customPort;
        dbName = customDb;
        user = customUser;
        password = customPass;
    }

    public static String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    }

    /**
     * Gets a new JDBC database connection.
     * @return active java.sql.Connection
     * @throws SQLException on database connectivity failure
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl(), user, password);
    }

    /**
     * Tests connectivity to the port terminal database.
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DBConnection Test Failed] " + e.getMessage());
            return false;
        }
    }

    public static String getHost() {
        return host;
    }

    public static String getPort() {
        return port;
    }

    public static String getDbName() {
        return dbName;
    }

    public static String getUser() {
        return user;
    }
}