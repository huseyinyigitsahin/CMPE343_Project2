import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dB_Connection {

    private final String userName = "myuser@localhost";
    private final String password = "1234";
    private final String dbName = "cmpe343_project2";
    private final String host = "localhost";
    private final int port = 3306;

    private Connection con;

    public Connection connect() {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  // Driver yüklenmezse null döner
        } catch (ClassNotFoundException e) {
            return null; // LoginScreen bu durumu düzgün karşılayacak
        }

        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useUnicode=true&characterEncoding=utf8";

            con = DriverManager.getConnection(url, userName, password);
            return con;  // başarılı bağlantı

        } catch (SQLException e) {
            return null; // başarısız bağlantı → login ekranı yönetir
        }
    }
}

 
