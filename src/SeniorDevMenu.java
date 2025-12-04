import java.sql.*;
import java.util.Stack;

public class SeniorDevMenu extends JuniorDevMenu {

    private final Stack<SeniorUndoAction> seniorUndoStack;

    public SeniorDevMenu(String username, String fullName, String role) {
        super(username, fullName, role);
        seniorUndoStack = new Stack<>();
    }

    @Override
    public void showMenu() {
        while (true) {
            System.out.println();
            System.out.println("=== SENIOR DEVELOPER MENU ===");
            System.out.println("User : " + fullName);
            System.out.println("Role : " + role);
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
            catch (Exception e) { System.out.println("Invalid input."); continue; }

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
                        System.out.println("Logging out...");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Unexpected Error: " + e.getMessage());
            }
        }
    }

    // ============================= UPDATE ===============================

    protected void handleUpdateContact() {
        System.out.println("\n--- UPDATE CONTACT ---");

        handleListContacts();

        System.out.print("Enter ID of contact to update (or 'q' to cancel): ");
        String idInput = scanner.nextLine().trim();
        if (idInput.equalsIgnoreCase("q")) return;

        int contactId;
        try { contactId = Integer.parseInt(idInput); }
        catch (Exception e) { System.out.println("Invalid ID."); return; }

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
        String columnName = null;
        String prompt = "Enter new value: ";

        switch (fieldChoice) {
            case "1": columnName = "first_name"; break;
            case "2": columnName = "middle_name"; break;
            case "3": columnName = "last_name"; break;
            case "4": columnName = "nickname"; break;
            case "5": columnName = "phone_primary"; prompt = "Enter new phone (digits only): "; break;
            case "6": columnName = "phone_secondary"; prompt = "Enter new phone (digits only): "; break;
            case "7": columnName = "email"; prompt = "Enter new email: "; break;
            case "8": columnName = "linkedin_url"; break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        System.out.print(prompt);
        String newValue = scanner.nextLine().trim();

        if ((columnName.equals("first_name") || columnName.equals("last_name")
                || columnName.equals("phone_primary") || columnName.equals("email"))
                && newValue.isEmpty()) {
            System.out.println("Error: This field cannot be empty!");
            return;
        }

        if (columnName.contains("phone") && !newValue.matches("\\d+")) {
            System.out.println("Phone must contain only digits.");
            return;
        }

        if (columnName.equals("email") && !newValue.contains("@")) {
            System.out.println("Invalid email format.");
            return;
        }

        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            // GET SNAPSHOT BEFORE UPDATE
            ContactSnapshot backup = getContactSnapshot(con, contactId);
            if (backup == null) {
                System.out.println("Contact ID not found.");
                return;
            }

            // PERFORM UPDATE
            String sql = "UPDATE contacts SET " + columnName + "=? WHERE contact_id=?";
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, newValue);
            pstmt.setInt(2, contactId);

            if (pstmt.executeUpdate() > 0) {
                System.out.println("Contact updated successfully!");

                seniorUndoStack.push(new SeniorUndoAction("UPDATE", backup));
            }

        } catch (Exception e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    // ============================= ADD ===============================

    private void handleAddContact() {
        System.out.println("\n--- ADD NEW CONTACT ---");

        System.out.print("First name: ");
        String first = scanner.nextLine().trim();
        if (first.isEmpty()) { System.out.println("First name cannot be empty."); return; }

        System.out.print("Middle name (opt): ");
        String middle = scanner.nextLine().trim();

        System.out.print("Last name: ");
        String last = scanner.nextLine().trim();
        if (last.isEmpty()) { System.out.println("Last name cannot be empty."); return; }

        System.out.print("Nickname: ");
        String nick = scanner.nextLine().trim();

        System.out.print("Primary phone: ");
        String phone1 = scanner.nextLine().trim();
        if (!phone1.matches("\\d+")) { System.out.println("Digits only."); return; }

        System.out.print("Secondary phone (opt): ");
        String phone2 = scanner.nextLine().trim();
        if (!phone2.isEmpty() && !phone2.matches("\\d+")) {
            System.out.println("Digits only."); return;
        }

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        if (!email.contains("@")) { System.out.println("Invalid email."); return; }

        System.out.print("LinkedIn URL (opt): ");
        String linkedin = scanner.nextLine().trim();

        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            String sql = """
                INSERT INTO contacts 
                (first_name,middle_name,last_name,nickname,phone_primary,phone_secondary,email,linkedin_url)
                VALUES (?,?,?,?,?,?,?,?)
                """;

            PreparedStatement pstmt =
                    con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, first);
            pstmt.setString(2, middle);
            pstmt.setString(3, last);
            pstmt.setString(4, nick);
            pstmt.setString(5, phone1);
            pstmt.setString(6, phone2);
            pstmt.setString(7, email);
            pstmt.setString(8, linkedin);

            if (pstmt.executeUpdate() > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                rs.next();
                int newId = rs.getInt(1);

                System.out.println("Contact added successfully (ID=" + newId + ")");

                seniorUndoStack.push(new SeniorUndoAction("ADD",
                        new ContactSnapshot(newId)));
            }

        } catch (Exception e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    private void handleAddMultipleContacts() {
        System.out.println("\n--- ADD MULTIPLE CONTACTS ---");
        System.out.print("How many contacts? ");

        int n;
        try { n = Integer.parseInt(scanner.nextLine().trim()); }
        catch (Exception e) { System.out.println("Invalid number."); return; }

        for (int i = 1; i <= n; i++) {
            System.out.println("\nContact #" + i);
            handleAddContact();
        }
    }

    // ============================= DELETE ===============================

    private void handleDeleteContact() {
        System.out.println("\n--- DELETE CONTACT ---");
        handleListContacts();

        System.out.print("Enter ID to delete: ");
        int id;
        try { id = Integer.parseInt(scanner.nextLine().trim()); }
        catch (Exception e) { System.out.println("Invalid ID."); return; }

        deleteSingle(id);
    }

    private void handleDeleteMultipleContacts() {
        System.out.println("\n--- DELETE MULTIPLE CONTACTS ---");
        System.out.print("Enter IDs comma-separated: ");

        String[] arr = scanner.nextLine().trim().split(",");
        for (String s : arr) {
            try { deleteSingle(Integer.parseInt(s.trim())); }
            catch (Exception e) { System.out.println("Invalid: " + s); }
        }
    }

    private void deleteSingle(int id) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            ContactSnapshot snap = getContactSnapshot(con, id);
            if (snap == null) {
                System.out.println("Contact ID not found: " + id);
                return;
            }

            PreparedStatement del =
                    con.prepareStatement("DELETE FROM contacts WHERE contact_id=?");
            del.setInt(1, id);

            if (del.executeUpdate() > 0) {
                System.out.println("Deleted ID: " + id);
                seniorUndoStack.push(new SeniorUndoAction("DELETE", snap));
            }

        } catch (Exception e) {
            System.out.println("Delete error: " + id);
        }
    }

    // ============================= UNDO ===============================

    private void handleUndoSenior() {
        if (seniorUndoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }

        SeniorUndoAction ua = seniorUndoStack.pop();

        switch (ua.type) {
            case "ADD": undoAdd(ua.snap); break;
            case "DELETE": undoDelete(ua.snap); break;
            case "UPDATE": undoUpdate(ua.snap); break;
            default: System.out.println("Unknown undo type.");
        }
    }

    private void undoAdd(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM contacts WHERE contact_id=?");
            ps.setInt(1, snap.contact_id);
            ps.executeUpdate();
            System.out.println("Undo ADD ok (deleted ID: " + snap.contact_id + ")");
        } catch (Exception e) {
            System.out.println("Undo ADD failed.");
        }
    }

    private void undoDelete(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO contacts 
                (contact_id,first_name,middle_name,last_name,nickname,phone_primary,phone_secondary,email,linkedin_url)
                VALUES (?,?,?,?,?,?,?,?,?)
            """);

            ps.setInt(1, snap.contact_id);
            ps.setString(2, snap.first_name);
            ps.setString(3, snap.middle_name);
            ps.setString(4, snap.last_name);
            ps.setString(5, snap.nickname);
            ps.setString(6, snap.phone_primary);
            ps.setString(7, snap.phone_secondary);
            ps.setString(8, snap.email);
            ps.setString(9, snap.linkedin_url);

            ps.executeUpdate();
            System.out.println("Undo DELETE ok (restored ID: " + snap.contact_id + ")");

        } catch (Exception e) {
            System.out.println("Undo DELETE failed.");
        }
    }

    private void undoUpdate(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        try (Connection con = db.connect()) {

            PreparedStatement ps = con.prepareStatement("""
                UPDATE contacts SET 
                first_name=?, middle_name=?, last_name=?, nickname=?,
                phone_primary=?, phone_secondary=?, email=?, linkedin_url=?
                WHERE contact_id=?
            """);

            ps.setString(1, snap.first_name);
            ps.setString(2, snap.middle_name);
            ps.setString(3, snap.last_name);
            ps.setString(4, snap.nickname);
            ps.setString(5, snap.phone_primary);
            ps.setString(6, snap.phone_secondary);
            ps.setString(7, snap.email);
            ps.setString(8, snap.linkedin_url);
            ps.setInt(9, snap.contact_id);

            ps.executeUpdate();
            System.out.println("Undo UPDATE ok (restored ID: " + snap.contact_id + ")");

        } catch (Exception e) {
            System.out.println("Undo UPDATE failed.");
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
                rs.getString("linkedin_url")
        );
    }

    // ============================= DATA CLASSES ===============================

    private static class ContactSnapshot {
        int contact_id;
        String first_name;
        String middle_name;
        String last_name;
        String nickname;
        String phone_primary;
        String phone_secondary;
        String email;
        String linkedin_url;

        ContactSnapshot(int id) { this.contact_id = id; }

        ContactSnapshot(int id, String f, String m, String l, String n,
                        String p1, String p2, String e, String li) {
            contact_id = id;
            first_name = f;
            middle_name = m;
            last_name = l;
            nickname = n;
            phone_primary = p1;
            phone_secondary = p2;
            email = e;
            linkedin_url = li;
        }
    }

    private static class SeniorUndoAction {
        String type; // ADD, DELETE, UPDATE
        ContactSnapshot snap;

        SeniorUndoAction(String type, ContactSnapshot snap) {
            this.type = type;
            this.snap = snap;
        }
    }
}
