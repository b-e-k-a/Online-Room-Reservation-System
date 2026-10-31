class HotelException       extends Exception { public HotelException(String m)       { super(m); } }
class AuthException        extends HotelException { public AuthException(String m)        { super(m); } }
class ValidationException  extends HotelException { public ValidationException(String m)  { super(m); } }
class RoomException        extends HotelException { public RoomException(String m)        { super(m); } }
class ReservationException extends HotelException { public ReservationException(String m) { super(m); } }
class PaymentException     extends HotelException { public PaymentException(String m)     { super(m); } }
class RefundException      extends HotelException { public RefundException(String m)      { super(m); } }
