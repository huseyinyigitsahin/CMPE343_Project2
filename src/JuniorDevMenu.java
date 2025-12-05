import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Stack;

public class JuniorDevMenu extends TesterMenu {

    // Stack to store history for Undo operations
    protected Stack<UndoAction> undoStack;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public JuniorDevMenu(String username, String fullName, String role, Scanner scanner, String passwordStrength) {
        super(username, fullName, role, scanner, passwordStrength);
        this.undoStack = new Stack<>();
    }

    public JuniorDevMenu(String username, String fullName, String role) {
        super(username, fullName, role, new Scanner(System.in), null);
        this.undoStack = new Stack<>();
    }

    // =========================================================================
    // MENU LOGIC
    // =========================================================================

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
            System.out.println("5. Update existing contact"); // Junior feature
            System.out.println("6. Undo last update");        // Junior feature
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
                    case 1: handleChangePassword(); break; // Inherited
                    case 2: handleListContacts(); break;   // Inherited
                    case 3: handleSearchContacts(); break; // Inherited
                    case 4: handleSortContacts(); break;   // Inherited
                    case 5: handleUpdateContact(); break;  // Defined below
                    case 6: handleUndo(); break;           // Defined below
                    case 7:
                        System.out.println(YELLOW + "Logging out..." + RESET);
                        return;
                    default:
                        System.out.println(RED + "Invalid option. Please select 1-7." + RESET);
                        waitForEnter();
                }
            } catch (Exception e) {
                System.out.println(RED + "An unexpected error occurred: " + e.getMessage() + RESET);
                waitForEnter();
            }
        }
    }

    // =========================================================================
    // UPDATE CONTACT LOGIC
    // =========================================================================

    protected void handleUpdateContact() {
        clearScreen();
        System.out.println(CYAN + "=== UPDATE CONTACT ===" + RESET);
        System.out.println("Tip: Use 'List all contacts' (Option 2) to find IDs.");
        System.out.println();

        System.out.print("Enter ID of contact to update (or 'q' to cancel): ");
        String idInput = scanner.nextLine().trim();
        
        if (idInput.equalsIgnoreCase("q")) return;

        int contactId;
        try {
            contactId = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid ID format." + RESET);
            waitForEnter();
            return;
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
        System.out.print("Select (1-8): ");
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
            default:
                System.out.println(RED + "Invalid selection." + RESET);
                waitForEnter();
                return;
        }

        System.out.print(prompt);
        String newValue = scanner.nextLine().trim();

        // --- VALIDATION ---
        
        // 1. Mandatory Fields Check
        if ((columnName.equals("first_name") || columnName.equals("last_name") || 
             columnName.equals("phone_primary") || columnName.equals("email")) && newValue.isEmpty()) {
            System.out.println(RED + "Error: This field cannot be empty!" + RESET);
            waitForEnter();
            return;
        }

        // 2. Numeric Check for Phones
        if (columnName.contains("phone") && !newValue.isEmpty() && !newValue.matches("\\d+")) {
            System.out.println(RED + "Error: Phone number must contain only digits!" + RESET);
            waitForEnter();
            return;
        }

        // 3. Email Format Check
        if (columnName.equals("email") && !newValue.contains("@")) {
            System.out.println(RED + "Error: Invalid email format! (Must contain '@')" + RESET);
            waitForEnter();
            return;
        }

        // --- DATABASE OPERATION ---

        Connection con = getConnection();
        if (con == null) {
            System.out.println(RED + "Connection failed. Cannot update." + RESET);
            waitForEnter();
            return;
        }

        try {
            // STEP A: Fetch the OLD value (for Undo)
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
                return;
            }

            // STEP B: Update to NEW value
            String updateSql = "UPDATE contacts SET " + columnName + " = ? WHERE contact_id = ?";
            try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                updateStmt.setString(1, newValue);
                updateStmt.setInt(2, contactId);

                int rows = updateStmt.executeUpdate();
                if (rows > 0) {
                    System.out.println(GREEN + "Contact updated successfully!" + RESET);
                    
                    // Push to Undo Stack
                    undoStack.push(new UndoAction(contactId, columnName, oldValue));
                } else {
                    System.out.println(RED + "Update failed." + RESET);
                }
            }

        } catch (SQLException e) {
            System.out.println(RED + "SQL Error: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
        
        waitForEnter();
    }

    // =========================================================================
    // UNDO LOGIC
    // =========================================================================

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
            System.out.println(RED + "Connection failed. Cannot undo." + RESET);
            undoStack.push(lastAction); // Put it back since we failed
            waitForEnter();
            return;
        }

        String sql = "UPDATE contacts SET " + lastAction.columnName + " = ? WHERE contact_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, lastAction.oldValue);
            pstmt.setInt(2, lastAction.contactId);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "Undo successful!" + RESET);
                System.out.println("Reverted field '" + lastAction.columnName + "' for ID " + lastAction.contactId);
                System.out.println("Value restored to: " + (lastAction.oldValue.isEmpty() ? "[EMPTY]" : lastAction.oldValue));
            } else {
                System.out.println(RED + "Could not undo. The contact might have been deleted." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Undo Error: " + e.getMessage() + RESET);
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }

        waitForEnter();
    }

    // =========================================================================
    // HELPER CLASS
    // =========================================================================

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