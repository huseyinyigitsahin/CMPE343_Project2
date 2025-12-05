import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginScreen {

    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    private static String lastUsername;
    private static String lastPasswordStrengthAtLogin;

    public static String getLastUsername() {
        return lastUsername;
    }

    public static String getLastPasswordStrengthAtLogin() {
        return lastPasswordStrengthAtLogin;
    }

    // AUTHENTICATE METHOD
    public static boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;

        lastPasswordStrengthAtLogin = evaluatePasswordStrength(password);
        lastUsername = username;

        String hashed = hashPassword(password);
        if (hashed.isEmpty()) return false;

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        if (con == null) {
            System.out.println(RED + "Connection failed." + RESET);
            return false;
        }

        String sql = "SELECT name, surname, role FROM users WHERE username = ? AND password_hash = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String surname = rs.getString("surname");
                    String role = rs.getString("role");

                    showLoadingBarRealistic();

                    System.out.println(GREEN + "Login successful." + RESET);
                    System.out.println("Welcome, " + name + " " + surname + " (" + role + ")");
                    
                    try { Thread.sleep(1500); } catch (Exception e) {}
                    
                    return true;
                } else {
                    System.out.println(RED + "Incorrect username or password." + RESET);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println(RED + "Database error." + RESET);
            return false;
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }

    // ==========================================================
    // HELPERS
    // ==========================================================

    private static void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    private static void printTitle() {
        System.out.println(CYAN + "====================================" + RESET);
        System.out.println(CYAN + "         CONTACT MANAGEMENT         " + RESET);
        System.out.println(CYAN + "====================================" + RESET);
    }

    private static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String evaluatePasswordStrength(String password) {
        if (password == null) return "very_weak";
        int length = password.length();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit  = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");

        if (length < 4) return "very_weak";
        if (length < 8) return "weak";
        int score = 0;
        if (hasLetter) score++;
        if (hasDigit)  score++;
        if (hasSymbol) score++;
        if (length >= 12 && score >= 2) return "strong";
        return "medium";
    }

    private static void showLoadingBarRealistic() {
        int[] steps = {10, 40, 70, 100};
        for (int percent : steps) {
            clearScreen();
            printTitle();
            System.out.println(YELLOW + "Authenticating..." + RESET);
            int barLen = 20;
            int filled = percent * barLen / 100;
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLen; i++) {
                if (i < filled) bar.append("=");
                else bar.append(" ");
            }
            System.out.println(CYAN + "[" + bar + "] " + percent + "%" + RESET);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        System.out.println();
    }
    
}