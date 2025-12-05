import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Stack;

public class JuniorDevMenu extends TesterMenu {

    // Basit uzunluk limitleri
    private static final int MAX_NAME_LEN      = 50;
    private static final int MAX_NICK_LEN      = 100;
    private static final int MAX_EMAIL_LEN     = 100;
    private static final int MAX_LINKEDIN_LEN  = 100;
    private static final int PHONE_LEN         = 10;

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
                    case 1: handleChangePassword(); break;
                    case 2: handleListContacts(); break;
                    case 3: handleSearchContacts(); break;
                    case 4: handleSortContacts(); break;
                    case 5: handleUpdateContact(); break;
                    case 6: handleUndo(); break;
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
            System.out.print(YELLOW + "Enter ID of contact to update: " + RESET);
            String idInput = scanner.nextLine().trim();
            
            if (idInput.equalsIgnoreCase("q")) return;

            int contactId;
            try {
                contactId = Integer.parseInt(idInput);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid ID format." + RESET);
                waitForEnter();
                continue;
            }

            if (contactId <= 0) {
                System.out.println(RED + "ID must be positive." + RESET);
                waitForEnter();
                continue;
            }

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
            System.out.print(YELLOW + "Select (1-9): " + RESET);
            String fieldChoice = scanner.nextLine().trim();

            String columnName = "";
            String prompt = "Enter new value: ";

            switch (fieldChoice) {
                case "1":
                    columnName = "first_name";
                    System.out.println();
                    System.out.println(CYAN + "Format for First Name:" + RESET);
                    System.out.println(YELLOW + "- Only letters are allowed (Turkish supported)." + RESET);
                    System.out.println(YELLOW + "- No spaces, no digits, no symbols." + RESET);
                    System.out.println(YELLOW + "- Max " + MAX_NAME_LEN + " characters." + RESET);
                    break;
                case "2":
                    columnName = "middle_name";
                    System.out.println();
                    System.out.println(CYAN + "Format for Middle Name:" + RESET);
                    System.out.println(YELLOW + "- Optional field." + RESET);
                    System.out.println(YELLOW + "- You may leave it empty." + RESET);
                    break;
                case "3":
                    columnName = "last_name";
                    System.out.println();
                    System.out.println(CYAN + "Format for Last Name:" + RESET);
                    System.out.println(YELLOW + "- Only letters are allowed (Turkish supported)." + RESET);
                    System.out.println(YELLOW + "- No spaces, no digits, no symbols." + RESET);
                    System.out.println(YELLOW + "- Max " + MAX_NAME_LEN + " characters." + RESET);
                    break;
                case "4":
                    columnName = "nickname";
                    System.out.println();
                    System.out.println(CYAN + "Format for Nickname:" + RESET);
                    System.out.println(YELLOW + "- Letters, digits, underscore and dot are allowed." + RESET);
                    System.out.println(YELLOW + "- No spaces." + RESET);
                    System.out.println(YELLOW + "- Max " + MAX_NICK_LEN + " characters." + RESET);
                    break;
                case "5":
                    columnName = "phone_primary";
                    prompt = "Enter new Primary Phone (digits only, " + PHONE_LEN + " digits): ";
                    System.out.println();
                    System.out.println(CYAN + "Format for Primary Phone:" + RESET);
                    System.out.println(YELLOW + "- Required field." + RESET);
                    System.out.println(YELLOW + "- Digits only, example: 5321112233" + RESET);
                    System.out.println(YELLOW + "- Must be exactly " + PHONE_LEN + " digits." + RESET);
                    break;
                case "6":
                    columnName = "phone_secondary";
                    prompt = "Enter new Secondary Phone (digits only, " + PHONE_LEN + " digits, optional): ";
                    System.out.println();
                    System.out.println(CYAN + "Format for Secondary Phone:" + RESET);
                    System.out.println(YELLOW + "- Optional field." + RESET);
                    System.out.println(YELLOW + "- If you fill, digits only, exactly " + PHONE_LEN + " digits." + RESET);
                    break;
                case "7":
                    columnName = "email";
                    prompt = "Enter new Email: ";
                    System.out.println();
                    System.out.println(CYAN + "Format for Email:" + RESET);
                    System.out.println(YELLOW + "- Required field." + RESET);
                    System.out.println(YELLOW + "- Must contain '@', no spaces." + RESET);
                    System.out.println(YELLOW + "- Max " + MAX_EMAIL_LEN + " characters." + RESET);
                    System.out.println(YELLOW + "- Supported providers for full features: gmail.com, outlook.com, hotmail.com, yahoo.com." + RESET);
                    System.out.println(YELLOW + "- If you use another provider, it will be saved but some features may not be available." + RESET);
                    System.out.println(YELLOW + "You cannot use characters like ! ? % ^ & * ( ) = + { } [ ] | ' \" < > , in email." + RESET);
                    break;
                case "8":
                    columnName = "linkedin_url";
                    System.out.println();
                    System.out.println(CYAN + "Format for LinkedIn URL:" + RESET);
                    System.out.println(YELLOW + "- Optional field." + RESET);
                    System.out.println(YELLOW + "- Max " + MAX_LINKEDIN_LEN + " characters." + RESET);
                    break;
                case "9":
                    columnName = "birth_date";
                    prompt = "Enter Birth Date (YYYY-MM-DD, or leave empty to clear): ";
                    System.out.println();
                    System.out.println(CYAN + "Format for Birth Date:" + RESET);
                    System.out.println(YELLOW + "- Example: 1999-04-23" + RESET);
                    System.out.println(YELLOW + "- Year between 1900 and 2100." + RESET);
                    break;
                default:
                    System.out.println(RED + "Invalid selection." + RESET);
                    waitForEnter();
                    continue;
            }

            System.out.println();
            System.out.print(YELLOW + prompt + RESET);
            String newValue = scanner.nextLine().trim();

            // ---- Zorunlu alanlar boş bırakılamaz ----
            if ((columnName.equals("first_name") || columnName.equals("last_name") || 
                 columnName.equals("phone_primary") || columnName.equals("email")) && newValue.isEmpty()) {
                System.out.println(RED + "Error: This field cannot be empty!" + RESET);
                waitForEnter();
                continue;
            }

            // ---- Alan bazlı validasyonlar ----

            // First / Last Name
            if (columnName.equals("first_name") || columnName.equals("last_name")) {
                if (!newValue.isEmpty()) {
                    if (newValue.length() > MAX_NAME_LEN) {
                        System.out.println(RED + "Error: Name is too long. Max " + MAX_NAME_LEN + " characters." + RESET);
                        waitForEnter();
                        continue;
                    }
                    if (!isValidName(newValue)) {
                        System.out.println(RED + "Invalid name format." + RESET);
                        System.out.println(YELLOW + "Rules: only letters (Turkish supported). No spaces, no digits, no symbols." + RESET);
                        waitForEnter();
                        continue;
                    }
                }
            }

            // Nickname
            if (columnName.equals("nickname")) {
                if (!newValue.isEmpty()) {
                    if (newValue.length() > MAX_NICK_LEN) {
                        System.out.println(RED + "Error: Nickname is too long. Max " + MAX_NICK_LEN + " characters." + RESET);
                        waitForEnter();
                        continue;
                    }
                    if (!isValidNickname(newValue)) {
                        System.out.println(RED + "Invalid nickname format." + RESET);
                        System.out.println(YELLOW + "Rules: letters, digits, underscore and dot are allowed. No spaces." + RESET);
                        waitForEnter();
                        continue;
                    }
                }
            }

            // Phones
            if (columnName.equals("phone_primary") || columnName.equals("phone_secondary")) {
                if (!newValue.isEmpty()) {
                    if (!newValue.matches("\\d+")) {
                        System.out.println(RED + "Error: Phone number must contain only digits!" + RESET);
                        waitForEnter();
                        continue;
                    }
                    if (newValue.length() != PHONE_LEN) {
                        System.out.println(RED + "Error: Phone number must be exactly " + PHONE_LEN + " digits." + RESET);
                        waitForEnter();
                        continue;
                    }
                }
            }

            // Email
            if (columnName.equals("email")) {
                if (!newValue.isEmpty()) {
                    if (newValue.length() > MAX_EMAIL_LEN) {
                        System.out.println(RED + "Error: Email is too long. Max " + MAX_EMAIL_LEN + " characters." + RESET);
                        waitForEnter();
                        continue;
                    }
                    if (newValue.contains(" ")) {
                        System.out.println(RED + "Error: Email cannot contain spaces!" + RESET);
                        waitForEnter();
                        continue;
                    }
                    char bad = findForbiddenEmailChar(newValue);
                    if (bad != 0) {
                        System.out.println(RED + "You cannot use the character '" + bad + "' in email." + RESET);
                        waitForEnter();
                        continue;
                    }
                    if (!newValue.contains("@")) {
                        System.out.println(RED + "Error: Invalid email format! (Must contain '@')" + RESET);
                        waitForEnter();
                        continue;
                    }

                    // Provider uyarısı (engellemeden)
                    if (!isValidEmailForEquals(newValue)) {
                        System.out.println(YELLOW + "Note: Full support is only guaranteed for gmail.com, outlook.com, hotmail.com and yahoo.com." + RESET);
                        System.out.println(YELLOW + "You can still save this email if you do not use these providers." + RESET);
                    }
                }
            }

            // LinkedIn length
            if (columnName.equals("linkedin_url")) {
                if (!newValue.isEmpty() && newValue.length() > MAX_LINKEDIN_LEN) {
                    System.out.println(RED + "Error: LinkedIn URL is too long. Max " + MAX_LINKEDIN_LEN + " characters." + RESET);
                    waitForEnter();
                    continue;
                }
            }

            // Birth Date
            if (columnName.equals("birth_date") && !newValue.isEmpty()) {
                if (!isValidExactDate(newValue)) {
                    System.out.println(RED + "Error: Invalid date format! Use YYYY-MM-DD, year 1900–2100." + RESET);
                    waitForEnter();
                    continue;
                }
            }

            // ================= DB KISMI =================
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
                        if (oldValue == null) oldValue = ""; 
                        idExists = true;
                    }
                }

                if (!idExists) {
                    System.out.println(RED + "Contact ID not found." + RESET);
                    waitForEnter();
                    continue;
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
                        
                        System.out.println(CYAN + "Updated Row:" + RESET);
                        printSingleContact(con, contactId);
                        
                        updateSuccess = true;
                    } else {
                        System.out.println(RED + "Update failed." + RESET);
                        waitForEnter();
                    }
                }

            } catch (SQLException e) {
                System.out.println(RED + "SQL Error: " + e.getMessage() + RESET);
                waitForEnter();
            } finally {
                try { con.close(); } catch (SQLException ignored) {}
            }
            
            if (updateSuccess) {
                System.out.println();
                System.out.println(CYAN + "What would you like to do next?" + RESET);
                System.out.println(GREEN + "1)" + RESET + " Update another contact");
                System.out.println(GREEN + "2)" + RESET + " Undo this update immediately");
                System.out.println(GREEN + "3)" + RESET + " Return to Main Menu");
                System.out.print(YELLOW + "Select (1-3): " + RESET);
                
                String nextAction = scanner.nextLine().trim();
                
                if (nextAction.equals("1")) {
                    continue; 
                } else if (nextAction.equals("2")) {
                    handleUndo(); 
                    continue; 
                } else {
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
                System.out.println("Reverted field '" + lastAction.columnName + "' for ID " + lastAction.contactId);
            } else {
                System.out.println(RED + "Could not undo. Contact might have been deleted." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Undo Error: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    protected void handleListContactsForUpdate() {
        Connection con = getConnection();
        if (con == null) return;
        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM contacts");
             ResultSet rs = stmt.executeQuery()) {
            printContactHeader();
            while (rs.next()) {
                printContactRow(rs);
            }
        } catch (Exception ignored) {}
        finally { try { con.close(); } catch (Exception ignored) {} }
    }

    protected void printSingleContact(Connection con, int id) {
        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM contacts WHERE contact_id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                printContactHeader();
                printContactRow(rs);
            }
        } catch (Exception ignored) {}
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
}