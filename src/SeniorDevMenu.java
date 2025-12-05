import java.sql.*;
import java.util.Scanner;
import java.util.Stack;

public class SeniorDevMenu extends JuniorDevMenu {

    private final Stack<SeniorUndoAction> seniorUndoStack;

    public SeniorDevMenu(String username, String fullName, String role, Scanner scanner, String passwordStrength) {
        super(username, fullName, role, scanner, passwordStrength);
        this.seniorUndoStack = new Stack<>();
    }

    @Override
    public void showMenu() {
        while (true) {
            clearScreen();
            String realFullName = loadRealFullName();
            System.out.println(CYAN + "=== SENIOR DEVELOPER MENU ===" + RESET);
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
            System.out.println("6. Add new contact");
            System.out.println("7. Add multiple contacts");
            System.out.println("8. Delete contact");
            System.out.println("9. Delete multiple contacts");
            System.out.println("10. Undo last action");
            System.out.println("11. Logout");
            System.out.print("Select (1-11): ");

            String input = scanner.nextLine().trim();
            int choice;

            try { choice = Integer.parseInt(input); }
            catch (Exception e) { 
                System.out.println(RED + "Invalid input." + RESET); 
                waitForEnter(); continue; 
            }

            try {
                switch (choice) {
                    case 1: handleChangePassword(); break;
                    case 2: handleListContacts(); break;
                    case 3: handleSearchContacts(); break;
                    case 4: handleSortContacts(); break;
                    case 5: handleUpdateContact(); break;
                    case 6: handleAddContact(); break;
                    case 7: handleAddMultipleContacts(); break;
                    case 8: handleDeleteContact(); break;
                    case 9: handleDeleteMultipleContacts(); break;
                    case 10: handleUndoSenior(); break;
                    case 11:
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

    // ============================= ADD ===============================

    private void handleAddContact() {
        clearScreen();
        System.out.println(CYAN + "=== ADD NEW CONTACT ===" + RESET);
        System.out.println(YELLOW + "Type 'q' at any prompt to cancel." + RESET);

        String first = promptWithCancel("First name: "); if(first==null) return;
        if (first.isEmpty()) { System.out.println(RED + "Required!" + RESET); waitForEnter(); return; }

        String middle = promptWithCancel("Middle name (opt): "); if(middle==null) return;

        String last = promptWithCancel("Last name: "); if(last==null) return;
        if (last.isEmpty()) { System.out.println(RED + "Required!" + RESET); waitForEnter(); return; }

        String nick = promptWithCancel("Nickname: "); if(nick==null) return;

        String phone1 = promptWithCancel("Primary phone (digits): "); if(phone1==null) return;
        if (!phone1.matches("\\d+")) { System.out.println(RED + "Digits only!" + RESET); waitForEnter(); return; }

        String phone2 = promptWithCancel("Secondary phone (opt): "); if(phone2==null) return;
        
        String email = promptWithCancel("Email: "); if(email==null) return;
        if (!email.contains("@")) { System.out.println(RED + "Invalid email!" + RESET); waitForEnter(); return; }

        String linkedin = promptWithCancel("LinkedIn URL (opt): "); if(linkedin==null) return;

        String bday = promptWithCancel("Birth Date (YYYY-MM-DD) (opt): "); if(bday==null) return;
        if (!bday.isEmpty() && !bday.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println(RED + "Invalid date format!" + RESET); waitForEnter(); return;
        }

        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            String sql = "INSERT INTO contacts (first_name,middle_name,last_name,nickname,phone_primary,phone_secondary,email,linkedin_url,birth_date) VALUES (?,?,?,?,?,?,?,?,?)";

            PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, first);
            pstmt.setString(2, middle);
            pstmt.setString(3, last);
            pstmt.setString(4, nick);
            pstmt.setString(5, phone1);
            pstmt.setString(6, phone2);
            pstmt.setString(7, email);
            pstmt.setString(8, linkedin);
            
            if (bday.isEmpty()) pstmt.setNull(9, java.sql.Types.DATE);
            else pstmt.setString(9, bday);

            if (pstmt.executeUpdate() > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                rs.next();
                int newId = rs.getInt(1);

                System.out.println(GREEN + "Contact added successfully (ID=" + newId + ")" + RESET);
                seniorUndoStack.push(new SeniorUndoAction("ADD", new ContactSnapshot(newId)));
            }

        } catch (Exception e) {
            System.out.println(RED + "SQL Error: " + e.getMessage() + RESET);
        }
        waitForEnter();
    }

    private String promptWithCancel(String msg) {
        System.out.print(msg);
        String in = scanner.nextLine().trim();
        if (in.equalsIgnoreCase("q")) return null;
        return in;
    }

    private void handleAddMultipleContacts() {
        clearScreen();
        System.out.println(CYAN + "=== ADD MULTIPLE CONTACTS ===" + RESET);
        System.out.print("How many contacts? (Max 10): ");

        int n;
        try { n = Integer.parseInt(scanner.nextLine().trim()); }
        catch (Exception e) { System.out.println(RED + "Invalid number." + RESET); waitForEnter(); return; }

        if (n > 10) { 
            System.out.println(RED + "Limit is 10 at a time." + RESET); 
            waitForEnter(); return; 
        }

        for (int i = 1; i <= n; i++) {
            System.out.println(YELLOW + "\n--- Contact #" + i + " ---" + RESET);
            handleAddContact(); 
        }
    }

    // ============================= DELETE ===============================

    private void handleDeleteContact() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== DELETE CONTACT ===" + RESET);
            
            handleListContactsForUpdate(); 

            System.out.println(YELLOW + "Enter 'q' to return to Main Menu." + RESET);
            System.out.print("Enter ID to delete: ");
            String in = scanner.nextLine().trim();
            
            if (in.equalsIgnoreCase("q")) return;

            int id;
            try { 
                id = Integer.parseInt(in); 
            } catch (Exception e) { 
                System.out.println(RED + "Invalid ID." + RESET); 
                waitForEnter();
                continue; 
            }

            boolean deleteSuccess = deleteSingle(id);

            if (deleteSuccess) {
                System.out.println();
                System.out.println(CYAN + "What would you like to do next?" + RESET);
                System.out.println("1. Delete another contact");
                System.out.println("2. Undo this deletion immediately");
                System.out.println("3. Return to Main Menu");
                System.out.print("Select (1-3): ");
                
                String nextAction = scanner.nextLine().trim();
                
                if (nextAction.equals("1")) {
                    continue; 
                } else if (nextAction.equals("2")) {
                    handleUndoSenior(); 
                    continue; 
                } else {
                    return; 
                }
            } else {
                waitForEnter();
            }
        }
    }

    private void handleDeleteMultipleContacts() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== DELETE MULTIPLE CONTACTS ===" + RESET);
            
            handleListContactsForUpdate(); 
            
            System.out.println(YELLOW + "Enter 'q' to return to Main Menu." + RESET);
            System.out.print("Enter IDs comma-separated (e.g. 10,12,15): ");
            String line = scanner.nextLine().trim();
            
            if (line.equalsIgnoreCase("q")) return;
            if (line.isEmpty()) {
                System.out.println(RED + "Input cannot be empty." + RESET);
                waitForEnter();
                continue;
            }

            String[] arr = line.split(",");
            int successCount = 0;

            for (String s : arr) {
                try { 
                    int id = Integer.parseInt(s.trim());
                    boolean deleted = deleteSingle(id);
                    if (deleted) {
                        successCount++;
                    }
                } catch (Exception e) { 
                    System.out.println(RED + "Invalid ID skipped: " + s + RESET); 
                }
            }

            if (successCount > 0) {
                System.out.println();
                System.out.println(GREEN + "Successfully deleted " + successCount + " contacts." + RESET);
                System.out.println(CYAN + "What would you like to do next?" + RESET);
                System.out.println("1. Delete more contacts");
                System.out.println("2. Undo last deletion (Restores the LAST deleted contact)");
                System.out.println("3. Return to Main Menu");
                System.out.print("Select (1-3): ");
                
                String nextAction = scanner.nextLine().trim();
                
                if (nextAction.equals("1")) {
                    continue; 
                } else if (nextAction.equals("2")) {
                    handleUndoSenior(); 
                    waitForEnter(); 
                    continue;
                } else {
                    return; 
                }
            } else {
                System.out.println(RED + "No contacts were deleted." + RESET);
                waitForEnter();
            }
        }
    }

    private boolean deleteSingle(int id) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            ContactSnapshot snap = getContactSnapshot(con, id);
            if (snap == null) {
                System.out.println(RED + "Contact ID not found: " + id + RESET);
                return false;
            }

            PreparedStatement del = con.prepareStatement("DELETE FROM contacts WHERE contact_id=?");
            del.setInt(1, id);

            if (del.executeUpdate() > 0) {
                System.out.println(GREEN + "Deleted ID: " + id + RESET);
                seniorUndoStack.push(new SeniorUndoAction("DELETE", snap));
                return true; 
            }

        } catch (Exception e) {
            System.out.println(RED + "Delete error: " + id + RESET);
        }
        return false;
    }

    // ============================= UNDO ===============================

    private void handleUndoSenior() {
        clearScreen();
        System.out.println(CYAN + "=== UNDO LAST ACTION (SENIOR) ===" + RESET);

        if (!seniorUndoStack.isEmpty()) {
            SeniorUndoAction ua = seniorUndoStack.pop();
            switch (ua.type) {
                case "ADD": undoAdd(ua.snap); break;
                case "DELETE": undoDelete(ua.snap); break;
                case "UPDATE": undoUpdateSenior(ua.snap); break; 
            }
            waitForEnter();
            return;
        }

        if (!undoStack.isEmpty()) {
            super.handleUndo();
            return;
        }

        System.out.println(YELLOW + "Nothing to undo." + RESET);
        waitForEnter();
    }

    private void undoAdd(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM contacts WHERE contact_id=?");
            ps.setInt(1, snap.contact_id);
            ps.executeUpdate();
            System.out.println(GREEN + "Undo ADD successful. (Deleted ID: " + snap.contact_id + ")" + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Undo ADD failed." + RESET);
        }
    }

    private void undoDelete(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {
            String sql = "INSERT INTO contacts (contact_id,first_name,middle_name,last_name,nickname,phone_primary,phone_secondary,email,linkedin_url,birth_date) VALUES (?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, snap.contact_id);
            ps.setString(2, snap.first_name);
            ps.setString(3, snap.middle_name);
            ps.setString(4, snap.last_name);
            ps.setString(5, snap.nickname);
            ps.setString(6, snap.phone_primary);
            ps.setString(7, snap.phone_secondary);
            ps.setString(8, snap.email);
            ps.setString(9, snap.linkedin_url);
            ps.setString(10, snap.birth_date); 

            ps.executeUpdate();
            System.out.println(GREEN + "Undo DELETE successful. (Restored ID: " + snap.contact_id + ")" + RESET);

        } catch (Exception e) {
            System.out.println(RED + "Undo DELETE failed: " + e.getMessage() + RESET);
        }
    }

    private void undoUpdateSenior(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {
            String sql = "UPDATE contacts SET first_name=?, middle_name=?, last_name=?, nickname=?, phone_primary=?, phone_secondary=?, email=?, linkedin_url=?, birth_date=? WHERE contact_id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, snap.first_name);
            ps.setString(2, snap.middle_name);
            ps.setString(3, snap.last_name);
            ps.setString(4, snap.nickname);
            ps.setString(5, snap.phone_primary);
            ps.setString(6, snap.phone_secondary);
            ps.setString(7, snap.email);
            ps.setString(8, snap.linkedin_url);
            ps.setString(9, snap.birth_date);
            ps.setInt(10, snap.contact_id);

            ps.executeUpdate();
            System.out.println(GREEN + "Undo UPDATE successful." + RESET);

        } catch (Exception e) {
            System.out.println(RED + "Undo UPDATE failed." + RESET);
        }
    }

    // ============================= HELPERS ===============================

    private ContactSnapshot getContactSnapshot(Connection con, int id) throws Exception {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM contacts WHERE contact_id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;

        return new ContactSnapshot(
                rs.getInt("contact_id"),
                rs.getString("first_name"),
                rs.getString("middle_name"),
                rs.getString("last_name"),
                rs.getString("nickname"),
                rs.getString("phone_primary"),
                rs.getString("phone_secondary"),
                rs.getString("email"),
                rs.getString("linkedin_url"),
                rs.getString("birth_date")
        );
    }

    // ============================= SNAPSHOT CLASS ===============================

    private static class ContactSnapshot {
        int contact_id;
        String first_name, middle_name, last_name, nickname;
        String phone_primary, phone_secondary, email, linkedin_url, birth_date;

        ContactSnapshot(int id) { this.contact_id = id; }

        ContactSnapshot(int id, String f, String m, String l, String n,
                        String p1, String p2, String e, String li, String bd) {
            contact_id = id;
            first_name = f; middle_name = m; last_name = l; nickname = n;
            phone_primary = p1; phone_secondary = p2; email = e; linkedin_url = li;
            birth_date = bd; 
        }
    }

    private static class SeniorUndoAction {
        String type; 
        ContactSnapshot snap;
        SeniorUndoAction(String type, ContactSnapshot snap) {
            this.type = type; this.snap = snap;
        }
    }
}