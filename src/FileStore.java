import java.io.*;
import java.util.*;

public class FileStore {
    static final String DIR   = "data/";
    static final String USERS = DIR + "users.txt";
    static final String ROOMS = DIR + "rooms.txt";
    static final String RES   = DIR + "reservations.txt";
    static final String FEED  = DIR + "feedback.txt";
    static final String GW    = DIR + "gateways.txt";
    static final String PEND  = DIR + "pending_payments.txt";

    static { new File(DIR).mkdirs(); }

    // ── generic write ───────────────────────────────
    private static void writeLines(String path, List<String> lines) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            for (String l : lines) { w.write(l); w.newLine(); }
        }
    }
    private static List<String> readLines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        File f = new File(path); if (!f.exists()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) out.add(line);
            }
        }
        return out;
    }

    // ── Users ───────────────────────────────────────
    public static void saveUsers(List<User> list) throws IOException {
        List<String> lines = new ArrayList<>();
        for (User u : list) lines.add(u.toFileLine());
        writeLines(USERS, lines);
    }
    public static List<User> loadUsers() throws IOException {
        List<User> out = new ArrayList<>();
        for (String line : readLines(USERS)) {
            String[] p = line.split("\\|");
            out.add(switch (p[4]) {
                case "ADMIN"   -> new Admin(p[0],p[1],p[2],p[3],true);
                case "MANAGER" -> new Manager(p[0],p[1],p[2],p[3],true);
                default        -> new Customer(p[0],p[1],p[2],p[3],true);
            });
        }
        return out;
    }

    // ── Rooms ───────────────────────────────────────
    public static void saveRooms(List<Room> list) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Room r : list) lines.add(r.toFileLine());
        writeLines(ROOMS, lines);
    }
    public static List<Room> loadRooms() throws IOException {
        List<Room> out = new ArrayList<>();
        for (String line : readLines(ROOMS)) {
            String[] p = line.split("\\|");
            // roomId|typeName|floor|price|totalQty|availableQty|status
            Room rt = switch (p[1]) {
                case "Deluxe" -> new Premium(p[0],Integer.parseInt(p[2]),Double.parseDouble(p[3]),Integer.parseInt(p[4]));
                case "Suite"  -> new SuiteRoom1(p[0],Integer.parseInt(p[2]),Double.parseDouble(p[3]),Integer.parseInt(p[4]));
                case "Family"  -> new Family(p[0],Integer.parseInt(p[2]),Double.parseDouble(p[3]),Integer.parseInt(p[4]));
                default       -> new StandardRoom1(p[0],Integer.parseInt(p[2]),Double.parseDouble(p[3]),Integer.parseInt(p[4]));
            };
            rt.setAvailableQty(Integer.parseInt(p[5]));
            rt.setStatus(Room.Status.valueOf(p[6]));
            out.add(rt);
        }
        return out;
    }

    // ── Reservations ────────────────────────────────
    public static void saveReservations(List<Reservation> list) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Reservation r : list) lines.add(r.toFileLine());
        writeLines(RES, lines);
    }
    public static List<Reservation> loadReservations() throws IOException {
        List<Reservation> out = new ArrayList<>();
        for (String line : readLines(RES)) out.add(Reservation.fromLine(line));
        return out;
    }

    // ── Feedback ────────────────────────────────────
    public static void saveFeedback(List<Feedback> list) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Feedback f : list) lines.add(f.toFileLine());
        writeLines(FEED, lines);
    }
    public static List<Feedback> loadFeedback() throws IOException {
        List<Feedback> out = new ArrayList<>();
        for (String line : readLines(FEED)) out.add(Feedback.fromLine(line));
        return out;
    }

    // ── Gateways ────────────────────────────────────
    public static void saveGateways(List<PaymentGateway> list) throws IOException {
        List<String> lines = new ArrayList<>();
        for (PaymentGateway g : list) lines.add(g.toFileLine());
        writeLines(GW, lines);
    }
    public static List<PaymentGateway> loadGateways() throws IOException {
        List<PaymentGateway> out = new ArrayList<>();
        for (String line : readLines(GW)) {
            String[] p = line.split("\\|");
            // name|className|enabled
            boolean en = p[2].equals("true");
            out.add(switch (p[1]) {
                case "CBEGateway"      -> new CBEGateway(p[0], en);
                case "TelebirrGateway" -> new TelebirrGateway(p[0], en);
                case "AwashGateway"    -> new AwashGateway(p[0], en);
                default                -> new GenericGateway(p[0], en);
            });
        }
        return out;
    }

    // ── Pending Payments ────────────────────────────
    public static void savePending(List<String> list) throws IOException { writeLines(PEND, list); }
    public static List<String> loadPending() throws IOException          { return readLines(PEND); }
}
