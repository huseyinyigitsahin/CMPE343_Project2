import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;

public class JuniorDevMenu extends TesterMenu {

    // Geri alma (Undo) işlemi için yapılan değişiklikleri tutan yığın
    private Stack<UndoAction> undoStack;

    public JuniorDevMenu(String username, String fullName, String role) {
        super(username, fullName, role);
        this.undoStack = new Stack<>();
    }

    @Override
    public void showMenu() {
        while (true) {
            System.out.println();
            System.out.println("=== JUNIOR DEVELOPER MENU ===");
            System.out.println("User : " + fullName);
            System.out.println("Role : " + role);
            System.out.println("1. Change password");
            System.out.println("2. List all contacts");
            System.out.println("3. Search contacts");
            System.out.println("4. Sort contacts");
            System.out.println("5. Update existing contact"); // Junior Özelliği
            System.out.println("6. Undo last update");        // Ekstra Özellik
            System.out.println("7. Logout");
            System.out.print("Select an option (1-7): ");

            String input = scanner.nextLine().trim();
            int choice;

            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            // switch bloğu içinde hata olursa program kapanmasın diye try-catch
            try {
                switch (choice) {
                    case 1: handleChangePassword(); break; // Parent'tan gelir
                    case 2: handleListContacts(); break;   // Parent'tan gelir
                    case 3: handleSearchContacts(); break; // Parent'tan gelir
                    case 4: handleSortContacts(); break;   // Parent'tan gelir
                    case 5: handleUpdateContact(); break;  // Yeni metod
                    case 6: handleUndo(); break;           // Yeni metod
                    case 7:
                        System.out.println("Logging out...");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private void handleUpdateContact() {
        System.out.println("\n--- UPDATE CONTACT ---");
        
        // Kullanıcı kime işlem yapacağını görsün
        handleListContacts(); 

        System.out.print("Enter ID of contact to update (or 'q' to cancel): ");
        String idInput = scanner.nextLine().trim();
        if (idInput.equalsIgnoreCase("q")) return;

        int contactId;
        try {
            contactId = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
            return;
        }

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

        // Seçime göre veritabanı sütun ismini belirle
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
                System.out.println("Invalid selection.");
                return;
        }

        System.out.print(prompt);
        String newValue = scanner.nextLine().trim();

        // --- VALIDATION (Doğrulama) ---
        // 1. Zorunlu alanlar boş olamaz
        if ((columnName.equals("first_name") || columnName.equals("last_name") || columnName.equals("phone_primary") || columnName.equals("email")) && newValue.isEmpty()) {
            System.out.println("Error: This field cannot be empty!");
            return;
        }
        // 2. Telefon sadece rakam içermeli
        if (columnName.contains("phone") && !newValue.isEmpty() && !newValue.matches("\\d+")) {
            System.out.println("Error: Phone number must contain only digits!");
            return;
        }
        // 3. Email @ içermeli
        if (columnName.equals("email") && !newValue.contains("@")) {
            System.out.println("Error: Invalid email format!");
            return;
        }

        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        if (con == null) {
            System.out.println("Connection failed. Cannot update.");
            return;
        }

        try {
            // ÖNCE ESKİ VERİYİ ÇEK (Undo yapabilmek için)
            String selectSql = "SELECT " + columnName + " FROM contacts WHERE contact_id = ?";
            String oldValue = "";
            boolean idExists = false;

            try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
                selectStmt.setInt(1, contactId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    oldValue = rs.getString(columnName);
                    if (oldValue == null) oldValue = ""; // Null gelirse boş string yap
                    idExists = true;
                }
            }

            if (!idExists) {
                System.out.println("Contact ID not found.");
                con.close();
                return;
            }

            // GÜNCELLEME İŞLEMİ
            String updateSql = "UPDATE contacts SET " + columnName + " = ? WHERE contact_id = ?";
            try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                updateStmt.setString(1, newValue);
                updateStmt.setInt(2, contactId);
                
                int rows = updateStmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Contact updated successfully!");
                    
                    // Undo yığınına ekle
                    undoStack.push(new UndoAction(contactId, columnName, oldValue));
                }
            }

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }

    // Geri Alma (Undo) Metodu
    private void handleUndo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }

        UndoAction lastAction = undoStack.pop();
        
        dB_Connection db = new dB_Connection();
        Connection con = db.connect();

        if (con == null) {
            System.out.println("Connection failed. Cannot undo.");
            // Bağlantı yoksa işlemi stack'e geri koy, kaybolmasın
            undoStack.push(lastAction); 
            return;
        }

        String sql = "UPDATE contacts SET " + lastAction.columnName + " = ? WHERE contact_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, lastAction.oldValue);
            pstmt.setInt(2, lastAction.contactId);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Undo successful! Reverted " + lastAction.columnName + " to previous value.");
            } else {
                System.out.println("Could not undo. Contact might have been deleted.");
            }
        } catch (SQLException e) {
            System.out.println("Undo Error: " + e.getMessage());
        } finally {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }

    // Undo işlemi için yardımcı basit sınıf (Inner Class)
    private class UndoAction {
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