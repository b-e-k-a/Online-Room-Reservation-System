import java.io.Console;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Utils {
    public static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DT_FMT       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Scanner SCANNER = new Scanner(System.in);

    public static String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String readLine(String label) {
        System.out.print("  " + label + ": ");
        return SCANNER.nextLine().trim();
    }

    public static String readPassword(String label) {
        Console con = System.console();
        if (con != null) {
            char[] pwd = con.readPassword("  " + label + ": ");
            return pwd == null ? "" : new String(pwd);
        }
        // Fallback for IDEs: still reads but notes it cannot hide
        System.out.print("  " + label + " (hidden in terminal): ");
        return SCANNER.nextLine().trim();
    }

    public static LocalDate parseDate(String s) throws ValidationException {
        try { return LocalDate.parse(s.trim(), DATE_FMT); }
        catch (DateTimeParseException e) { throw new ValidationException("Date must be yyyy-MM-dd"); }
    }

    public static String now() {
        return LocalDateTime.now().format(DT_FMT);
    }

    public static String genId(String prefix, int count) {
        return prefix + String.format("%04d", count + 1);
    }

    public static void header(String title) {
        int w = 56;
        String bar = "═".repeat(w);
        int pad = (w - title.length()) / 2;
        String padded = " ".repeat(Math.max(0, pad)) + title;
        System.out.println("\n  ╔" + bar + "╗");
        System.out.printf("  ║%-" + w + "s║%n", padded);
        System.out.println("  ╚" + bar + "╝");
    }

    public static void line() {
        System.out.println("  " + "─".repeat(58));
    }
}
