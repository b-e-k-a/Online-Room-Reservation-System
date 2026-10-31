// ═══════════════════════════════════════════════════════
//  PAYMENT GATEWAY HIERARCHY  (Polymorphism)
// ═══════════════════════════════════════════════════════
abstract class PaymentGateway {
    private String  name;
    private boolean enabled;

    public PaymentGateway(String name, boolean enabled) {
        this.name = name; this.enabled = enabled;
    }

    public abstract String processPayment(double amount);
    public abstract String processRefund(double amount);

    public String  getName()             { return name; }
    public boolean isEnabled()           { return enabled; }
    public void    setEnabled(boolean e) { this.enabled = e; }

    public String toFileLine() { return name + "|" + getClass().getSimpleName() + "|" + enabled; }

    @Override public String toString() {
        return String.format("  %-18s [%s]", name, enabled ? "✔ ENABLED" : "✘ DISABLED");
    }
}

// ── Built-in types ───────────────────────────────────
class CBEGateway extends PaymentGateway {
    public CBEGateway(String name, boolean e) { super(name, e); }
    @Override public String processPayment(double a) {
        return String.format("  ✔ %s: Payment $%.2f OK. Ref#CBE-%05d", getName(), a, (int)(Math.random()*99999));
    }
    @Override public String processRefund(double a) {
        return String.format("  ✔ %s: Refund $%.2f initiated. Ref#RCBE-%05d", getName(), a, (int)(Math.random()*99999));
    }
}
class TelebirrGateway extends PaymentGateway {
    public TelebirrGateway(String name, boolean e) { super(name, e); }
    @Override public String processPayment(double a) {
        return String.format("  ✔ %s: Payment $%.2f OK. TxID#TLB-%05d", getName(), a, (int)(Math.random()*99999));
    }
    @Override public String processRefund(double a) {
        return String.format("  ✔ %s: Refund $%.2f initiated. TxID#RTLB-%05d", getName(), a, (int)(Math.random()*99999));
    }
}
class AwashGateway extends PaymentGateway {
    public AwashGateway(String name, boolean e) { super(name, e); }
    @Override public String processPayment(double a) {
        return String.format("  ✔ %s: Payment $%.2f OK. Ref#AWB-%05d", getName(), a, (int)(Math.random()*99999));
    }
    @Override public String processRefund(double a) {
        return String.format("  ✔ %s: Refund $%.2f initiated. Ref#RAWB-%05d", getName(), a, (int)(Math.random()*99999));
    }
}
// Generic gateway (admin-added)
class GenericGateway extends PaymentGateway {
    public GenericGateway(String name, boolean e) { super(name, e); }
    @Override public String processPayment(double a) {
        return String.format("  ✔ %s: Payment $%.2f processed. Ref#GEN-%05d", getName(), a, (int)(Math.random()*99999));
    }
    @Override public String processRefund(double a) {
        return String.format("  ✔ %s: Refund $%.2f processed. Ref#RGEN-%05d", getName(), a, (int)(Math.random()*99999));
    }
}
