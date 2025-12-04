import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class LoginScreen {

    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    private static int failedAttempts = 0;

    private static String lastUsername;
    private static String lastPasswordStrengthAtLogin;

    public static String getLastUsername() {
        return lastUsername;
    }

    public static String getLastPasswordStrengthAtLogin() {
        return lastPasswordStrengthAtLogin;
    }

    // EKRANI GERÇEKTEN TEMİZLE (cursor en üste, ekran silinir)
    private static void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    private static void printTitle() {
        System.out.println(CYAN + "====================================" + RESET);
        System.out.println(CYAN + "         CONTACT MANAGEMENT         " + RESET);
        System.out.println(CYAN + "              SYSTEM                " + RESET);
        System.out.println(CYAN + "====================================" + RESET);
        System.out.println();
    }

    private static void waitForEnterTryAgain(Scanner scanner) {
        System.out.println();
        System.out.print("Press ENTER to try again...");
        scanner.nextLine();  // sadece ENTER bekle
    }

    private static void waitForEnterContinue(Scanner scanner) {
        System.out.println();
        System.out.print("Press ENTER to continue...");
        scanner.nextLine();
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
        boolean hasLetter = password.matches(".[A-Za-z].");
        boolean hasDigit  = password.matches(".[0-9].");
        boolean hasSymbol = password.matches(".[^A-Za-z0-9].");

        if (length < 4) return "very_weak";
        if (length < 8) return "weak";

        int score = 0;
        if (hasLetter) score++;
        if (hasDigit)  score++;
        if (hasSymbol) score++;

        if (length >= 12 && score >= 2) return "strong";
        return "medium";
    }

    // Başarılı login sonrası loading bar
    private static void showLoadingBarRealistic() {

        int[] steps = {10, 25, 40, 55, 70, 85, 100};

        for (int percent : steps) {

            clearScreen();
            printTitle();

            System.out.println(YELLOW + "LOADING..." + RESET);
            System.out.println();

            int barLen = 22;
            int filled = percent * barLen / 100;

            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLen; i++) {
                if (i < filled) {
                    bar.append("█");
                } else {
                    bar.append(" ");
                }
            }

            System.out.println(
                CYAN + "[" + bar + "] " + percent + "%" + RESET
            );

            try {
                Thread.sleep(550);
            } catch (InterruptedException ignored) {}
        }

        System.out.println();
        System.out.println(GREEN + "Loaded successfully." + RESET);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        clearScreen();
    }

    public static boolean runLoginFlow(Scanner scanner) {

        while (true) {

            // HER SEFERİN BAŞINDA EKRANI TAMAMEN TEMİZLE + BAŞLIK YAZ
            clearScreen();
            printTitle();

            System.out.print(GREEN + "Username: " + RESET);
            String username = scanner.nextLine();
            if (username != null) username = username.trim();

            System.out.print(GREEN + "Password: " + RESET);
            String password = scanner.nextLine();

            System.out.println();

            // INPUT VALIDATION
            if (username == null || username.isBlank()) {
                System.out.println(RED + "Please enter a username." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            if (password == null || password.isBlank()) {
                System.out.println(RED + "Please enter a password." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            if (username.length() < 2 || username.length() > 25) {
                System.out.println(RED + "Username must be between 2 and 25 characters." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            if (password.length() < 2 || password.length() > 50) {
                System.out.println(RED + "Password must be between 2 and 50 characters." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            String hashed = hashPassword(password);
            if (hashed.isEmpty()) {
                System.out.println(RED + "Something went wrong. Please try again." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            dB_Connection db = new dB_Connection();
            Connection con = db.connect();

            if (con == null) {
                System.out.println(RED + "We are having trouble connecting right now. Please try again later." + RESET);
                waitForEnterTryAgain(scanner);
                continue;
            }

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                String sql = "SELECT name, surname, role FROM users WHERE username = ? AND password_hash = ?";
                ps = con.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, hashed);

                rs = ps.executeQuery();

                if (rs.next()) {
                    failedAttempts = 0;
                    lastUsername = username;
                    lastPasswordStrengthAtLogin = evaluatePasswordStrength(password);

                    showLoadingBarRealistic();

                    String name = rs.getString("name");
                    String surname = rs.getString("surname");
                    String role = rs.getString("role");

                    System.out.println(GREEN + "Login successful." + RESET);
                    System.out.println("Welcome, " + name + " " + surname + " (" + role + ")");
                    waitForEnterContinue(scanner);
                    return true;
                } else {
                    failedAttempts++;

                    System.out.println(RED + "Incorrect username or password." + RESET);

                    if (failedAttempts > 3 && failedAttempts < 5) {
                        System.out.println(YELLOW + "You have entered an incorrect password more than 3 times." + RESET);
                        System.out.println(YELLOW + "Please contact your manager." + RESET);
                    }

                    if (failedAttempts >= 5) {
                        System.out.println();
                        System.out.println(RED + "Too many failed attempts. Session will be terminated." + RESET);
                        System.exit(0);
                    }

                    waitForEnterTryAgain(scanner);
                }

            } catch (SQLException e) {
                System.out.println(RED + "An error occurred while checking your credentials. Please try again." + RESET);
                waitForEnterTryAgain(scanner);
            } finally {
                try {
                    if (rs != null) rs.close();
                } catch (SQLException ignored) {}

                try {
                    if (ps != null) ps.close();
                } catch (SQLException ignored) {}

                try {
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}