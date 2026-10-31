import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    public enum Status { CONFIRMED, CANCELLED, REFUND_REQUESTED, REFUNDED, CHECKED_OUT }
    public enum PayStatus { DEPOSIT_PAID,UNPAID, FULLY_PAID, REFUNDED }

    private final String resId;
    private final String customerId;
    private String roomId;
    private final LocalDate checkIn, checkOut;
    private final double totalCost;
    private double paidAmount;
    private double initialPaidAmount;
    private Status    status;
    private PayStatus payStatus;
    private String receiptDateTime;

    // Guest details
    private final String fullName;
    private final int    age;
    private final String finNumber;
    private final String phone;
    private final int    adults;
    private final int    children;
    private final String gateway;
    private double baseRoomCost=0;
    private double serviceCharge=0;
    private String serviceLabel="N/A";

    // Full constructor (new booking)
    public Reservation(String resId, String customerId, String roomId,
                       LocalDate checkIn, LocalDate checkOut, double totalCost,
                       String fullName, int age, String finNumber, String phone,
                       int adults, int children, String gateway, double baseRoomCost, double  serviceCharge,String serviceLabel) {
        this.resId          = resId;
        this.customerId     = customerId;
        this.roomId         = roomId;
        this.checkIn        = checkIn;
        this.checkOut       = checkOut;
        this.totalCost      = totalCost;
        this.paidAmount     = 0;
        this.initialPaidAmount=0;
        this.status         = Status.CONFIRMED;
        this.payStatus      = PayStatus.UNPAID;
        this.fullName       = fullName;
        this.age            = age;
        this.finNumber      = finNumber;
        this.phone          = phone;
        this.adults         = adults;
        this.children       = children;
        this.gateway        = gateway;
        this.baseRoomCost   = baseRoomCost;
        this.serviceCharge=serviceCharge;
        this.serviceLabel =serviceLabel;
        this.receiptDateTime = Utils.now();
    }

    // Load from file
    public Reservation(String resId, String customerId, String roomId,
                       LocalDate checkIn, LocalDate checkOut, double totalCost,
                       double paidAmount, Status status, PayStatus payStatus,
                       String fullName, int age, String finNumber, String phone,
                       int adults, int children, String gateway, String receiptDateTime) {
        this.resId=resId; this.customerId=customerId; this.roomId=roomId;
        this.checkIn=checkIn; this.checkOut=checkOut; this.totalCost=totalCost;
        this.paidAmount=paidAmount; this.status=status; this.payStatus=payStatus;
        this.fullName=fullName; this.age=age; this.finNumber=finNumber;
        this.phone=phone; this.adults=adults; this.children=children;
        this.gateway=gateway; this.receiptDateTime=receiptDateTime;
    }

    public int    nights()        { return (int) ChronoUnit.DAYS.between(checkIn, checkOut); }
    public double depositAmount() { return totalCost * 0.30; }
    public double remaining()     { return Math.max(0, totalCost - paidAmount); }

    public void addPayment(double amt)   { this.paidAmount += amt; }
    public void setPayStatus(PayStatus p){ this.payStatus = p; }
    public void setStatus(Status s)      { this.status = s; }
    public void setRoomId(String r)      { this.roomId = r; }
    public void setInitialPaidAmount(double amt) {this.initialPaidAmount=amt;}
    // Getters
    public double getInitialPaidAmount() {return initialPaidAmount;}
    public double getBaseRoomCost() { return baseRoomCost; }
    public double getServiceCharge() { return serviceCharge; }
    public String getServiceLabel()  { return serviceLabel; }
    public String    getResId()          { return resId; }
    public String    getCustomerId()     { return customerId; }
    public String    getRoomId()         { return roomId; }
    public LocalDate getCheckIn()        { return checkIn; }
    public LocalDate getCheckOut()       { return checkOut; }
    public double    getTotalCost()      { return totalCost; }
    public double    getPaidAmount()     { return paidAmount; }
    public Status    getStatus()         { return status; }
    public PayStatus getPayStatus()      { return payStatus; }
    public String    getFullName()       { return fullName; }
    public int       getAge()            { return age; }
    public String    getFinNumber()      { return finNumber; }
    public String    getPhone()          { return phone; }
    public int       getAdults()         { return adults; }
    public int       getChildren()       { return children; }
    public String    getGateway()        { return gateway; }
    public String    getReceiptDateTime(){ return receiptDateTime;
    }

    // ── Serialization ────────────────────────────────────
    public String toFileLine() {
        return String.join("|",
                resId, customerId, roomId, checkIn.format(Utils.DATE_FMT), checkOut.format(Utils.DATE_FMT),
                String.valueOf(totalCost), String.valueOf(paidAmount), status.name(), payStatus.name(), fullName, String.valueOf(age), finNumber, phone,
                String.valueOf(adults), String.valueOf(children), gateway, receiptDateTime,String.valueOf(baseRoomCost), String.valueOf(serviceCharge),serviceLabel.replace("|",";"),String.valueOf(initialPaidAmount));
    }
    public static Reservation fromLine(String line) {
        String[] p = line.split("\\|", -1);

        // Core fields (always present — original format)
        String   resId      = p[0].trim();
        String   customerId = p[1].trim();
        String   roomId     = p[2].trim();
        LocalDate checkIn   = LocalDate.parse(p[3].trim(), Utils.DATE_FMT);
        LocalDate checkOut  = LocalDate.parse(p[4].trim(), Utils.DATE_FMT);
        double   totalCost  = Double.parseDouble(p[5].trim());
        double   paidAmount = Double.parseDouble(p[6].trim());
        Status   status     = Status.valueOf(p[7].trim());
        PayStatus payStatus = PayStatus.valueOf(p[8].trim());
        String   fullName   = p[9].trim();
        int      age        = Integer.parseInt(p[10].trim());
        String   finNumber  = p[11].trim();
        String   phone      = p[12].trim();
        int      adults     = Integer.parseInt(p[13].trim());
        int      children   = Integer.parseInt(p[14].trim());
        String   gateway    = p[15].trim();

        // receiptDateTime — field 16 (added in v2)
        String receiptDateTime = (p.length > 16 && !p[16].trim().isEmpty())
                ? p[16].trim()
                : "N/A";

        // amenities fields — fields 17, 18, 19 (added in v3)
        // Use safe defaults if missing or not a valid number
        double baseRoomCost    = 0.0;
        double amenitiesCharge = 0.0;
        String amenitiesLabel  = "N/A";

        if (p.length > 17) {
            // Safely parse baseRoomCost
            try {
                baseRoomCost = Double.parseDouble(p[17].trim());
            } catch (NumberFormatException e) {
                baseRoomCost = 0.0; // old record — default to 0
            }
        }

        if (p.length > 18) {
            // Safely parse amenitiesCharge
            try {
                amenitiesCharge = Double.parseDouble(p[18].trim());
            } catch (NumberFormatException e) {
                amenitiesCharge = 0.0; // old record — default to 0
            }
        }

        if (p.length > 19) {
            amenitiesLabel = p[19].trim().replace(";", "|");
        }

        // Build the reservation from all parsed fields
        Reservation r = new Reservation(
                resId, customerId, roomId,
                checkIn, checkOut, totalCost,
                fullName, age, finNumber, phone,
                adults, children, gateway,
                baseRoomCost, amenitiesCharge, amenitiesLabel
        );
        // Add after the amenitiesLabel loading block:
        double initialPaidAmount = 0.0;
        if (p.length > 20) {
            try {
                initialPaidAmount = Double.parseDouble(p[20].trim());
            } catch (NumberFormatException e) {
                initialPaidAmount = paidAmount; // fallback to paidAmount for old records
            }
        }

// Then after building the reservation r:
        r.initialPaidAmount = initialPaidAmount;

        // Restore mutable fields
        r.paidAmount    = paidAmount;
        r.status        = status;
        r.payStatus     = payStatus;
        r.receiptDateTime = receiptDateTime;

        return r;

    }
    public String receipt(String username) {
        double amountPaid= (paidAmount>0)? paidAmount :initialPaidAmount;
        double balance =totalCost-amountPaid;
        boolean partial  = (amountPaid < totalCost-0.001);

        String payLine = partial ? String.format("PARTIAL PAID (30%%): $%-10.2f", amountPaid)
                : String.format(" $%-10.2f", amountPaid);
        String statusLabel;
        if (payStatus == PayStatus.DEPOSIT_PAID) {
            statusLabel = "PARTIALLY PAID";
        } else if (payStatus == PayStatus.FULLY_PAID) {
            statusLabel = "FULLY PAID";
        } else if (payStatus == PayStatus.REFUNDED) {
            statusLabel = "REFUNDED";
        } else {
            // Still UNPAID — payment submitted but not yet approved
            statusLabel = partial
                    ? "PARTIALLY PAID (Pending Approval)"
                    : "FULLY PAID (Pending Approval)";}

        return String.format("""

  ╔══════════════════════════════════════════════════════════╗
   ║          ★ GRAND HOTEL RESERVATION RECEIPT  ★         ║
  ╠══════════════════════════════════════════════════════════╣
  ║  Receipt Date & Time : %-35s║
  ╠══════════════════════════════════════════════════════════╣
     GUEST  INFORMATION
  ║  Reservation ID      : %-35s║
  ║  Account Username    : %-35s║
  ║  Full Name           : %-35s║
  ║  Age                 : %-35s║
  ║  FIN Number          : %-35s║
  ║  Phone               : %-35s║
  ║  Adults              : %-35s║
  ║  Children            : %-35s║
  ╠══════════════════════════════════════════════════════════╣
     BOOKING  DETAILS
  ║  Room ID             : %-35s║
  ║  Check-In            : %-35s║
  ║  Check-Out           : %-35s║
  ║  Number of Nights    : %-35s║
  ╠══════════════════════════════════════════════════════════╣
      PAYMENT  SUMMARY
     Room cost           : $%-34.2f║
  ║  Services            : %-35s║
  ║  Service Charge      : $%-34.2f║
  ║  Total Cost          : $%-34.2f║
  ║  Amount Paid         : %-35s║
  ║  Balance Due         : $%-34.2f║
  ║  Payment Gateway     : %-35s║
  ║  Payment Status      : %-35s║
  ║  Booking Status      : %-35s║
  ╚══════════════════════════════════════════════════════════╝
  ║     ⚠  Payment is pending Admin approval...              ║
  ╚══════════════════════════════════════════════════════════╝
             Thank you for choosing Grand Hotel!
""",
                receiptDateTime,
                resId, username, fullName, age, finNumber, phone, adults, children,
                roomId, checkIn, checkOut, nights(),baseRoomCost, serviceLabel, serviceCharge,
                totalCost, payLine, balance, gateway, statusLabel, status);
    }

    @Override public String toString() {
        return String.format(
                "  %-10s | Room:%-6s | %-12s→%-12s | Total:$%7.2f | Paid:$%7.2f | %-18s | [%-16s] | %s",
                resId, roomId, checkIn, checkOut,
                totalCost, paidAmount, payStatus, status, fullName);
    }
}
