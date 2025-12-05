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

    public static void main(String[] args) {

        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        LoginScreen.showInitialSplash(scanner);

        LoginScreen.showPreLoginLoadingBar();

        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {

                LoginScreen.printLoginFormHeader();

                System.out.print(LoginScreen.CYAN + "|  " + LoginScreen.YELLOW + "Username : " + LoginScreen.RESET);
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
                    System.out.println();
                    System.out.println(LoginScreen.RED + ">> Username cannot be empty." + LoginScreen.RESET);
                    System.out.println(LoginScreen.YELLOW + "Press ENTER to try again..." + LoginScreen.RESET);
                    scanner.nextLine();
                    attempts++;
                    continue;
                }

                System.out.print(LoginScreen.CYAN + "|  " + LoginScreen.YELLOW + "Password : " + LoginScreen.RESET);
                String password = "";
                if (scanner.hasNextLine()) {
                    password = scanner.nextLine();
                }

                LoginScreen.printLoginFooter();

                boolean success = LoginScreen.authenticate(username, password);

                if (success) {
                    LoginScreen.showPostLoginLoadingBar();

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
                            try {
                                con.close();
                            } catch (SQLException ignored) {
                            }
                        }
                    }

                    // Small welcome message before menu
                    System.out.println(LoginScreen.GREEN + "Login successful." + LoginScreen.RESET);
                    System.out.println("Welcome, " + fullName + " (" + role + ")");
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }

                    // Role-based menus
                    if ("Tester".equalsIgnoreCase(role)) {
                        TesterMenu testerMenu =
                                new TesterMenu(username, fullName, role, scanner, passwordStrength);
                        testerMenu.showMenu();
                    } else if ("Junior Developer".equalsIgnoreCase(role)) {
                        JuniorDevMenu juniorMenu =
                                new JuniorDevMenu(username, fullName, role, scanner, passwordStrength);
                        juniorMenu.showMenu();
                    } else if ("Senior Developer".equalsIgnoreCase(role)) {
                        SeniorDevMenu seniorMenu =
                                new SeniorDevMenu(username, fullName, role, scanner, passwordStrength);
                        seniorMenu.showMenu();
                    } else if ("Manager".equalsIgnoreCase(role)) {
                        ManagerMenu managerMenu =
                                new ManagerMenu(username, fullName, role, scanner, passwordStrength);
                        managerMenu.showMenu();
                    } else {
                        System.out.println(LoginScreen.RED + ">> Unknown role: " + role + LoginScreen.RESET);
                        System.out.println(LoginScreen.YELLOW + "Press ENTER to go back to login..." + LoginScreen.RESET);
                        scanner.nextLine();
                    }

                    System.out.println(LoginScreen.GREEN +
                            "\nLogged out successfully. Returning to login screen..." +
                            LoginScreen.RESET);
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                    attempts = 0;

                } else {
                    attempts++;
                    if (attempts >= MAX_ATTEMPTS) {
                        System.out.println(LoginScreen.RED +
                                ">> SYSTEM LOCKED: Too many failed attempts." +
                                LoginScreen.RESET);
                        break;
                    }

                    // Wrong password: show error & ask if they want to try again or quit
                    LoginScreen.showLoginErrorPrompt("Incorrect username or password.");
                    String resp = scanner.nextLine().trim();
                    if (resp.equalsIgnoreCase("q")) {
                        printShutdownAscii();
                        scanner.close();
                        return;
                    }
                }

            } catch (Exception e) {
                System.out.println(LoginScreen.RED +
                        ">> An unexpected error occurred. Restarting login..." +
                        LoginScreen.RESET);
                attempts++;
            }
        }

        scanner.close();
        printShutdownAscii();
    }


    private static void printShutdownAscii() {


    int barWidth = 30;
    int steps = 20;
    int delayMs = 90;

    for (int i = 0; i <= steps; i++) {

        int progress = i * 100 / steps;
        int filled = progress * barWidth / 100;

        LoginScreen.clearScreen();
        System.out.println();
        System.out.println(LoginScreen.YELLOW + "Shutting down the system..." + LoginScreen.RESET);
        System.out.println();

        StringBuilder bar = new StringBuilder();
        for (int j = 0; j < barWidth; j++) {
            if (j < filled) bar.append("=");
            else bar.append(" ");
        }

        System.out.println(LoginScreen.CYAN + "[" + bar + "] " + progress + "%" + LoginScreen.RESET);
        System.out.println();

        try { Thread.sleep(delayMs); } catch (Exception ignored) {}
    }

    LoginScreen.clearScreen();
    System.out.println();
    System.out.println(LoginScreen.RED + "#############################################");
    System.out.println("#                                           #");
    System.out.println("#           GOODBYE! SEE YOU SOON           #");
    System.out.println("#                                           #");
    System.out.println("#############################################" + LoginScreen.RESET);
    }

}

