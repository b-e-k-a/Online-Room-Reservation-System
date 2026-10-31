import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class HotelSystem {

    private List<User>           users    = new ArrayList<>();
    private List<Room>       rooms    = new ArrayList<>();
    private List<Reservation>    res      = new ArrayList<>();
    private List<Feedback>       feedback = new ArrayList<>();
    private List<PaymentGateway> gateways = new ArrayList<>();
    private List<String>         pending  = new ArrayList<>(); // "resId|amount|DEPOSIT|FULL"
    private User currentUser = null;

    // ════════════════════════════════════════════════
    //  START
    // ════════════════════════════════════════════════
    public void start() {
        load(); seed();
        checkoutSweep();
        banner();
        boolean on = true;
        while (on) {
            printMainMenu();
            String c = Utils.readLine("Enter choice");
            switch (c) {
                case "1" -> login("ADMIN");
                case "2" -> login("MANAGER");
                case "3" -> registerCustomer();
                case "4" -> login("CUSTOMER");
                case "5" -> { System.out.println("\n  Goodbye! See you soon.\n"); on = false; }
                default  -> System.out.println("  [!] Invalid option. Try again.");
            }
        }
    }

    private void banner() {
        System.out.println("""

  ╔══════════════════════════════════════════════════════════╗
   ║       GRAND HOTEL Room RESERVATION SYSTEM              ║
  ╚══════════════════════════════════════════════════════════╝
""");
    }

    private void printMainMenu() {
        System.out.println("""
  ╔═══★════════════════════════════════════════════╗
  ║                  MAIN MENU                    ║
  ╠═══════════════════════★════════════════════════╣
  ║     1.  Admin Login                           ║
  ║     2.  Manager Login                          ║
  ║     3.  Customer Register                     ║
  ║     4.  Customer Login                         ║
  ║     5.  Exit                                  ║
  ╚════════════════════════════════════════════════╝""");
    }

    // ════════════════════════════════════════════════
    //  AUTH
    // ════════════════════════════════════════════════
    private void login(String role) {
        Utils.header(role + " LOGIN");
        try {
            String uname   = Utils.readLine("Username");
            String pwd     = Utils.readPassword("Password");
            currentUser    = authenticate(uname, pwd, role);
            System.out.println("\n  ✔ Welcome, " + currentUser.getUsername()
                    + "! [" + role + "]\n");
            checkoutSweep();
            routeDashboard();
        } catch (AuthException e) {
            System.out.println("  [!] " + e.getMessage());
        }
    }

    private User authenticate(String uname, String pwd, String role) throws AuthException {
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(uname) && u.getRole().equals(role)) {
                if (!u.verifyPassword(pwd)) throw new AuthException("Incorrect password.");
                return u;
            }
        }
        throw new AuthException("No " + role + " account found for '" + uname + "'.");
    }
    private void registerCustomer() {
        Utils.header("CUSTOMER REGISTRATION");
        try {
            String uname = Utils.readLine("Enter username");
            if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(uname))) {
                System.out.println("  [!] Username already taken."); return;
            }
            String email = Utils.readLine("Email address");
            String pwd   = Utils.readPassword("Password (min 6 characters)");
            String conf  = Utils.readPassword("Confirm password");
            if (!pwd.equals(conf)) { System.out.println("  [!] Passwords do not match."); return; }
            String id = Utils.genId("C", users.size());
            users.add(new Customer(id, uname, pwd, email));
            save();
            System.out.println("  ✔ Account created! Your ID: " + id + ". \n You Can login Now.");
        } catch (ValidationException e) {
            System.out.println("  [!] " + e.getMessage());
        }
    }
    // ════════════════════════════════════════════════
    //  DASHBOARD ROUTING  (Polymorphism via instanceof)
    // ════════════════════════════════════════════════
    private void routeDashboard() {
        if      (currentUser instanceof Admin)    adminDash();
        else if (currentUser instanceof Manager)  managerDash();
        else                                       customerDash();
    }

    // ════════════════════════════════════════════════
    //  ADMIN DASHBOARD
    // ════════════════════════════════════════════════
    private void adminDash() {
        boolean on = true;
        while (on) {
            System.out.println(currentUser.menuText());
            switch (Utils.readLine("Choice")) {
                case "1"  -> adminManagerAccount();
                case "2"  -> adminUpdateManager();
                case "3"  -> adminResetPassword();
                case "4"  -> adminAddRoom();
                case "5"  -> adminEditRoom();
                case "6"  -> adminRemoveRoom();
                case "7"  -> displayRooms(true);
                case "8"  -> adminCancelBooking();
                case "9"  -> displayAllReservations();
                case "10" -> adminViewPayments();
                case "11" -> adminApprovePayment();
                case "12" -> adminManageGateways();
                case "13" -> printAllUsers();
                case "0" -> { currentUser = null; on = false; System.out.println("  Logged out.\n"); }
                default   -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private void adminManagerAccount() {
        Utils.header("MANAGER ACCOUNT");
        Manager mgr = findManager();
        if (mgr != null) {
            System.out.println("  Manager account already exists:\n" + mgr);
            System.out.println("\n  Only one manager account is allowed.");
            return;
        }
        System.out.println("  No manager account found. Creating one now...\n");
        try {
            String uname = Utils.readLine("Manager username");
            if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(uname))) {
                System.out.println("  [!] Username taken."); return;
            }
            String email = Utils.readLine("Manager email");
            String pwd   = Utils.readPassword("Password");
            String id    = Utils.genId("M", users.size());
            users.add(new Manager(id, uname, pwd, email));
            save();
            System.out.println("  ✔ Manager account created. ID: " + id);
        } catch (ValidationException e) { System.out.println("  [!] " + e.getMessage()); }
    }

    private void adminUpdateManager() {
        Utils.header("UPDATE MANAGER ACCOUNT");
        Manager mgr = findManager();
        if (mgr == null) { System.out.println("  No manager account exists."); return; }
        System.out.println("  Current manager: " + mgr + "\n");
        System.out.println("  What to update? 1=Username  2=Email  3=Password  4=Replace manager entirely");
        switch (Utils.readLine("Option")) {
            case "1" -> {
                String nu = Utils.readLine("New username");
                if (users.stream().filter(u -> u != mgr).anyMatch(u -> u.getUsername().equalsIgnoreCase(nu))) {
                    System.out.println("  [!] Username taken."); return;
                }
                mgr.setUsername(nu); save(); System.out.println("  ✔ Username updated.");
            }
            case "2" -> {
                String ne = Utils.readLine("New email");
                mgr.setEmail(ne); save(); System.out.println("  ✔ Email updated.");
            }
            case "3" -> {
                try {
                    String np = Utils.readPassword("New password");
                    mgr.resetPassword(np); save(); System.out.println("  ✔ Password updated.");
                } catch (ValidationException e) { System.out.println("  [!] " + e.getMessage()); }
            }
            case "4" -> {
                users.remove(mgr);
                System.out.println("  Old manager removed. Creating replacement:");
                adminManagerAccount();
            }
            default -> System.out.println("  [!] Invalid option.");
        }
    }

    private void adminResetPassword() {
        Utils.header("RESET USER PASSWORD");
        System.out.println("  All users:\n");
        users.forEach(System.out::println);
        System.out.println();
        String uid = Utils.readLine("Enter user ID to reset");
        User target = users.stream().filter(u -> u.getId().equalsIgnoreCase(uid.trim())).findFirst().orElse(null);
        if (target == null) { System.out.println("  [!] User ID not found."); return; }
        System.out.println("  Found: " + target);
        try {
            String np = Utils.readPassword("New password (min 6 chars)");
            target.resetPassword(np);
            save();
            System.out.println("  ✔ Password reset for " + target.getUsername());
        } catch (ValidationException e) { System.out.println("  [!] " + e.getMessage()); }
    }

    private void adminAddRoom() {
        Utils.header("ADD ROOM TYPE");
        try {
            String rid = Utils.readLine("Room ID (e.g. R101)").toUpperCase().trim();
            if (rooms.stream().anyMatch(r -> r.getRoomId().equals(rid))) {
                System.out.println("  [!] Room ID already exists."); return;
            }
            System.out.println("  Types:  1 = Standard    2 = Premium    3 = Suite   4 = Family ");
            String t     = Utils.readLine("Type (1/2/3/4)").trim();
            int floor    = Integer.parseInt(Utils.readLine("Floor number"));
            double price = Double.parseDouble(Utils.readLine("Price per night ($)"));
            int qty      = Integer.parseInt(Utils.readLine("Quantity (number of rooms)"));
            if (floor < 1)  throw new ValidationException("Floor must be ≥1");
            if (price <= 0) throw new ValidationException("Price must be positive");
            if (qty < 1)    throw new ValidationException("Quantity must be ≥1");
            Room room = switch (t) {
                case "2" -> new Premium(rid, floor, price, qty);
                case "3" -> new SuiteRoom1(rid, floor, price, qty);
                case "4" -> new Family(rid, floor, price, qty);
                default  -> new StandardRoom1(rid, floor, price, qty);
            };
            rooms.add(room); save();
            System.out.println("  ✔ Room " + rid + " (" + room.getTypeName() + " x" + qty + ") added.");
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid number entered.");
        } catch (ValidationException e) {
            System.out.println("  [!] " + e.getMessage());
        }
    }

    private void adminEditRoom() {
        Utils.header("EDIT ROOM");
        displayRooms(true);
        String rid = Utils.readLine("Room ID to edit").toUpperCase().trim();
        Room room = findRoom(rid);
        if (room == null) { System.out.println("  [!] Room not found."); return; }
        System.out.println("  Editing: " + room.display() + "\n");
        System.out.println("  Press ENTER to keep current value.");
        try {
            String pStr = Utils.readLine("New price/night (current: $" + room.getPrice() + ")");
            if (!pStr.isBlank()) room.setPrice(Double.parseDouble(pStr));

            String fStr = Utils.readLine("New floor (current: " + room.getFloor() + ")");
            if (!fStr.isBlank()) room.setFloor(Integer.parseInt(fStr));

            String qStr = Utils.readLine("New total quantity (current: " + room.getTotalQty() + ")");
            if (!qStr.isBlank()) {
                int newQty = Integer.parseInt(qStr);
                int diff   = newQty - room.getTotalQty();
                room.setTotalQty(newQty);
                room.setAvailableQty(Math.max(0, room.getAvailableQty() + diff));
            }
            save();
            System.out.println("  ✔ Room " + rid + " updated.");
        } catch (NumberFormatException e) { System.out.println("  [!] Invalid number, edit aborted."); }
    }

    private void adminRemoveRoom() {
        Utils.header("REMOVE ROOM");
        displayRooms(true);

        String rid = Utils.readLine("Room ID to remove").toUpperCase().trim();
        Room room = findRoom(rid);

        if (room == null) {
            System.out.println("  [!] Room ID '" + rid + "' not found.");
            return;
        }

        // Show current state
        System.out.println();
        System.out.printf("  Found  : %s | %s | Floor %d | Total qty: %d | Available: %d%n",
                room.getRoomId(), room.getTypeName(), room.getFloor(),
                room.getTotalQty(), room.getAvailableQty());
        System.out.println();

        // Ask how many units to remove
        System.out.println("  How many units do you want to remove?");
        System.out.printf("  (Max removable: %d — only available units can be removed)%n",
                room.getAvailableQty());
        System.out.println("  Type 'all' to remove the entire room entry.");
        System.out.println();

        String input = Utils.readLine("Units to remove").trim();

        // ── Remove ALL ────────────────────────────────────
        if (input.equalsIgnoreCase("all")) {
            // Check if any units are currently booked
            int booked = room.getTotalQty() - room.getAvailableQty();
            if (booked > 0) {
                System.out.printf(
                        "  [!] Cannot remove all — %d unit(s) are currently booked.%n", booked);
                System.out.println("  Please cancel those reservations first.");
                return;
            }
            String removedId = room.getRoomId();
            rooms.remove(room);
            save();
            System.out.printf("  ✔ Room entry '%s' completely removed.%n%n", removedId);
            return;
        }

        // ── Remove specific number of units ───────────────
        int unitsToRemove;
        try {
            unitsToRemove = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid input. Enter a number or 'all'.");
            return;
        }

        if (unitsToRemove <= 0) {
            System.out.println("  [!] Number must be greater than 0.");
            return;
        }

        if (unitsToRemove > room.getAvailableQty()) {
            System.out.printf(
                    "  [!] You can only remove up to %d available unit(s).%n",
                    room.getAvailableQty());
            System.out.println("  Booked units cannot be removed.");
            return;
        }

        // Remove units one by one — each removal decrements ID
        String originalId = room.getRoomId();
        for (int i = 0; i < unitsToRemove; i++) {
            if (room.getTotalQty() == 1) {
                // Last unit — remove the entire room entry
                rooms.remove(room);
                save();
                System.out.printf("  ✔ Last unit removed. Room entry '%s' deleted entirely.%n",
                        room.getRoomId());
                return;
            }
            // Decrement both total and available
            room.setTotalQty(room.getTotalQty() - 1);
            room.setAvailableQty(room.getAvailableQty() - 1);

            // Decrement room ID (same logic as booking)
            room.setRoomId(decrementRoomId(room.getRoomId()));
        }

        save();

        System.out.println();
        Utils.line();
        System.out.printf("  ✔ Removed %d unit(s) from room '%s'%n", unitsToRemove, originalId);
        System.out.printf("  ✔ New Room ID   : %s%n",  room.getRoomId());
        System.out.printf("  ✔ New Total Qty : %d%n",  room.getTotalQty());
        System.out.printf("  ✔ Available     : %d%n",  room.getAvailableQty());
        Utils.line();
        System.out.println();
    }
    // Decrements the numeric part of a room ID
// e.g. "R105" → "R104",  "AB210" → "AB209"
    private String decrementRoomId(String roomId) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder number = new StringBuilder();

        for (char c : roomId.toCharArray()) {
            if (Character.isLetter(c)) prefix.append(c);
            else number.append(c);
        }

        if (number.isEmpty()) return roomId; // no number part — return as is

        int    num    = Integer.parseInt(number.toString());
        int    digits = number.length();  // preserve original digit width
        String fmt    = "%0" + digits + "d";

        return prefix + String.format(fmt, num - 1);
    }
    private void adminCancelBooking() {
        Utils.header("CANCEL ANY BOOKING");
        displayAllReservations();
        if(res.isEmpty()){System.out.println("  [!] No Reservation to Cancel."); return;}
        String rid = Utils.readLine("Reservation ID to cancel").toUpperCase().trim();
        Reservation r = findRes(rid);

        if (r == null) {
            System.out.println("  [!] Reservation not found."); return;
        }
        if (r.getStatus() == Reservation.Status.CANCELLED ||
                r.getStatus() == Reservation.Status.REFUNDED) {
            System.out.println("  [!] Already cancelled or refunded."); return;
        }

        // ── Restore room availability and ID ──────────────
        Room room = findRoomForCancellation(r.getRoomId());
        if (room != null) room.increaseAvailable();

        // ── Set reservation status ────────────────────────
        r.setStatus(Reservation.Status.CANCELLED);
        save();

        System.out.printf("  ✔ Reservation %s cancelled successfully.%n", rid);
    }

    private void adminViewPayments() {
        Utils.header("ALL PAYMENTS");
        if (res.isEmpty()) { System.out.println("  No payments recorded.\n"); return; }
        System.out.println();
        double grandTotal = 0;
        for (Reservation r : res) {
            System.out.printf("  %-10s | %-14s | Total:$%8.2f | Paid:$%8.2f | %-18s | [%s]%n",
                    r.getResId(), r.getGateway(), r.getTotalCost(), r.getPaidAmount(),
                    r.getPayStatus(), r.getStatus());
            grandTotal += r.getPaidAmount();
        }
        Utils.line();
        System.out.printf("  %-42s GRAND TOTAL RECEIVED: $%.2f%n%n", "", grandTotal);

        if (!pending.isEmpty()) {
            System.out.println("  PENDING APPROVAL .....");
            pending.forEach(p -> System.out.println("  >> " + p));
        }
        System.out.println();
    }

    private void adminApprovePayment() {
        Utils.header("APPROVE PAYMENT");

        if (pending.isEmpty()) {
            System.out.println("  No payments awaiting approval.\n");
            return;
        }

        // Show pending list with correct labels
        System.out.println("  Pending payments:\n");
        for (int i = 0; i < pending.size(); i++) {
            String[] p = pending.get(i).split("\\|", 3); // limit=3 ensures correct split
            if (p.length < 3) {
                System.out.println("  [!] Corrupted entry skipped: " + pending.get(i));
                continue;
            }
            String resId     = p[0].trim();
            double amount    = Double.parseDouble(p[1].trim());
            String type      = p[2].trim(); // "DEPOSIT" or "FULL"
            String typeLabel = type.equalsIgnoreCase("DEPOSIT")
                    ? "PARTIAL (30%)" : "FULL PAYMENT";

            Reservation r    = findRes(resId);
            String name      = (r != null) ? r.getFullName() : "Unknown";

            System.out.printf("  %d. %-10s | %-20s | $%8.2f | %s%n",
                    i + 1, resId, name, amount, typeLabel);
        }

        System.out.println();
        String inp = Utils.readLine(
                "Enter Reservation ID to approve (or 0 to cancel)").toUpperCase().trim();

        if (inp.equals("0")) return;

        // Find matching pending entry
        String found = null;
        for (String entry : pending) {
            String[] p = entry.split("\\|", 3);
            if (p.length >= 1 && p[0].trim().equalsIgnoreCase(inp)) {
                found = entry;
                break;
            }
        }

        if (found == null) {
            System.out.println("  [!] Reservation ID not found in pending list.");
            return;
        }

        // Parse the found entry safely
        String[] parts = found.split("\\|", 3);
        if (parts.length < 3) {
            System.out.println("  [!] Pending entry is corrupted. Cannot process.");
            return;
        }

        String resId  = parts[0].trim();
        double amount = Double.parseDouble(parts[1].trim());
        String type   = parts[2].trim(); // "DEPOSIT" or "FULL"

        Reservation r = findRes(resId);
        if (r == null) {
            System.out.println("  [!] Reservation record not found.");
            return;
        }

        // Apply payment amount
        r.addPayment(amount);

        // ── Set status based on type ──────────────────────
        if (type.equalsIgnoreCase("DEPOSIT")) {
            r.setPayStatus(Reservation.PayStatus.DEPOSIT_PAID);
        } else {
            r.setPayStatus(Reservation.PayStatus.FULLY_PAID);
            Room room = findRoom(r.getRoomId());
            if (room != null) room.setStatus(Room.Status.OCCUPIED);
        }

        // Remove from pending and save
        pending.remove(found);
        save();

        // ── Print correct confirmation ────────────────────
        System.out.println();
        Utils.line();
        if (r.getPayStatus() == Reservation.PayStatus.DEPOSIT_PAID) {
            System.out.printf("  ✔ Payment approved for: %s%n",        r.getFullName());
            System.out.printf("  ✔ Status  →  PARTIALLY PAID%n");
            System.out.printf("  ✔ Deposit received : $%.2f%n",        amount);
            System.out.printf("  ✔ Balance remaining: $%.2f (pay at check-in)%n", r.remaining());
        } else {
            System.out.printf("  ✔ Payment approved for: %s%n",        r.getFullName());
            System.out.printf("  ✔ Status  →  FULLY PAID%n");
            System.out.printf("  ✔ Amount received  : $%.2f%n",        amount);
        }
        Utils.line();
        System.out.println();
    }

    private void adminManageGateways() {
        Utils.header("MANAGE PAYMENT GATEWAYS");
        boolean on = true;
        while (on) {
            System.out.println("\n  Current Gateways:");
            for (int i = 0; i < gateways.size(); i++)
                System.out.println("  " + (i+1) + ". " + gateways.get(i));
            System.out.println("""
  
  Options:
    A. Add new gateway
    D. Delete a gateway
    T. Toggle enable/disable
    X. Back""");
            switch (Utils.readLine("Option").toUpperCase()) {
                case "A" -> {
                    String name = Utils.readLine("Gateway name (e.g. BankX)").trim();
                    if (name.isBlank()) { System.out.println("  [!] Name cannot be empty."); break; }
                    if (gateways.stream().anyMatch(g -> g.getName().equalsIgnoreCase(name))) {
                        System.out.println("  [!] Gateway already exists."); break;
                    }
                    gateways.add(new GenericGateway(name, true));
                    save(); System.out.println("  ✔ Gateway '" + name + "' added.");
                }
                case "D" -> {
                    String name = Utils.readLine("Gateway name to delete").trim();
                    boolean removed = gateways.removeIf(g -> g.getName().equalsIgnoreCase(name));
                    if (removed) { save(); System.out.println("  ✔ Removed."); }
                    else System.out.println("  [!] Not found.");
                }
                case "T" -> {
                    String name = Utils.readLine("Gateway name to toggle").trim();
                    PaymentGateway gw = gateways.stream()
                            .filter(g -> g.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                    if (gw == null) { System.out.println("  [!] Not found."); break; }
                    gw.setEnabled(!gw.isEnabled());
                    save(); System.out.println("  ✔ " + gw.getName() + " → " + (gw.isEnabled()?"ENABLED":"DISABLED"));
                }
                case "X" -> on = false;
                default  -> System.out.println("  [!] Invalid.");
            }
        }
    }

    // ════════════════════════════════════════════════
    //  MANAGER DASHBOARD
    // ════════════════════════════════════════════════
    private void managerDash() {
        boolean on = true;
        while (on) {
            System.out.println(currentUser.menuText());
            switch (Utils.readLine("Choice")) {
                case "1" -> displayAllReservations();
                case "2" -> managerUpdateStatus();
                case "3" -> managerChangeRoomId();
                case "4" -> managerRefund();
                case "5" -> managerViewFeedback();
                case "6" -> { currentUser = null; on = false; System.out.println("  Logged out.\n"); }
                default  -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private void managerUpdateStatus() {
        Utils.header("UPDATE ROOM STATUS");
        displayRooms(true);
        String rid = Utils.readLine("Room ID").toUpperCase().trim();
        Room room = findRoom(rid);
        if (room == null) { System.out.println("  [!] Room not found."); return; }
        System.out.println("  1 = AVAILABLE    2 = OCCUPIED    3 = OUT_OF_SERVICE");
        Room.Status newStatus = switch (Utils.readLine("New status")) {
            case "1" -> Room.Status.AVAILABLE;
            case "2" -> Room.Status.OCCUPIED;
            case "3" -> Room.Status.OUT_OF_SERVICE;
            default  -> null;
        };
        if (newStatus == null) { System.out.println("  [!] Invalid."); return; }
        room.setStatus(newStatus);
        save();
        System.out.println("  ✔ Room " + rid + " status → " + newStatus);
    }

    private void managerChangeRoomId() {
        Utils.header("CHANGE ROOM ID");
        displayRooms(true);
        String oldId = Utils.readLine("Current Room ID").toUpperCase().trim();
        Room room = findRoom(oldId);
        if (room == null) { System.out.println("  [!] Room not found."); return; }
        String newId = Utils.readLine("New Room ID").toUpperCase().trim();
        if (rooms.stream().anyMatch(r -> r.getRoomId().equals(newId))) {
            System.out.println("  [!] New ID already in use."); return;
        }
        room.setRoomId(newId);
        res.stream().filter(r -> r.getRoomId().equals(oldId)).forEach(r -> r.setRoomId(newId));
        save();
        System.out.println("  ✔ Room ID changed: " + oldId + " → " + newId);
    }

    private void managerRefund() {
        Utils.header("PROCESS REFUND");

        List<Reservation> reqs = res.stream()
                .filter(r -> r.getStatus() == Reservation.Status.REFUND_REQUESTED)
                .collect(Collectors.toList());

        if (reqs.isEmpty()) {
            System.out.println("  No pending refund requests.\n");
            return;
        }

        // Show numbered list
        System.out.println("\n  Pending Refund Requests:\n");
        for (int i = 0; i < reqs.size(); i++) {
            Reservation r = reqs.get(i);
            System.out.printf("  [%d] %s | %-20s | $%.2f | Gateway: %s%n",
                    i + 1, r.getResId(), r.getFullName(),
                    r.getPaidAmount(), r.getGateway());
        }

        System.out.println("\n  Enter numbers to refund (e.g: 1,3,5 or 2-4) or type 'all':");
        String input = Utils.readLine("Selection").trim();

        // Build list of selected reservations
        List<Reservation> selected = new ArrayList<>();

        if (input.equalsIgnoreCase("all")) {
            selected.addAll(reqs);
        } else {
            String[] parts = input.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.contains("-")) {
                    // Handle range like "1-3"
                    String[] range = part.split("-");
                    try {
                        int from = Integer.parseInt(range[0].trim()) - 1;
                        int to   = Integer.parseInt(range[1].trim()) - 1;
                        for (int i = from; i <= to; i++) {
                            if (i >= 0 && i < reqs.size()) selected.add(reqs.get(i));
                            else System.out.println("  [!] Number " + (i+1) + " out of range — skipped.");
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("  [!] Invalid range '" + part + "' — skipped.");
                    }
                } else {
                    // Handle single number
                    try {
                        int idx = Integer.parseInt(part) - 1;
                        if (idx >= 0 && idx < reqs.size()) selected.add(reqs.get(idx));
                        else System.out.println("  [!] Number " + (idx+1) + " out of range — skipped.");
                    } catch (NumberFormatException e) {
                        System.out.println("  [!] '" + part + "' is not valid — skipped.");
                    }
                }
            }
        }

        if (selected.isEmpty()) {
            System.out.println("  No valid selections made.");
            return;
        }

        // Confirm before processing
        System.out.printf("%n  You selected %d refund(s). Confirm? (yes/no): ", selected.size());
        String confirm = Utils.readLine("").trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("  Refund cancelled.");
            return;
        }

        // Process each selected refund
        System.out.println();
        int successCount = 0;
        double totalRefunded = 0;

        for (Reservation r : selected) {
            PaymentGateway gw = gateways.stream()
                    .filter(g -> g.getName().equalsIgnoreCase(r.getGateway()) && g.isEnabled())
                    .findFirst().orElse(null);

            double refAmt = r.getPaidAmount();

            if (gw != null) {
                System.out.println(gw.processRefund(refAmt));
            } else {
                System.out.printf("  ✔ Manual refund of $%.2f processed for %s.%n",
                        refAmt, r.getFullName());
            }

            r.setStatus(Reservation.Status.REFUNDED);
            r.setPayStatus(Reservation.PayStatus.REFUNDED);

            Room room = findRoomForCancellation(r.getRoomId());
            if (room != null) room.increaseAvailable();
            successCount++;
            totalRefunded += refAmt;
            System.out.printf("  ✔ Refunded: %-10s | %-20s | $%.2f%n",
                    r.getResId(), r.getFullName(), refAmt);
        }

        save();
        // Summary
        System.out.println();
        Utils.line();
        System.out.printf("  Refunds processed : %d%n", successCount);
        System.out.printf("  Total refunded    : $%.2f%n", totalRefunded);
        Utils.line();
        System.out.println();
    }

    private void managerViewFeedback() {
        Utils.header("CUSTOMER FEEDBACK");
        if (feedback.isEmpty()) { System.out.println("  No feedback submitted yet.\n"); return; }
        System.out.println();
        feedback.forEach(System.out::println);
        System.out.println();
    }

    // ════════════════════════════════════════════════
    //  CUSTOMER DASHBOARD
    // ════════════════════════════════════════════════
    private void customerDash() {
        boolean on = true;
        while (on) {
            System.out.println(currentUser.menuText());
            switch (Utils.readLine("Choice")) {
                case "1" -> customerSearch();
                case "2" -> displayRooms(true);
                case "3" -> customerBook();
                case "4" -> customerMyRes();
                case "5" -> customerCancel();
                case "6" -> customerPayBalance();
                case "7" -> customerFeedback();
                case "8" -> { currentUser = null; on = false; System.out.println("  Logged out.\n"); }
                default  -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private void customerSearch() {
        Utils.header("SEARCH ROOMS");
        System.out.println("  1. By type only   2. By price only   3. By type AND price");
        String opt = Utils.readLine("Search option").trim();
        List<Room> result = new ArrayList<>(rooms);
        try {
            if (opt.equals("1") || opt.equals("3")) {
                System.out.println("  Types: Standard / Deluxe / Suite");
                String type = Utils.readLine("Room type");
                result = result.stream()
                        .filter(r -> r.getTypeName().equalsIgnoreCase(type.trim()))
                        .collect(Collectors.toList());
            }
            if (opt.equals("2") || opt.equals("3")) {
                double maxP = Double.parseDouble(Utils.readLine("Maximum price per night ($)"));
                result = result.stream().filter(r -> r.getPrice() <= maxP).collect(Collectors.toList());
            }
        } catch (NumberFormatException e) { System.out.println("  [!] Invalid number."); return; }

        System.out.println("\n  Search Results:\n");
        List<Room> avail = result.stream().filter(Room::hasAvailableRoom).collect(Collectors.toList());
        if (avail.isEmpty()) System.out.println("  No available rooms match your criteria.");
        else avail.forEach(r -> System.out.println(r.display() + "\n"));
        System.out.println();
    }

    private void customerBook() {
        Utils.header("BOOK A ROOM");
        long active = res.stream()
                .filter(r -> r.getCustomerId().equals(currentUser.getId())
                        && r.getStatus() == Reservation.Status.CONFIRMED)
                .count();
        if (active >= 3) {
            System.out.println("  [!] You have reached the maximum of 3 active reservations.\n"); return;
        }
        displayRooms(false);
        try {
            // ── Guest details ───────────────────────────
            Utils.header("GUEST INFORMATION");
            String fullName = Utils.readLine("Full Name");
            if (fullName.isBlank()) throw new ValidationException("Full name cannot be empty.");
            String[] nameparts=fullName.trim().split("\\s+");
            if(nameparts.length<2) throw new ValidationException("Please Enter First name and last name.");
            int age = Integer.parseInt(Utils.readLine("Age"));
            if (age <= 17) throw new ValidationException("You must be older than 17 to make a reservation.");
            String fin = Utils.readLine("FIN Number (12 digits)");
            if (!fin.matches("\\d{12}"))
                throw new ValidationException("FIN must be exactly 12 digits.");

// ── FIN uniqueness check ──────────────────────────────
// Check if this FIN is already used by someone else
            for (Reservation existing : res) {
                boolean sameFIN  = existing.getFinNumber().equals(fin);
                boolean sameName = existing.getFullName().equalsIgnoreCase(fullName.trim());

                if (sameFIN && !sameName) {
                    // FIN belongs to a different person — reject
                    throw new ValidationException(
                            "FIN " + fin + " is already registered under a different name. " +
                                    "Each FIN number belongs to one person only.");
                }

                if (!sameFIN && sameName) {
                    // Same name but different FIN — reject
                    throw new ValidationException(
                            "The name '" + fullName.trim() + "' is already registered with a different FIN number. " +
                                    "Please use your FIN number.");
                }
            }
// If we reach here: either FIN+Name are both new, or both match existing — both are OK

            String phone = Utils.readLine("Phone Number");
            if (phone.isBlank() || phone.length() != 10)
                throw new ValidationException("Phone number must be 10 digits.");
            for (Reservation existing : res) {
                boolean samePhone  = existing.getPhone().equals(phone);
                boolean sameName = existing.getFullName().equalsIgnoreCase(fullName.trim());

                if (samePhone && !sameName) {
                    throw new ValidationException(
                            "PHONE " + phone + " is already registered under a different name. " +
                                    "Each Phone number belongs to one person only.");
                }

                if (!samePhone && sameName) {
                    // Same name but different FIN — reject
                    throw new ValidationException(
                            "The name '" + fullName.trim() + "' is already registered with a different FIN number. " +
                                    "Please use your FIN number.");
                }
            }
            int adults   = Integer.parseInt(Utils.readLine("Number of Adults"));
            if (adults < 0) throw new ValidationException("Number of Adult cannot be negative.");
            int children = Integer.parseInt(Utils.readLine("Number of Children"));
            if (children < 0) throw new ValidationException("Children cannot be negative.");

            // ── Room selection ──────────────────────────
            String roomId = Utils.readLine("Room ID to book").toUpperCase().trim();
            Room room = findRoom(roomId);
            if (room == null) throw new RoomException("Room '" + roomId + "' not found.");
            if (!room.hasAvailableRoom()) throw new RoomException("No available units for room " + roomId + ".");
            String assignedRoomId=room.bookOneUnit();

            LocalDate ci = Utils.parseDate(Utils.readLine("Check-in date  (yyyy-MM-dd)"));
            LocalDate co = Utils.parseDate(Utils.readLine("Check-out date (yyyy-MM-dd)"));
            if (!ci.isBefore(co))       throw new ValidationException("Check-out must be after check-in.");
            if (ci.isBefore(LocalDate.now())) throw new ValidationException("Check-in date cannot be in the past.");

            int    nights = (int)(co.toEpochDay() - ci.toEpochDay());
            double total  = room.totalCost(nights); // polymorphic call

            // ── Cost breakdown display ──────────────────────────
            double baseCost       = room.baseCost(nights);
            double amenitiesTotal = room.totalServiceCost(nights);

            System.out.println();
            Utils.line();
            System.out.printf("  Room            : %s (%s)%n",
                    assignedRoomId, room.getTypeName());
            System.out.printf("  Price/night     : $%.2f  ×  %d nights%n",
                    room.getPrice(), nights);
            Utils.line();
            System.out.printf(     " Room Cost      : $%.2f%n", baseCost);

            if (amenitiesTotal > 0) {
                System.out.printf("  Services       : %s%n",
                        room.serviceChargeLebel());
                System.out.printf("  Service Charge: $%.2f%n", amenitiesTotal);
            } else {
                System.out.printf("  Services       : %s%n",
                        room.serviceChargeLebel());
            }

            Utils.line();
            System.out.printf("  TOTAL COST      : $%.2f%n", total);
            Utils.line();
            System.out.printf("  PARTIAL (30%%)   : $%.2f  |  Full amount: $%.2f%n%n",
                    total * 0.30, total);
            // ── Payment option ──────────────────────────
            System.out.println("  Payment option");
            System.out.println("  1. Pay full amount now");
            System.out.println("  2. Pay deposit (30%) now — Pay Rest at check-in");

            // Keep asking until a valid option is entered
            String payOpt;
            while (true) {
                payOpt = Utils.readLine(" Choose Payment option ").trim();
                if (payOpt.equals("1") || payOpt.equals("2")) {
                    break;  // valid input — exit loop
                }
                System.out.println("  [!] Invalid option. Please enter 1 or 2 only.");
            }

            boolean deposit = payOpt.equals("2");
            double  paying  = deposit ? total * 0.30 : total;
            // ── Gateway ─────────────────────────────────
            List<PaymentGateway> enabled = gateways.stream()
                    .filter(PaymentGateway::isEnabled).collect(Collectors.toList());
            if (enabled.isEmpty()) throw new PaymentException("No payment gateways are currently enabled.");
            System.out.println("\n  Available Payment Gateways:");
            for (int i = 0; i < enabled.size(); i++)
                System.out.println("  " + (i+1) + ". " + enabled.get(i).getName());

            int gwIdx = Integer.parseInt(Utils.readLine("Select gateway number")) - 1;
            if (gwIdx < 0 || gwIdx >= enabled.size()) throw new PaymentException("Invalid gateway selection.");
            PaymentGateway gw = enabled.get(gwIdx);
            System.out.printf("%n  Confirm: Book room %s for %d nights and pay $%.2f via %s? (yes/no) ",
                    roomId, nights, paying, gw.getName());
            String conf = Utils.readLine("").trim();
            if (!conf.equalsIgnoreCase("yes")) { System.out.println("  Booking cancelled."); return; }
            // ── Create reservation ──────────────────────
            String resId = Utils.genId("RES", res.size());

            Reservation booking = new Reservation(resId, currentUser.getId(), assignedRoomId, ci, co, total, fullName,
                    age, fin, phone, adults, children, gw.getName(), room.baseCost(nights),
                    room.totalServiceCost(nights), room.serviceChargeLebel());// amenities label
            booking.setInitialPaidAmount(paying);
            res.add(booking);
            //   room.decreaseAvailable();
            pending.add(resId + "|" + paying + "|" + (deposit ? "DEPOSIT" : "FULL"));
            save();


            System.out.println(gw.processPayment(paying));
            System.out.println("   Your payment has been submitted and is pending admin approval.");
            System.out.println(booking.receipt(currentUser.getUsername()));

        } catch (ValidationException | RoomException | PaymentException e) {
            System.out.println("  [!] " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid number entered.");
        }
    }

    private void customerMyRes() {
        Utils.header("MY RESERVATIONS");
        List<Reservation> mine = res.stream()
                .filter(r -> r.getCustomerId().equals(currentUser.getId()))
                .collect(Collectors.toList());
        if (mine.isEmpty()) {
            System.out.println("  You have no reservations.\n");
            return;
        }
        System.out.println();
        for (Reservation r : mine) {
            // Use same logic as receipt() — paidAmount if approved, else initialPaidAmount
            double shown   = (r.getPaidAmount() > 0)
                    ? r.getPaidAmount()
                    : r.getInitialPaidAmount();
            double balance = r.getTotalCost() - shown;
            boolean partial = (shown < r.getTotalCost() - 0.001);

            // Payment status label — same logic as receipt()
            String payLabel;
            if (r.getPayStatus() == Reservation.PayStatus.DEPOSIT_PAID) {
                payLabel = "PARTIALLY PAID";
            } else if (r.getPayStatus() == Reservation.PayStatus.FULLY_PAID) {
                payLabel = "FULLY PAID";
            } else if (r.getPayStatus() == Reservation.PayStatus.REFUNDED) {
                payLabel = "REFUNDED";
            } else {
                payLabel = partial
                        ? "PARTIALLY PAID (Pending Approval)"
                        : "FULLY PAID (Pending Approval)";
            }

            // Booking status label
            String statusLabel = switch (r.getStatus()) {
                case CONFIRMED        -> "CONFIRMED";
                case CANCELLED        -> "CANCELLED";
                case REFUND_REQUESTED -> "REFUND REQUESTED";
                case REFUNDED         -> "REFUNDED";
                case CHECKED_OUT      -> "CHECKED OUT";
            };

            System.out.println(

                    " ─────────────────────────────────────────────────────");
            System.out.printf("  Reservation ID  : %s%n",    r.getResId());
            System.out.printf("  Room            : %s%n",    r.getRoomId());
            System.out.printf("  Guest Name      : %s%n",    r.getFullName());
            System.out.printf("  Check-In        : %s%n",    r.getCheckIn());
            System.out.printf("  Check-Out       : %s%n",    r.getCheckOut());
            System.out.printf("  Nights          : %d%n",    r.nights());
            System.out.printf("  Room Cost  : $%.2f%n", r.getBaseRoomCost());
            System.out.printf("  Service       : %s%n",    r.getServiceLabel());
            System.out.printf("  Service Charge: $%.2f%n", r.getServiceCharge());
            System.out.printf("  Total Cost      : $%.2f%n", r.getTotalCost());
            System.out.printf("  Amount Paid     : $%.2f%n", shown);
            System.out.printf("  Balance Due     : $%.2f%n", balance);
            System.out.printf("  Payment Status  : %s%n",    payLabel);
            System.out.printf("  Booking Status  : %s%n",    statusLabel);
            System.out.printf("  Gateway         : %s%n",    r.getGateway());
            System.out.println();
        }
    }

    private void customerCancel() {
        Utils.header("CANCEL & REQUEST REFUND");
        customerMyRes();

        boolean hasReservations=res.stream().anyMatch(r->r.getCustomerId().equals(currentUser.getId()));
        if(!hasReservations){System.out.println("  [!] Reservation not found.");return;}

        String rid = Utils.readLine("Reservation ID to cancel").toUpperCase().trim();
        Reservation r = res.stream()
                .filter(x -> x.getResId().equals(rid)
                        && x.getCustomerId().equals(currentUser.getId()))
                .findFirst().orElse(null);

        if (res == null) {
            System.out.println("  [!] Reservation not found."); return;
        }
        if (r.getStatus() == Reservation.Status.CANCELLED  ||
                r.getStatus() == Reservation.Status.REFUNDED) {
            System.out.println("  [!] Already cancelled or refunded."); return;
        }

        // ── Restore room availability and ID ──────────────
        Room room = findRoomForCancellation(r.getRoomId());
        if (room != null) {
            room.increaseAvailable();
        } else {
            System.out.println("  [!] Warning: room not found for ID restore.");
        }

        // ── Set reservation status ────────────────────────
        if (r.getPaidAmount() > 0) {
            r.setStatus(Reservation.Status.REFUND_REQUESTED);
            System.out.printf("  ✔ Reservation %s cancelled.%n", rid);
            System.out.printf("  ✔ Refund of $%.2f requested — manager will process.%n",
                    r.getPaidAmount());
        } else {
            r.setStatus(Reservation.Status.CANCELLED);
            System.out.printf("  ✔ Reservation %s cancelled. No payment to refund.%n", rid);
        }

        save();
    }
    private void customerPayBalance() {
        Utils.header("PAY REMAINING BALANCE");
        List<Reservation> balDue = res.stream()
                .filter(r -> r.getCustomerId().equals(currentUser.getId())
                        && r.getPayStatus() == Reservation.PayStatus.DEPOSIT_PAID)
                .collect(Collectors.toList());
        if (balDue.isEmpty()) { System.out.println("  No reservations with outstanding balance.\n"); return; }
        System.out.println();
        balDue.forEach(r -> System.out.printf("  %s | Room: %s | Balance due: $%.2f%n",
                r.getResId(), r.getRoomId(), r.remaining()));
        System.out.println();
        String rid = Utils.readLine("Reservation ID").toUpperCase().trim();
        Reservation r = balDue.stream().filter(x -> x.getResId().equals(rid)).findFirst().orElse(null);
        if (r == null) { System.out.println("  [!] Not found."); return; }

        List<PaymentGateway> enabled = gateways.stream().filter(PaymentGateway::isEnabled).collect(Collectors.toList());
        if (enabled.isEmpty()) { System.out.println("  [!] No gateways available."); return; }
        System.out.println("  Gateways:");
        for (int i = 0; i < enabled.size(); i++) System.out.println("  " + (i+1) + ". " + enabled.get(i).getName());
        try {
            int idx = Integer.parseInt(Utils.readLine("Gateway number")) - 1;
            if (idx < 0 || idx >= enabled.size()) { System.out.println("  [!] Invalid."); return; }
            PaymentGateway gw = enabled.get(idx);
            double bal = r.remaining();
            String conf = Utils.readLine(String.format("Pay $%.2f via %s? (yes/no)", bal, gw.getName()));
            if (!conf.equalsIgnoreCase("yes")) { System.out.println("  Cancelled."); return; }
            pending.add(r.getResId() + "|" + bal + "|FULL");
            save();
            System.out.println(gw.processPayment(bal));
            System.out.println("  Balance payment submitted — pending admin approval.");
        } catch (NumberFormatException e) { System.out.println("  [!] Invalid input."); }
    }

    private void customerFeedback() {
        Utils.header("SUBMIT FEEDBACK");

        // ── Check if customer has any reservations first ──────
        List<Reservation> myRes = res.stream()
                .filter(r -> r.getCustomerId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        if (myRes.isEmpty()) {
            System.out.println("  [!] You have no reservations.");
            System.out.println("  [!] You can only submit feedback after making a booking.");
            System.out.println();
            return;
        }

        // ── Show reservations and ask which one ───────────────
        System.out.println("  Your Reservations:\n");
        for (int i = 0; i < myRes.size(); i++) {
            Reservation r = myRes.get(i);
            System.out.printf("  [%d] %-10s | Room: %-6s | %s → %s | [%s]%n",
                    i + 1,
                    r.getResId(),
                    r.getRoomId(),
                    r.getCheckIn(),
                    r.getCheckOut(),
                    r.getStatus());
        }
        System.out.println();

        // ── Ask which reservation the feedback is for ─────────
        Reservation selectedRes = null;
        while (true) {
            String inp = Utils.readLine(
                            "Enter reservation number to give feedback for (1-" + myRes.size() + ")")
                    .trim();
            try {
                int idx = Integer.parseInt(inp) - 1;
                if (idx >= 0 && idx < myRes.size()) {
                    selectedRes = myRes.get(idx);
                    break;
                }
                System.out.println("  [!] Please enter a number between 1 and " + myRes.size());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a number.");
            }
        }

        // ── Check if already submitted feedback for this reservation ──
        final String resId = selectedRes.getResId();
        boolean alreadyGiven = feedback.stream()
                .anyMatch(f -> f.getReservationId().equals(resId));
        if (alreadyGiven) {
            System.out.println("  [!] You have already submitted feedback for reservation "
                    + resId + ".");
            System.out.println();
            return;
        }

        // ── Collect feedback message ──────────────────────────
        String msg = Utils.readLine("Your message (comments / suggestions)");
        if (msg.isBlank()) {
            System.out.println("  [!] Feedback cannot be empty.");
            return;
        }

        // ── Collect rating with validation loop ───────────────
        int rating;
        while (true) {
            try {
                rating = Integer.parseInt(
                        Utils.readLine("Rating (1=Poor  2=Fair  3=Good  4=Very Good  5=Excellent)"));
                if (rating >= 1 && rating <= 5) break;
                System.out.println("  [!] Rating must be between 1 and 5.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a number 1-5.");
            }
        }

        // ── Save feedback ─────────────────────────────────────
        String fid = Utils.genId("FB", feedback.size());
        feedback.add(new Feedback(
                fid,
                currentUser.getId(),
                currentUser.getUsername(),
                resId,          // linked to specific reservation
                msg,
                rating,
                LocalDate.now()
        ));
        save();

        System.out.println();
        System.out.println("  ✔ Thank you! Your feedback has been sent to the manager.");
        System.out.printf ("  ✔ Feedback ID : %s | Reservation : %s%n%n", fid, resId);
    }

// ── Auto-checkout sweep ──────────────────────────────────
// Runs on login and startup — checks all confirmed reservations
// whose checkout date has passed and frees the room automatically
    private void checkoutSweep() {
        LocalDate today = LocalDate.now();
        boolean   changed = false;

        for (Reservation r : res) {
            // Only process CONFIRMED reservations whose checkout date
            // is today or in the past
            if (r.getStatus() == Reservation.Status.CONFIRMED
                    && !r.getCheckOut().isAfter(today)) {

                // Only free the room if payment is settled
                // (fully paid or deposit paid — not unpaid)
                if (r.getPayStatus() == Reservation.PayStatus.FULLY_PAID
                        || r.getPayStatus() == Reservation.PayStatus.DEPOSIT_PAID) {

                    // Mark reservation as CHECKED OUT
                    r.setStatus(Reservation.Status.CHECKED_OUT);

                    // Free the room
                    Room room = findRoomForCancellation(r.getRoomId());
                    if (room != null) room.increaseAvailable();

                    System.out.printf(
                            "  [AUTO-CHECKOUT] Reservation %-10s | Room %-6s | %s checked out.%n",
                            r.getResId(), r.getRoomId(), r.getFullName());

                    changed = true;
                }
            }
        }

        if (changed) {
            save();
            System.out.println();
        }
    }
    // ════════════════════════════════════════════════
    //  SHARED HELPERS
    // ════════════════════════════════════════════════
    private void displayRooms(boolean all) {
        checkoutSweep();
        Utils.header(all ? "ALL ROOMS" : "AVAILABLE ROOMS");
        System.out.println();
        List<Room> show = all ? rooms
                : rooms.stream().filter(Room::hasAvailableRoom).collect(Collectors.toList());
        if (show.isEmpty()) System.out.println("  No rooms to display.");
        else show.forEach(r -> System.out.println(r.display() + "\n"));
        System.out.println();
    }

    private void displayAllReservations() {
        Utils.header("ALL RESERVATIONS");
        if (res.isEmpty()) { System.out.println("  No reservations yet.\n"); return; }
        System.out.println();
        res.forEach(System.out::println);
        System.out.println();
    }

    private void printAllUsers() {
        System.out.println("\n--- ALL USERS ---\n");
        System.out.printf("  %-10s | %-12s | %-20s | %s%n", "ID", "ROLE", "USERNAME", "EMAIL");
        System.out.println("  " + "-".repeat(60));
        users.forEach(System.out::println);
        System.out.println();
    }

    // Find by current room ID (for admin/display purposes)
    private Room findRoom(String id) {
        return rooms.stream()
                .filter(r -> r.getRoomId().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }

    // Find by reservation's booked room ID — matches by floor + type
// because after bookings the current room ID has changed
    private Room findRoomForCancellation(String bookedRoomId) {
        // First try exact match (works if no bookings happened after)
        Room exact = findRoom(bookedRoomId);
        if (exact != null) return exact;

        // Extract floor number from booked ID e.g. "R104" → floor digit is "1"
        // Compare floor numbers between booked ID and current room IDs
        String bookedPrefix = extractLetterPrefix(bookedRoomId);
        int    bookedNumber = extractNumberPart(bookedRoomId);
        int    bookedFloor  = bookedNumber / 100; // e.g. 104 / 100 = 1

        for (Room room : rooms) {
            String currentPrefix = extractLetterPrefix(room.getRoomId());
            int    currentNumber = extractNumberPart(room.getRoomId());
            int    currentFloor  = currentNumber / 100;

            // Match by same prefix letter and same floor
            if (currentPrefix.equalsIgnoreCase(bookedPrefix)
                    && currentFloor == bookedFloor) {
                return room;
            }
        }
        return null;
    }

    // Helper: extract letter prefix e.g. "R104" → "R"
    private String extractLetterPrefix(String id) {
        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isLetter(c)) sb.append(c);
            else break;
        }
        return sb.toString();
    }

    // Helper: extract number part e.g. "R104" → 104
    private int extractNumberPart(String id) {
        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }

    private Reservation findRes(String id) {
        return res.stream().filter(r -> r.getResId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private Manager findManager() {
        return (Manager) users.stream().filter(u -> u instanceof Manager).findFirst().orElse(null);
    }

    // ════════════════════════════════════════════════
    //  LOAD / SAVE / SEED
    // ════════════════════════════════════════════════
    private void load() {
        try {
            users    = FileStore.loadUsers();
            rooms    = FileStore.loadRooms();
            res      = FileStore.loadReservations();
            feedback = FileStore.loadFeedback();
            gateways = FileStore.loadGateways();
            pending  = FileStore.loadPending();
        } catch (IOException e) {
            System.out.println("  [!] Load error (starting fresh): " + e.getMessage());
        }
    }

    private void save() {
        try {
            FileStore.saveUsers(users);
            FileStore.saveRooms(rooms);
            FileStore.saveReservations(res);
            FileStore.saveFeedback(feedback);
            FileStore.saveGateways(gateways);
            FileStore.savePending(pending);
        } catch (IOException e) {
            System.out.println("  [!] Save error: " + e.getMessage());
        }
    }

    private void seed() {
        // Default admin
        if (users.stream().noneMatch(u -> u.getRole().equals("ADMIN"))) {
            try { users.add(new Admin("A0001", "admin", "Admin@123", "admin@hotel.com")); }
            catch (ValidationException ignored) {}
            System.out.println("  [*] Default admin  →  username: admin  |  password: Admin@123");
        }

        // Default gateways
        if (gateways.isEmpty()) {
            gateways.add(new CBEGateway("CBE", true));
            gateways.add(new TelebirrGateway("Telebirr", true));
            gateways.add(new AwashGateway("Awash Bank", true));
        }
        save();
    }
}
