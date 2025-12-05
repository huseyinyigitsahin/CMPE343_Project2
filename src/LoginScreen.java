import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Random;

public class LoginScreen {

    // Color constants
    public static final String RESET = "\u001b[0m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String CYAN = "\u001b[36m";
    public static final String BLUE = "\u001b[34m";
    public static final String WHITE_BOLD = "\u001b[1;37m";

    private static String lastUsername;
    private static String lastPasswordStrengthAtLogin;

    // Tips for loading bar (plain English sentences, no "Tip #1")
    private static final String[] WORK_MESSAGES = {
        "Keeping your contact list up to date prevents losing important people.",
        "When adding a new contact, try to fill in email, phone and role completely.",
        "Instead of creating duplicate contacts, update existing records when possible.",
        "Well-organized contact groups (Tester, Manager, Customer) make reporting easier.",
        "Writing short notes for each contact helps you sound prepared in the next meeting.",
        "Be careful when sharing screenshots; contact details may contain sensitive data.",
        "Always lock your computer before leaving your desk, even for a short break.",
        "Do not reuse the same password for both email and internal systems.",
        "Using search and filters correctly keeps you from getting lost in large contact lists.",
        "Consistent name formatting gives a far more professional impression."
    };

    public static String getLastUsername() {
        return lastUsername;
    }

    public static String getLastPasswordStrengthAtLogin() {
        return lastPasswordStrengthAtLogin;
    }

    /* ===================== VISUAL / FLOW METHODS ===================== */

    // 1) First screen: CMPE343 ASCII + names + "Press ENTER to continue"
    public static void showInitialSplash(Scanner scanner) {
        clearScreen();

        System.out.println(BLUE + "========================================================================" + RESET);
        System.out.println(BLUE + "   ____ __  __ ____  _____ _____ _  _  _____ " + RESET);
        System.out.println(BLUE + " / ___|  \\/  |  _ \\| ____|___ /| || ||___ / " + RESET);
        System.out.println(BLUE + "| |   | |\\/| | |_) |  _|   |_ \\| || |_ |_ \\ " + RESET);
        System.out.println(BLUE + "| |___| |  | |  __/| |___ ___) |__   _|__) |" + RESET);
        System.out.println(BLUE + " \\____|_|  |_|_|   |_____|____/   |_||____/ " + RESET);
        System.out.println();
        System.out.println(YELLOW + "            Welcome to the CMPE 343 Course Project!             " + RESET);
        System.out.println(BLUE + "========================================================================" + RESET);
        System.out.println();
        System.out.println(CYAN + "      Project by: MERT FAHRI CAKAR, BURAK ARSLAN," + RESET);
        System.out.println(CYAN + "      NERMIN ZEHRA SIPAHIOGLU, HUSEYIN YIGIT SAHIN" + RESET);
        System.out.println();

        System.out.print(YELLOW + "Press ENTER to continue..." + RESET);
        scanner.nextLine(); // wait until user presses ENTER
    }

    // 2) After splash: full-screen loading bar (only on login flow)
    public static void showPreLoginLoadingBar() {
        showLoadingBarScreen("System is starting, please wait...");
    }

    // 5) After correct password: full-screen loading bar again
    public static void showPostLoginLoadingBar() {
        showLoadingBarScreen("Verifying your credentials, please wait...");
    }

    /**
     * Full-screen loading screen in 3 phases (like slides):
     *  - Phase 1: title + bar (0–35%)
     *  - Phase 2: title + tip1 + bar (35–70%)
     *  - Phase 3: title + tip2 + bar (70–100%)
     * Each phase clears the screen, so previous tip/bar is not visible anymore.
     */
    private static void showLoadingBarScreen(String title) {
        int barWidth = 30;
        int stepsPerPhase = 10;   // 10 frames per phase
        int delayMs = 120;        // 120ms per frame  → ~3.6s total + small pause

        Random random = new Random();

        // Pick 2 different random tips
        int firstIndex = random.nextInt(WORK_MESSAGES.length);
        int secondIndex;
        do {
            secondIndex = random.nextInt(WORK_MESSAGES.length);
        } while (secondIndex == firstIndex);

        // Phase 1: only title + bar (0–35%)
        for (int i = 0; i <= stepsPerPhase; i++) {
            int progress = 0 + (35 * i / stepsPerPhase);
            int filled = progress * barWidth / 100;

            clearScreen();
            System.out.println(CYAN + title + RESET);
            System.out.println();

            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < barWidth; j++) {
                if (j < filled) bar.append("=");
                else bar.append(" ");
            }
            System.out.println(CYAN + "[" + bar + "] " + progress + "%" + RESET);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
            }
        }

        // Phase 2: title + first tip + bar (35–70%)
        for (int i = 0; i <= stepsPerPhase; i++) {
            int progress = 35 + ((70 - 35) * i / stepsPerPhase);
            int filled = progress * barWidth / 100;

            clearScreen();
            System.out.println(CYAN + title + RESET);
            System.out.println();
            System.out.println(YELLOW + WORK_MESSAGES[firstIndex] + RESET);
            System.out.println();

            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < barWidth; j++) {
                if (j < filled) bar.append("=");
                else bar.append(" ");
            }
            System.out.println(CYAN + "[" + bar + "] " + progress + "%" + RESET);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
            }
        }

        // Phase 3: title + second tip + bar (70–100%)
        for (int i = 0; i <= stepsPerPhase; i++) {
            int progress = 70 + ((100 - 70) * i / stepsPerPhase);
            int filled = progress * barWidth / 100;

            clearScreen();
            System.out.println(CYAN + title + RESET);
            System.out.println();
            System.out.println(YELLOW + WORK_MESSAGES[secondIndex] + RESET);
            System.out.println();

            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < barWidth; j++) {
                if (j < filled) bar.append("=");
                else bar.append(" ");
            }
            System.out.println(CYAN + "[" + bar + "] " + progress + "%" + RESET);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            Thread.sleep(500); // short pause at 100%
        } catch (InterruptedException ignored) {
        }
    }

    // 4) Contact Management + login form header
    public static void printLoginFormHeader() {
        clearScreen();

        System.out.println(CYAN + "====================================" + RESET);
        System.out.println(CYAN + "         CONTACT MANAGEMENT         " + RESET);
        System.out.println(CYAN + "====================================" + RESET);
        System.out.println();
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
        System.out.println(CYAN + "|           " + WHITE_BOLD + "SECURE SYSTEM LOGIN" + CYAN + "                |" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
        System.out.println(CYAN + "|                                              |" + RESET);
        System.out.println(CYAN + "| " + RESET + "Please enter your credentials below.         " + CYAN + "|" + RESET);
        System.out.println(CYAN + "| " + RESET + "Type " + RED + "'q'" + RESET + " to quit the application.            " + CYAN + "|" + RESET);
        System.out.println(CYAN + "|                                              |" + RESET);
    }

    public static void printLoginFooter() {
        System.out.println(CYAN + "|                                              |" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
    }

    // Wrong password prompt (kept on screen, user chooses)
    public static void showLoginErrorPrompt(String msg) {
        System.out.println();
        System.out.println(RED + ">> " + msg + RESET);
        System.out.println(YELLOW + "Press ENTER or write something to try again, or type 'q' then ENTER to quit." + RESET);
        System.out.print("> ");
    }

    /* ===================== AUTH / PASSWORD METHODS ===================== */

    public static boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        lastPasswordStrengthAtLogin = evaluatePasswordStrength(password);
        lastUsername = username;

        String hashed = hashPassword(password);
        if (hashed.isEmpty()) {
            return false;
        }

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
                return rs.next(); // true if user + password_hash match
            }
        } catch (SQLException e) {
            System.out.println(RED + "Database error." + RESET);
            return false;
        } finally {
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static String hashPassword(String password) {
        if (password == null) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String evaluatePasswordStrength(String password) {
        if (password == null) {
            return "very_weak";
        }

        int length = password.length();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");

        if (length < 4) {
            return "very_weak";
        } else if (length < 8) {
            return "weak";
        } else {
            int score = 0;
            if (hasLetter) score++;
            if (hasDigit)  score++;
            if (hasSymbol) score++;

            if (length >= 12 && score >= 2) {
                return "strong";
            } else {
                return "medium";
            }
        }
    }

    /* ===================== UTILS ===================== */

    public static void clearScreen() {
        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
    }
}
