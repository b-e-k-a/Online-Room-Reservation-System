import java.time.LocalDate;
public class Feedback{
    private final String    feedbackId;
    private final String    customerId;
    private final String    customerName;
    private final String    reservationId;  // NEW
    private final String    message;
    private final int       rating;
    private final LocalDate date;

    public Feedback(String id, String cid, String cname,
                    String reservationId,              // NEW
                    String msg, int rating, LocalDate date) {
        this.feedbackId    = id;
        this.customerId    = cid;
        this.customerName  = cname;
        this.reservationId = reservationId;            // NEW
        this.message       = msg;
        this.rating        = rating;
        this.date          = date;
    }

    public String toFileLine() {
        return feedbackId + "|" + customerId + "|" + customerName + "|" +
                reservationId + "|" +                               // NEW
                message.replace("|", ";") + "|" + rating + "|" +
                date.format(Utils.DATE_FMT);
    }
    public static Feedback fromLine(String line) {
        String[] p = line.split("\\|", -1);

        // Old format had 6 fields, new format has 7 (with reservationId)
        if (p.length >= 7) {
            // New format: id|cid|cname|reservationId|message|rating|date
            return new Feedback(p[0], p[1], p[2], p[3],
                    p[4].replace(";", "|"),
                    Integer.parseInt(p[5]),
                    LocalDate.parse(p[6], Utils.DATE_FMT));
        } else {
            // Old format: id|cid|cname|message|rating|date
            return new Feedback(p[0], p[1], p[2], "N/A",  // reservationId = N/A
                    p[3].replace(";", "|"),
                    Integer.parseInt(p[4]),
                    LocalDate.parse(p[5], Utils.DATE_FMT));
        }
    }

    public String getFeedbackId()   { return feedbackId; }
    public String getCustomerId()   { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getReservationId() { return reservationId; }

    @Override public String toString() {
        String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
        return String.format(
                "  [%s] %s | %s (%s) | Reservation: %s | Rating: %s%n  %s Message: %s",
                feedbackId, date, customerName, customerId,
                reservationId, stars, " ".repeat(4), message);
    }
}
