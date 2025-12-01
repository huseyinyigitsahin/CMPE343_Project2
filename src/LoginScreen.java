import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginScreen {

    // Hash password with SHA-256
    private static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    // Very simple password strength estimation
    // Returns: "very_weak", "weak", "medium", "strong"
    private static String evaluatePasswordStrength(String password) {
        if (password == null) return "very_weak";

        int length = password.length();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit  = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");

        if (length < 4) {
            return "very_weak";
        }

        if (length < 8) {
            return "weak";
        }

        // length >= 8 at this point
        int score = 0;
        if (hasLetter) score++;
        if (hasDigit) score++;
        if (hasSymbol) score++;

        if (length >= 12 && score >= 2) {
            return "strong";
        }

        return "medium";
    }

    // ✅ STATIC hale getirildi: AppMain burayı çağıracak
    public static boolean authenticate(String username, String password) {

        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // Username required
        if (username == null || username.isBlank()) {
            System.out.println("Please enter a username.");
            return false;
        }

        // Password required
        if (password == null || password.isBlank()) {
            System.out.println("Please enter a password.");
            return false;
        }

        // Username length: min 2, max 25
        if (username.length() < 2 || username.length() > 25) {
            System.out.println("Username must be between 2 and 25 characters.");
            return false;
        }

        // Password length: min 2, max 50 (hocanın istediği)
        if (password.length() < 2 || password.length() > 50) {
            System.out.println("Password must be between 2 and 50 characters.");
            return false;
        }

        String hashedPassword = hashPassword(password);
        if (hashedPassword.isEmpty()) {
            System.out.println("Something went wrong. Please try again.");
            return false;
        }

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        if (con == null) {
            System.out.println("We’re having trouble connecting right now. Please try again in a moment.");
            return false;
        }

        String sql = "SELECT name, surname, role FROM users WHERE username = ? AND password_hash = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    String name = rs.getString("name");
                    String surname = rs.getString("surname");
                    String role = rs.getString("role");

                    System.out.println();
                    System.out.println("Login successful.");
                    System.out.println("Welcome, " + name + " " + surname + " (" + role + ")");

                    // Password strength warning (for successful login)
                    String strength = evaluatePasswordStrength(password);
                    if (strength.equals("very_weak") || strength.equals("weak")) {
                        System.out.println("Note: Your password looks weak. "
                                + "For better security, consider changing it later.");
                    }

                    return true;
                } else {
                    System.out.println("Incorrect username or password.");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("Something went wrong. Please try again.");
            return false;
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }
}

