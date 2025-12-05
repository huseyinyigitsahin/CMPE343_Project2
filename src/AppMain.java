import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class AppMain {

    private static final int MAX_ATTEMPTS = 10;

    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static final String BLUE = "\u001B[34m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String WHITE_BOLD = "\u001B[1;37m";

    public static void main(String[] args) {

        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        
        displayWelcomeMessage();

        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                clearScreen();

                printLoginHeader();

                System.out.print(CYAN + "|  " + YELLOW + "Username : " + RESET);
                
                String username = null;
                if (scanner.hasNextLine()) {
                    username = scanner.nextLine().trim();
                }

                if (username != null && username.equalsIgnoreCase("q")) {
                    printShutdownAscii(); 
                    scanner.close();
                    return;
                }

                if (username == null || username.isBlank()) {
                    printError("Username cannot be empty.");
                    attempts++;
                    pressEnterToContinue(scanner);
                    continue;
                }

                System.out.print(CYAN + "|  " + YELLOW + "Password : " + RESET);
                String password = "";
                if (scanner.hasNextLine()) {
                    password = scanner.nextLine();
                }

                printLoginFooter(); 

                boolean success = LoginScreen.authenticate(username, password);

                if (success) {
                    String fullName = username;
                    String role = "Tester"; 
                    String passwordStrength = LoginScreen.getLastPasswordStrengthAtLogin();

                    dB_Connection db = new dB_Connection();
                    Connection con = db.connect();

                    if (con != null) {
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
                    }

                    if ("Tester".equalsIgnoreCase(role)) {
                        TesterMenu testerMenu = new TesterMenu(username, fullName, role, scanner, passwordStrength);
                        testerMenu.showMenu();
                    } 
                    else if ("Junior Developer".equalsIgnoreCase(role)) {
                        JuniorDevMenu juniorMenu = new JuniorDevMenu(username, fullName, role, scanner, passwordStrength);
                        juniorMenu.showMenu();
                    } 
                    else if ("Senior Developer".equalsIgnoreCase(role)) {
                        SeniorDevMenu seniorMenu = new SeniorDevMenu(username, fullName, role, scanner, passwordStrength);
                        seniorMenu.showMenu();
                    } 
                   else if ("Manager".equalsIgnoreCase(role)) {
                        ManagerMenu managerMenu = new ManagerMenu(username, fullName, role, scanner, passwordStrength);
                        managerMenu.showMenu();
                    }

                    else {
                        System.out.println(RED + ">> Unknown role: " + role + RESET);
                        pressEnterToContinue(scanner);
                    }
                    
                    System.out.println(GREEN + "\nLogged out successfully. Returning to login screen..." + RESET);
                    try { Thread.sleep(1500); } catch (Exception e) {}
                    attempts = 0; 
                
                } else {
                    attempts++;
                    if (attempts >= MAX_ATTEMPTS) {
                        System.out.println(RED + ">> SYSTEM LOCKED: Too many failed attempts." + RESET);
                        break;
                    }
                }

            } catch (Exception e) {
                System.out.println(RED + ">> An unexpected error occurred. Restarting login..." + RESET);
                attempts++;
            }
        }

        scanner.close();
        printShutdownAscii();
    }

    public static void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    private static void printLoginHeader() {
        System.out.println(); 
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
        System.out.println(CYAN + "|           " + WHITE_BOLD + "SECURE SYSTEM LOGIN" + CYAN + "                |" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
        System.out.println(CYAN + "|                                              |" + RESET);
        System.out.println(CYAN + "| " + RESET + "Please enter your credentials below.         " + CYAN + "|" + RESET);
        System.out.println(CYAN + "| " + RESET + "Type " + RED + "'q'" + RESET + " to quit the application.            " + CYAN + "|" + RESET);
        System.out.println(CYAN + "|                                              |" + RESET);
    }

    private static void printLoginFooter() {
        System.out.println(CYAN + "|                                              |" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
    }

    private static void printError(String msg) {
        System.out.println(CYAN + "|  " + RED + "ERROR: " + msg + String.format("%" + (40 - msg.length()) + "s", "") + CYAN + "|" + RESET);
        System.out.println(CYAN + "+----------------------------------------------+" + RESET);
    }

    private static void pressEnterToContinue(Scanner scanner) {
        System.out.print("Press ENTER to continue...");
        scanner.nextLine();
    }

    public static void displayWelcomeMessage() {
        clearScreen();
        
        System.out.println(BLUE + "========================================================================" + RESET);
        System.out.println(BLUE + "   ____ __  __ ____  _____ _____ _  _  _____ " + RESET);
        System.out.println(BLUE + " / ___|  \\/  |  _ \\| ____|___ /| || ||___ / " + RESET);
        System.out.println(BLUE + "| |   | |\\/| | |_) |  _|   |_ \\| || |_ |_ \\ " + RESET);
        System.out.println(BLUE + "| |___| |  | |  __/| |___ ___) |__   _|__) |" + RESET);
        System.out.println(BLUE + " \\____|_|  |_|_|   |_____|____/   |_||____/ " + RESET);
        System.out.println();
        System.out.println(YELLOW + "            Welcome to the CMPE 343 Course Project!             " + RESET);
        System.out.println(BLUE + "========================================================================" + RESET);
        
        System.out.println(CYAN + "      Project by: MERT FAHRI CAKAR, BURAK ARSLAN, " + RESET);
        System.out.println(CYAN + "      NERMIN ZEHRA SIPAHIOGLU, HUSEYIN YIGIT SAHIN" + RESET);
        System.out.println();

        System.out.print(YELLOW + "       System Loading... [");
        
        for (int i = 0; i < 20; i++) {
            System.out.print("="); 
            try {
                Thread.sleep(30); 
            } catch (InterruptedException e) {
            }
        }
        System.out.println("] 100% Ready!" + RESET);
        System.out.println();
        
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    private static void printShutdownAscii() {
        clearScreen();
        System.out.println();
        System.out.println(YELLOW + "       Saving Data... [||||||||||] 100%" + RESET);
        System.out.println(RED + "#############################################");
        System.out.println("#                                           #");
        System.out.println("#           GOODBYE! SEE YOU SOON           #");
        System.out.println("#                                           #");
        System.out.println("#############################################" + RESET);
    }
}
