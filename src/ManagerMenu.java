import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Stack;

public class ManagerMenu extends TesterMenu {

    private static final int MAX_USERNAME_LEN = 50;
    private static final int MAX_NAME_LEN = 50;
    private static final int MAX_SURNAME_LEN = 50;
    private static final int MAX_PASSWORD_LEN = 50;

    private final Stack<UserSnapshot> undoUserStack;

    public ManagerMenu(String username, String fullName, String role, Scanner scanner, String passwordStrengthAtLogin) {
        super(username, fullName, role, scanner, passwordStrengthAtLogin);
        this.undoUserStack = new Stack<>();
    }

    @Override
    public void showMenu() {
        while (true) {
            clearScreen();
            String realFullName = loadRealFullName();

            System.out.println(CYAN + "=== MANAGER MENU ===" + RESET);
            System.out.println(GREEN + "User: " + RESET + realFullName + " (" + username + ")");
            System.out.println(GREEN + "Role: " + RESET + role);
            System.out.println();

            if (passwordStrengthAtLogin != null && !passwordStrengthAtLogin.isBlank()) {
                printPasswordStrengthBanner();
                System.out.println();
            }

            System.out.println(CYAN + "Please select an option:" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Change password");
            System.out.println(GREEN + "2)" + RESET + " List all users");
            System.out.println(GREEN + "3)" + RESET + " Add new user");
            System.out.println(GREEN + "4)" + RESET + " Update existing user");
            System.out.println(GREEN + "5)" + RESET + " Delete / fire user");
            System.out.println(GREEN + "6)" + RESET + " Contacts statistical info");
            System.out.println(GREEN + "7)" + RESET + " Undo last action (Add/Update/Delete)");
            System.out.println(GREEN + "8)" + RESET + " Logout");
            System.out.print(YELLOW + "Select an option (1-8): " + RESET);

            String input = scanner.nextLine().trim();
            int choice;

            if (input.isEmpty()) {
                System.out.println(RED + "Please enter a number." + RESET);
                waitForEnter();
                continue;
            }

            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Please enter a valid number." + RESET);
                waitForEnter();
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> handleChangePassword();
                    case 2 -> handleListUsers(true);
                    case 3 -> handleAddUser();
                    case 4 -> handleUpdateUser();
                    case 5 -> handleDeleteUser();
                    case 6 -> handleContactsStatistics();
                    case 7 -> handleUndoManager();
                    case 8 -> {
                        System.out.println(YELLOW + "Logging out. Goodbye, " + realFullName + "." + RESET);
                        return;
                    }
                    default -> {
                        System.out.println(RED + "Invalid option." + RESET);
                        waitForEnter();
                    }
                }
            } catch (Exception e) {
                System.out.println(RED + "Unexpected error: " + e.getMessage() + RESET);
                waitForEnter();
            }
        }
    }

    // ============================= SMALL HELPERS =============================

    private boolean isCancelKeyword(String in) {
        String t = (in == null ? "" : in.trim()).toLowerCase();
        return t.equals("q") || t.equals("quit") || t.equals("exit");
    }

    private boolean isValidUsernameFormat(String text) {
        if (text == null) return false;
        text = text.trim();
        if (text.isEmpty()) return false;
        if (text.length() > MAX_USERNAME_LEN) return false;
        if (text.contains(" ")) return false;
        // letters (with Turkish), digits, underscore, dot
        return text.matches("[A-Za-zÇĞİÖŞÜçğıöşü0-9_.]+");
    }

    // Name / surname: only letters (Turkish), no space, no digit, no symbol
    private boolean isValidPureName(String text, int maxLen) {
        if (text == null) return false;
        text = text.trim();
        if (text.isEmpty()) return false;
        if (text.length() > maxLen) return false;
        return text.matches("[A-Za-zÇĞİÖŞÜçğıöşü]+");
    }

    // Kullanıcıya "retry mi back mi?" soran helper
    private boolean askRetry() {
        while (true) {
            System.out.print("Would you like to try again? (Y/N): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) {
                return true;
            } else if (ans.equals("n") || ans.equals("no")) {
                return false;
            } else {
                System.out.println(YELLOW + "Please enter Y or N." + RESET);
            }
        }
    }

    // ============================= LIST USERS =============================

    private void handleListUsers(boolean pause) {
        clearScreen();
        System.out.println(CYAN + "=== USER LIST ===" + RESET);

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Database connection failed." + RESET);
            if (pause) waitForEnter();
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

        if (pause) waitForEnter();
    }

    // ============================= ADD USER (VALIDATION + UNDO + PW STRENGTH) =============================

    private void handleAddUser() {
        clearScreen();
        System.out.println(CYAN + "=== ADD NEW USER ===" + RESET);
        System.out.println(YELLOW + "You can type 'q' at any time to cancel." + RESET);
        System.out.println(YELLOW + "Username: letters (Turkish supported), digits, _ and . are allowed. No spaces." + RESET);
        System.out.println(YELLOW + "Name / Surname: only letters (Turkish supported). No spaces, no digits, no symbols." + RESET);
        System.out.println(YELLOW + "Max length: username/name/surname/password = 50 characters." + RESET);
        System.out.println();

        // ---------- USERNAME ----------
        String newUsername;
        while (true) {
            System.out.print("Username: ");
            newUsername = scanner.nextLine().trim();
            if (isCancelKeyword(newUsername)) return;

            if (!isValidUsernameFormat(newUsername)) {
                System.out.println(RED + "Invalid username format." + RESET);
                System.out.println("Rules: letters (Turkish), digits, underscore and dot are allowed. No spaces. Max 50 chars.");
                continue;
            }
            break;
        }

        // ---------- NAME ----------
        String name;
        while (true) {
            System.out.print("Name: ");
            name = scanner.nextLine().trim();
            if (isCancelKeyword(name)) return;

            if (!isValidPureName(name, MAX_NAME_LEN)) {
                System.out.println(RED + "Invalid name format." + RESET);
                System.out.println("Rules: only letters (Turkish supported). No spaces, no digits, no symbols. Max 50 chars.");
                continue;
            }
            break;
        }

        // ---------- SURNAME ----------
        String surname;
        while (true) {
            System.out.print("Surname: ");
            surname = scanner.nextLine().trim();
            if (isCancelKeyword(surname)) return;

            if (!isValidPureName(surname, MAX_SURNAME_LEN)) {
                System.out.println(RED + "Invalid surname format." + RESET);
                System.out.println("Rules: only letters (Turkish supported). No spaces, no digits, no symbols. Max 50 chars.");
                continue;
            }
            break;
        }

        // ---------- ROLE ----------
        String roleStr = selectRole();
        if (roleStr == null) {
            System.out.println(YELLOW + "Operation cancelled." + RESET);
            waitForEnter();
            return;
        }

        // ---------- PASSWORD + STRENGTH + SUGGESTION ----------
        String password;

        String suggestedPassword = generateStrongPasswordSuggestion();
        System.out.println();
        System.out.println("Here is a strong password suggestion (optional):");
        System.out.println(GREEN + suggestedPassword + RESET);
        System.out.println("You can type this exactly as the new user's password, or create your own.");
        System.out.println();

        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();
            if (password == null) password = "";
            password = password.trim();
            if (isCancelKeyword(password)) {
                System.out.println(YELLOW + "Add user cancelled." + RESET);
                waitForEnter();
                return;
            }

            if (password.isEmpty()) {
                System.out.println(RED + "Password cannot be empty." + RESET);
                continue;
            }
            if (password.length() > MAX_PASSWORD_LEN) {
                System.out.println(RED + "Password is too long (max " + MAX_PASSWORD_LEN + ")." + RESET);
                continue;
            }

            String strength = evaluatePasswordStrength(password);
            System.out.println("Password strength: " + YELLOW + strength.toUpperCase() + RESET);

            if ("very_weak".equals(strength) || "weak".equals(strength)) {
                System.out.print(
                        RED +
                        "This password is " + strength.replace('_', ' ') +
                        ". Are you sure you want to use it? (y/n, q = cancel add user): " +
                        RESET
                );
                String ans = scanner.nextLine().trim().toLowerCase();
                if (isCancelKeyword(ans)) {
                    System.out.println(YELLOW + "Add user cancelled." + RESET);
                    waitForEnter();
                    return;
                }
                if (ans.equals("n") || ans.equals("no")) {
                    System.out.println(YELLOW + "Okay, please enter a stronger password." + RESET);
                    continue;
                }
                if (!(ans.equals("y") || ans.equals("yes"))) {
                    System.out.println(YELLOW + "Please answer with y or n (or q to cancel)." + RESET);
                    continue;
                }
            }

            System.out.print("Confirm password: ");
            String confirm = scanner.nextLine();
            if (confirm == null) confirm = "";
            confirm = confirm.trim();

            if (isCancelKeyword(confirm)) {
                System.out.println(YELLOW + "Add user cancelled." + RESET);
                waitForEnter();
                return;
            }

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

        boolean added = false;
        int newUserId = -1;

        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, newUsername);
            ps.setString(2, hash);
            ps.setString(3, name);
            ps.setString(4, surname);
            ps.setString(5, roleStr);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newUserId = keys.getInt(1);
                    }
                }
                System.out.println(GREEN + "User added successfully. (ID = " + newUserId + ")" + RESET);
                added = true;

                undoUserStack.push(new UserSnapshot("ADD", newUserId, newUsername, hash, name, surname, roleStr));
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                System.out.println(RED + "Username already exists." + RESET);
            } else {
                System.out.println(RED + "Error: " + msg + RESET);
            }
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        if (!added) {
            waitForEnter();
            return;
        }

        while (true) {
            System.out.println();
            System.out.println(CYAN + "What would you like to do next?" + RESET);
            System.out.println("1. Add another user");
            System.out.println("2. Return to MANAGER menu");
            System.out.println("3. Undo this add immediately");
            System.out.print("Select (1-3): ");
            String next = scanner.nextLine().trim();

            if (next.equals("1")) {
                handleAddUser();
                return;
            } else if (next.equals("2")) {
                return;
            } else if (next.equals("3")) {
                handleUndoManager();
                return;
            } else {
                System.out.println(YELLOW + "Please select 1, 2 or 3." + RESET);
            }
        }
    }

    // ============================= UPDATE USER =============================

    private void handleUpdateUser() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== UPDATE USER ===" + RESET);

            handleListUsers(false);
            System.out.println();

            System.out.print("Enter user ID to update (or 'q' to cancel): ");
            String idInput = scanner.nextLine().trim();
            if (isCancelKeyword(idInput)) return;

            int userId;
            try {
                userId = Integer.parseInt(idInput);
                if (userId <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid ID format. Please enter a number." + RESET);
                if (askRetry()) {
                    continue;
                } else {
                    return;
                }
            }

            Connection con = getConnection();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                if (askRetry()) {
                    continue;
                } else {
                    return;
                }
            }

            String selectSql = "SELECT username, name, surname, role, password_hash FROM users WHERE user_id = ?";

            String currentUsername = null;
            String currentName = null;
            String currentSurname = null;
            String currentRole = null;
            String currentHash = null;

            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(RED + "User ID not found." + RESET);
                        if (askRetry()) {
                            continue;
                        } else {
                            return;
                        }
                    }
                    currentUsername = rs.getString("username");
                    currentName = rs.getString("name");
                    currentSurname = rs.getString("surname");
                    currentRole = rs.getString("role");
                    currentHash = rs.getString("password_hash");
                }

                if (currentUsername == null) currentUsername = "";
                if (currentName == null) currentName = "";
                if (currentSurname == null) currentSurname = "";
                if (currentRole == null) currentRole = "";
                if (currentHash == null) currentHash = "";

                System.out.println("Current username : " + currentUsername);
                System.out.println("Current name     : " + currentName + " " + currentSurname);
                System.out.println("Current role     : " + currentRole);
                System.out.println();

                // ---------- NEW USERNAME ----------
                System.out.print("New username (leave blank to keep '" + currentUsername + "'): ");
                String newUsername = scanner.nextLine().trim();
                if (newUsername.isEmpty()) {
                    newUsername = currentUsername;
                } else {
                    if (!isValidUsernameFormat(newUsername)) {
                        System.out.println(RED + "Invalid username format." + RESET);
                        waitForEnter();
                        continue;
                    }
                }

                // ---------- NEW NAME ----------
                System.out.print("New name (leave blank to keep '" + currentName + "'): ");
                String newName = scanner.nextLine().trim();
                if (newName.isEmpty()) {
                    newName = currentName;
                } else {
                    if (!isValidPureName(newName, MAX_NAME_LEN)) {
                        System.out.println(RED + "Invalid name format." + RESET);
                        waitForEnter();
                        continue;
                    }
                }

                // ---------- NEW SURNAME ----------
                System.out.print("New surname (leave blank to keep '" + currentSurname + "'): ");
                String newSurname = scanner.nextLine().trim();
                if (newSurname.isEmpty()) {
                    newSurname = currentSurname;
                } else {
                    if (!isValidPureName(newSurname, MAX_SURNAME_LEN)) {
                        System.out.println(RED + "Invalid surname format." + RESET);
                        waitForEnter();
                        continue;
                    }
                }

                // ---------- ROLE ----------
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
                boolean updateSuccess = false;

                try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                    updatePs.setString(1, newUsername);
                    updatePs.setString(2, newName);
                    updatePs.setString(3, newSurname);
                    updatePs.setString(4, newRole);
                    updatePs.setInt(5, userId);

                    int rows = updatePs.executeUpdate();
                    if (rows > 0) {
                        System.out.println(GREEN + "User updated successfully." + RESET);
                        updateSuccess = true;

                        undoUserStack.push(new UserSnapshot(
                                "UPDATE",
                                userId,
                                currentUsername,
                                currentHash,
                                currentName,
                                currentSurname,
                                currentRole
                        ));
                    } else {
                        System.out.println(YELLOW + "No changes applied." + RESET);
                    }
                }

                if (updateSuccess) {
                    System.out.print("Reset this user's password? (y/n): ");
                    String resetAns = scanner.nextLine().trim().toLowerCase();
                    if (resetAns.equals("y") || resetAns.equals("yes")) {
                        resetUserPassword(con, userId);
                    }
                }

                if (updateSuccess) {
                    System.out.println();
                    System.out.println(CYAN + "What would you like to do next?" + RESET);
                    System.out.println("1. Update another user");
                    System.out.println("2. Return to MANAGER menu");
                    System.out.println("3. Undo this update immediately");
                    System.out.print("Select (1-3): ");
                    String nextAction = scanner.nextLine().trim();
                    if (nextAction.equals("1")) {
                        continue;
                    } else if (nextAction.equals("3")) {
                        handleUndoManager();
                        continue;
                    } else {
                        break;
                    }
                }

            } catch (SQLException e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
            } finally {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
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
                System.out.println(RED + "Password is too long." + RESET);
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
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    // ============================= DELETE USER (UNDO DESTEKLİ) =============================

    private void handleDeleteUser() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== DELETE / FIRE USER ===" + RESET);

            handleListUsers(false);
            System.out.println();

            System.out.print("Enter user ID to delete (or 'q' to cancel): ");
            String idInput = scanner.nextLine().trim();
            if (isCancelKeyword(idInput)) return;

            int userId;
            try {
                userId = Integer.parseInt(idInput);
                if (userId <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid ID format. Please enter a number." + RESET);
                if (askRetry()) {
                    continue;
                } else {
                    return;
                }
            }

            Connection con = getConnection();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                if (askRetry()) {
                    continue;
                } else {
                    return;
                }
            }

            UserSnapshot backup = null;
            boolean deleteSuccess = false;

            try {
                String selectSql = "SELECT * FROM users WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println(RED + "User ID not found." + RESET);
                            if (askRetry()) {
                                continue;
                            } else {
                                return;
                            }
                        }

                        String uname = rs.getString("username");
                        if (uname != null && uname.equals(username)) {
                            System.out.println(RED + "You cannot delete yourself." + RESET);
                            if (askRetry()) {
                                continue;
                            } else {
                                return;
                            }
                        }

                        backup = new UserSnapshot(
                            "DELETE",
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("role")
                        );

                        System.out.println("User to delete: " + uname + " (" + rs.getString("role") + ")");
                        System.out.print("Are you sure? (y/n): ");
                        String ans = scanner.nextLine().trim().toLowerCase();
                        if (!(ans.equals("y") || ans.equals("yes"))) {
                            System.out.println(YELLOW + "Delete cancelled." + RESET);
                            waitForEnter();
                            continue;
                        }
                    }
                }

                String deleteSql = "DELETE FROM users WHERE user_id = ?";
                try (PreparedStatement delPs = con.prepareStatement(deleteSql)) {
                    delPs.setInt(1, userId);
                    int rows = delPs.executeUpdate();
                    if (rows > 0) {
                        System.out.println(GREEN + "User deleted successfully." + RESET);
                        if (backup != null) {
                            undoUserStack.push(backup);
                        }
                        deleteSuccess = true;
                    } else {
                        System.out.println(RED + "No user deleted." + RESET);
                    }
                }

            } catch (SQLException e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
            } finally {
                try { con.close(); } catch (SQLException ignored) {}
            }

            if (deleteSuccess) {
                System.out.println();
                System.out.println(CYAN + "What would you like to do next?" + RESET);
                System.out.println("1. Delete another user");
                System.out.println("2. Return to MANAGER menu");
                System.out.println("3. Undo this deletion immediately");
                System.out.print("Select (1-3): ");
                String nextAction = scanner.nextLine().trim();

                if (nextAction.equals("1")) {
                    continue;
                } else if (nextAction.equals("3")) {
                    handleUndoManager();
                    continue;
                } else {
                    return;
                }
            } else {
                waitForEnter();
            }
        }
    }

    // ============================= UNDO (ADD / UPDATE / DELETE) =============================

    private void handleUndoManager() {
        clearScreen();
        System.out.println(CYAN + "=== UNDO LAST USER ACTION ===" + RESET);

        if (undoUserStack.isEmpty()) {
            System.out.println(YELLOW + "Nothing to undo." + RESET);
            waitForEnter();
            return;
        }

        UserSnapshot snap = undoUserStack.pop();
        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Connection failed." + RESET);
            undoUserStack.push(snap);
            waitForEnter();
            return;
        }

        try {
            if ("DELETE".equals(snap.actionType)) {
                String sql = "INSERT INTO users (user_id, username, password_hash, name, surname, role) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, snap.user_id);
                    ps.setString(2, snap.username);
                    ps.setString(3, snap.password_hash);
                    ps.setString(4, snap.name);
                    ps.setString(5, snap.surname);
                    ps.setString(6, snap.role);
                    ps.executeUpdate();
                    System.out.println(GREEN + "Undo successful. User '" + snap.username + "' restored." + RESET);
                }
            } else if ("UPDATE".equals(snap.actionType)) {
                String sql = "UPDATE users SET username=?, password_hash=?, name=?, surname=?, role=? WHERE user_id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, snap.username);
                    ps.setString(2, snap.password_hash);
                    ps.setString(3, snap.name);
                    ps.setString(4, snap.surname);
                    ps.setString(5, snap.role);
                    ps.setInt(6, snap.user_id);
                    ps.executeUpdate();
                    System.out.println(GREEN + "Undo successful. User '" + snap.username + "' reverted to previous state." + RESET);
                }
            } else if ("ADD".equals(snap.actionType)) {
                String sql = "DELETE FROM users WHERE user_id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, snap.user_id);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println(GREEN + "Undo ADD successful. User '" + snap.username + "' removed." + RESET);
                    } else {
                        System.out.println(YELLOW + "Nothing removed. User may have been deleted already." + RESET);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(RED + "Undo failed: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    // ============================= CONTACT STATS =============================

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
            System.out.println(YELLOW + "\nTop 5 First Names (Most Frequent):" + RESET);
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
                    System.out.printf("  %-15s : %d%n", fn, cnt);
                }
                if (!any) {
                    System.out.println("  (no data)");
                }
            }

            System.out.println(YELLOW + "\nTop 5 Surnames (Most Frequent):" + RESET);
            String surnameSql = "SELECT last_name, COUNT(*) AS cnt " +
                             "FROM contacts GROUP BY last_name " +
                             "HAVING last_name IS NOT NULL AND last_name <> '' " +
                             "ORDER BY cnt DESC, last_name ASC LIMIT 5";
            try (PreparedStatement ps = con.prepareStatement(surnameSql);
                 ResultSet rs = ps.executeQuery()) {

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String ln = rs.getString("last_name");
                    int cnt = rs.getInt("cnt");
                    if (ln == null) ln = "(NULL)";
                    System.out.printf("  %-15s : %d%n", ln, cnt);
                }
                if (!any) {
                    System.out.println("  (no data)");
                }
            }

            System.out.println(YELLOW + "\nEmail Provider Statistics:" + RESET);
            String emailSql = "SELECT SUBSTRING_INDEX(email, '@', -1) as provider, COUNT(*) as cnt " +
                              "FROM contacts WHERE email LIKE '%@%' " +
                              "GROUP BY provider ORDER BY cnt DESC LIMIT 5";
            try (PreparedStatement ps = con.prepareStatement(emailSql);
                 ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while(rs.next()){
                    any = true;
                    System.out.printf("  %-20s : %d%n", rs.getString("provider"), rs.getInt("cnt"));
                }
                if(!any) System.out.println("  (no data)");
            }

            System.out.println(YELLOW + "\nLinkedIn URL Statistics:" + RESET);
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

            System.out.println(YELLOW + "\nAge Statistics (based on birth_date):" + RESET);
            String ageSql =
                    "SELECT " +
                    "MIN(birth_date) AS oldest_date, " +
                    "MAX(birth_date) AS youngest_date, " +
                    "AVG(TIMESTAMPDIFF(YEAR, birth_date, CURDATE())) AS avg_age, " +
                    "COUNT(*) as total_birth_dates " +
                    "FROM contacts WHERE birth_date IS NOT NULL";
            try (PreparedStatement ps = con.prepareStatement(ageSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_birth_dates");
                    if (total > 0) {
                        String oldest = rs.getString("oldest_date");
                        String youngest = rs.getString("youngest_date");
                        double avgAge = rs.getDouble("avg_age");

                        System.out.println("  Oldest birth date  : " + oldest);
                        System.out.println("  Youngest birth date: " + youngest);
                        System.out.printf("  Average age        : %.1f years%n", avgAge);
                    } else {
                        System.out.println("  No birth date data available.");
                    }
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

    // ============================= ROLE SELECTION =============================

    private String selectRole() {
        while (true) {
            System.out.println();
            System.out.println(CYAN + "Select role:" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Tester");
            System.out.println(GREEN + "2)" + RESET + " Junior Developer");
            System.out.println(GREEN + "3)" + RESET + " Senior Developer");
            System.out.println(GREEN + "4)" + RESET + " Manager");
            System.out.print(YELLOW + "Your choice (1-4, 'q' to cancel): " + RESET);
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

    // ============================= SNAPSHOT CLASS =============================

    private static class UserSnapshot {
        String actionType;  // ADD / UPDATE / DELETE
        int user_id;
        String username;
        String password_hash;
        String name;
        String surname;
        String role;

        public UserSnapshot(String actionType, int user_id, String username, String password_hash, String name, String surname, String role) {
            this.actionType = actionType;
            this.user_id = user_id;
            this.username = username;
            this.password_hash = password_hash;
            this.name = name;
            this.surname = surname;
            this.role = role;
        }
    }
}