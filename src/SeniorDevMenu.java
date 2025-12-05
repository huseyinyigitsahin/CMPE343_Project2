import java.sql.*;
import java.util.Scanner;
import java.util.Stack;

public class SeniorDevMenu extends JuniorDevMenu {

    private final Stack<SeniorUndoAction> seniorUndoStack;

    // Maksimum alan uzunluğu (tüm text inputlar için)
    private static final int MAX_FIELD_LEN = 100;

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
            System.out.println(GREEN + "User: " + RESET + realFullName + " (" + username + ")");
            System.out.println(GREEN + "Role: " + RESET + role);
            System.out.println();

            if (passwordStrengthAtLogin != null && !passwordStrengthAtLogin.isBlank()) {
                printPasswordStrengthBanner();
                System.out.println();
            }

            System.out.println(CYAN + "Please select an option:" + RESET);
            System.out.println(GREEN + "1)"  + RESET + " Change password");
            System.out.println(GREEN + "2)"  + RESET + " List all contacts");
            System.out.println(GREEN + "3)"  + RESET + " Search contacts");
            System.out.println(GREEN + "4)"  + RESET + " Sort contacts");
            System.out.println(GREEN + "5)"  + RESET + " Update existing contact");
            System.out.println(GREEN + "6)"  + RESET + " Add new contact");
            System.out.println(GREEN + "7)"  + RESET + " Add multiple contacts");
            System.out.println(GREEN + "8)"  + RESET + " Delete contact");
            System.out.println(GREEN + "9)"  + RESET + " Delete multiple contacts");
            System.out.println(GREEN + "10)" + RESET + " Undo last action");
            System.out.println(GREEN + "11)" + RESET + " Logout");
            System.out.print(YELLOW + "Select (1-11): " + RESET);

            String input = readTrimmed();
            int choice;

            try {
                choice = Integer.parseInt(input);
            } catch (Exception e) {
                System.out.println(RED + "Please enter a number between 1 and 11." + RESET);
                waitForEnter();
                continue;
            }

            try {
                switch (choice) {
                    case 1:  handleChangePassword();          break;
                    case 2:  handleListContacts();            break;
                    case 3:  handleSearchContacts();          break;
                    case 4:  handleSortContacts();            break;
                    case 5:  handleUpdateContact();           break; // JuniorDevMenu
                    case 6:  handleAddContact();              break;
                    case 7:  handleAddMultipleContacts();     break;
                    case 8:  handleDeleteContact();           break;
                    case 9:  handleDeleteMultipleContacts();  break;
                    case 10: handleUndoSenior();              break;
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

    // ============================= COMMON HELPERS ===============================

    /** Sadece q/quit/exit iptal olarak algılanır. */
    private boolean isCancelKeyword(String in) {
        String t = trimOrEmpty(in).toLowerCase();
        return t.equals("q") || t.equals("quit") || t.equals("exit");
    }

    /** 'b' veya 'back' bir önceki adıma dönmek için. */
    private boolean isBackCommand(String in) {
        String t = trimOrEmpty(in).toLowerCase();
        return t.equals("b") || t.equals("back");
    }

    /** MAX_FIELD_LEN kontrolü ile okuma; çok uzunsa null döner. */
    private String readLimitedLine() {
        String val = readTrimmed();
        if (val.length() > MAX_FIELD_LEN) {
            return null;
        }
        return val;
    }

    /** Türkçe locale ile ilk harfi büyük, kalanı küçük yap. */
    private String capitalizeNameTr(String text) {
        text = trimOrEmpty(text);
        if (text.isEmpty()) return text;
        java.util.Locale tr = new java.util.Locale("tr", "TR");
        String lower = text.toLowerCase(tr);
        String first = lower.substring(0, 1).toUpperCase(tr);
        if (lower.length() == 1) return first;
        return first + lower.substring(1);
    }

    /** 2 harfli isimler için emin misin sorusu. */
    private boolean confirmShortName(String label, String value) {
        if (value == null) return false;
        if (value.length() > 2) return true;

        while (true) {
            System.out.print(
                YELLOW + label + " is only " + value.length() +
                " characters. Are you sure (y/n, q = cancel): " + RESET
            );
            String ans = readTrimmed().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) {
                return true;
            } else if (ans.equals("n") || ans.equals("no")) {
                return false; // tekrar girmesi için false
            } else if (isCancelKeyword(ans)) {
                System.out.println(YELLOW + "Contact creation cancelled." + RESET);
                return false;
            } else {
                System.out.println(YELLOW + "Please answer with y or n (or q to cancel)." + RESET);
            }
        }
    }

    // ============================= ADD (BACK + CANCEL DESTEKLİ) ===============================

    private void handleAddContact() {
        clearScreen();
        System.out.println(CYAN + "=== ADD NEW CONTACT ===" + RESET);
        System.out.println(YELLOW + "You can type 'q' at any point to cancel and go back to the SENIOR menu." + RESET);
        System.out.println(YELLOW + "You can type 'b' to go back to the previous field." + RESET);
        System.out.println(YELLOW + "All fields are required, except Email." + RESET);
        System.out.println(YELLOW + "Email is optional if this person does not use gmail/outlook/hotmail/yahoo yet." + RESET);
        System.out.println(YELLOW + "Maximum length for any text field is " + MAX_FIELD_LEN + " characters." + RESET);
        System.out.println();

        dB_Connection db = new dB_Connection();
        Connection con = null;

        // Kullanıcının girdiği ham değerler (henüz capitalize edilmemiş)
        String firstRaw  = "";
        String middleRaw = "";
        String lastRaw   = "";
        String nick      = "";
        String phone1    = "";
        String phone2    = "";
        String email     = "";
        String linkedin  = "";
        String bday      = "";

        try {
            con = db.connect();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                waitForEnter();
                return;
            }

            // Tahmini sonraki ID (sadece göster, kullanıcı değiştiremiyor)
            Integer nextId = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT MAX(contact_id) AS max_id FROM contacts");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxId = rs.getInt("max_id");
                    if (!rs.wasNull()) {
                        nextId = maxId + 1;
                    } else {
                        nextId = 1;
                    }
                }
            } catch (Exception ignored) {}

            if (nextId != null) {
                System.out.println(YELLOW + "Next contact ID (auto): " + GREEN + nextId + RESET);
                System.out.println(YELLOW + "This ID is assigned by the system and cannot be changed." + RESET);
                System.out.println();
            }

            // Adım sayacı: 0 = first, 1 = middle, 2 = last, 3 = nick, 4 = phone1,
            // 5 = phone2, 6 = email, 7 = linkedin, 8 = bday
            int step = 0;

            while (true) {
                if (step == 9) {
                    // Tüm alanlar toplandı, preview & kaydet'e geçeceğiz
                    break;
                }

                switch (step) {

                    // ================== STEP 0: FIRST NAME ==================
                    case 0: {
                        System.out.println(CYAN + "First Name format:" + RESET + " Ahmet, Ece, Ali");
                        System.out.println(YELLOW + "Rules:" + RESET + " only letters are allowed (Turkish supported). No spaces, no digits, no symbols.");
                        System.out.print("First name (q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            System.out.println(YELLOW + "You are at the first field, cannot go back." + RESET);
                            break;
                        }

                        if (!isValidName(tmp)) {
                            System.out.println(RED + "Invalid first name format." + RESET);
                            break;
                        }

                        if (!confirmShortName("First name", tmp)) {
                            // tekrar iste
                            break;
                        }

                        firstRaw = tmp;
                        step++;  // sonraki alana geç
                        break;
                    }

                    // ================== STEP 1: MIDDLE NAME (REQUIRED) ==================
                    case 1: {
                        System.out.println();
                        System.out.println(CYAN + "Middle Name (required)" + RESET);
                        System.out.println(YELLOW + "Rules:" + RESET + " same as first name. Cannot be empty." );
                        System.out.print("Middle name (b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // first name'e dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            System.out.println(RED + "Middle name is required." + RESET);
                            break;
                        }

                        middleRaw = tmp;
                        if (!isValidName(middleRaw)) {
                            System.out.println(RED + "Invalid middle name format." + RESET);
                            break;
                        }

                        step++;
                        break;
                    }

                    // ================== STEP 2: LAST NAME (REQUIRED) ==================
                    case 2: {
                        System.out.println();
                        System.out.println(CYAN + "Last Name format:" + RESET + " Yilmaz, Demir");
                        System.out.println(YELLOW + "Rules:" + RESET + " only letters are allowed (Turkish supported). No spaces, no digits, no symbols.");
                        System.out.print("Last name (b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // middle name'e dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            System.out.println(RED + "Last name is required." + RESET);
                            break;
                        }

                        if (!isValidName(tmp)) {
                            System.out.println(RED + "Invalid last name format." + RESET);
                            break;
                        }
                        if (!confirmShortName("Last name", tmp)) {
                            break;
                        }

                        lastRaw = tmp;
                        step++;
                        break;
                    }

                    // ================== STEP 3: NICKNAME (REQUIRED) ==================
                    case 3: {
                        System.out.println();
                        System.out.println(CYAN + "Nickname (required) format:" + RESET + " ali_k, user.123");
                        System.out.println(YELLOW + "Rules:" + RESET + " letters, digits, underscore and dot are allowed. No spaces.");
                        System.out.print("Nickname (b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // last name'e dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            System.out.println(RED + "Nickname is required." + RESET);
                            break;
                        }

                        nick = tmp;
                        if (!isValidNickname(nick)) {
                            System.out.println(RED + "Invalid nickname format." + RESET);
                            break;
                        }

                        step++;
                        break;
                    }

                    // ================== STEP 4: PRIMARY PHONE (REQUIRED) ==================
                    case 4: {
                        System.out.println();
                        System.out.println(CYAN + "Primary Phone format:" + RESET + " 5321112233");
                        System.out.println(YELLOW + "Rules:" + RESET + " must contain exactly 10 digits. Only numbers 0-9 are allowed, no spaces, no symbols.");
                        System.out.print("Primary phone (b = back, q = cancel): ");
                        String raw = readLimitedLine();
                        if (raw == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(raw)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(raw)) {
                            step--;   // nick'e dön
                            break;
                        }

                        if (raw.isEmpty()) {
                            System.out.println(RED + "Primary phone is required." + RESET);
                            break;
                        }

                        if (!raw.matches("\\d+")) {
                            System.out.println(RED + "Phone number must contain digits only." + RESET);
                            break;
                        }
                        if (raw.length() != 10) {
                            System.out.println(RED + "Phone number must be exactly 10 digits." + RESET);
                            break;
                        }

                        phone1 = raw;
                        step++;
                        break;
                    }

                    // ================== STEP 5: SECONDARY PHONE (REQUIRED) ==================
                    case 5: {
                        System.out.println();
                        System.out.println(CYAN + "Secondary Phone (required)" + RESET);
                        System.out.println(YELLOW + "Rules:" + RESET + " must be a 10-digit number like 5321112233 (digits only).");
                        System.out.print("Secondary phone (b = back, q = cancel): ");
                        String raw = readLimitedLine();
                        if (raw == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(raw)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(raw)) {
                            step--;   // primary phone'a dön
                            break;
                        }

                        if (raw.isEmpty()) {
                            System.out.println(RED + "Secondary phone is required." + RESET);
                            break;
                        }

                        if (!raw.matches("\\d+")) {
                            System.out.println(RED + "Phone number must contain digits only." + RESET);
                            break;
                        }
                        if (raw.length() != 10) {
                            System.out.println(RED + "Phone number must be exactly 10 digits." + RESET);
                            break;
                        }

                        phone2 = raw;
                        step++;
                        break;
                    }

                    // ================== STEP 6: EMAIL (OPTIONAL, DOMAIN KURALLI) ==================
                    case 6: {
                        System.out.println();
                        System.out.println(CYAN + "Email (optional)" + RESET);
                        System.out.println("Supported providers: gmail.com, outlook.com, hotmail.com, yahoo.com");
                        System.out.println(YELLOW + "Rules:" + RESET + " if you enter email, it must be valid and from a supported provider. No spaces.");
                        System.out.println(YELLOW + "Forbidden characters:" + RESET + " ! ? % ^ & * ( ) = + { } [ ] | ' \" < > ,");
                        System.out.print("Email (ENTER = no email, b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // secondary phone'a dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            email = "";
                            System.out.println(YELLOW +
                                "You left email empty. If this person starts using a supported email provider later," +
                                " you can add it from the update menu."
                                + RESET);
                            step++;
                            break;
                        }

                        email = tmp;
                        char bad = findForbiddenEmailChar(email);
                        if (bad != 0) {
                            System.out.println(RED + "You cannot use the character '" + bad + "' in email." + RESET);
                            break;
                        }
                        if (!isValidEmailForEquals(email)) {
                            System.out.println(RED + "Invalid email format or unsupported domain." + RESET);
                            System.out.println(YELLOW + "Supported domains: gmail.com, outlook.com, hotmail.com, yahoo.com" + RESET);
                            break;
                        }

                        step++;
                        break;
                    }

                    // ================== STEP 7: LINKEDIN (REQUIRED) ==================
                    case 7: {
                        System.out.println();
                        System.out.println(CYAN + "LinkedIn URL (required)" + RESET);
                        System.out.println(YELLOW + "Example:" + RESET + " https://www.linkedin.com/in/username");
                        System.out.print("LinkedIn URL (b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // email'e dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            System.out.println(RED + "LinkedIn URL is required." + RESET);
                            break;
                        }

                        linkedin = tmp;
                        if (linkedin.length() < 5) {
                            System.out.println(RED + "LinkedIn URL is too short." + RESET);
                            break;
                        }

                        step++;
                        break;
                    }

                    // ================== STEP 8: BIRTH DATE (REQUIRED) ==================
                    case 8: {
                        System.out.println();
                        System.out.println(CYAN + "Birth Date (required)" + RESET);
                        System.out.println(YELLOW + "Format:" + RESET + " YYYY-MM-DD  (example: 1999-11-23)");
                        System.out.print("Birth date (b = back, q = cancel): ");
                        String tmp = readLimitedLine();
                        if (tmp == null) {
                            System.out.println(RED + "Input is too long. Maximum " + MAX_FIELD_LEN + " characters." + RESET);
                            break;
                        }
                        if (isCancelKeyword(tmp)) {
                            System.out.println(YELLOW + "Add contact cancelled." + RESET);
                            waitForEnter();
                            return;
                        }
                        if (isBackCommand(tmp)) {
                            step--;   // linkedin'e dön
                            break;
                        }

                        if (tmp.isEmpty()) {
                            System.out.println(RED + "Birth date is required." + RESET);
                            break;
                        }

                        bday = tmp;
                        if (!isValidExactDate(bday)) {
                            System.out.println(RED + "Invalid date format or out of range." + RESET);
                            break;
                        }

                        step++;
                        break;
                    }

                    default:
                        step = 9;
                        break;
                }
            }

            // ---- İsimleri DB'ye girmeden önce normalize et (ilk harf büyük, geri kalanı küçük) ----
            String first  = capitalizeNameTr(firstRaw);
            String middle = capitalizeNameTr(middleRaw);
            String last   = capitalizeNameTr(lastRaw);

            // ---- Kaydetmeden önce özet + onay ----
            clearScreen();
            System.out.println(CYAN + "=== NEW CONTACT PREVIEW ===" + RESET);
            System.out.println("First Name : " + first);
            System.out.println("Middle Name: " + middle);
            System.out.println("Last Name  : " + last);
            System.out.println("Nickname   : " + nick);
            System.out.println("Phone 1    : " + phone1);
            System.out.println("Phone 2    : " + phone2);
            System.out.println("Email      : " + (email.isEmpty() ? "(none)" : email));
            System.out.println("LinkedIn   : " + linkedin);
            System.out.println("Birth Date : " + bday);
            System.out.println();

            while (true) {
                System.out.print(YELLOW + "Do you want to save this contact (y/n, q = cancel): " + RESET);
                String ans = readTrimmed().toLowerCase();
                if (ans.equals("y") || ans.equals("yes")) {
                    break; // kaydetmeye geç
                } else if (ans.equals("n") || ans.equals("no") || isCancelKeyword(ans)) {
                    System.out.println(YELLOW + "Contact creation cancelled. Nothing was saved." + RESET);
                    waitForEnter();
                    return;
                } else {
                    System.out.println(YELLOW + "Please answer with y or n (or q to cancel)." + RESET);
                }
            }

            // ===== INSERT INTO DB =====
            String sql = "INSERT INTO contacts " +
                    "(first_name, middle_name, last_name, nickname, phone_primary, phone_secondary, email, linkedin_url, birth_date) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)";

            try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, first);
                pstmt.setString(2, middle);
                pstmt.setString(3, last);
                pstmt.setString(4, nick);
                pstmt.setString(5, phone1);
                pstmt.setString(6, phone2);

                if (email.isEmpty()) {
                    pstmt.setNull(7, Types.VARCHAR);
                } else {
                    pstmt.setString(7, email);
                }

                pstmt.setString(8, linkedin);
                pstmt.setString(9, bday);

                int affected = pstmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        int newId = -1;
                        if (rs.next()) {
                            newId = rs.getInt(1);
                        }
                        System.out.println();
                        System.out.println(GREEN + "Contact added successfully (ID = " + newId + ")." + RESET);
                        seniorUndoStack.push(new SeniorUndoAction("ADD", new ContactSnapshot(newId)));
                    }
                } else {
                    System.out.println(RED + "Contact could not be added." + RESET);
                }
            }

        } catch (Exception e) {
            System.out.println(RED + "SQL Error: " + e.getMessage() + RESET);
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }

        waitForEnter();
    }

    private void handleAddMultipleContacts() {
        clearScreen();
        System.out.println(CYAN + "=== ADD MULTIPLE CONTACTS ===" + RESET);
        System.out.println(YELLOW + "Type 'q' to cancel." + RESET);
        System.out.print("How many contacts? (1-10, q to cancel): ");

        String in = readTrimmed();
        if (isCancelKeyword(in)) {
            System.out.println(YELLOW + "Cancelled." + RESET);
            waitForEnter();
            return;
        }

        int n;
        try {
            n = Integer.parseInt(in);
        } catch (Exception e) {
            System.out.println(RED + "Invalid number." + RESET);
            waitForEnter();
            return;
        }

        if (n <= 0) {
            System.out.println(RED + "Number must be at least 1." + RESET);
            waitForEnter();
            return;
        }

        if (n > 10) {
            System.out.println(RED + "Limit is 10 at a time." + RESET);
            waitForEnter();
            return;
        }

        for (int i = 1; i <= n; i++) {
            clearScreen();
            System.out.println(CYAN + "=== ADD MULTIPLE CONTACTS ===" + RESET);
            System.out.println(YELLOW + "Contact #" + i + " of " + n + RESET);
            System.out.println();
            handleAddContact(); // kendi içinde q/b ile yönetiliyor
        }
    }

    // ============================= DELETE ===============================

    private void handleDeleteContact() {
        while (true) {
            clearScreen();
            System.out.println(CYAN + "=== DELETE CONTACT ===" + RESET);

            handleListContactsForUpdate(); // JuniorDevMenu

            System.out.println();
            System.out.println(YELLOW + "Type 'q' to return to SENIOR menu." + RESET);
            System.out.print("Enter ID to delete (q to cancel): ");
            String in = readTrimmed();

            if (isCancelKeyword(in)) {
                return;
            }

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
                System.out.println(GREEN + "1)" + RESET + " Delete another contact");
                System.out.println(GREEN + "2)" + RESET + " Undo this deletion immediately");
                System.out.println(GREEN + "3)" + RESET + " Return to SENIOR menu");
                System.out.print(YELLOW + "Select (1-3, q = back): " + RESET);

                String nextAction = readTrimmed();
                if (isCancelKeyword(nextAction)) {
                    return;
                }

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

            System.out.println();
            System.out.println(YELLOW + "Type 'q' to return to SENIOR menu." + RESET);
            System.out.print("Enter IDs comma-separated (e.g. 10,12,15) or q to cancel: ");
            String line = readTrimmed();

            if (isCancelKeyword(line)) return;

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
                System.out.println(GREEN + "1)" + RESET + " Delete more contacts");
                System.out.println(GREEN + "2)" + RESET + " Undo last deletion (restores the LAST deleted contact)");
                System.out.println(GREEN + "3)" + RESET + " Return to SENIOR menu");
                System.out.print(YELLOW + "Select (1-3, q = back): " + RESET);

                String nextAction = readTrimmed();
                if (isCancelKeyword(nextAction)) {
                    return;
                }

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
        Connection con = null;
        try {
            con = db.connect();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                return false;
            }

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
            System.out.println(RED + "Delete error for ID " + id + ": " + e.getMessage() + RESET);
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
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
                case "ADD":
                    undoAdd(ua.snap);
                    break;
                case "DELETE":
                    undoDelete(ua.snap);
                    break;
                case "UPDATE":
                    undoUpdateSenior(ua.snap);
                    break;
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
        Connection con = null;
        try {
            con = db.connect();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                return;
            }
            PreparedStatement ps = con.prepareStatement("DELETE FROM contacts WHERE contact_id=?");
            ps.setInt(1, snap.contact_id);
            ps.executeUpdate();
            System.out.println(GREEN + "Undo ADD successful. (Deleted ID: " + snap.contact_id + ")" + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Undo ADD failed." + RESET);
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void undoDelete(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        Connection con = null;
        try {
            con = db.connect();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                return;
            }

            String sql = "INSERT INTO contacts " +
                    "(contact_id, first_name, middle_name, last_name, nickname, phone_primary, phone_secondary, email, linkedin_url, birth_date) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)";
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
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void undoUpdateSenior(ContactSnapshot snap) {
        dB_Connection db = new dB_Connection();
        Connection con = null;
        try {
            con = db.connect();
            if (con == null) {
                System.out.println(RED + "Database connection failed." + RESET);
                return;
            }

            String sql = "UPDATE contacts SET first_name=?, middle_name=?, last_name=?, nickname=?, " +
                    "phone_primary=?, phone_secondary=?, email=?, linkedin_url=?, birth_date=? WHERE contact_id=?";
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
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // ============================= SNAPSHOT HELPERS ===============================

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