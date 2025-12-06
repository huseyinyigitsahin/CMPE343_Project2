import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection logic for the MySQL database.
 * <p>
 * This class holds the configuration credentials (host, port, database name, user, password)
 * and provides a utility method to establish a JDBC connection using the MySQL Connector/J driver.
 * </p>
 */
public class dB_Connection {

    private final String userName = "myuser";
    private final String password = "1234";
    private final String dbName = "cmpe343_project2";
    private final String host = "localhost";
    private final int port = 3306;

    private Connection con;

    /**
     * Attempts to establish a connection to the specific MySQL database.
     * <p>
     * This method performs the following steps:
     * <ol>
     * <li>Loads the MySQL JDBC driver (`com.mysql.cj.jdbc.Driver`).</li>
     * <li>Constructs the connection URL with Unicode and UTF-8 encoding support.</li>
     * <li>Authenticates against the database using the stored credentials.</li>
     * </ol>
     * </p>
     *
     * @return A valid {@link Connection} object if the connection is successful; 
     * {@code null} if the driver is not found or if a database access error occurs.
     */
    public Connection connect() {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            return null;
        }

        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useUnicode=true&characterEncoding=utf8";

            con = DriverManager.getConnection(url, userName, password);
            return con;

        } catch (SQLException e) {
            return null;
        }
    }
}