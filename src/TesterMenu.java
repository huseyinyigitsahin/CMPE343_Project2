import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class TesterMenu {

    // ====== ANSI COLORS ======
    protected static final String RESET  = "\u001B[0m";
    protected static final String RED    = "\u001B[31m";
    protected static final String GREEN  = "\u001B[32m";
    protected static final String YELLOW = "\u001B[33m";
    protected static final String CYAN   = "\u001B[36m";

    // Commands for advanced search
    protected static final String CMD_QUIT = "quit";
    protected static final String CMD_BACK = "back";

    // Search input maximum length
    protected static final int MAX_SEARCH_LEN = 100;

    // One format for the whole contact table
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
        this.username = trimOrEmpty(username);
        this.fullName = trimOrEmpty(fullName);
        this.role = trimOrEmpty(role);
        this.scanner = scanner;
        this.passwordStrengthAtLogin = trimOrEmpty(passwordStrengthAtLogin);
    }

    // ====== MAIN TESTER MENU ======
    public void showMenu() {
        while (true) {
            clearScreen();

            String realFullName = loadRealFullName();

            System.out.println(CYAN + "=== TESTER MENU ===" + RESET);
            System.out.println(GREEN + "User: " + RESET + realFullName + " (" + username + ")");
            System.out.println(GREEN + "Role: " + RESET + role);
            System.out.println();

            printPasswordStrengthBanner();
            System.out.println();

            System.out.println(CYAN + "Please select an option:" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Change password");
            System.out.println(GREEN + "2)" + RESET + " List all contacts");
            System.out.println(GREEN + "3)" + RESET + " Search contacts");
            System.out.println(GREEN + "4)" + RESET + " Sort contacts");
            System.out.println(GREEN + "5)" + RESET + " Logout");
            System.out.print(YELLOW + "Select an option (1-5): " + RESET);

            String input = readTrimmed();

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
                    System.out.println(CYAN + "Logging out. Goodbye, " + realFullName + "." + RESET);
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
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    protected static String trimOrEmpty(String s) {
        return (s == null) ? "" : s.trim();
    }

    protected String readTrimmed() {
        String s = scanner.nextLine();
        return trimOrEmpty(s);
    }

    protected String toLowerTr(String text) {
        text = trimOrEmpty(text);
        if (text.isEmpty())
            return "";
        return text.toLowerCase(new java.util.Locale("tr", "TR"));
    }

    protected void waitForEnter() {
        while (true) {
            System.out.print(YELLOW + "Press ENTER to continue: " + RESET);
            String input = scanner.nextLine();
            if (trimOrEmpty(input).isEmpty())
                return;
            System.out.println(YELLOW + "Please press only ENTER. Do not type anything." + RESET);
        }
    }

    protected String normalizePhone(String raw) {
        raw = trimOrEmpty(raw);
        if (raw.isEmpty())
            return "";
        return raw.replaceAll("[^0-9]", "");
    }

    protected String loadRealFullName() {
        Connection con = getConnection();
        if (con == null)
            return fullName;

        String sql = "SELECT name, surname FROM users WHERE username = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String n = trimOrEmpty(rs.getString("name"));
                    String s = trimOrEmpty(rs.getString("surname"));
                    if (!n.isEmpty() && !s.isEmpty()) {
                        return n + " " + s;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                con.close();
            } catch (Exception ignored) {
            }
        }

        return fullName;
    }

    // Ask user if they want to retry or go back
    protected boolean askRetryOrBack() {
        while (true) {
            System.out.println();
            System.out.println(CYAN + "What would you like to do next" + RESET);
            System.out.println("  " + GREEN + "R" + RESET + "  Try again");
            System.out.println("  " + YELLOW + "B" + RESET + "  Go back");
            System.out.print("Your choice (R or B): ");

            String choice = readTrimmed().toLowerCase();

            if (choice.equals("r") || choice.equals("retry")) {
                return true;
            } else if (choice.equals("b") || choice.equals("back")) {
                return false;
            } else {
                System.out.println(YELLOW + "Please enter R or B." + RESET);
            }
        }
    }

    // Operator label for showing next to input
    protected String getOperatorLabel(String op) {
        if (op == null)
            return "";
        switch (op) {
            case "starts":
                return "STARTS WITH";
            case "contains":
                return "CONTAINS";
            case "equals":
                return "EQUALS";
            default:
                return op.toUpperCase();
        }
    }

    // ====== VALIDATION HELPERS ======
    //
    // Name and surname:
    // - only letters allowed (English + Turkish),
    // - NO spaces,
    // - NO dot,
    // - NO digits, NO symbols.
    //
    protected boolean isValidName(String text) {
        text = trimOrEmpty(text);
        if (text.isEmpty())
            return false;
        return text.matches("[A-Za-zÇĞİÖŞÜçğıöşü]+");
    }

    // Nickname: Turkish letters, digits, underscore and dot allowed, no spaces, can
    // be all digits
    protected boolean isValidNickname(String text) {
        text = trimOrEmpty(text);
        if (text.isEmpty())
            return false;
        if (text.contains(" "))
            return false;

        return text.matches("[A-Za-zÇĞİÖŞÜçğıöşü0-9_.]+");
    }

    // Phone: exact 10 digits after normalization (for equals)
    protected boolean isValidPhoneExact(String raw) {
        raw = trimOrEmpty(raw);
        if (raw.isEmpty())
            return false;
        String digits = normalizePhone(raw);
        return digits.length() == 10 && digits.matches("\\d+");
    }

    protected String normalizedPhoneForSearch(String raw) {
        return normalizePhone(raw);
    }

    // Find forbidden character in email
    protected char findForbiddenEmailChar(String email) {
        email = trimOrEmpty(email);
        if (email.isEmpty())
            return 0;
        String forbidden = "!?%^&*()=+{}[]|\"'<>,"; // common problematic ones
        for (int i = 0; i < email.length(); i++) {
            char c = email.charAt(i);
            if (forbidden.indexOf(c) >= 0) {
                return c;
            }
        }
        return 0;
    }

    // Email: basic format and limited domains (for equals)
    protected boolean isValidEmailForEquals(String email) {
        email = trimOrEmpty(email);
        if (email.isEmpty())
            return false;
        if (email.contains(" "))
            return false;

        String regex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
               if (!email.matches(regex)) {
            return false;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1)
            return false;

        String domain = email.substring(atIndex + 1).toLowerCase();

        return domain.equals("gmail.com")
                || domain.equals("outlook.com")
                || domain.equals("hotmail.com")
                || domain.equals("yahoo.com");
    }

    // GÜNCELLENDİ: LocalDate ile gerçek tarih + gelecekte olamaz
    protected boolean isValidExactDate(String date) {
        date = trimOrEmpty(date);
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            LocalDate parsed = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            // doğum günü gelecekte olamaz
            if (parsed.isAfter(today)) {
                return false;
            }
            return true;
        } catch (DateTimeParseException e) {
            // 2023-02-30 gibi hatalı tarihleri yakalar
            return false;
        }
    }

    // ====== PASSWORD HELPERS ======

    protected String hashPassword(String password) {
        if (password == null)
            return "";
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
        if (password == null)
            return "very_weak";

        int length = password.length();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");

        if (length < 4) {
            return "very_weak";
        }

        if (length < 8) {
            return "weak";
        }

        int score = 0;
        if (hasLetter)
            score++;
        if (hasDigit)
            score++;
        if (hasSymbol)
            score++;

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
                msg = "Your current password is VERY WEAK. Please change it as soon as possible from option 1.";
                color = RED;
                break;
            case "weak":
                msg = "Your current password is WEAK. We strongly recommend changing it from option 1.";
                color = YELLOW;
                break;
            case "medium":
                msg = "Your current password has MEDIUM strength. You may consider creating a stronger one.";
                color = CYAN;
                break;
            case "strong":
                msg = "Your current password is STRONG.";
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
                "Updated At");
        System.out.printf(CONTACT_ROW_FORMAT,
                "----",
                "-------------------------",
                "---------------",
                "----------------------",
                "----------------------------",
                "----------------------------",
                "------------",
                "-------------------",
                "-------------------");
    }

    protected void printContactRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("contact_id");
        String firstName    = trimOrEmpty(rs.getString("first_name"));
        String middleName   = trimOrEmpty(rs.getString("middle_name"));
        String lastName     = trimOrEmpty(rs.getString("last_name"));
        String nickname     = trimOrEmpty(rs.getString("nickname"));
        String phonePrimary = trimOrEmpty(rs.getString("phone_primary"));
        String phoneSecondary = trimOrEmpty(rs.getString("phone_secondary"));
        String email        = trimOrEmpty(rs.getString("email"));
        String linkedin     = trimOrEmpty(rs.getString("linkedin_url"));
        String birthDate    = trimOrEmpty(rs.getString("birth_date"));
        String createdAt    = trimOrEmpty(rs.getString("created_at"));
        String updatedAt    = trimOrEmpty(rs.getString("updated_at"));

        String fullNameStr;
        if (!middleName.isEmpty()) {
            fullNameStr = firstName + " " + middleName + " " + lastName;
        } else {
            fullNameStr = firstName + " " + lastName;
        }

        String phones = "";
        if (!phonePrimary.isEmpty()) {
            phones = phonePrimary;
        }
        if (!phoneSecondary.isEmpty()) {
            if (!phones.isBlank())
                phones += " / ";
            phones += phoneSecondary;
        }

        System.out.printf(CONTACT_ROW_FORMAT,
                String.valueOf(id),
                fullNameStr,
                nickname,
                phones,
                email,
                linkedin,
                birthDate,
                createdAt,
                updatedAt);
    }

    // ====== 1) CHANGE PASSWORD ======

    protected void handleChangePassword() {
        clearScreen();
        System.out.println(CYAN + "=== CHANGE PASSWORD ===" + RESET);
        System.out.println(YELLOW + "If you know your current password, you can change it here." + RESET);
        System.out.println(YELLOW + "If you do not know your current password, please contact your manager." + RESET);
        System.out.println("Type " + RED + "q" + RESET + " to cancel.");
        System.out.println();

        System.out.print("Current password: ");
        String currentPassword = scanner.nextLine();
        if (currentPassword != null)
            currentPassword = currentPassword.trim();

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
            System.out.println(RED + "We are having trouble connecting right now. Please try again later." + RESET);
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
            System.out.println(CYAN + "Here is a strong password suggestion (optional):" + RESET);
            System.out.println(GREEN + suggestedPassword + RESET);
            System.out.println("You can type this exactly as your new password, or create your own.");
            System.out.println();

            String newPassword;

            while (true) {
                System.out.print("New password: ");
                newPassword = scanner.nextLine();
                if (newPassword != null)
                    newPassword = newPassword.trim();

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
                    System.out.println(
                            RED + "This password is not strong. Consider using the suggested strong password above."
                                    + RESET);
                }

                System.out.print("Do you want to use this password (" + GREEN + "y" + RESET + " / " + YELLOW + "n"
                        + RESET + ", " + RED + "q" + RESET + " to cancel): ");
                String choice = readTrimmed().toLowerCase();

                if ("q".equals(choice)) {
                    System.out.println(YELLOW + "Password change cancelled." + RESET);
                    waitForEnter();
                    return;
                } else if ("y".equals(choice) || "yes".equals(choice)) {
                    break;
                } else if ("n".equals(choice) || "no") {
                    System.out.println(CYAN + "Okay, let us try again." + RESET);
                } else {
                    System.out.println(YELLOW + "Please answer with y or n." + RESET);
                }
            }

            System.out.print("Confirm new password: ");
            String confirmPassword = scanner.nextLine();
            if (confirmPassword != null)
                confirmPassword = confirmPassword.trim();

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
            try {
                con.close();
            } catch (SQLException ignored) {
            }
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
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }

        waitForEnter();
    }

    // ====== 3) SEARCH MENU (SIMPLE + ADVANCED) ======

    protected void handleSearchContacts() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== SEARCH CONTACTS ===" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Simple search one field");
            System.out.println(GREEN + "2)" + RESET + " Advanced search multiple fields");
            System.out.println(GREEN + "0)" + RESET + " Back to TESTER menu");
            System.out.print(YELLOW + "Select an option: " + RESET);

            String choice = readTrimmed();

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
        System.out.println(CYAN + "Select operator for " + label + ":" + RESET);
        System.out.println(GREEN + "1)" + RESET + " Starts with");
        System.out.println(GREEN + "2)" + RESET + " Contains");
        System.out.println(GREEN + "3)" + RESET + " Equals");
        System.out.println(GREEN + "0)" + RESET + " Back");
        System.out.print(YELLOW + "Your choice (0-3): " + RESET);

        String opChoice = readTrimmed().toLowerCase();

        if ("0".equals(opChoice)) {
            return "back";
        } else if ("1".equals(opChoice)) {
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
            System.out.println(CYAN + "Search by:" + RESET);
            System.out.println(GREEN + "1)" + RESET + " First Name");
            System.out.println(GREEN + "2)" + RESET + " Last Name");
            System.out.println(GREEN + "3)" + RESET + " Primary Phone");
            System.out.println(GREEN + "4)" + RESET + " Email");
            System.out.println(GREEN + "5)" + RESET + " Nickname");
            System.out.println(GREEN + "0)" + RESET + " Back to SEARCH menu");
            System.out.print(YELLOW + "Select field: " + RESET);

            String option = readTrimmed();
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

                // First choose operator on a clean screen
                String op;
                while (true) {
                    clearScreen();
                    System.out.println(CYAN + "=== SIMPLE SEARCH: " + fieldLabel.toUpperCase() + " ===" + RESET);
                    op = selectOperator(fieldLabel);
                    if (op == null) {
                        System.out.println(RED + "Invalid operator. Please try again." + RESET);
                        if (askRetryOrBack()) {
                            continue;
                        } else {
                            stayOnSameField = false;
                            break;
                        }
                    }
                    if (op.equals("back")) {
                        stayOnSameField = false;
                        break;
                    }
                    break;
                }
                if (!stayOnSameField || op == null || op.equals("back")) {
                    break;
                }

                clearScreen();
                System.out.println(CYAN + "=== SIMPLE SEARCH: " + fieldLabel.toUpperCase() + " ===" + RESET);

                // Show format examples depending on field and operator
                System.out.println();
                if (columnName.equals("first_name") || columnName.equals("last_name")) {
                    System.out.println(CYAN + "Format example:" + RESET + " Ahmet, Ece, Ali");
                    System.out.println(YELLOW + "Rules:" + RESET
                            + " only letters are allowed. No spaces, no digits, no symbols. Turkish letters are supported.");
                } else if (columnName.equals("nickname")) {
                    System.out.println(CYAN + "Format example:" + RESET + " ali_k, user.123");
                    System.out.println(
                            YELLOW + "Rules:" + RESET + " letters, digits, underscore and dot are allowed. No spaces.");
                } else if (columnName.equals("phone_primary")) {
                    if ("equals".equals(op)) {
                        System.out.println(CYAN + "Format example:" + RESET + " 5321112233");
                        System.out.println(YELLOW + "Rules for EQUALS:" + RESET
                                + " it must be exactly 10 digits after leading zero. Example: 5321112233.");
                    } else {
                        System.out.println(CYAN + "Format examples:" + RESET + " 532, 53211");
                        System.out.println(YELLOW + "Rules for STARTS WITH or CONTAINS:" + RESET
                                + " you can type a part of the number. It must contain digits only, no letters.");
                    }
                } else if (columnName.equals("email")) {
                    if ("equals".equals(op)) {
                        System.out.println(CYAN + "Format examples:" + RESET + " user@gmail.com, test@outlook.com");
                        System.out.println(YELLOW + "Rules for EQUALS:" + RESET
                                + " must be a valid email with domain gmail.com, outlook.com, hotmail.com or yahoo.com. No spaces.");
                    } else {
                        System.out.println(CYAN + "Format examples:" + RESET + " gmail.com, outlook.com, user@");
                        System.out.println(YELLOW + "Rules for STARTS WITH or CONTAINS:" + RESET
                                + " you can search a part of the email. No spaces and no forbidden characters.");
                    }
                    System.out.println(YELLOW + "Forbidden characters in email:" + RESET
                            + " ! ? % ^ & * ( ) = + { } [ ] | ' \" < > ,");
                }

                System.out.println();
                System.out.println(
                        YELLOW + "Maximum length for search text is " + MAX_SEARCH_LEN + " characters." + RESET);
                System.out.println(YELLOW + "You can type 0 to go back." + RESET);
                System.out.println();

                String opLabel = getOperatorLabel(op);
                System.out.print("Enter search text for " + fieldLabel + " (" + CYAN + opLabel + RESET + "): ");
                String keyword = readTrimmed();

                if (keyword.equals("0")) {
                    stayOnSameField = false;
                    break;
                }

                if (keyword.isEmpty()) {
                    System.out.println(RED + "Search text cannot be empty." + RESET);
                    if (askRetryOrBack()) {
                        continue;
                    } else {
                        stayOnSameField = false;
                        break;
                    }
                }

                if (keyword.length() > MAX_SEARCH_LEN) {
                    System.out.println(RED + "Search text is too long. Please use a shorter value." + RESET);
                    if (askRetryOrBack()) {
                        continue;
                    } else {
                        stayOnSameField = false;
                        break;
                    }
                }

                boolean isPhone = columnName.equals("phone_primary");
                boolean isEmail = columnName.equals("email");

                if (columnName.equals("first_name") || columnName.equals("last_name")) {
                    if (!isValidName(keyword)) {
                        System.out.println(RED + "Invalid name format." + RESET);
                        System.out.println(YELLOW + "Rules:" + RESET
                                + " only letters are allowed. No spaces, no digits, no symbols. Turkish letters are supported.");
                        if (askRetryOrBack()) {
                            continue;
                        } else {
                            stayOnSameField = false;
                            break;
                        }
                    }
                } else if (columnName.equals("nickname")) {
                    if (!isValidNickname(keyword)) {
                        System.out.println(RED + "Invalid nickname format." + RESET);
                        System.out.println(YELLOW + "Rules:" + RESET
                                + " letters, digits, underscore and dot are allowed. No spaces.");
                        if (askRetryOrBack()) {
                            continue;
                        } else {
                            stayOnSameField = false;
                            break;
                        }
                    }
                } else if (isEmail) {
                    char bad = findForbiddenEmailChar(keyword);
                    if (bad != 0) {
                        System.out.println(RED + "You cannot use the character '" + bad + "' in email." + RESET);
                        System.out.println(YELLOW + "Please remove this character and try again." + RESET);
                        if (askRetryOrBack()) {
                            continue;
                        } else {
                            stayOnSameField = false;
                            break;
                        }
                    }
                    if (keyword.contains(" ")) {
                        System.out.println(RED + "Email cannot contain spaces." + RESET);
                        if (askRetryOrBack()) {
                            continue;
                        } else {
                            stayOnSameField = false;
                            break;
                        }
                    }
                    if ("equals".equals(op)) {
                        if (!isValidEmailForEquals(keyword)) {
                            System.out.println(RED + "Invalid email format for equals." + RESET);
                            System.out.println(YELLOW + "Rules:" + RESET
                                    + " must look like user@gmail.com and domain must be gmail.com, outlook.com, hotmail.com or yahoo.com. No spaces.");
                            if (askRetryOrBack()) {
                                continue;
                            } else {
                                stayOnSameField = false;
                                break;
                            }
                        }
                    }
                } else if (isPhone) {
                    String digits = normalizePhone(keyword);
                    if ("equals".equals(op)) {
                        if (!isValidPhoneExact(keyword)) {
                            System.out.println(RED + "Invalid phone format for equals." + RESET);
                            System.out.println(YELLOW + "Rules:" + RESET
                                    + " after removing spaces and symbols it must contain exactly 10 digits. Example: 5321112233.");
                            if (askRetryOrBack()) {
                                continue;
                            } else {
                                stayOnSameField = false;
                                break;
                            }
                        }
                    } else {
                        if (digits.isEmpty()) {
                            System.out.println(RED + "Phone number must contain at least one digit." + RESET);
                            if (askRetryOrBack()) {
                                continue;
                            } else {
                                stayOnSameField = false;
                                break;
                            }
                        }
                        if (!digits.matches("\\d+")) {
                            System.out.println(RED + "Phone number must contain digits only." + RESET);
                            if (askRetryOrBack()) {
                                continue;
                            } else {
                                stayOnSameField = false;
                                break;
                            }
                        }
                        if (digits.length() > 10) {
                            System.out.println(RED
                                    + "Phone number is too long. Maximum 10 digits after leading zero are used in this system."
                                    + RESET);
                            if (askRetryOrBack()) {
                                continue;
                            } else {
                                stayOnSameField = false;
                                break;
                            }
                        }
                    }
                }

                Connection con = getConnection();
                if (con == null) {
                    System.out.println(RED + "Database connection failed." + RESET);
                    waitForEnter();
                    return;
                }

                String sql;
                String pattern;

                if (isPhone) {
                    String normalized = normalizedPhoneForSearch(keyword);
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
                    try {
                        con.close();
                    } catch (Exception ignored) {
                    }
                }

                while (true) {
                    clearScreen();
                    System.out.println(CYAN + "=== SIMPLE SEARCH OPTIONS ===" + RESET);
                    System.out.println("Current field: " + fieldLabel);
                    System.out.println();
                    System.out.println("What would you like to do next");
                    System.out.println("  " + GREEN + "Y" + RESET + "  Another simple search on the same field");
                    System.out.println("  " + YELLOW + "B" + RESET + "  Back to SIMPLE SEARCH menu");
                    System.out.print("Your choice (Y or B): ");

                    String again = readTrimmed().toLowerCase();

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
            System.out.println(CYAN + "=== ADVANCED SEARCH QUICK FILTER RESULTS ===" + RESET);
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
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // ====== ADVANCED SEARCH (CUSTOM, ALWAYS AND) ======

    protected void advancedSearch() {

        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== ADVANCED SEARCH ===" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Quick filter: upcoming birthdays this month");
            System.out.println(GREEN + "2)" + RESET + " Quick filter: contacts added in the last 10 days");
            System.out.println(GREEN + "3)" + RESET + " Quick filter: contacts with missing info");
            System.out.println(GREEN + "4)" + RESET + " Custom advanced search (multi field, AND)");
            System.out.println(GREEN + "0)" + RESET + " Back to SEARCH menu");
            System.out.print(YELLOW + "Your choice (0-4, or 'quit' to exit): " + RESET);

            String mainChoice = readTrimmed();
            if (mainChoice.equalsIgnoreCase(CMD_QUIT)) {
                System.out.println(YELLOW + "Leaving ADVANCED SEARCH." + RESET);
                waitForEnter();
                return;
            }

            if ("0".equals(mainChoice)) {
                return;
            }

            if ("1".equals(mainChoice) || "2".equals(mainChoice) || "3".equals(mainChoice)) {
                runQuickFilter(mainChoice);

                while (true) {
                    clearScreen();
                    System.out.println(CYAN + "=== ADVANCED SEARCH OPTIONS ===" + RESET);
                    System.out.println("What would you like to do next");
                    System.out.println("  " + GREEN + "A" + RESET + "  Run another ADVANCED SEARCH");
                    System.out.println("  " + GREEN + "S" + RESET + "  Go to SIMPLE SEARCH menu");
                    System.out.println("  " + YELLOW + "B" + RESET + "  Return to the SEARCH menu");
                    System.out.print("Your choice (A / S / B): ");

                    String choice = readTrimmed().toLowerCase();

                    if (choice.equals("a")) {
                        // tekrar advanced search menüsüne
                        break;
                    } else if (choice.equals("s")) {
                        simpleSearch();
                        return;
                    } else if (choice.equals("b")) {
                        return;
                    } else {
                        System.out.println(YELLOW + "Please enter A, S or B." + RESET);
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
            System.out.println(CYAN + "=== ADVANCED SEARCH CUSTOM FORM ===" + RESET);
            System.out.println(RED + "Important: you must use at least two fields in this form." + RESET);
            System.out.println();
            System.out.println(CYAN + "Available fields:" + RESET);
            System.out.println("  " + GREEN + "1)" + RESET + " First Name");
            System.out.println("  " + GREEN + "2)" + RESET + " Last Name");
            System.out.println("  " + GREEN + "3)" + RESET + " Primary Phone digits only, example 5321112233");
            System.out.println("  " + GREEN + "4)" + RESET + " Email");
            System.out.println("  " + GREEN + "5)" + RESET + " Nickname");
            System.out.println("  " + GREEN + "6)" + RESET + " Birth Date YYYY-MM-DD or by month or year");
            System.out.println();
            System.out.println(YELLOW + "You can type '" + CMD_QUIT + "' at any time to cancel advanced search." + RESET);
            System.out.println("When selecting fields, you can type '" + CMD_BACK
                    + "' to remove the last condition and change it.");
            System.out.println();

            final int MAX_CONDITIONS = 6;
            String[] columns = new String[MAX_CONDITIONS];
            String[] labels  = new String[MAX_CONDITIONS];
            String[] ops     = new String[MAX_CONDITIONS];
            String[] val1    = new String[MAX_CONDITIONS];
            String[] val2    = new String[MAX_CONDITIONS];

            int count = 0;
            boolean backToAdvancedMenu = false;

            // condition toplama
            while (true) {
                System.out.println();
                System.out.println(CYAN + "Selected filters so far: " + count + RESET);

                if (count >= MAX_CONDITIONS) {
                    System.out.println(YELLOW + "You have reached the maximum number of conditions (" +
                            MAX_CONDITIONS + ")." + RESET);
                    break;
                }

                System.out.println();
                System.out.println(CYAN + "Select field for condition " + (count + 1) + ":" + RESET);
                System.out.println(GREEN + "1)" + RESET + " First Name");
                System.out.println(GREEN + "2)" + RESET + " Last Name");
                System.out.println(GREEN + "3)" + RESET + " Primary Phone");
                System.out.println(GREEN + "4)" + RESET + " Email");
                System.out.println(GREEN + "5)" + RESET + " Nickname");
                System.out.println(GREEN + "6)" + RESET + " Birth Date");
                System.out.print(YELLOW +
                        "Your choice (1-6, 0 to cancel, '" + CMD_BACK + "' to edit previous, '" + CMD_QUIT + "' to exit): "
                        + RESET);

                String fieldOptionRaw = readTrimmed();
                String fieldOption = fieldOptionRaw.toLowerCase();

                if (fieldOption.equals(CMD_QUIT)) {
                    System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                    waitForEnter();
                    return;
                }

                if (fieldOption.equals(CMD_BACK)) {
                    if (count == 0) {
                        System.out.println(YELLOW
                                + "There is no previous condition to go back to. Returning to ADVANCED SEARCH menu."
                                + RESET);
                        backToAdvancedMenu = true;
                        break;
                    } else {
                        // son condition'ı sil
                        count--;
                        columns[count] = null;
                        labels[count]  = null;
                        ops[count]     = null;
                        val1[count]    = null;
                        val2[count]    = null;
                        System.out.println(YELLOW + "Last condition removed. You can enter it again." + RESET);
                        continue;
                    }
                }

                if (fieldOption.equals("0")) {
                    System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                    waitForEnter();
                    count = 0;
                    backToAdvancedMenu = true;
                    break;
                }

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
                    System.out.println(CYAN + "Birth Date search mode:" + RESET);
                    System.out.println(GREEN + "1)" + RESET + " Exact date YYYY-MM-DD");
                    System.out.println(GREEN + "2)" + RESET + " By month for example 11 or november");
                    System.out.println(GREEN + "3)" + RESET + " By year for example 1999");
                    System.out.print(YELLOW + "Your choice (1 2 3, or '" + CMD_QUIT + "' to exit): " + RESET);
                    String dateMode = readTrimmed();

                    if (dateMode.equalsIgnoreCase(CMD_QUIT)) {
                        System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                        waitForEnter();
                        return;
                    }

                    if ("1".equals(dateMode)) {
                        op = "date_eq";
                        System.out.print("Enter exact birth date YYYY-MM-DD (or '" + CMD_QUIT + "' to exit): ");
                        value1 = readTrimmed();
                        if (value1.equalsIgnoreCase(CMD_QUIT)) {
                            System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (!isValidExactDate(value1)) {
                            System.out.println(RED + "Invalid date format or future date. Example: 1995-04-23." + RESET);
                            continue;
                        }
                    } else if ("2".equals(dateMode)) {
                        System.out.print("Enter month number 1-12 or name like november (or '" + CMD_QUIT
                                + "' to exit): ");
                        String monthInput = readTrimmed();
                        if (monthInput.equalsIgnoreCase(CMD_QUIT)) {
                            System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (monthInput.isEmpty()) {
                            System.out.println(RED + "Month cannot be empty. Condition ignored." + RESET);
                            continue;
                        }
                        int monthNum = parseMonthToInt(monthInput);
                        if (monthNum == -1) {
                            System.out.println(RED
                                    + "Invalid month. Please enter 1-12 or a valid month name like november." + RESET);
                            continue;
                        }
                        op = "month";
                        value1 = String.valueOf(monthNum);
                    } else if ("3".equals(dateMode)) {
                        op = "year";
                        System.out.print("Enter year for example 1999 (or '" + CMD_QUIT + "' to exit): ");
                        value1 = readTrimmed();
                        if (value1.equalsIgnoreCase(CMD_QUIT)) {
                            System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (!value1.matches("\\d{4}")) {
                            System.out.println(RED + "Year must be four digits like 1999. Condition ignored." + RESET);
                            continue;
                        }
                        int yearInt = Integer.parseInt(value1);
                        int currentYear = LocalDate.now().getYear();
                        if (yearInt > currentYear || yearInt < 1900) {
                            System.out.println(RED + "Year must be between 1900 and " + currentYear + "." + RESET);
                            continue;
                        }
                    } else {
                        System.out.println(RED + "Invalid choice. Condition ignored." + RESET);
                        continue;
                    }
                } else {
                    String opTmp = selectOperator(label);
                    if (opTmp == null || opTmp.equals("back")) {
                        System.out.println(RED + "Invalid operator. Condition ignored." + RESET);
                        continue;
                    }
                    op = opTmp;

                    String opLabel = getOperatorLabel(op);
                    System.out.print("Enter search text for " + label + " (" + CYAN + opLabel + RESET
                            + ", or '" + CMD_QUIT + "' to exit): ");
                    value1 = readTrimmed();
                    if (value1.equalsIgnoreCase(CMD_QUIT)) {
                        System.out.println(YELLOW + "Advanced custom search cancelled." + RESET);
                        waitForEnter();
                        return;
                    }
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
                    } else if (columnName.equals("email")) {
                        char bad = findForbiddenEmailChar(value1);
                        if (bad != 0) {
                            System.out.println(RED + "You cannot use the character '" + bad + "' in email." + RESET);
                            System.out.println(YELLOW + "Please remove this character. Condition ignored." + RESET);
                            continue;
                        }
                        if (value1.contains(" ")) {
                            System.out.println(RED + "Email cannot contain spaces. Condition ignored." + RESET);
                            continue;
                        }
                    } else if (columnName.equals("first_name") || columnName.equals("last_name")) {
                        if (!isValidName(value1)) {
                            System.out.println(RED + "Invalid name format. Condition ignored." + RESET);
                            System.out.println(YELLOW + "Rules:" + RESET
                                    + " only letters are allowed. No spaces, no digits, no symbols. Turkish letters are supported.");
                            continue;
                        }
                    } else if (columnName.equals("nickname")) {
                        if (!isValidNickname(value1)) {
                            System.out.println(RED + "Invalid nickname format. Condition ignored." + RESET);
                            continue;
                        }
                    }
                }

                columns[count] = columnName;
                labels[count]  = label;
                ops[count]     = op;
                val1[count]    = value1;
                val2[count]    = value2;
                count++;

                System.out.println(GREEN + "Filter added. Currently selected: " + count + RESET);

                if (count >= 2 && count < MAX_CONDITIONS) {
                    System.out.print(YELLOW + "Do you want to add another condition (y or n): " + RESET);
                    String more = readTrimmed().toLowerCase();
                    if (more.equals("y") || more.equals("yes")) {
                        continue;
                    } else if (more.equals("n") || more.equals("no")) {
                        break;
                    } else {
                        System.out.println(YELLOW + "Unknown answer, continuing with current conditions." + RESET);
                        break;
                    }
                }
            }

            if (backToAdvancedMenu) {
                // kullanıcı back ile hiç condition yokken çıktı veya 0 ile iptal etti
                waitForEnter();
                continue; // ADVANCED SEARCH ana menüsüne dön
            }

            if (count < 2) {
                System.out.println();
                System.out.println(
                        RED + "Advanced search requires at least 2 fields. You selected " + count + "." + RESET);

                if (count == 1) {
                    System.out.println();
                    System.out.println(
                            CYAN + "You have only one condition. For single field searches, SIMPLE SEARCH is better."
                                    + RESET);
                    System.out.print(YELLOW + "Do you want to go to SIMPLE SEARCH menu now (y or n): " + RESET);
                    String goSimple = readTrimmed().toLowerCase();
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

            // Filtre özetini (kutular içinde) hazırla
            StringBuilder filterSummary = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (labels[i] == null || ops[i] == null || val1[i] == null) continue;
                String opText;
                switch (ops[i]) {
                    case "starts":
                        opText = "STARTS WITH";
                        break;
                    case "contains":
                        opText = "CONTAINS";
                        break;
                    case "equals":
                        opText = "EQUALS";
                        break;
                    case "date_eq":
                        opText = "DATE";
                        break;
                    case "month":
                        opText = "MONTH";
                        break;
                    case "year":
                        opText = "YEAR";
                        break;
                    default:
                        opText = ops[i].toUpperCase();
                }
                if (filterSummary.length() > 0) {
                    filterSummary.append("  ");
                }
                filterSummary.append("[")
                             .append(labels[i])
                             .append(" - ")
                             .append(opText)
                             .append(" : \"")
                             .append(val1[i])
                             .append("\"]");
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
                        int monthNum = Integer.parseInt(val1[i]);
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

                if (filterSummary.length() > 0) {
                    System.out.println(CYAN + "Applied filters: " + RESET + filterSummary.toString());
                    System.out.println();
                }

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
                try {
                    con.close();
                } catch (Exception ignored) {
                }
            }

            while (true) {
                clearScreen();
                System.out.println(CYAN + "=== ADVANCED SEARCH OPTIONS ===" + RESET);
                System.out.println("What would you like to do next");
                System.out.println("  " + GREEN + "A" + RESET + "  Run another ADVANCED SEARCH");
                System.out.println("  " + GREEN + "S" + RESET + "  Go to SIMPLE SEARCH menu");
                System.out.println("  " + YELLOW + "B" + RESET + "  Return to the SEARCH menu");
                System.out.print("Your choice (A / S / B): ");

                String choice = readTrimmed().toLowerCase();

                if (choice.equals("a")) {
                    // tekrar advanced search menüsüne dön
                    break;
                } else if (choice.equals("s")) {
                    simpleSearch();
                    return;
                } else if (choice.equals("b")) {
                    return;
                } else {
                    System.out.println(YELLOW + "Please enter A, S or B." + RESET);
                    waitForEnter();
                }
            }
        }
    }

    // Convert month name or number to 1-12
    protected int parseMonthToInt(String raw) {
        String t = trimOrEmpty(raw).toLowerCase();
        try {
            int m = Integer.parseInt(t);
            if (m >= 1 && m <= 12)
                return m;
        } catch (NumberFormatException ignored) {
        }

        switch (t) {
            case "january":
                return 1;
            case "february":
                return 2;
            case "march":
                return 3;
            case "april":
                return 4;
            case "may":
                return 5;
            case "june":
                return 6;
            case "july":
                return 7;
            case "august":
                return 8;
            case "september":
                return 9;
            case "october":
                return 10;
            case "november":
                return 11;
            case "december":
                return 12;
            default:
                return -1; // invalid month
        }
    }

    // ====== 4) SORT CONTACTS ======

    protected void handleSortContacts() {

        clearScreen();
        System.out.println(CYAN + "=== SORT CONTACTS ===" + RESET);
        System.out.println(CYAN + "Sort by:" + RESET);
        System.out.println(GREEN + "1)" + RESET + " First Name");
        System.out.println(GREEN + "2)" + RESET + " Last Name");
        System.out.println(GREEN + "3)" + RESET + " Primary Phone");
        System.out.println(GREEN + "4)" + RESET + " Email");
        System.out.println(GREEN + "5)" + RESET + " Birth Date");
        System.out.println(GREEN + "0)" + RESET + " Back to TESTER menu");
        System.out.print(YELLOW + "Select field (0-5): " + RESET);

        String option = readTrimmed();
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

        System.out.print(
                "Order " + GREEN + "A" + RESET + " for ascending, " + YELLOW + "D" + RESET + " for descending: ");
        String orderInput = readTrimmed();
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

        // For textual fields, sort by LOWER(TRIM(field)) to avoid leading spaces & case
        // issues
        String orderExpr = columnName;
        if (columnName.equals("first_name") ||
                columnName.equals("last_name") ||
                columnName.equals("email") ||
                columnName.equals("nickname")) {
            orderExpr = "LOWER(TRIM(" + columnName + "))";
        }

        String sql = "SELECT * FROM contacts ORDER BY " + orderExpr + " " + order + ", contact_id ASC";

        int count = 0;

        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            clearScreen();
            System.out
                    .println(CYAN + "=== SORTED CONTACTS (" + columnName.toUpperCase() + " " + order + ") ===" + RESET);
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
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }

        waitForEnter();
    }
}
