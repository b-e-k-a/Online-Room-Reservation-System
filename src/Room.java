// ═══════════════════════════════════════════════════════
//  ROOM TYPE  — represents a category with quantity pool
//  (Inheritance + Polymorphism)
// ═══════════════════════════════════════════════════════
abstract class Room {
    public enum Status { AVAILABLE, OCCUPIED, OUT_OF_SERVICE,ALL_ARE_OCCUPIED }
    private String roomId;        // e.g. R101
    private int    floor;
    private double pricePerNight;
    private int    totalQty;      // total rooms of this type/id
    private int    availableQty;  // decreases on booking
    private Status status;
    // Base cost without any amenities
    public double baseCost(int nights) {
        return getPrice() * nights;
    }

    // Total amenities charge for all nights
    public double totalServiceCost(int nights) {
        return serviceCostPerNight() * nights;
    }

    public Room(String roomId, int floor, double price, int qty) {
        this.roomId       = roomId;
        this.floor        = floor;
        this.pricePerNight = price;
        this.totalQty     = qty;
        this.availableQty = qty;
        this.status       = Status.AVAILABLE;
    }

    // ── Abstract (Polymorphism) ──────────────────────
    public abstract String getTypeName();
    public abstract String getAmenities();
    public abstract double serviceCostPerNight();
    public abstract String serviceChargeLebel();

    /** Polymorphic cost — subclasses can override */
    public double totalCost(int nights) { return pricePerNight * nights; }
    //public double serviceCost(int nights) { return pricePerNight * nights; }

    // ── Booking helpers ──────────────────────────────
    public boolean hasAvailableRoom() {
        return status == Status.AVAILABLE && availableQty > 0;
    }
    public void decreaseAvailable() { if (availableQty > 0) availableQty--; }
    // public void increaseAvailable() { if (availableQty < totalQty) availableQty++; }

    // ── Getters / Setters ────────────────────────────
    public String getRoomId()           { return roomId; }
    public void   setRoomId(String id)  { this.roomId = id; }
    public int    getFloor()            { return floor; }
    public void   setFloor(int f)       { this.floor = f; }
    public double getPrice()            { return pricePerNight; }
    public void   setPrice(double p)    { this.pricePerNight = p; }
    public int    getTotalQty()         { return totalQty; }
    public void   setTotalQty(int q)    { this.totalQty = q; }
    public int    getAvailableQty()     { return availableQty; }
    public void   setAvailableQty(int q){ this.availableQty = q; }
    public Status getStatus()           { return status; }
    public void   setStatus(Status s)   { this.status = s; }

    public String bookOneUnit() {
        if (!hasAvailableRoom()) return null;

        String assignedRoomId = this.roomId;  // customer gets current ID

        availableQty--;

        if (availableQty > 0) {
            // Decrement room ID — e.g. R104 → R103
            this.roomId = decrementRoomId(this.roomId);
        } else {
            // Last unit booked — mark fully occupied
            this.status = Status.OCCUPIED;
        }

        return assignedRoomId;
    }
    public void increaseAvailable() {
        // Always increment — no guard check needed
        // because a cancellation always means one unit is coming back
        availableQty++;

        // Also bring totalQty back if it somehow drifted
        if (availableQty > totalQty) totalQty = availableQty;

        // Increment room ID immediately
        // e.g. R103 → R104
        this.roomId = incrementRoomId(this.roomId);

        // Restore status to AVAILABLE immediately
        this.status = Status.AVAILABLE;
    }

    // ── Helper: extract letter prefix ───────────────────────
// e.g. "R104" → "R",   "AB205" → "AB"
    private String extractPrefix(String id) {
        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isLetter(c)) sb.append(c);
            else break;
        }
        return sb.toString();
    }

    // ── Helper: extract number part ─────────────────────────
// e.g. "R104" → 104,   "R99" → 99
    private int extractNumber(String id) {
        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }

    // ── Helper: increment room ID ───────────────────────────
// e.g. "R103" → "R104",  "AB209" → "AB210"
    private String incrementRoomId(String id) {
        String prefix  = extractPrefix(id);
        int    number  = extractNumber(id);
        int    digits  = String.valueOf(number).length();
        String fmt     = "%0" + digits + "d";
        return prefix + String.format(fmt, number + 1);
    }

    // ── Helper: decrement room ID ───────────────────────────
// e.g. "R104" → "R103",  "AB210" → "AB209"
    private String decrementRoomId(String id) {
        String prefix  = extractPrefix(id);
        int    number  = extractNumber(id);
        int    digits  = String.valueOf(number).length();
        String fmt     = "%0" + digits + "d";
        return prefix + String.format(fmt, number - 1);
    }

    public String toFileLine() {
        return String.join("|", roomId, getTypeName(), String.valueOf(floor),
                String.valueOf(pricePerNight), String.valueOf(totalQty),
                String.valueOf(availableQty), status.name());
    }

    public String display() {
        return String.format(
                "  %-7s | %-10s | Floor %-2d | $%7.2f/night | Total:%-3d | Avail:%-3d | [%s]%n  %16s Services: %s",
                roomId, getTypeName(), floor, pricePerNight,
                totalQty, availableQty, status, "", getAmenities());
    }
}

// ── Standard ─────────────────────────────────────────
class StandardRoom1 extends Room {
    public StandardRoom1(String id, int fl, double p, int qty) { super(id, fl, p, qty); }
    @Override public String getTypeName()       { return "Standard"; }
    @Override public String getAmenities()       { return "WiFi, TV, AC"; }
    @Override public  double serviceCostPerNight(){return 0.00;}
    @Override public  String serviceChargeLebel() { return "No Extra serviceCharge";}

}

class Family extends Room {
    public Family(String id, int fl, double p, int qty) { super(id, fl, p, qty); }
    @Override public String getTypeName()       { return "Family"; }
    @Override public String getAmenities()       { return "WiFi, TV, AC, Multiple Bed Room"; }
    @Override public  double serviceCostPerNight(){return 0.00;}
    @Override public  String serviceChargeLebel() { return "No Extra serviceCharge";}
}

// ── Deluxe ───────────────────────────────────────────
class Premium extends Room {
    private static final double Mini_Bar = 35.0;
    public Premium(String id, int fl, double p, int qty) { super(id, fl, p, qty); }
    @Override public String getTypeName()       { return "Premium"; }
    @Override public String getAmenities()       { return "WiFi, TV, AC, Mini-Bar, Sea View"; }
    @Override public double totalCost(int nights){ return getPrice() * nights * 1.10; } // 10% premium
    //@Override public double serviceCost(int nights){ return Mini_Bar; }
    @Override public  double serviceCostPerNight(){return getPrice() * 0.10;} //10%
    @Override public  String serviceChargeLebel() { return String.format("Premium (10%% of s%.2f/night)", getPrice());}
}

// ── Suite ────────────────────────────────────────────
class SuiteRoom1 extends Room {
    private static final double BREAKFAST = 25.0;
    public SuiteRoom1(String id, int fl, double p, int qty) { super(id, fl, p, qty); }
    @Override public String getTypeName()       { return "Suite"; }
    //@Override public int    getCapacityPerRoom() { return 6; }
    @Override public String getAmenities()       { return "WiFi, TV, AC, Jacuzzi, Lounge, Breakfast."; }
    @Override public double totalCost(int nights){ return (getPrice() + BREAKFAST) * nights; }
    // @Override public double serviceCost(int nights){ return (BREAKFAST) ; }
    @Override public  double serviceCostPerNight(){return BREAKFAST;} //10%
    @Override public  String serviceChargeLebel(){ return "Breakfast cost (25$/night)";}
}
