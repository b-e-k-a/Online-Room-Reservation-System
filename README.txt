╔══════════════════════════════════════════════════════════════╗
║         GRAND HOTEL ROOM RESERVATION SYSTEM                 ║
║         Java Console Application  –  README                 ║
╚══════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 HOW TO COMPILE & RUN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1. Make sure Java 17+ is installed.
  2. Open a terminal and navigate to the src/ folder:

       cd src

  3. Compile all files:

       javac *.java

  4. Run the program:

       java Main

  5. A "data/" folder will be created automatically to store
     all persistent data (users, rooms, reservations, etc.).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 DEFAULT ADMIN CREDENTIALS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Username : admin
  Password : Admin@123

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 FILE STRUCTURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  src/
    Main.java              – Entry point
    HotelSystem.java       – Main controller (all menus)
    User.java              – User hierarchy (Admin/Manager/Customer)
    Room.java              – Room hierarchy (Standard/Deluxe/Suite)
    Reservation.java       – Booking model + receipt generator
    PaymentGateway.java    – Gateway hierarchy (CBE/Telebirr/Awash/Custom)
    Feedback.java          – Customer feedback model
    FileStore.java         – All file I/O persistence
    Utils.java             – Password hiding, hashing, helpers
    Exceptions.java        – Custom exception hierarchy

  data/  (auto-created at runtime)
    users.txt
    rooms.txt
    reservations.txt
    feedback.txt
    gateways.txt
    pending_payments.txt

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 FEATURE SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  MAIN MENU
    • Admin login
    • Manager login
    • Customer login / register
    • Exit

  ADMIN
    • Create Manager accounts
    • Reset any user's password by User ID
    • Add / Edit / Remove rooms
    • View all rooms (grouped by capacity)
    • Cancel any booking
    • View all reservations
    • View all payments
    • Approve pending payments
    • Manage gateways (add, delete, toggle enable/disable)
    • View all users

  MANAGER
    • View all customer reservations
    • Update room status (Available / Occupied / Out-of-Service)
    • Change room number by Room ID (reassignment)
    • Process customer refunds
    • View customer feedback

  CUSTOMER
    • Search rooms by type and/or max price
    • Browse available rooms (grouped by capacity)
    • Book a room (full name, age>17, FIN 12-digits, phone)
    • Max 3 active reservations per customer
    • Pay in full OR deposit 30% now + rest at check-in
    • Receive printed booking receipt
    • View my reservations
    • Cancel reservation + request refund
    • Pay remaining balance
    • Submit feedback (rating 1–5 + message)

  SECURITY
    • Passwords hidden at input (java.io.Console / fallback)
    • Passwords stored as SHA-256 hashes
    • Input validated (username, email, password, FIN, age, dates)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 OOP CONCEPTS USED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Abstraction   – User, Room, PaymentGateway are abstract classes
  • Inheritance   – Admin/Manager/Customer extend User;
                    Standard/Deluxe/Suite extend Room;
                    CBE/Telebirr/Awash/Custom extend PaymentGateway
  • Polymorphism  – totalCost(), processPayment(), menuText() etc.
                    behave differently per subclass
  • Encapsulation – Private fields with public getters/setters
  • Exception Handling – Custom hierarchy rooted at HotelException
  • File I/O      – BufferedReader/Writer for all persistence
