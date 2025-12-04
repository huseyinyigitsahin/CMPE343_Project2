import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AppMain {

    private static final int MAX_ATTEMPTS = 10;

    public static void main(String[] args) {

        // Scanner'ı sistemin varsayılan diline göre başlatıyoruz.
        // Bu, VS Code veya CMD fark etmeksizin en uyumlu yöntemdir.
        Scanner scanner = new Scanner(System.in);
        
        displayWelcomeMessage();

        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                System.out.println("\n--- LOGIN ---");
                System.out.println("Type 'q' to quit.");
                System.out.print("Username: ");
                
                if (scanner.hasNextLine()) {
                    String username = scanner.nextLine();
                    
                    if (username != null) username = username.trim();

                    // Çıkış kontrolü
                    if (username != null && username.equalsIgnoreCase("q")) {
                        printShutdownAscii(); 
                        scanner.close();
                        return;
                    }

                    if (username == null || username.isBlank()) {
                        System.out.println(">> Please enter a username.");
                        attempts++;
                        continue;
                    }

                    System.out.print("Password: ");
                    String password = "";
                    if (scanner.hasNextLine()) {
                        password = scanner.nextLine();
                    }

                    boolean success = LoginScreen.authenticate(username, password);

                    if (success) {
                        String fullName = username;
                        String role = "Tester"; 

                        dB_Connection db = new dB_Connection();
                        Connection con = db.connect();

                        if (con == null) {
                            System.out.println(">> Database connection failed. Please try again.");
                            break;
                        }

                        // Veritabanından kullanıcı bilgilerini çek
                        String sql = "SELECT name, surname, role FROM users WHERE username = ?";

                        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                            pstmt.setString(1, username);
                            try (ResultSet rs = pstmt.executeQuery()) {
                                if (rs.next()) {
                                    String name = rs.getString("name");
                                    String surname = rs.getString("surname");
                                    role = rs.getString("role");
                                    if (name != null && surname != null) {
                                        fullName = name + " " + surname;
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            System.out.println(">> Error loading user profile.");
                        } finally {
                            try { con.close(); } catch (SQLException ignored) {}
                        }

                        // ROL YÖNLENDİRMESİ
                        if ("Tester".equalsIgnoreCase(role)) {
                            TesterMenu testerMenu = new TesterMenu(username, fullName, role);
                            testerMenu.showMenu();
                            
                            System.out.println("Logged out successfully.");
                            attempts = 0; 
                        } 
                        else if ("Junior Developer".equalsIgnoreCase(role)) {
                            JuniorDevMenu juniorMenu = new JuniorDevMenu(username, fullName, role);
                            juniorMenu.showMenu();

                            System.out.println("Logged out successfully.");
                            attempts = 0;
                        } 
                        else if ("Senior Developer".equalsIgnoreCase(role)) {
                            SeniorDevMenu seniorMenu = new SeniorDevMenu(username, fullName, role);
                            seniorMenu.showMenu();

                            System.out.println("Logged out successfully.");
                            attempts = 0;
                        } 
                        else if ("Manager".equalsIgnoreCase(role)) {
                            System.out.println(">> Manager menu is under construction.");
                        }
                        else {
                            System.out.println(">> Unknown role: " + role);
                        }

                    } else {
                        attempts++;
                        if (attempts >= MAX_ATTEMPTS) {
                            System.out.println(">> Too many failed attempts. System locking down.");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(">> An unexpected error occurred. Please try again.");
                attempts++;
            }
        }

        scanner.close();
        printShutdownAscii();
    }

    public static void displayWelcomeMessage() {
        // ANSI Renk Kodları
        String ANSI_RESET = "\u001B[0m";
        String ANSI_PURPLE = "\u001B[35m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_CYAN = "\u001B[36m";

        System.out.println(ANSI_PURPLE + "========================================================================" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "   ____ __  __ ____  _____ _____ _  _  _____ " + ANSI_RESET);
        System.out.println(ANSI_PURPLE + " / ___|  \\/  |  _ \\| ____|___ /| || ||___ / " + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "| |   | |\\/| | |_) |  _|   |_ \\| || |_ |_ \\ " + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "| |___| |  | |  __/| |___ ___) |__   _|__) |" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + " \\____|_|  |_|_|   |_____|____/   |_||____/ " + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_YELLOW + "            Welcome to the CMPE 343 Course Project!             " + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "========================================================================" + ANSI_RESET);
        
        // --- DÜZELTME: Karakter bozulmasını önlemek için İngilizce karakterler kullanıldı ---
        System.out.println(ANSI_CYAN + "      Project by: MERT FAHRI CAKAR, BURAK ARSLAN, " + ANSI_RESET);
        System.out.println(ANSI_CYAN + "      NERMIN ZEHRA SIPAHIOGLU, HUSEYIN YIGIT SAHIN" + ANSI_RESET);
        System.out.println();

        System.out.print(ANSI_YELLOW + "       System Loading... [");
        
        // Animasyon
        for (int i = 0; i < 20; i++) {
            System.out.print("▓");
            try {
                Thread.sleep(50); // Biraz hızlandırdım (50ms)
            } catch (InterruptedException e) {
                // Hata olursa devam et
            }
        }
        System.out.println("] 100% Ready!" + ANSI_RESET);
        System.out.println();
    }

    private static void printShutdownAscii() {
        System.out.println();
        System.out.println("       Saving Data... [||||||||||] 100%");
        System.out.println("#############################################");
        System.out.println("#                                           #");
        System.out.println("#           GOODBYE! SEE YOU SOON           #");
        System.out.println("#                                           #");
        System.out.println("#############################################");
    }
}