import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Random;

/**
 * Handles the user interface and logic for the login process.
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Rendering the application splash screen and ASCII art.</li>
 * <li>Displaying animated loading bars with helpful tips.</li>
 * <li>Rendering the login form and accepting user input.</li>
 * <li>Authenticating credentials against the database.</li>
 * <li>Hashing passwords and evaluating password strength.</li>
 * </ul>
 * </p>
 */
public class LoginScreen {

    // ============================= ANSI COLOR CONSTANTS =============================
    public static final String RESET = "\u001b[0m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String CYAN = "\u001b[36m";
    public static final String BLUE = "\u001b[34m";
    public static final String WHITE_BOLD = "\u001b[1;37m";

    /** Stores the username of the last attempted login (successful or not). */
    private static String lastUsername;

    /** Stores the strength rating of the password used in the last login attempt. */
    private static String lastPasswordStrengthAtLogin;

    /**
     * A collection of professional tips displayed dynamically during the loading animation.
     */
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

    /**
     * Retrieves the username used in the last authentication attempt.
     * @return The username string.
     */
    public static String getLastUsername() {
        return lastUsername;
    }

    /**
     * Retrieves the strength evaluation (e.g., "weak", "strong") of the last password entered.
     * @return The password strength string.
     */
    public static String getLastPasswordStrengthAtLogin() {
        return lastPasswordStrengthAtLogin;
    }

    /* ===================== VISUAL / FLOW METHODS ===================== */

    /**
     * Displays the initial application splash screen.
     * <p>
     * Clears the console and prints the project logo (ASCII art), course details,
     * and group members. Pauses execution until the user presses ENTER.
     * </p>
     *
     * @param scanner The scanner instance used to detect the ENTER key press.
     */
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
        System.out.println(CYAN + "      Project by: MERT FAHRİ ÇAKAR, BURAK ARSLAN," + RESET);
        System.out.println(CYAN + "      NERMİN ZEHRA SİPAHİOĞLU, HUSEYİN YİĞİT SAHİN" + RESET);
        System.out.println();

        System.out.print(YELLOW + "Press ENTER to continue..." + RESET);
        scanner.nextLine(); // wait until user presses ENTER
    }

    /**
     * Triggers the loading bar animation specifically for the system startup phase.
     */
    public static void showPreLoginLoadingBar() {
        showLoadingBarScreen("System is starting, please wait...");
    }

    /**
     * Triggers the loading bar animation specifically for the credential verification phase.
     */
    public static void showPostLoginLoadingBar() {
        showLoadingBarScreen("Verifying your credentials, please wait...");
    }

    /**
     * Renders a full-screen animated loading bar.
     * <p>
     * The animation is divided into three phases to simulate a loading process:
     * <ul>
     * <li><b>Phase 1 (0-35%):</b> Displays only the title and the progress bar.</li>
     * <li><b>Phase 2 (35-70%):</b> Displays the title, a random tip, and the progress bar.</li>
     * <li><b>Phase 3 (70-100%):</b> Displays the title, a second random tip, and the progress bar.</li>
     * </ul>
     * This method uses {@link Thread#sleep(long)} to create the animation effect.
     * </p>
     *
     * @param title The title text to display above the loading bar.
     */
    private static void showLoadingBarScreen(String title) {
        int barWidth = 30;
        int stepsPerPhase = 10;   // 10 frames per phase
        int delayMs = 120;        // 120ms per frame  -> ~3.6s total + small pause

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

    /**
     * Prints the header and borders for the login form.
     * Clears the screen before rendering to ensure a clean UI.
     */
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

    /**
     * Prints the footer border for the login form.
     */
    public static void printLoginFooter() {
        System.out.println(CYAN + "|                                              |" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
    }

    /**
     * Displays an error prompt when login fails.
     * Keeps the prompt on screen until the user acknowledges it.
     *
     * @param msg The error message to display (e.g., "Incorrect username or password").
     */
    public static void showLoginErrorPrompt(String msg) {
        System.out.println();
        System.out.println(RED + ">> " + msg + RESET);
        System.out.println(YELLOW + "Press ENTER or write something to try again, or type 'q' then ENTER to quit." + RESET);
        System.out.print("> ");
    }

    /* ===================== AUTH / PASSWORD METHODS ===================== */

    /**
     * Authenticates a user against the database.
     * <p>
     * 1. Evaluates the input password strength.
     * 2. Hashes the input password using SHA-256.
     * 3. Queries the database for a matching username and password hash.
     * </p>
     *
     * @param username The entered username.
     * @param password The entered raw password.
     * @return true if credentials are valid, false otherwise.
     */
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

    /**
     * Hashes a password using the SHA-256 algorithm.
     *
     * @param password The raw password string.
     * @return The hexadecimal string representation of the hash, or an empty string if hashing fails.
     */
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

    /**
     * Evaluates the strength of a password based on length and character diversity.
     *
     * @param password The password to evaluate.
     * @return One of: "very_weak", "weak", "medium", or "strong".
     */
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

    /**
     * Clears the console screen using ANSI escape codes.
     * Moves the cursor to the top-left corner and clears the display buffer.
     */
    public static void clearScreen() {
        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
    }
}