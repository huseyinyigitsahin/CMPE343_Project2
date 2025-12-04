import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class TesterMenu {

    // ====== ANSI COLORS ======
    protected static final String RESET  = "\u001B[0m";
    protected static final String RED    = "\u001B[31m";
    protected static final String GREEN  = "\u001B[32m";
    protected static final String YELLOW = "\u001B[33m";
    protected static final String CYAN   = "\u001B[36m";

    // Tek bir format tüm tablo için – hizalı görünsün diye
    protected static final String CONTACT_ROW_FORMAT =
            "%-4s %-25s %-15s %-22s %-28s %-28s %-12s %-19s %-19s%n";

    // ====== FIELDS ======
    protected final String username;
    protected final String fullName;
    protected final String role;
    protected final Scanner scanner;

    protected final String passwordStrengthAtLogin;

    // ====== CONSTRUCTOR ======
    public TesterMenu(String username,
                      String fullName,
                      String role,
                      Scanner scanner,
                      String passwordStrengthAtLogin) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.scanner = scanner;
        this.passwordStrengthAtLogin = passwordStrengthAtLogin;
    }

    // ====== MAIN TESTER MENU ======
    public void showMenu() {
        while (true) {
            clearScreen();

            String realFullName = loadRealFullName();

            System.out.println(CYAN + "=== TESTER MENU ===" + RESET);
            System.out.println("User : " + realFullName + " (" + username + ")");
            System.out.println("Role : " + role);
            System.out.println();

            printPasswordStrengthBanner();
            System.out.println();

            System.out.println("1. Change password");
            System.out.println("2. List all contacts");
            System.out.println("3. Search contacts");
            System.out.println("4. Sort contacts");
            System.out.println("5. Logout");
            System.out.print("Select an option (1-5): ");

            String input = scanner.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Please enter a number between 1 and 5." + RESET);
                waitForEnter();
                continue;
            }

            switch (choice) {
                case 1:
                    handleChangePassword();
                    break;
                case 2:
                    handleListContacts();
                    break;
                case 3:
                    handleSearchContacts();
                    break;
                case 4:
                    handleSortContacts();
                    break;
                case 5:
                    System.out.println("Logging out. Goodbye, " + realFullName + ".");
                    return;
                default:
                    System.out.println(RED + "Please enter a number between 1 and 5." + RESET);
                    waitForEnter();
            }
        }
    }

    // ====== BASIC HELPERS ======

    protected Connection getConnection() {
        dB_Connection db = new dB_Connection();
        return db.connect();
    }

    protected void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    protected String toLowerTr(String text) {
        if (text == null) return "";
        return text.toLowerCase(new java.util.Locale("tr", "TR"));
    }

    protected void waitForEnter() {
        while (true) {
            System.out.print("Press ENTER to continue... ");
            String input = scanner.nextLine();
            if (input.isEmpty()) return;
            System.out.println(YELLOW + "Please press ONLY ENTER. Do not type anything." + RESET);
        }
    }

    protected String normalizePhone(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9]", "");
    }

    protected String loadRealFullName() {
        Connection con = getConnection();
        if (con == null) return fullName;

        String sql = "SELECT name, surname FROM users WHERE username = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String n = rs.getString("name");
                    String s = rs.getString("surname");
                    if (n != null && s != null) {
                        return n + " " + s;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { con.close(); } catch (Exception ignored) {}
        }

        return fullName;
    }

    // ====== PASSWORD HELPERS ======

    protected String hashPassword(String password) {
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

    protected String evaluatePasswordStrength(String password) {
        if (password == null) return "very_weak";

        int length = password.length();
        boolean hasLetter = password.matches(".[A-Za-z].");
        boolean hasDigit  = password.matches(".[0-9].");
        boolean hasSymbol = password.matches(".[^A-Za-z0-9].");

        if (length < 4) {
            return "very_weak";
        }

        if (length < 8) {
            return "weak";
        }

        int score = 0;
        if (hasLetter) score++;
        if (hasDigit)  score++;
        if (hasSymbol) score++;

        if (length >= 12 && score >= 2) {
            return "strong";
        }

        return "medium";
    }

    protected void printPasswordStrengthBanner() {
        if (passwordStrengthAtLogin == null || passwordStrengthAtLogin.isBlank()) {
            return;
        }

        String strength = passwordStrengthAtLogin.toLowerCase();
        String msg;
        String color;

        switch (strength) {
            case "very_weak":
                msg = "Your current password is VERY WEAK. Please change it as soon as possible from option [1].";
                color = RED;
                break;
            case "weak":
                msg = "Your current password is WEAK. We strongly recommend changing it from option [1].";
                color = YELLOW;
                break;
            case "medium":
                msg = "Your current password has MEDIUM strength. You may consider creating a stronger one.";
                color = CYAN;
                break;
            case "strong":
                msg = "Your current password is STRONG. Nice job!";
                color = GREEN;
                break;
            default:
                return;
        }

        System.out.println(color + "[Password check at login] " + msg + RESET);
    }

    protected String generateStrongPasswordSuggestion() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String symbols = "!@#$%^&*()-_=+[]{}";

        String all = upper + lower + digits + symbols;

        SecureRandom random = new SecureRandom();
        int length = 14;

        StringBuilder sb = new StringBuilder();

        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(symbols.charAt(random.nextInt(symbols.length())));

        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = random.nextInt(chars.length);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }

        return new String(chars);
    }

    // ====== CONTACT TABLE HELPERS ======

    protected void printContactHeader() {
        System.out.printf(CONTACT_ROW_FORMAT,
                "ID",
                "Full Name",
                "Nickname",
                "Phones",
                "Email",
                "LinkedIn",
                "Birth Date",
                "Created At",
                "Updated At"
        );
        System.out.printf(CONTACT_ROW_FORMAT,
                "----",
                "-------------------------",
                "---------------",
                "----------------------",
                "----------------------------",
                "----------------------------",
                "------------",
                "-------------------",
                "-------------------"
        );
    }

    protected void printContactRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("contact_id");
        String firstName = rs.getString("first_name");
        String middleName = rs.getString("middle_name");
        String lastName = rs.getString("last_name");
        String nickname = rs.getString("nickname");
        String phonePrimary = rs.getString("phone_primary");
        String phoneSecondary = rs.getString("phone_secondary");
        String email = rs.getString("email");
        String linkedin = rs.getString("linkedin_url");
        String birthDate = rs.getString("birth_date");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        String fullNameStr;
        if (middleName != null && !middleName.isBlank()) {
            fullNameStr = firstName + " " + middleName + " " + lastName;
        } else {
            fullNameStr = firstName + " " + lastName;
        }

        if (nickname == null) nickname = "";

        String phones = "";
        if (phonePrimary != null && !phonePrimary.isBlank()) {
            phones = phonePrimary;
        }
        if (phoneSecondary != null && !phoneSecondary.isBlank()) {
            if (!phones.isBlank()) phones += " / ";
            phones += phoneSecondary;
        }

        if (email == null) email = "";
        if (linkedin == null) linkedin = "";
        if (birthDate == null) birthDate = "";
        if (createdAt == null) createdAt = "";
        if (updatedAt == null) updatedAt = "";

        System.out.printf(CONTACT_ROW_FORMAT,
                String.valueOf(id),
                fullNameStr,
                nickname,
                phones,
                email,
                linkedin,
                birthDate,
                createdAt,
                updatedAt
        );
    }

    // ====== 1) CHANGE PASSWORD ======

    protected void handleChangePassword() {
        clearScreen();
        System.out.println(CYAN + "=== CHANGE PASSWORD ===" + RESET);
        System.out.println(YELLOW + "If you know your current password, you can change it here." + RESET);
        System.out.println(YELLOW + "If you DO NOT know your current password, please contact your manager." + RESET);
        System.out.println("Type 'q' to cancel.\n");

        System.out.print("Current password: ");
        String currentPassword = scanner.nextLine();
        if (currentPassword != null) currentPassword = currentPassword.trim();

        if (currentPassword != null && currentPassword.equalsIgnoreCase("q")) {
            System.out.println(YELLOW + "Password change cancelled." + RESET);
            waitForEnter();
            return;
        }

        if (currentPassword == null || currentPassword.isBlank()) {
            System.out.println(RED + "Current password cannot be empty." + RESET);
            waitForEnter();
            return;
        }

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "We’re having trouble connecting right now. Please try again later." + RESET);
            waitForEnter();
            return;
        }

        String selectSql = "SELECT password_hash FROM users WHERE username = ?";

        try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {

            selectStmt.setString(1, username);

            try (ResultSet rs = selectStmt.executeQuery()) {

                if (!rs.next()) {
                    System.out.println(RED + "User not found. Please contact your manager." + RESET);
                    waitForEnter();
                    return;
                }

                String storedHash = rs.getString("password_hash");
                String currentHash = hashPassword(currentPassword);

                if (!currentHash.equals(storedHash)) {
                    System.out.println(RED + "Current password is incorrect." + RESET);
                    waitForEnter();
                    return;
                }
            }

            String suggestedPassword = generateStrongPasswordSuggestion();
            System.out.println();
            System.out.println("Here is a STRONG password suggestion (optional):");
            System.out.println(GREEN + suggestedPassword + RESET);
            System.out.println("You can type this exactly as your new password, or create your own.\n");

            String newPassword;

            while (true) {
                System.out.print("New password: ");
                newPassword = scanner.nextLine();
                if (newPassword != null) newPassword = newPassword.trim();

                if (newPassword != null && newPassword.equalsIgnoreCase("q")) {
                    System.out.println(YELLOW + "Password change cancelled." + RESET);
                    waitForEnter();
                    return;
                }

                if (newPassword == null || newPassword.isBlank()) {
                    System.out.println(RED + "New password cannot be empty." + RESET);
                    continue;
                }

                if (newPassword.length() < 2 || newPassword.length() > 50) {
                    System.out.println(RED + "Password must be between 2 and 50 characters." + RESET);
                    continue;
                }

                if (newPassword.equals(currentPassword)) {
                    System.out.println(RED + "New password must be different from the current password." + RESET);
                    continue;
                }

                String strength = evaluatePasswordStrength(newPassword);
                System.out.println("Password strength: " + YELLOW + strength.toUpperCase() + RESET);

                if ("very_weak".equals(strength) || "weak".equals(strength)) {
                    System.out.println(RED + "This password is not strong. Consider using the suggested strong password above." + RESET);
                }

                System.out.print("Do you want to use this password? (y/n, 'q' to cancel): ");
                String choice = scanner.nextLine();
                if (choice != null) choice = choice.trim().toLowerCase();

                if ("q".equals(choice)) {
                    System.out.println(YELLOW + "Password change cancelled." + RESET);
                    waitForEnter();
                    return;
                } else if ("y".equals(choice) || "yes".equals(choice)) {
                    break;
                } else if ("n".equals(choice) || "no".equals(choice)) {
                    System.out.println("Okay, let's try again.\n");
                } else {
                    System.out.println(YELLOW + "Please answer with 'y' or 'n'." + RESET);
                }
            }

            System.out.print("Confirm new password: ");
            String confirmPassword = scanner.nextLine();
            if (confirmPassword != null) confirmPassword = confirmPassword.trim();

            if (confirmPassword != null && confirmPassword.equalsIgnoreCase("q")) {
                System.out.println(YELLOW + "Password change cancelled." + RESET);
                waitForEnter();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                System.out.println(RED + "New passwords do not match." + RESET);
                waitForEnter();
                return;
            }

            String newHash = hashPassword(newPassword);
            if (newHash.isEmpty()) {
                System.out.println(RED + "Something went wrong. Please try again." + RESET);
                waitForEnter();
                return;
            }

            String updateSql = "UPDATE users SET password_hash = ? WHERE username = ?";

            try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                updateStmt.setString(1, newHash);
                updateStmt.setString(2, username);

                int rows = updateStmt.executeUpdate();
                if (rows == 1) {
                    System.out.println(GREEN + "Your password has been updated successfully." + RESET);
                } else {
                    System.out.println(RED + "Password could not be updated. Please try again." + RESET);
                }
            }

        } catch (SQLException e) {
            System.out.println(RED + "Something went wrong while changing your password. Please try again." + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    // ====== 2) LIST ALL CONTACTS ======

    protected void handleListContacts() {
        clearScreen();
        System.out.println(CYAN + "=== CONTACT LIST ===" + RESET);

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String sql = "SELECT * FROM contacts";

        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean empty = true;

            printContactHeader();

            while (rs.next()) {
                empty = false;
                printContactRow(rs);
            }

            if (empty) {
                System.out.println(YELLOW + "No contacts found." + RESET);
            }

        } catch (Exception e) {
            System.out.println(RED + "Error while listing contacts: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    // ====== 3) SEARCH MENU (SIMPLE + ADVANCED) ======

    protected void handleSearchContacts() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== SEARCH CONTACTS ===" + RESET);
            System.out.println("1) Simple search (one field)");
            System.out.println("2) Advanced search (multiple fields)");
            System.out.println("0) Back to menu");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                simpleSearch();
            } else if (choice.equals("2")) {
                advancedSearch();
            } else if (choice.equals("0")) {
                return;
            } else {
                System.out.println(RED + "Please select 0, 1, or 2." + RESET);
                waitForEnter();
            }
        }
    }

    // ====== OPERATOR HELPER ======

    protected String selectOperator(String label) {
        System.out.println();
        System.out.println("Select operator for " + label + ":");
        System.out.println("1) Starts with");
        System.out.println("2) Contains");
        System.out.println("3) Equals");
        System.out.print("Your choice (1-3): ");

        String opChoice = scanner.nextLine().trim();

        if ("1".equals(opChoice)) {
            return "starts";
        } else if ("2".equals(opChoice)) {
            return "contains";
        } else if ("3".equals(opChoice)) {
            return "equals";
        } else {
            return null;
        }
    }

    // ====== SIMPLE SEARCH ======

    protected void simpleSearch() {

        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== SIMPLE SEARCH MENU ===" + RESET);
            System.out.println("Search by:");
            System.out.println("1) First Name");
            System.out.println("2) Last Name");
            System.out.println("3) Primary Phone");
            System.out.println("4) Email");
            System.out.println("5) Nickname");
            System.out.println("0) Back to SEARCH menu");
            System.out.print("Select field: ");

            String option = scanner.nextLine().trim();
            String columnName;
            String fieldLabel;

            if (option.equals("0")) {
                return;
            }

            switch (option) {
                case "1":
                    columnName = "first_name";
                    fieldLabel = "First Name";
                    break;
                case "2":
                    columnName = "last_name";
                    fieldLabel = "Last Name";
                    break;
                case "3":
                    columnName = "phone_primary";
                    fieldLabel = "Primary Phone";
                    break;
                case "4":
                    columnName = "email";
                    fieldLabel = "Email";
                    break;
                case "5":
                    columnName = "nickname";
                    fieldLabel = "Nickname";
                    break;
                default:
                    System.out.println(RED + "Invalid option. Please try again." + RESET);
                    waitForEnter();
                    continue;
            }

            boolean stayOnSameField = true;

            while (stayOnSameField) {

                clearScreen();
                System.out.println(CYAN + "=== SIMPLE SEARCH: " + fieldLabel.toUpperCase() + " ===" + RESET);

                String op = selectOperator(fieldLabel);
                if (op == null) {
                    System.out.println(RED + "Invalid operator. Please try again." + RESET);
                    waitForEnter();
                    continue;
                }

                System.out.print("Enter search text (searching by " + fieldLabel + "): ");
                String keyword = scanner.nextLine().trim();

                if (keyword.isEmpty()) {
                    System.out.println(RED + "Search text cannot be empty." + RESET);
                    waitForEnter();
                    continue;
                }

                Connection con = getConnection();
                if (con == null) {
                    System.out.println(RED + "Database connection failed." + RESET);
                    waitForEnter();
                    return;
                }

                String sql;
                String pattern;

                boolean isPhone = columnName.equals("phone_primary");

                if (isPhone) {
                    String normalized = normalizePhone(keyword);
                    if (normalized.isEmpty()) {
                        System.out.println(RED + "Phone number must contain digits." + RESET);
                        waitForEnter();
                        try { con.close(); } catch (Exception ignored) {}
                        continue;
                    }
                    sql = "SELECT * FROM contacts WHERE " + columnName + " LIKE ?";

                    if ("starts".equals(op)) {
                        pattern = normalized + "%";
                    } else if ("equals".equals(op)) {
                        pattern = normalized;
                    } else {
                        pattern = "%" + normalized + "%";
                    }

                } else {
                    sql = "SELECT * FROM contacts WHERE LOWER(" + columnName + ") LIKE ?";
                    String base = toLowerTr(keyword);

                    if ("starts".equals(op)) {
                        pattern = base + "%";
                    } else if ("equals".equals(op)) {
                        pattern = base;
                    } else {
                        pattern = "%" + base + "%";
                    }
                }

                int matchedCount = 0;

                try (PreparedStatement stmt = con.prepareStatement(sql)) {

                    stmt.setString(1, pattern);

                    clearScreen();
                    System.out.println(CYAN + "=== SIMPLE SEARCH RESULTS (" + fieldLabel + ") ===" + RESET);
                    printContactHeader();

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            matchedCount++;
                            printContactRow(rs);
                        }
                    }

                    if (matchedCount == 0) {
                        System.out.println(YELLOW + "No matching contacts found." + RESET);
                    } else {
                        System.out.println();
                        System.out.println(GREEN + "Matched " + matchedCount + " contact(s)." + RESET);
                    }

                    waitForEnter();

                } catch (Exception e) {
                    System.out.println(RED + "Error while searching contacts: " + e.getMessage() + RESET);
                    waitForEnter();
                } finally {
                    try { con.close(); } catch (Exception ignored) {}
                }

                while (true) {
                    clearScreen();
                    System.out.println(CYAN + "=== SIMPLE SEARCH OPTIONS ===" + RESET);
                    System.out.println("Current field: " + fieldLabel);
                    System.out.println();
                    System.out.println("What would you like to do next?");
                    System.out.println("  [Y]  Another SIMPLE SEARCH on the same field");
                    System.out.println("  [B]  Back to SIMPLE SEARCH menu");
                    System.out.print("Your choice (Y / B): ");

                    String again = scanner.nextLine().trim().toLowerCase();

                    if (again.equals("y") || again.equals("yes")) {
                        break;
                    } else if (again.equals("b")) {
                        stayOnSameField = false;
                        break;
                    } else {
                        System.out.println(YELLOW + "Please enter Y or B." + RESET);
                        waitForEnter();
                    }
                }
            }
        }
    }

    // ====== QUICK FILTERS (ADVANCED) ======

    protected void runQuickFilter(String mainChoice) {
        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String sql;

        if ("1".equals(mainChoice)) {
            sql = "SELECT * FROM contacts " +
                  "WHERE birth_date IS NOT NULL " +
                  "AND MONTH(birth_date) = MONTH(CURDATE()) " +
                  "AND DAYOFMONTH(birth_date) >= DAYOFMONTH(CURDATE()) " +
                  "ORDER BY MONTH(birth_date), DAYOFMONTH(birth_date)";
        } else if ("2".equals(mainChoice)) {
            sql = "SELECT * FROM contacts " +
                  "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 10 DAY) " +
                  "ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM contacts " +
                  "WHERE (email IS NULL OR email = '') " +
                  "   OR (phone_primary IS NULL OR phone_primary = '') " +
                  "   OR (linkedin_url IS NULL OR linkedin_url = '')";
        }

        int matchedCount = 0;

        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            clearScreen();
            System.out.println(CYAN + "=== ADVANCED SEARCH - QUICK FILTER RESULTS ===" + RESET);
            printContactHeader();

            while (rs.next()) {
                matchedCount++;
                printContactRow(rs);
            }

            System.out.println();
            String color = (matchedCount >= 2) ? GREEN : RED;
            System.out.println(color + "Total filtered rows: " + matchedCount + RESET);
            waitForEnter();

        } catch (Exception e) {
            System.out.println(RED + "Error while running quick filter: " + e.getMessage() + RESET);
            waitForEnter();
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }

    // ====== ADVANCED SEARCH (CUSTOM, ALWAYS AND) ======

    protected void advancedSearch() {

        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== ADVANCED SEARCH ===" + RESET);
            System.out.println("1) Quick filter: Upcoming birthdays (this month)");
            System.out.println("2) Quick filter: Contacts added in the last 10 days");
            System.out.println("3) Quick filter: Contacts with missing info");
            System.out.println("4) Custom advanced search (multi-field, AND)");
            System.out.println("0) Back to SEARCH menu");
            System.out.print("Your choice: ");

            String mainChoice = scanner.nextLine().trim();

            if ("0".equals(mainChoice)) {
                return;
            }

            if ("1".equals(mainChoice) || "2".equals(mainChoice) || "3".equals(mainChoice)) {
                runQuickFilter(mainChoice);

                while (true) {
                    clearScreen();
                    System.out.println(CYAN + "=== ADVANCED SEARCH OPTIONS ===" + RESET);
                    System.out.println("What would you like to do next?");
                    System.out.println("  [R]  Run another ADVANCED SEARCH");
                    System.out.println("  [B]  Return to the SEARCH menu");
                    System.out.print("Your choice (R / B): ");

                    String choice = scanner.nextLine().trim().toLowerCase();

                    if (choice.equals("r")) {
                        break;
                    } else if (choice.equals("b")) {
                        return;
                    } else {
                        System.out.println(YELLOW + "Please enter R or B." + RESET);
                        waitForEnter();
                    }
                }

                continue;
            }

            if (!"4".equals(mainChoice)) {
                System.out.println(RED + "Invalid option. Please try again." + RESET);
                waitForEnter();
                continue;
            }

            clearScreen();
            System.out.println(CYAN + "=== ADVANCED SEARCH (CUSTOM FORM) ===" + RESET);
            System.out.println(RED + "IMPORTANT: You must use at least TWO fields in this form." + RESET);
            System.out.println();
            System.out.println("Available fields:");
            System.out.println("  1) First Name");
            System.out.println("  2) Last Name");
            System.out.println("  3) Primary Phone (digits only, e.g. 5321112233)");
            System.out.println("  4) Email");
            System.out.println("  5) Nickname");
            System.out.println("  6) Birth Date (YYYY-MM-DD or by month / year)");
            System.out.println();

            String[] columns = new String[2];
            String[] labels  = new String[2];
            String[] ops     = new String[2];
            String[] val1    = new String[2];
            String[] val2    = new String[2];

            int count = 0;

            while (count < 2) {
                System.out.println();
                System.out.println("Selected filters so far: " + count);
                System.out.print("Do you want to add a condition? (y/n): ");
                String ans = scanner.nextLine().trim().toLowerCase();

                if (ans.equals("n") || ans.equals("no")) {
                    break;
                } else if (!ans.equals("y") && !ans.equals("yes")) {
                    System.out.println(YELLOW + "Please answer with 'y' or 'n'." + RESET);
                    continue;
                }

                System.out.println();
                System.out.println("Select field for condition " + (count + 1) + ":");
                System.out.println("1) First Name");
                System.out.println("2) Last Name");
                System.out.println("3) Primary Phone");
                System.out.println("4) Email");
                System.out.println("5) Nickname");
                System.out.println("6) Birth Date");
                System.out.print("Your choice (1-6): ");
                String fieldOption = scanner.nextLine().trim();

                String columnName;
                String label;

                switch (fieldOption) {
                    case "1":
                        columnName = "first_name";
                        label = "First Name";
                        break;
                    case "2":
                        columnName = "last_name";
                        label = "Last Name";
                        break;
                    case "3":
                        columnName = "phone_primary";
                        label = "Primary Phone";
                        break;
                    case "4":
                        columnName = "email";
                        label = "Email";
                        break;
                    case "5":
                        columnName = "nickname";
                        label = "Nickname";
                        break;
                    case "6":
                        columnName = "birth_date";
                        label = "Birth Date";
                        break;
                    default:
                        System.out.println(RED + "Invalid field option. Condition ignored." + RESET);
                        continue;
                }

                String op = null;
                String value1 = null;
                String value2 = null;

                if ("6".equals(fieldOption)) {
                    System.out.println();
                    System.out.println("Birth Date search mode:");
                    System.out.println("1) Exact date (YYYY-MM-DD)");
                    System.out.println("2) By month (e.g. '11' or 'november')");
                    System.out.println("3) By year (e.g. '1999')");
                    System.out.print("Your choice (1 / 2 / 3): ");
                    String dateMode = scanner.nextLine().trim();

                    if ("1".equals(dateMode)) {
                        op = "date_eq";
                        System.out.print("Enter exact birth date (YYYY-MM-DD): ");
                        value1 = scanner.nextLine().trim();
                        if (value1.isEmpty()) {
                            System.out.println(RED + "Date cannot be empty. Condition ignored." + RESET);
                            continue;
                        }
                    } else if ("2".equals(dateMode)) {
                        op = "month";
                        System.out.print("Enter month (number 1-12 or name like 'november'): ");
                        value1 = scanner.nextLine().trim();
                        if (value1.isEmpty()) {
                            System.out.println(RED + "Month cannot be empty. Condition ignored." + RESET);
                            continue;
                        }
                    } else if ("3".equals(dateMode)) {
                        op = "year";
                        System.out.print("Enter year (e.g. 1999): ");
                        value1 = scanner.nextLine().trim();
                        if (value1.isEmpty()) {
                            System.out.println(RED + "Year cannot be empty. Condition ignored." + RESET);
                            continue;
                        }
                    } else {
                        System.out.println(RED + "Invalid choice. Condition ignored." + RESET);
                        continue;
                    }
                } else {
                    String opTmp = selectOperator(label);
                    if (opTmp == null) {
                        System.out.println(RED + "Invalid operator. Condition ignored." + RESET);
                        continue;
                    }
                    op = opTmp;

                    System.out.print("Enter search text for " + label + ": ");
                    value1 = scanner.nextLine().trim();
                    if (value1.isEmpty()) {
                        System.out.println(RED + "Search text cannot be empty. Condition ignored." + RESET);
                        continue;
                    }

                    if (columnName.equals("phone_primary")) {
                        String normalized = normalizePhone(value1);
                        if (normalized.isEmpty()) {
                            System.out.println(RED + "Phone number must contain digits. Condition ignored." + RESET);
                            continue;
                        }
                        value1 = normalized;
                    }
                }

                columns[count] = columnName;
                labels[count]  = label;
                ops[count]     = op;
                val1[count]    = value1;
                val2[count]    = value2;
                count++;

                System.out.println(GREEN + "Filter added. Currently selected: " + count + RESET);
            }

            if (count < 2) {
                System.out.println();
                System.out.println(RED + "Advanced search requires AT LEAST 2 fields. You selected " + count + "." + RESET);

                if (count == 1) {
                    System.out.println();
                    System.out.println("You have only one condition. For single-field searches, Simple Search is better.");
                    System.out.print("Do you want to go to SIMPLE SEARCH menu now? (y/n): ");
                    String goSimple = scanner.nextLine().trim().toLowerCase();
                    if (goSimple.equals("y") || goSimple.equals("yes")) {
                        simpleSearch();
                        return;
                    }
                }

                waitForEnter();
                return;
            }

            Connection con = getConnection();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                waitForEnter();
                return;
            }

            StringBuilder sql = new StringBuilder("SELECT * FROM contacts WHERE 1=1");

            for (int i = 0; i < count; i++) {
                String col = columns[i];
                String op = ops[i];

                if ("date_eq".equals(op)) {
                    sql.append(" AND ").append(col).append(" = ?");
                } else if ("month".equals(op)) {
                    sql.append(" AND MONTH(").append(col).append(") = ?");
                } else if ("year".equals(op)) {
                    sql.append(" AND YEAR(").append(col).append(") = ?");
                } else if ("starts".equals(op) || "contains".equals(op) || "equals".equals(op)) {
                    if (col.equals("phone_primary")) {
                        sql.append(" AND ").append(col).append(" LIKE ?");
                    } else {
                        sql.append(" AND LOWER(").append(col).append(") LIKE ?");
                    }
                }
            }

            int matchedCount = 0;

            try (PreparedStatement stmt = con.prepareStatement(sql.toString())) {

                int paramIndex = 1;
                for (int i = 0; i < count; i++) {
                    String col = columns[i];
                    String op = ops[i];

                    if ("date_eq".equals(op)) {
                        stmt.setString(paramIndex++, val1[i]);
                    } else if ("month".equals(op)) {
                        int monthNum = parseMonthToInt(val1[i]);
                        stmt.setInt(paramIndex++, monthNum);
                    } else if ("year".equals(op)) {
                        stmt.setInt(paramIndex++, Integer.parseInt(val1[i]));
                    } else {
                        if (col.equals("phone_primary")) {
                            String base = val1[i];
                            String pattern;
                            if ("starts".equals(op)) {
                                pattern = base + "%";
                            } else if ("equals".equals(op)) {
                                pattern = base;
                            } else {
                                pattern = "%" + base + "%";
                            }
                            stmt.setString(paramIndex++, pattern);
                        } else {
                            String base = toLowerTr(val1[i]);
                            String pattern;
                            if ("starts".equals(op)) {
                                pattern = base + "%";
                            } else if ("equals".equals(op)) {
                                pattern = base;
                            } else {
                                pattern = "%" + base + "%";
                            }
                            stmt.setString(paramIndex++, pattern);
                        }
                    }
                }

                clearScreen();
                System.out.println(CYAN + "=== ADVANCED SEARCH RESULTS ===" + RESET);
                printContactHeader();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        matchedCount++;
                        printContactRow(rs);
                    }
                }

                System.out.println();
                String color = (matchedCount >= 2) ? GREEN : RED;
                System.out.println(color + "Total filtered rows: " + matchedCount + RESET);
                waitForEnter();

            } catch (Exception e) {
                System.out.println(RED + "Error while performing advanced search: " + e.getMessage() + RESET);
                waitForEnter();
            } finally {
                try { con.close(); } catch (Exception ignored) {}
            }

            while (true) {
                clearScreen();
                System.out.println(CYAN + "=== ADVANCED SEARCH OPTIONS ===" + RESET);
                System.out.println("What would you like to do next?");
                System.out.println("  [R]  Run another ADVANCED SEARCH");
                System.out.println("  [B]  Return to the SEARCH menu");
                System.out.print("Your choice (R / B): ");

                String choice = scanner.nextLine().trim().toLowerCase();

                if (choice.equals("r")) {
                    break;
                } else if (choice.equals("b")) {
                    return;
                } else {
                    System.out.println(YELLOW + "Please enter R or B." + RESET);
                    waitForEnter();
                }
            }
        }
    }

    // Ay adını ya da sayıyı 1-12'ye çevirir
    protected int parseMonthToInt(String raw) {
        String t = raw.trim().toLowerCase();
        try {
            int m = Integer.parseInt(t);
            if (m >= 1 && m <= 12) return m;
        } catch (NumberFormatException ignored) {}

        switch (t) {
            case "january":   return 1;
            case "february":  return 2;
            case "march":     return 3;
            case "april":     return 4;
            case "may":       return 5;
            case "june":      return 6;
            case "july":      return 7;
            case "august":    return 8;
            case "september": return 9;
            case "october":   return 10;
            case "november":  return 11;
            case "december":  return 12;
            default:          return 1;
        }
    }

    // ====== 4) SORT CONTACTS ======

    protected void handleSortContacts() {

        clearScreen();
        System.out.println(CYAN + "=== SORT CONTACTS ===" + RESET);
        System.out.println("Sort by:");
        System.out.println("1) First Name");
        System.out.println("2) Last Name");
        System.out.println("3) Primary Phone");
        System.out.println("4) Email");
        System.out.println("5) Birth Date");
        System.out.println("0) Back to TESTER menu");
        System.out.print("Select field (0-5): ");

        String option = scanner.nextLine().trim();
        String columnName;

        if ("0".equals(option)) {
            return;
        }

        switch (option) {
            case "1":
                columnName = "first_name";
                break;
            case "2":
                columnName = "last_name";
                break;
            case "3":
                columnName = "phone_primary";
                break;
            case "4":
                columnName = "email";
                break;
            case "5":
                columnName = "birth_date";
                break;
            default:
                System.out.println(RED + "Invalid option. Returning to menu." + RESET);
                waitForEnter();
                return;
        }

        System.out.print("Order (A = Ascending, D = Descending): ");
        String orderInput = scanner.nextLine().trim();
        String order;

        if (orderInput.equalsIgnoreCase("D")) {
            order = "DESC";
        } else {
            order = "ASC";
        }

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String sql = "SELECT * FROM contacts ORDER BY " + columnName + " " + order;

        int count = 0;

        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            clearScreen();
            System.out.println(CYAN + "=== SORTED CONTACTS (" + columnName.toUpperCase() + " " + order + ") ===" + RESET);
            printContactHeader();

            while (rs.next()) {
                count++;
                printContactRow(rs);
            }

            if (count == 0) {
                System.out.println(YELLOW + "No contacts found." + RESET);
            }

        } catch (Exception e) {
            System.out.println(RED + "Error while sorting contacts: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }
}