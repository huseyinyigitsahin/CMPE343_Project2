import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Stack;

public class JuniorDevMenu extends TesterMenu {

    private static final int MAX_NAME_LEN = 50;
    private static final int MAX_NICK_LEN = 100;
    private static final int MAX_EMAIL_LEN = 100;
    private static final int MAX_LINKEDIN_LEN = 100;
    private static final int PHONE_LEN = 10;

    protected Stack<UndoAction> undoStack;

    public JuniorDevMenu(String username, String fullName, String role, Scanner scanner, String passwordStrength) {
        super(username, fullName, role, scanner, passwordStrength);
        this.undoStack = new Stack<>();
    }

    @Override
    public void showMenu() {
        while (true) {
            clearScreen();
            String realFullName = loadRealFullName();

            System.out.println(CYAN + "=== JUNIOR DEVELOPER MENU ===" + RESET);
            System.out.println(GREEN + "User: " + RESET + realFullName + " (" + username + ")");
            System.out.println(GREEN + "Role: " + RESET + role);
            System.out.println();

            if (passwordStrengthAtLogin != null) {
                printPasswordStrengthBanner();
                System.out.println();
            }

            System.out.println(CYAN + "Please select an option:" + RESET);
            System.out.println(GREEN + "1)" + RESET + " Change password");
            System.out.println(GREEN + "2)" + RESET + " List all contacts");
            System.out.println(GREEN + "3)" + RESET + " Search contacts");
            System.out.println(GREEN + "4)" + RESET + " Sort contacts");
            System.out.println(GREEN + "5)" + RESET + " Update existing contact");
            System.out.println(GREEN + "6)" + RESET + " Undo last update");
            System.out.println(GREEN + "7)" + RESET + " Logout");
            System.out.print(YELLOW + "Select an option (1-7): " + RESET);

            String input = scanner.nextLine().trim();
            int choice;

            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid input. Please enter a number." + RESET);
                waitForEnter();
                continue;
            }

            try {
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
                        handleUpdateContact();
                        break;
                    case 6:
                        handleUndo();
                        break;
                    case 7:
                        System.out.println(YELLOW + "Logging out..." + RESET);
                        return;
                    default:
                        System.out.println(RED + "Invalid option." + RESET);
                        waitForEnter();
                }
            } catch (Exception e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
                waitForEnter();
            }
        }
    }

    protected void handleUpdateContact() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== UPDATE CONTACT ===" + RESET);

            handleListContactsForUpdate();

            System.out.println();
            System.out.println(YELLOW + "Enter 'q' to return to Main Menu." + RESET);
            System.out.print("Enter ID of contact to update: ");
            String idInput = scanner.nextLine().trim();

            if (idInput.equalsIgnoreCase("q")) return;

            int contactId;
            try {
                contactId = Integer.parseInt(idInput);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid ID format. Please enter a number." + RESET);
                waitForEnter();
                continue;
            }

            if (contactId <= 0) {
                System.out.println(RED + "ID must be positive." + RESET);
                waitForEnter();
                continue;
            }

            while (true) {
                System.out.println();
                System.out.println(CYAN + "Which field do you want to update?" + RESET);
                System.out.println(GREEN + "1)" + RESET + " First Name");
                System.out.println(GREEN + "2)" + RESET + " Middle Name");
                System.out.println(GREEN + "3)" + RESET + " Last Name");
                System.out.println(GREEN + "4)" + RESET + " Nickname");
                System.out.println(GREEN + "5)" + RESET + " Primary Phone");
                System.out.println(GREEN + "6)" + RESET + " Secondary Phone");
                System.out.println(GREEN + "7)" + RESET + " Email");
                System.out.println(GREEN + "8)" + RESET + " LinkedIn URL");
                System.out.println(GREEN + "9)" + RESET + " Birth Date");
                System.out.println(GREEN + "0)" + RESET + " Back to Main Menu");
                System.out.print(YELLOW + "Select (0-9): " + RESET);

                String fieldChoice = scanner.nextLine().trim();

                if (fieldChoice.equals("0") || fieldChoice.equalsIgnoreCase("q")) {
                    return;
                }

                String columnName = "";
                String newValue = "";

                boolean inputValid = false;

                while (!inputValid) {
                    switch (fieldChoice) {
                        case "1":
                            columnName = "first_name";
                            System.out.println(CYAN + "\nFormat for First Name:" + RESET);
                            System.out.println(YELLOW + "- Only letters (Turkish supported)." + RESET);
                            System.out.println(YELLOW + "- Max 50 chars. No digits/spaces." + RESET);
                            break;
                        case "2":
                            columnName = "middle_name";
                            System.out.println(CYAN + "\nFormat for Middle Name:" + RESET);
                            System.out.println(YELLOW + "- Optional. Only letters." + RESET);
                            break;
                        case "3":
                            columnName = "last_name";
                            System.out.println(CYAN + "\nFormat for Last Name:" + RESET);
                            System.out.println(YELLOW + "- Only letters. Max 50 chars." + RESET);
                            break;
                        case "4":
                            columnName = "nickname";
                            System.out.println(CYAN + "\nFormat for Nickname:" + RESET);
                            System.out.println(YELLOW + "- Letters, digits, dot, underscore allowed." + RESET);
                            break;
                        case "5":
                            columnName = "phone_primary";
                            System.out.println(CYAN + "\nFormat for Primary Phone:" + RESET);
                            System.out.println(YELLOW + "- Exactly 10 digits (e.g. 5321112233)." + RESET);
                            break;
                        case "6":
                            columnName = "phone_secondary";
                            System.out.println(CYAN + "\nFormat for Secondary Phone:" + RESET);
                            System.out.println(YELLOW + "- Optional. Exactly 10 digits." + RESET);
                            break;
                        case "7":
                            columnName = "email";
                            System.out.println(CYAN + "\nFormat for Email:" + RESET);
                            System.out.println(YELLOW + "- Must contain '@'. No spaces." + RESET);
                            break;
                        case "8":
                            columnName = "linkedin_url";
                            System.out.println(CYAN + "\nFormat for LinkedIn:" + RESET);
                            System.out.println(YELLOW + "- Max 100 chars." + RESET);
                            break;
                        case "9":
                            columnName = "birth_date";
                            System.out.println(CYAN + "\nFormat for Birth Date:" + RESET);
                            System.out.println(YELLOW + "- Format: YYYY-MM-DD (e.g. 1995-04-23)." + RESET);
                            break;
                        default:
                            System.out.println(RED + "Invalid selection. Please try again." + RESET);
                            break;
                    }

                    if (columnName.isEmpty())
                        break;

                    System.out.print("Enter new value (or 'q' to cancel): ");
                    newValue = scanner.nextLine().trim();

                    if (newValue.equalsIgnoreCase("q")) {
                        System.out.println(YELLOW + "Update cancelled." + RESET);
                        return;
                    }

                    boolean hasError = false;

                    if ((columnName.equals("first_name") || columnName.equals("last_name") ||
                            columnName.equals("phone_primary") || columnName.equals("email")) && newValue.isEmpty()) {
                        System.out.println(RED + ">> Error: This field cannot be empty!" + RESET);
                        hasError = true;
                    }

                    if (!hasError && (columnName.equals("first_name") || columnName.equals("last_name"))) {
                        if (!newValue.isEmpty()) {
                            if (newValue.length() > MAX_NAME_LEN) {
                                System.out.println(RED + ">> Error: Name is too long." + RESET);
                                hasError = true;
                            } else if (!isValidName(newValue)) {
                                System.out.println(RED + ">> Error: Name must contain only letters." + RESET);
                                hasError = true;
                            }
                        }
                    }

                    if (!hasError && columnName.equals("nickname")) {
                        if (!newValue.isEmpty()) {
                            if (newValue.length() > MAX_NICK_LEN) {
                                System.out.println(RED + ">> Error: Nickname is too long." + RESET);
                                hasError = true;
                            } else if (!isValidNickname(newValue)) {
                                System.out.println(RED + ">> Error: Invalid characters in nickname." + RESET);
                                hasError = true;
                            }
                        }
                    }

                    if (!hasError && columnName.contains("phone")) {
                        if (!newValue.isEmpty()) {
                            if (!newValue.matches("\\d+")) {
                                System.out.println(RED + ">> Error: Phone must contain digits only." + RESET);
                                hasError = true;
                            } else if (newValue.length() != PHONE_LEN) {
                                System.out.println(RED + ">> Error: Phone must be exactly 10 digits." + RESET);
                                hasError = true;
                            }
                        }
                    }

                    if (!hasError && columnName.equals("email")) {
                        if (!newValue.isEmpty()) {
                            if (newValue.length() > MAX_EMAIL_LEN) {
                                System.out.println(RED + ">> Error: Email is too long." + RESET);
                                hasError = true;
                            } else if (!newValue.contains("@")) {
                                System.out.println(RED + ">> Error: Invalid email format (missing '@')." + RESET);
                                hasError = true;
                            } else {
                                char bad = findForbiddenEmailChar(newValue);
                                if (bad != 0) {
                                    System.out.println(RED + ">> Error: Forbidden character '" + bad + "' in email."
                                            + RESET);
                                    hasError = true;
                                }
                            }
                        }
                    }

                    if (!hasError && columnName.equals("linkedin_url")) {
                        if (!newValue.isEmpty() && newValue.length() > MAX_LINKEDIN_LEN) {
                            System.out.println(RED + ">> Error: LinkedIn URL is too long." + RESET);
                            hasError = true;
                        }
                    }

                    if (!hasError && columnName.equals("birth_date") && !newValue.isEmpty()) {
                        if (!isValidExactDate(newValue)) {
                            System.out.println(RED + ">> Error: Invalid date. Use YYYY-MM-DD (e.g. 1990-01-30)."
                                    + RESET);
                            hasError = true;
                        }
                    }

                    if (hasError) {
                        System.out.println(YELLOW + "Please try again observing the rules above.\n" + RESET);
                        continue;
                    }

                    inputValid = true;
                }

                if (columnName.isEmpty())
                    continue;

                Connection con = getConnection();
                if (con == null) {
                    System.out.println(RED + "Connection failed." + RESET);
                    return;
                }

                boolean updateSuccess = false;

                try {
                    String selectSql = "SELECT " + columnName + " FROM contacts WHERE contact_id = ?";
                    String oldValue = "";
                    boolean idExists = false;

                    try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
                        selectStmt.setInt(1, contactId);
                        ResultSet rs = selectStmt.executeQuery();
                        if (rs.next()) {
                            oldValue = rs.getString(columnName);
                            if (oldValue == null)
                                oldValue = "";
                            idExists = true;
                        }
                    }

                    if (!idExists) {
                        System.out.println(RED + "Contact ID not found." + RESET);
                        waitForEnter();
                        break;
                    }

                    String updateSql = "UPDATE contacts SET " + columnName + " = ? WHERE contact_id = ?";
                    try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                        if (newValue.isEmpty() && columnName.equals("birth_date")) {
                            updateStmt.setNull(1, java.sql.Types.DATE);
                        } else {
                            updateStmt.setString(1, newValue);
                        }

                        updateStmt.setInt(2, contactId);

                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println(GREEN + "Contact updated successfully!" + RESET);
                            undoStack.push(new UndoAction(contactId, columnName, oldValue));

                            System.out.println("Updated Row:");
                            printSingleContact(con, contactId);

                            updateSuccess = true;
                        } else {
                            System.out.println(YELLOW + "No changes applied." + RESET);
                            updateSuccess = false;
                        }
                    }

                } catch (SQLException e) {
                    System.out.println(RED + "SQL Error: " + e.getMessage() + RESET);
                    waitForEnter();
                    break;
                } finally {
                    try {
                        con.close();
                    } catch (SQLException ignored) {
                    }
                }

                if (updateSuccess) {
                    System.out.println();
                    System.out.println(CYAN + "What would you like to do next?" + RESET);
                    System.out.println("1. Update another contact");
                    System.out.println("2. Undo this update immediately");
                    System.out.println("3. Return to Main Menu");
                    System.out.print("Select (1-3): ");

                    String nextAction = scanner.nextLine().trim();

                    if (nextAction.equals("1")) {
                        break;
                    } else if (nextAction.equals("2")) {
                        handleUndo();
                        break;
                    } else {
                        return;
                    }
                } else {
                    if (askRetry())
                        continue;
                    else
                        return;
                }
            }
        }
    }

    protected void handleUndo() {
        clearScreen();
        System.out.println(CYAN + "=== UNDO LAST UPDATE ===" + RESET);

        if (undoStack.isEmpty()) {
            System.out.println(YELLOW + "Nothing to undo." + RESET);
            waitForEnter();
            return;
        }

        UndoAction lastAction = undoStack.pop();

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Connection failed." + RESET);
            undoStack.push(lastAction);
            waitForEnter();
            return;
        }

        String sql = "UPDATE contacts SET " + lastAction.columnName + " = ? WHERE contact_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            if (lastAction.oldValue == null || lastAction.oldValue.isEmpty()) {
                if (lastAction.columnName.equals("birth_date")) {
                    pstmt.setNull(1, java.sql.Types.DATE);
                } else {
                    pstmt.setString(1, "");
                }
            } else {
                pstmt.setString(1, lastAction.oldValue);
            }

            pstmt.setInt(2, lastAction.contactId);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "Undo successful!" + RESET);
                System.out.println(
                        "Reverted field '" + lastAction.columnName + "' for ID " + lastAction.contactId);
                System.out.println("Value restored to: "
                        + (lastAction.oldValue.isEmpty() ? "[EMPTY]" : lastAction.oldValue));
            } else {
                System.out.println(RED + "Could not undo. Contact might have been deleted." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Undo Error: " + e.getMessage() + RESET);
        } finally {
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }

        waitForEnter();
    }

    protected void handleListContactsForUpdate() {
        Connection con = getConnection();
        if (con == null)
            return;
        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM contacts");
                ResultSet rs = stmt.executeQuery()) {
            printContactHeader();
            while (rs.next()) {
                printContactRow(rs);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                con.close();
            } catch (Exception ignored) {
            }
        }
    }

    protected void printSingleContact(Connection con, int id) {
        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM contacts WHERE contact_id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                printContactHeader();
                printContactRow(rs);
            }
        } catch (Exception ignored) {
        }
    }

    protected class UndoAction {
        int contactId;
        String columnName;
        String oldValue;

        public UndoAction(int contactId, String columnName, String oldValue) {
            this.contactId = contactId;
            this.columnName = columnName;
            this.oldValue = oldValue;
        }
    }

    private boolean askRetry() {
        while (true) {
            System.out.print(YELLOW + "Would you like to try again? (y/n): " + RESET);
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes"))
                return true;
            if (ans.equals("n") || ans.equals("no"))
                return false;
        }
    }
}