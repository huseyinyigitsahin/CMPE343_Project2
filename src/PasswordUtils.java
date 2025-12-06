import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    // Ortak hash fonksiyonu
    public static String hashPassword(String password) {
        if (password == null) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    // Ortak şifre güç değerlendirme fonksiyonu
    public static String evaluatePasswordStrength(String password) {
        if (password == null) {
            return "very_weak";
        }

        int length = password.length();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit  = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");

        if (length < 4) {
            return "very_weak";
        }

        if (length < 8) {
            return "weak";
        }

        int score = 0;
        if (hasLetter) score++;
        if (hasDigit)  score++;
        if (hasSymbol) score++;

        if (length >= 12 && score >= 2) {
            return "strong";
        }

        return "medium";
    }
}
