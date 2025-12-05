import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Stack;

public class JuniorDevMenu extends TesterMenu {

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
            System.out.println("User : " + realFullName + " (" + username + ")");
            System.out.println("Role : " + role);
            System.out.println();

            if (passwordStrengthAtLogin != null) {
                printPasswordStrengthBanner();
                System.out.println();
            }

            System.out.println("1. Change password");
            System.out.println("2. List all contacts");
            System.out.println("3. Search contacts");
            System.out.println("4. Sort contacts");
            System.out.println("5. Update existing contact");
            System.out.println("6. Undo last update");
            System.out.println("7. Logout");
            System.out.print("Select an option (1-7): ");

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

            System.out.println(YELLOW + "Enter 'q' to return to Main Menu." + RESET);
            System.out.print("Enter ID of contact to update: ");
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
            System.out.println("Which field do you want to update?");
            System.out.println("1. First Name");
            System.out.println("2. Middle Name");
            System.out.println("3. Last Name");
            System.out.println("4. Nickname");
            System.out.println("5. Primary Phone");
            System.out.println("6. Secondary Phone");
            System.out.println("7. Email");
            System.out.println("8. LinkedIn URL");
            System.out.println("9. Birth Date");
            System.out.print("Select (1-9): ");
            String fieldChoice = scanner.nextLine().trim();

            String columnName = "";
            String prompt = "Enter new value: ";

            switch (fieldChoice) {
                case "1": columnName = "first_name"; break;
                case "2": columnName = "middle_name"; break;
                case "3": columnName = "last_name"; break;
                case "4": columnName = "nickname"; break;
                case "5": columnName = "phone_primary"; prompt = "Enter new Phone (digits only): "; break;
                case "6": columnName = "phone_secondary"; prompt = "Enter new Phone (digits only): "; break;
                case "7": columnName = "email"; prompt = "Enter new Email: "; break;
                case "8": columnName = "linkedin_url"; break;
                case "9": columnName = "birth_date"; prompt = "Enter Birth Date (YYYY-MM-DD): "; break;
                default:
                    System.out.println(RED + "Invalid selection." + RESET);
                    waitForEnter();
                    continue;
            }

            System.out.print(prompt);
            String newValue = scanner.nextLine().trim();

            if ((columnName.equals("first_name") || columnName.equals("last_name") || 
                 columnName.equals("phone_primary") || columnName.equals("email")) && newValue.isEmpty()) {
                System.out.println(RED + "Error: This field cannot be empty!" + RESET);
                waitForEnter();
                continue;
            }

            if (columnName.contains("phone") && !newValue.isEmpty() && !newValue.matches("\\d+")) {
                System.out.println(RED + "Error: Phone number must contain only digits!" + RESET);
                waitForEnter();
                continue;
            }

            if (columnName.equals("email") && !newValue.contains("@")) {
                System.out.println(RED + "Error: Invalid email format! (Must contain '@')" + RESET);
                waitForEnter();
                continue;
            }
            
            if (columnName.equals("birth_date") && !newValue.isEmpty()) {
                if (!newValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    System.out.println(RED + "Error: Invalid date format! Use YYYY-MM-DD." + RESET);
                    waitForEnter();
                    continue;
                }
            }

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
                        
                        System.out.println("Updated Row:");
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
                System.out.println("1. Update another contact");
                System.out.println("2. Undo this update immediately");
                System.out.println("3. Return to Main Menu");
                System.out.print("Select (1-3): ");
                
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