import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AppMain {

    private static final int MAX_ATTEMPTS = 10;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== APPLICATION START ===");
        System.out.println("Type 'q' as username to quit.\n");

        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                System.out.print("Username: ");
                String username = scanner.nextLine();

                if (username != null) {
                    username = username.trim();
                }

                // Exit option
                if (username != null && username.equalsIgnoreCase("q")) {
                    System.out.println("Goodbye.");
                    scanner.close();
                    return;
                }

                if (username == null || username.isBlank()) {
                    System.out.println("Please enter a username.\n");
                    attempts++;
                    if (attempts >= MAX_ATTEMPTS) {
                        System.out.println("Too many attempts. Please try again later.");
                        break;
                    }
                    continue;
                }

                System.out.print("Password: ");
                String password = scanner.nextLine();

                // 🔹 Burada LoginScreen'in authenticate metodunu kullanıyoruz
                boolean success = LoginScreen.authenticate(username, password);

                if (success) {
                    // LoginScreen zaten "Login successful" ve "Welcome..." yazıyor.
                    // Burada artık role ve isim bilgilerini çekip menüye geçeceğiz.
                    String fullName = username;
                    String role = "Tester";

                    // Kullanıcı bilgilerini DB'den al (name, surname, role)
                    dB_Connection db = new dB_Connection();
                    Connection con = db.connect();

                    if (con == null) {
                        System.out.println("We’re having trouble connecting right now. You will be logged out.");
                        break;
                    }

                    String sql = "SELECT name, surname, role FROM users WHERE username = ?";

                    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                        pstmt.setString(1, username);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                String name = rs.getString("name");
                                String surname = rs.getString("surname");
                                role = rs.getString("role");
                                if (name != null && surname != null) {
                                    fullName = name + " " + surname;
                                }
                            } else {
                                System.out.println("User record could not be loaded. You will be logged out.");
                                break;
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Something went wrong while loading your profile. You will be logged out.");
                        break;
                    } finally {
                        try { con.close(); } catch (SQLException ignored) {}
                    }

                    // Şu an sadece Tester rolünü destekliyoruz
                    if (!"Tester".equalsIgnoreCase(role)) {
                        System.out.println("Your role (" + role + ") is not supported in this demo yet.");
                        System.out.println("You will be logged out.");
                        break;
                    }

                    // 🔹 Tester menüsüne geçiş
                    TesterMenu testerMenu = new TesterMenu(username, fullName, role);
                    testerMenu.showMenu();

                    // Menüden çıkınca uygulamayı sonlandırıyoruz
                    break;

                } else {
                    attempts++;
                    if (attempts >= MAX_ATTEMPTS) {
                        System.out.println("Too many attempts. Please try again later.");
                        break;
                    }
                }

                System.out.println();

            } catch (Exception e) {
                System.out.println("Something went wrong. Please try again.\n");
                attempts++;
                if (attempts >= MAX_ATTEMPTS) {
                    System.out.println("Too many attempts. Please try again later.");
                    break;
                }
            }
        }

        scanner.close();
        System.out.println("Application closed.");
    }
}
