import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TesterMenu {

    protected final String username;
    protected final String fullName;
    protected final String role;
    protected final Scanner scanner;

    public TesterMenu(String username, String fullName, String role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.scanner = new Scanner(System.in);
    }

    // ==================== MAIN MENU =====================
    public void showMenu() {
        while (true) {
            System.out.println();
            System.out.println("=== TESTER MENU ===");
            System.out.println("User : " + fullName + " (" + username + ")");
            System.out.println("Role : " + role);
            System.out.println();
            System.out.println("1. Change password");
            System.out.println("2. List all contacts");
            System.out.println("3. Search contacts");
            System.out.println("4. Sort contacts");
            System.out.println("5. Logout");
            System.out.print("Select an option (1-5): ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Please enter a valid option.");
                continue;
            }

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number between 1 and 5.");
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
                    System.out.println("Logging out. Goodbye, " + fullName + ".");
                    return;
                default:
                    System.out.println("Please enter a number between 1 and 5.");
            }
        }
    }

    // Şifreyi SHA-256 ile hashleyen yardımcı metod
    private String hashPassword(String password) {
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

    // Şifrenin gücünü tahmin eden basit fonksiyon
    // "very_weak", "weak", "medium", "strong" döndürür
    private String evaluatePasswordStrength(String password) {
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

        int score = 0;
        if (hasLetter) score++;
        if (hasDigit)  score++;
        if (hasSymbol) score++;

        if (length >= 12 && score >= 2) {
            return "strong";
        }

        return "medium";
    }

    // 🔐 Change password: eski şifreyi doğrula, yeni şifreyi strength + onay ile al, DB'de güncelle
    protected void handleChangePassword() {
        System.out.println();
        System.out.println("=== CHANGE PASSWORD ===");
        System.out.println("If you know your current password, you can change it here.");
        System.out.println("If you forgot your password, please contact your manager.");
        System.out.println("Type 'q' to cancel at any time.");
        System.out.println();

        // 1) Eski şifreyi al
        System.out.print("Current password: ");
        String currentPassword = scanner.nextLine();
        if (currentPassword != null) currentPassword = currentPassword.trim();

        if (currentPassword != null && currentPassword.equalsIgnoreCase("q")) {
            System.out.println("Password change cancelled.");
            return;
        }

        if (currentPassword == null || currentPassword.isBlank()) {
            System.out.println("Current password cannot be empty.");
            return;
        }

        // 2) Veritabanına bağlan
        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        if (con == null) {
            System.out.println("We’re having trouble connecting right now. Please try again later.");
            return;
        }

        // 3) Eski şifre doğru mu kontrol et
        String selectSql = "SELECT password_hash FROM users WHERE username = ?";

        try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {

            selectStmt.setString(1, username);

            try (ResultSet rs = selectStmt.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("User not found. Please contact your manager.");
                    return;
                }

                String storedHash = rs.getString("password_hash");
                String currentHash = hashPassword(currentPassword);

                if (!currentHash.equals(storedHash)) {
                    System.out.println("Current password is incorrect.");
                    System.out.println("If you cannot remember your password, please contact your manager.");
                    return;
                }
            }

            // 4) Yeni şifreyi al (strength + onay akışı ile)
            String newPassword;

            while (true) {
                System.out.print("New password: ");
                newPassword = scanner.nextLine();
                if (newPassword != null) newPassword = newPassword.trim();

                if (newPassword != null && newPassword.equalsIgnoreCase("q")) {
                    System.out.println("Password change cancelled.");
                    return;
                }

                if (newPassword == null || newPassword.isBlank()) {
                    System.out.println("New password cannot be empty.");
                    continue;
                }

                // Policy: min 2, max 50 (hocanın istediği)
                if (newPassword.length() < 2 || newPassword.length() > 50) {
                    System.out.println("Password must be between 2 and 50 characters.");
                    continue;
                }

                // Mevcut şifreyle aynı olmasını engelle
                if (newPassword.equals(currentPassword)) {
                    System.out.println("New password must be different from the current password.");
                    continue;
                }

                // Şifre gücünü değerlendir
                String strength = evaluatePasswordStrength(newPassword);
                System.out.println("Password strength: " + strength.toUpperCase());

                if (strength.equals("very_weak") || strength.equals("weak")) {
                    System.out.println("This password looks weak. For better security, consider a longer one with letters, numbers and symbols.");
                } else if (strength.equals("medium")) {
                    System.out.println("This password is okay, but you can still make it stronger.");
                } else {
                    System.out.println("Nice. This looks like a strong password.");
                }

                // Kullanıcıya sor: bu şifreyi gerçekten kullanmak istiyor musun?
                while (true) {
                    System.out.print("Do you want to use this password? (y/n, or 'q' to cancel): ");
                    String choice = scanner.nextLine();
                    if (choice != null) choice = choice.trim().toLowerCase();

                    if ("q".equals(choice)) {
                        System.out.println("Password change cancelled.");
                        return;
                    } else if ("y".equals(choice) || "yes".equals(choice)) {
                        // Bu şifreyi kabul et, confirm aşamasına geç
                        break;
                    } else if ("n".equals(choice) || "no".equals(choice)) {
                        System.out.println("Okay, let's try again.\n");
                        newPassword = null;
                        break;
                    } else {
                        System.out.println("Please answer with 'y' or 'n'.");
                    }
                }

                if (newPassword != null) {
                    // Kullanıcı bu şifreyi kullanmayı kabul etti
                    break;
                }
            }

            // 5) Confirm new password
            System.out.print("Confirm new password: ");
            String confirmPassword = scanner.nextLine();
            if (confirmPassword != null) confirmPassword = confirmPassword.trim();

            if (confirmPassword != null && confirmPassword.equalsIgnoreCase("q")) {
                System.out.println("Password change cancelled.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                System.out.println("New passwords do not match.");
                return;
            }

            // 6) Yeni şifreyi hashle ve DB'de güncelle
            String newHash = hashPassword(newPassword);
            if (newHash.isEmpty()) {
                System.out.println("Something went wrong. Please try again.");
                return;
            }

            String updateSql = "UPDATE users SET password_hash = ? WHERE username = ?";

            try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                updateStmt.setString(1, newHash);
                updateStmt.setString(2, username);

                int rows = updateStmt.executeUpdate();
                if (rows == 1) {
                    System.out.println("Your password has been updated successfully.");
                } else {
                    System.out.println("Password could not be updated. Please try again.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Something went wrong while changing your password. Please try again.");
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }

    // ==================== LIST CONTACTS ======================
    protected void handleListContacts() {
        System.out.println("\n=== ALL CONTACTS ===");

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();
        if (con == null) {
            System.out.println("DB Error.");
            return;
        }

        String sql = "SELECT * FROM contacts ORDER BY contact_id ASC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-5s %-12s %-12s %-12s %-12s %-12s %-12s %-25s %-25s\n",
                    "ID","First","Middle","Last","Nick","Phone1","Phone2","Email","LinkedIn");
            System.out.println("--------------------------------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-12s %-12s %-12s %-12s %-12s %-12s %-25s %-25s\n",
                        rs.getInt("contact_id"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name"),
                        rs.getString("nickname"),
                        rs.getString("phone_primary"),
                        rs.getString("phone_secondary"),
                        rs.getString("email"),
                        rs.getString("linkedin_url"));
            }

        } catch (Exception e) {
            System.out.println("List error: " + e.getMessage());
        }
    }

    // ==================== SEARCH CONTACTS ======================
    protected void handleSearchContacts() {
        System.out.println("\n=== SEARCH CONTACTS ===");
        System.out.print("Keyword: ");
        String k = scanner.nextLine().trim();

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        String sql = "SELECT * FROM contacts WHERE first_name LIKE ? OR last_name LIKE ? OR nickname LIKE ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.printf("%d - %s %s (%s)\n",
                        rs.getInt("contact_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("nickname"));
            }

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    // ==================== SORT CONTACTS ======================
    protected void handleSortContacts() {
        System.out.println("\n=== SORT CONTACTS ===");
        System.out.println("1. First Name A→Z");
        System.out.println("2. Last Name A→Z");
        System.out.println("3. Birth Date (Oldest→Newest)");
        System.out.print("Choice: ");

        String c = scanner.nextLine().trim();
        String order;

        switch (c) {
            case "1": order = "first_name ASC"; break;
            case "2": order = "last_name ASC"; break;
            case "3": order = "birth_date ASC"; break;
            default:
                System.out.println("Invalid option.");
                return;
        }

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM contacts ORDER BY " + order);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("Sorted Results:");
            while (rs.next()) {
                System.out.printf("%d - %s %s\n",
                        rs.getInt("contact_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"));
            }

        } catch (SQLException e) {
            System.out.println("Sort Error: " + e.getMessage());
        }
    }
}
