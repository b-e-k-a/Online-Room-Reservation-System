// ═══════════════════════════════════════════════════════
//  USER HIERARCHY
// ═══════════════════════════════════════════════════════
abstract class User {
    private final String id;
    private String username;
    private String hashedPassword;
    private String email;

    public User(String id, String username, String plainPwd, String email) throws ValidationException {
        validateFields(username, plainPwd, email);
        this.id = id; this.username = username;
        this.hashedPassword = Utils.hash(plainPwd); this.email = email;
    }
    public User(String id, String username, String hashedPwd, String email, boolean hashed) {
        this.id = id; this.username = username;
        this.hashedPassword = hashedPwd; this.email = email;
    }

    private static void validateFields(String u, String p, String e) throws ValidationException {
        if (u == null || u.length() < 3)      throw new ValidationException("Username must be ≥3 characters");
        if (!u.matches("[a-zA-Z0-9_]+"))       throw new ValidationException("Username: only letters, digits, underscore");
        if (p == null || p.length() < 6)      throw new ValidationException("Password must be ≥6 characters");
        if (e == null || !e.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"))
            throw new ValidationException("Invalid email format");
    }

    public boolean verifyPassword(String plain) { return hashedPassword.equals(Utils.hash(plain)); }
    public void resetPassword(String newPlain) throws ValidationException {
        if (newPlain == null || newPlain.length() < 6)
            throw new ValidationException("New password must be ≥6 characters");
        this.hashedPassword = Utils.hash(newPlain);
    }

    public abstract String getRole();
    public abstract String menuText();

    public String getId()             { return id; }
    public String getUsername()       { return username; }
    public String getEmail()          { return email; }
    public String getHashedPassword() { return hashedPassword; }
    public void   setUsername(String u) { this.username = u; }
    public void   setEmail(String e)    { this.email = e; }

    public String toFileLine() {
        return id + "|" + username + "|" + hashedPassword + "|" + email + "|" + getRole();
    }

    @Override public String toString() {
        return String.format("  [%-8s] ID:%-8s  User:%-16s Email:%s",
                getRole(), id, username, email);
    }
}

// ── ADMIN ────────────────────────────────────────────
class Admin extends User {
    public Admin(String id, String u, String p, String e) throws ValidationException { super(id,u,p,e); }
    public Admin(String id, String u, String hp, String e, boolean h)               { super(id,u,hp,e,h); }
    @Override public String getRole() { return "ADMIN"; }
    @Override public String menuText() {
        return """
          
          ╔════════════════════════════════════════════════════════╗
          ║                  ADMIN DASHBOARD                      ║
          ╠════════════════════════════════════════════════════════╣
          ║   1.  Create Manager Account                   ║
          ║   2.  Update Manager Account                          ║
          ║   3.  Reset Any User Password                         ║
          ║   4.  Add Room Type                                   ║
          ║   5.  Edit Room                                       ║
          ║   6.  Delete Room                                     ║
          ║   7.  View All Rooms                                  ║
          ║   8.  Cancel Any Booking                              ║
          ║   9.  View All Reservations                           ║
          ║  10.  View All Payments                               ║
          ║  11.  Approve Pending Payment                         ║
          ║  12.  Update Payment Gateways                         ║
          ║  13.  View All Users                                  ║
          ║  0.   Logout                                          ║
          ╚════════════════════════════════════════════════════════╝""";
    }
}

// ── MANAGER ──────────────────────────────────────────
class Manager extends User {
    public Manager(String id, String u, String p, String e) throws ValidationException { super(id,u,p,e); }
    public Manager(String id, String u, String hp, String e, boolean h)               { super(id,u,hp,e,h); }
    @Override public String getRole() { return "MANAGER"; }
    @Override public String menuText() {
        return """

          ╔════════════════════════════════════════════════════════╗
          ║                 MANAGER DASHBOARD                     ║
          ╠════════════════════════════════════════════════════════╣
          ║   1.  View All Customer Reservations                  ║
          ║   2.  Update Room Status                              ║
          ║   3.  Change Room ID                                  ║
          ║   4.  Process Customer Refund                         ║
          ║   5.  View Customer Feedback                          ║
          ║   6.  Logout                                          ║
          ╚════════════════════════════════════════════════════════╝""";
    }
}

// ── CUSTOMER ─────────────────────────────────────────
class Customer extends User {
    public Customer(String id, String u, String p, String e) throws ValidationException { super(id,u,p,e); }
    public Customer(String id, String u, String hp, String e, boolean h)               { super(id,u,hp,e,h); }
    @Override public String getRole() { return "CUSTOMER"; }
    @Override public String menuText() {
        return """

          ╔════════════════════════════════════════════════════════╗
           ║                CUSTOMER DASHBOARD                    ║
          ╠════════════════════════════════════════════════════════╣
          ║   1.  Search Rooms (by type)                          ║
          ║   2.  Show Available Rooms                            ║
          ║   3.  Book a Room                                     ║
          ║   4.  View My Reservations                            ║
          ║   5.  Cancel Reservation & Request Refund             ║
          ║   6.  Pay Remaining Balance                           ║
          ║   7.  Submit Feedback                                 ║
          ║   8.  Logout                                          ║
          ╚════════════════════════════════════════════════════════╝""";
    }
}
