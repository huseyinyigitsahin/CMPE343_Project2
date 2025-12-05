import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class ManagerMenu extends TesterMenu {

    private static final int MAX_USERNAME_LEN = 50;
    private static final int MAX_NAME_LEN = 50;
    private static final int MAX_SURNAME_LEN = 50;
    private static final int MAX_PASSWORD_LEN = 50;

    public ManagerMenu(String username, String fullName, String role, Scanner scanner, String passwordStrengthAtLogin) {
        super(username, fullName, role, scanner, passwordStrengthAtLogin);
    }
    @Override
    public void showMenu() {
        while (true) {
            clearScreen();
            String realFullName = loadRealFullName();
            System.out.println(CYAN + "=== MANAGER MENU ===" + RESET);
            System.out.println("User : " + realFullName + " (" + username + ")");
            System.out.println("Role : " + role);
            System.out.println();
            if (passwordStrengthAtLogin != null && !passwordStrengthAtLogin.isBlank()) {
                printPasswordStrengthBanner();
                System.out.println();
            }
            System.out.println("1. Change password");
            System.out.println("2. List all users");
            System.out.println("3. Add new user");
            System.out.println("4. Update existing user");
            System.out.println("5. Delete / fire user");
            System.out.println("6. Contacts statistical info");
            System.out.println("7. Logout");
            System.out.print("Select an option (1-7): ");
            String input = scanner.nextLine().trim();
            int choice;
            if (input.isEmpty()) {
                System.out.println(RED + "Please enter a number between 1 and 7." + RESET);
                waitForEnter();
                continue;
            }
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Please enter a valid number between 1 and 7." + RESET);
                waitForEnter();
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> handleChangePassword();    
                    case 2 -> handleListUsers();
                    case 3 -> handleAddUser();
                    case 4 -> handleUpdateUser();
                    case 5 -> handleDeleteUser();
                    case 6 -> handleContactsStatistics();
                    case 7 -> {
                        System.out.println("Logging out. Goodbye, " + realFullName + ".");
                        return;
                    }
                    default -> {
                        System.out.println(RED + "Please enter a number between 1 and 7." + RESET);
                        waitForEnter();
                    }
                }
            } catch (Exception e) {
                System.out.println(RED + "Unexpected error: " + e.getMessage() + RESET);
                waitForEnter();
            }
        }
    }

    //  LIST ALL USERS

    private void handleListUsers() {
        clearScreen();
        System.out.println(CYAN + "=== USER LIST ===" + RESET);
        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }
        String sql = "SELECT user_id, username, name, surname, role, created_at FROM users ORDER BY user_id";
        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            System.out.printf("%-4s %-15s %-22s %-18s %-20s%n",
                    "ID", "Username", "Full Name", "Role", "Created At");
            System.out.printf("%-4s %-15s %-22s %-18s %-20s%n",
                    "----", "---------------", "----------------------", "----------------", "-------------------");
            boolean empty = true;
            while (rs.next()) {
                empty = false;
                int id = rs.getInt("user_id");
                String uname = rs.getString("username");
                String name = rs.getString("name");
                String surname = rs.getString("surname");
                String r = rs.getString("role");
                String created = rs.getString("created_at");

                if (uname == null) uname = "";
                if (name == null) name = "";
                if (surname == null) surname = "";
                if (r == null) r = "";
                if (created == null) created = "";
                String fn = (name + " " + surname).trim();
                System.out.printf("%-4d %-15s %-22s %-18s %-20s%n",
                        id, uname, fn, r, created);
            }

            if (empty) {
                System.out.println(YELLOW + "No users found." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Error while listing users: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }
    // ADD NEW USER
    private void handleAddUser() {
        clearScreen();
        System.out.println(CYAN + "=== ADD NEW USER ===" + RESET);
        System.out.println("Type 'q' at any time to cancel.\n");
      
        String newUsername;
        while (true) {
            System.out.print("Username: ");
            newUsername = scanner.nextLine().trim();
            if (newUsername.equalsIgnoreCase("q")) return;

            if (newUsername.isEmpty()) {
                System.out.println(RED + "Username cannot be empty." + RESET);
                continue;
            }
            if (newUsername.length() > MAX_USERNAME_LEN) {
                System.out.println(RED + "Username is too long (max " + MAX_USERNAME_LEN + " characters)." + RESET);
                continue;
            }
            break;
        }
    
        String name;
        while (true) {
            System.out.print("Name: ");
            name = scanner.nextLine().trim();
            if (name.equalsIgnoreCase("q")) return;

            if (name.isEmpty()) {
                System.out.println(RED + "Name cannot be empty." + RESET);
                continue;
            }
            if (name.length() > MAX_NAME_LEN) {
                System.out.println(RED + "Name is too long (max " + MAX_NAME_LEN + " characters)." + RESET);
                continue;
            }
            break;
        }

 
        String surname;
        while (true) {
            System.out.print("Surname: ");
            surname = scanner.nextLine().trim();
            if (surname.equalsIgnoreCase("q")) return;

            if (surname.isEmpty()) {
                System.out.println(RED + "Surname cannot be empty." + RESET);
                continue;
            }
            if (surname.length() > MAX_SURNAME_LEN) {
                System.out.println(RED + "Surname is too long (max " + MAX_SURNAME_LEN + " characters)." + RESET);
                continue;
            }
            break;
        }

      
        String roleStr = selectRole();
        if (roleStr == null) {
            System.out.println(YELLOW + "Operation cancelled." + RESET);
            waitForEnter();
            return;
        }

      
        String password;
        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();
            if (password == null) password = "";
            password = password.trim();
            if (password.equalsIgnoreCase("q")) return;

            if (password.isEmpty()) {
                System.out.println(RED + "Password cannot be empty." + RESET);
                continue;
            }
            if (password.length() > MAX_PASSWORD_LEN) {
                System.out.println(RED + "Password is too long (max " + MAX_PASSWORD_LEN + " characters)." + RESET);
                continue;
            }

            System.out.print("Confirm password: ");
            String confirm = scanner.nextLine();
            if (confirm == null) confirm = "";
            confirm = confirm.trim();

            if (!password.equals(confirm)) {
                System.out.println(RED + "Passwords do not match. Try again." + RESET);
                continue;
            }
            break;
        }

        String hash = hashPassword(password);
        if (hash == null || hash.isEmpty()) {
            System.out.println(RED + "Could not hash password." + RESET);
            waitForEnter();
            return;
        }

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, name, surname, role) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, hash);
            ps.setString(3, name);
            ps.setString(4, surname);
            ps.setString(5, roleStr);

            ps.executeUpdate();
            System.out.println(GREEN + "User added successfully." + RESET);

        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                System.out.println(RED + "Username already exists. Please try another one." + RESET);
            } else {
                System.out.println(RED + "Error while adding user: " + msg + RESET);
            }
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    // UPDATE EXISTING USER
    private void handleUpdateUser() {
        clearScreen();
        System.out.println(CYAN + "=== UPDATE USER ===" + RESET);
        System.out.println("Tip: You can use 'List all users' to see IDs.\n");

        System.out.print("Enter user ID to update (or 'q' to cancel): ");
        String idInput = scanner.nextLine().trim();
        if (idInput.equalsIgnoreCase("q")) return;

        int userId;
        try {
            userId = Integer.parseInt(idInput);
            if (userId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid ID format." + RESET);
            waitForEnter();
            return;
        }

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String selectSql = "SELECT username, name, surname, role FROM users WHERE user_id = ?";

        String currentUsername = null;
        String currentName = null;
        String currentSurname = null;
        String currentRole = null;

        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println(RED + "User ID not found." + RESET);
                    waitForEnter();
                    return;
                }
                currentUsername = rs.getString("username");
                currentName = rs.getString("name");
                currentSurname = rs.getString("surname");
                currentRole = rs.getString("role");
            }

            if (currentUsername == null) currentUsername = "";
            if (currentName == null) currentName = "";
            if (currentSurname == null) currentSurname = "";
            if (currentRole == null) currentRole = "";

            System.out.println("Current username : " + currentUsername);
            System.out.println("Current name     : " + currentName + " " + currentSurname);
            System.out.println("Current role     : " + currentRole);
            System.out.println();

            System.out.print("New username (leave blank to keep '" + currentUsername + "'): ");
            String newUsername = scanner.nextLine().trim();
            if (newUsername.isEmpty()) {
                newUsername = currentUsername;
            } else if (newUsername.length() > MAX_USERNAME_LEN) {
                System.out.println(RED + "Username is too long (max " + MAX_USERNAME_LEN + " characters)." + RESET);
                waitForEnter();
                return;
            }

            System.out.print("New name (leave blank to keep '" + currentName + "'): ");
            String newName = scanner.nextLine().trim();
            if (newName.isEmpty()) {
                newName = currentName;
            } else if (newName.length() > MAX_NAME_LEN) {
                System.out.println(RED + "Name is too long (max " + MAX_NAME_LEN + " characters)." + RESET);
                waitForEnter();
                return;
            }

            System.out.print("New surname (leave blank to keep '" + currentSurname + "'): ");
            String newSurname = scanner.nextLine().trim();
            if (newSurname.isEmpty()) {
                newSurname = currentSurname;
            } else if (newSurname.length() > MAX_SURNAME_LEN) {
                System.out.println(RED + "Surname is too long (max " + MAX_SURNAME_LEN + " characters)." + RESET);
                waitForEnter();
                return;
            }

            System.out.println("Current role: " + currentRole);
            System.out.print("Change role? (y/n): ");
            String changeRoleAns = scanner.nextLine().trim().toLowerCase();
            String newRole = currentRole;
            if (changeRoleAns.equals("y") || changeRoleAns.equals("yes")) {
                String selectedRole = selectRole();
                if (selectedRole != null) {
                    newRole = selectedRole;
                }
            }

            String updateSql = "UPDATE users SET username=?, name=?, surname=?, role=? WHERE user_id=?";

            try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                updatePs.setString(1, newUsername);
                updatePs.setString(2, newName);
                updatePs.setString(3, newSurname);
                updatePs.setString(4, newRole);
                updatePs.setInt(5, userId);

                int rows = updatePs.executeUpdate();
                if (rows > 0) {
                    System.out.println(GREEN + "User information updated successfully." + RESET);
                } else {
                    System.out.println(YELLOW + "No changes applied." + RESET);
                }
            }

            System.out.print("Do you want to reset this user's password? (y/n): ");
            String resetAns = scanner.nextLine().trim().toLowerCase();
            if (resetAns.equals("y") || resetAns.equals("yes")) {
                resetUserPassword(con, userId);
            }

        } catch (SQLException e) {
            System.out.println(RED + "Error while updating user: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    private void resetUserPassword(Connection outerCon, int userId) {
        System.out.println();
        System.out.println(CYAN + "=== RESET USER PASSWORD ===" + RESET);

        String password;
        while (true) {
            System.out.print("New password: ");
            password = scanner.nextLine();
            if (password == null) password = "";
            password = password.trim();

            if (password.isEmpty()) {
                System.out.println(RED + "Password cannot be empty." + RESET);
                continue;
            }
            if (password.length() > MAX_PASSWORD_LEN) {
                System.out.println(RED + "Password is too long (max " + MAX_PASSWORD_LEN + " characters)." + RESET);
                continue;
            }

            System.out.print("Confirm new password: ");
            String confirm = scanner.nextLine();
            if (confirm == null) confirm = "";
            confirm = confirm.trim();

            if (!password.equals(confirm)) {
                System.out.println(RED + "Passwords do not match. Try again." + RESET);
                continue;
            }
            break;
        }

        String hash = hashPassword(password);
        if (hash == null || hash.isEmpty()) {
            System.out.println(RED + "Could not hash password." + RESET);
            return;
        }

        String sql = "UPDATE users SET password_hash=? WHERE user_id=?";

        try (PreparedStatement ps = outerCon.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println(GREEN + "Password reset successfully." + RESET);
        } catch (SQLException e) {
            System.out.println(RED + "Error while resetting password: " + e.getMessage() + RESET);
        }
    }

    // DELETE USER

    private void handleDeleteUser() {
        clearScreen();
        System.out.println(CYAN + "=== DELETE / FIRE USER ===" + RESET);
        System.out.println("Type 'q' to cancel.\n");

        System.out.print("Enter user ID to delete: ");
        String idInput = scanner.nextLine().trim();
        if (idInput.equalsIgnoreCase("q")) return;

        int userId;
        try {
            userId = Integer.parseInt(idInput);
            if (userId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid ID format." + RESET);
            waitForEnter();
            return;
        }

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        String selectSql = "SELECT username, name, surname, role FROM users WHERE user_id = ?";

        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println(RED + "User ID not found." + RESET);
                    waitForEnter();
                    return;
                }

                String uname = rs.getString("username");
                String name = rs.getString("name");
                String surname = rs.getString("surname");
                String r = rs.getString("role");

                if (uname == null) uname = "";
                if (name == null) name = "";
                if (surname == null) surname = "";
                if (r == null) r = "";

                if (uname.equals(username)) {
                    System.out.println(RED + "You cannot delete yourself." + RESET);
                    waitForEnter();
                    return;
                }

                System.out.println("User to delete: " + uname + " - " + name + " " + surname + " (" + r + ")");
                System.out.print("Are you sure? (y/n): ");
                String ans = scanner.nextLine().trim().toLowerCase();
                if (!(ans.equals("y") || ans.equals("yes"))) {
                    System.out.println(YELLOW + "Delete cancelled." + RESET);
                    waitForEnter();
                    return;
                }
            }

            String deleteSql = "DELETE FROM users WHERE user_id = ?";
            try (PreparedStatement delPs = con.prepareStatement(deleteSql)) {
                delPs.setInt(1, userId);
                int rows = delPs.executeUpdate();
                if (rows > 0) {
                    System.out.println(GREEN + "User deleted successfully." + RESET);
                } else {
                    System.out.println(RED + "No user deleted." + RESET);
                }
            }

        } catch (SQLException e) {
            System.out.println(RED + "Error while deleting user: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }
    // CONTACTS STATISTICAL INFO
    private void handleContactsStatistics() {
        clearScreen();
        System.out.println(CYAN + "=== CONTACTS STATISTICAL INFO ===" + RESET);

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            waitForEnter();
            return;
        }

        try {
            //(top 5)
            System.out.println(YELLOW + "\nTop 5 first names:" + RESET);
            String nameSql = "SELECT first_name, COUNT(*) AS cnt " +
                             "FROM contacts GROUP BY first_name " +
                             "HAVING first_name IS NOT NULL AND first_name <> '' " +
                             "ORDER BY cnt DESC, first_name ASC LIMIT 5";
            try (PreparedStatement ps = con.prepareStatement(nameSql);
                 ResultSet rs = ps.executeQuery()) {

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String fn = rs.getString("first_name");
                    int cnt = rs.getInt("cnt");
                    if (fn == null) fn = "(NULL)";
                    System.out.println("  " + fn + " : " + cnt);
                }
                if (!any) {
                    System.out.println("  (no data)");
                }
            }
            System.out.println(YELLOW + "\nLinkedIn URL statistics:" + RESET);  //LinkedIn istatistikleri
            String linkedinSql =
                    "SELECT " +
                    "SUM(CASE WHEN linkedin_url IS NOT NULL AND linkedin_url <> '' THEN 1 ELSE 0 END) AS with_linkedin, " +
                    "SUM(CASE WHEN linkedin_url IS NULL OR linkedin_url = '' THEN 1 ELSE 0 END) AS without_linkedin " +
                    "FROM contacts";
            try (PreparedStatement ps = con.prepareStatement(linkedinSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int withL = rs.getInt("with_linkedin");
                    int withoutL = rs.getInt("without_linkedin");
                    System.out.println("  With LinkedIn   : " + withL);
                    System.out.println("  Without LinkedIn: " + withoutL);
                } else {
                    System.out.println("  (no data)");
                }
            }

            //Yaş istatistikleri genç falan
            System.out.println(YELLOW + "\nAge statistics (based on birth_date):" + RESET);
            String ageSql =
                    "SELECT " +
                    "MIN(birth_date) AS oldest_date, " +
                    "MAX(birth_date) AS youngest_date, " +
                    "AVG(TIMESTAMPDIFF(YEAR, birth_date, CURDATE())) AS avg_age " +
                    "FROM contacts WHERE birth_date IS NOT NULL";
            try (PreparedStatement ps = con.prepareStatement(ageSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String oldest = rs.getString("oldest_date");
                    String youngest = rs.getString("youngest_date");
                    double avgAge = rs.getDouble("avg_age");
                    boolean avgNull = rs.wasNull();

                    System.out.println("  Oldest birth date  : " + (oldest == null ? "-" : oldest));
                    System.out.println("  Youngest birth date: " + (youngest == null ? "-" : youngest));
                    if (avgNull) {
                        System.out.println("  Average age        : -");
                    } else {
                        System.out.printf("  Average age        : %.1f%n", avgAge);
                    }
                } else {
                    System.out.println("  No birth_date data found.");
                }
            }

        } catch (SQLException e) {
            System.out.println(RED + "Error while calculating statistics: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        System.out.println();
        waitForEnter();
    }
    // ROLE SELECTION HELPER
    private String selectRole() {
        while (true) {
            System.out.println();
            System.out.println("Select role:");
            System.out.println("1) Tester");
            System.out.println("2) Junior Developer");
            System.out.println("3) Senior Developer");
            System.out.println("4) Manager");
            System.out.print("Your choice (1-4, 'q' to cancel): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            if (choice.equals("q")) return null;
            switch (choice) {
                case "1": return "Tester";
                case "2": return "Junior Developer";
                case "3": return "Senior Developer";
                case "4": return "Manager";
                default:
                    System.out.println(YELLOW + "Please select 1, 2, 3 or 4 (or 'q' to cancel)." + RESET);
            }
        }
    }
}
