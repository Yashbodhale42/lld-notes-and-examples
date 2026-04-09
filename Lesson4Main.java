// ============================================================================
// LLD LESSON 4: Dependency Inversion Principle (DIP) + Dependency Injection
// ============================================================================
//
// RULE: "High-level modules should NOT depend on low-level modules.
//        Both should depend on ABSTRACTIONS (interfaces)."
//
// This is the principle that ties ALL of SOLID together.
// Without DIP, your code is a tangled web where changing one class
// forces changes in 10 others. With DIP, classes are loosely coupled
// and independently replaceable.
//
// DIP = the PRINCIPLE (the "what")
// DI  = the TECHNIQUE (the "how") — injecting dependencies from outside
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ❌ BAD DESIGN — High-level module directly depends on low-level module  ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

// Scenario: An OrderService that processes orders.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Low-level module: specific database implementation
class MySQLDatabase {
    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
}

// Low-level module: specific email implementation
class GmailService {
    public void send(String to, String body) {
        System.out.println("Sending via Gmail to " + to + ": " + body);
    }
}

// Low-level module: specific payment implementation
class StripePayment {
    public boolean charge(double amount) {
        System.out.println("Charging ₹" + amount + " via Stripe");
        return true;
    }
}

// ❌ HIGH-LEVEL module directly creates and depends on LOW-LEVEL modules
class OrderServiceBAD {
    
    // Hardcoded dependencies — created RIGHT HERE inside the class
    private MySQLDatabase database = new MySQLDatabase();
    private GmailService emailService = new GmailService();
    private StripePayment paymentService = new StripePayment();

    public void placeOrder(String item, double price, String customerEmail) {
        // Process payment
        boolean paid = paymentService.charge(price);
        
        if (paid) {
            // Save order
            database.save("Order: " + item + " - ₹" + price);
            
            // Send confirmation
            emailService.send(customerEmail, "Your order for " + item + " is confirmed!");
        }
    }
}

// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  WHY IS THIS BAD? — The Dependency Chain Problem                        ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//   OrderServiceBAD ──────→ MySQLDatabase     (concrete class)
//         │ ──────────────→ GmailService      (concrete class)
//         │ ──────────────→ StripePayment     (concrete class)
//
//   PROBLEM 1: Can't switch to MongoDB without modifying OrderServiceBAD
//   PROBLEM 2: Can't switch to SendGrid without modifying OrderServiceBAD
//   PROBLEM 3: Can't switch to Razorpay without modifying OrderServiceBAD
//   PROBLEM 4: Can't test OrderServiceBAD without a real DB, email server, payment gateway
//   PROBLEM 5: OrderServiceBAD "knows" about MySQL, Gmail, Stripe — it's coupled to ALL of them
//
//   The high-level policy ("place an order") is CHAINED to low-level details
//   ("use MySQL", "use Gmail", "use Stripe"). Change any detail → change the policy class.
//   That's backwards. The policy should be STABLE. The details should be SWAPPABLE.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ✅ GOOD DESIGN — Depend on abstractions, not concretions               ║
// ╚═══════════════════════════════════════════════════════════════════════════╝


// ── Step 1: Define abstractions (interfaces) for each dependency ────────────

/**
 * Abstraction for ANY database. MySQL, MongoDB, PostgreSQL — doesn't matter.
 * The high-level module only knows THIS interface exists.
 */
interface OrderRepository {
    void save(Order order);
    Order findById(String orderId);
}

/**
 * Abstraction for ANY notification mechanism.
 * Email, SMS, Push, Slack — the high-level module doesn't care.
 */
interface NotificationService {
    void notify(String recipient, String message);
}

/**
 * Abstraction for ANY payment processor.
 * Stripe, Razorpay, PayPal — swappable without touching business logic.
 */
interface PaymentProcessor {
    PaymentResult charge(double amount, String customerId);
}


// ── Step 2: Create concrete implementations of each abstraction ─────────────

// --- Order Entity ---
class Order {
    private String id;
    private String item;
    private double price;
    private String customerEmail;
    private OrderStatus status;

    public Order(String id, String item, double price, String customerEmail) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.PENDING;
    }

    // Getters
    public String getId() { return id; }
    public String getItem() { return item; }
    public double getPrice() { return price; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}

enum OrderStatus { PENDING, PAID, FAILED, SHIPPED, DELIVERED }

// --- Payment Result (instead of raw boolean) ---
class PaymentResult {
    private final boolean success;
    private final String transactionId;
    private final String failureReason;

    private PaymentResult(boolean success, String txnId, String reason) {
        this.success = success;
        this.transactionId = txnId;
        this.failureReason = reason;
    }

    // Factory methods — cleaner than constructors for result types
    public static PaymentResult success(String txnId) {
        return new PaymentResult(true, txnId, null);
    }
    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }

    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getFailureReason() { return failureReason; }
}


// --- Concrete Repository Implementations ---

class MySQLOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        System.out.println("  [MySQL] Saved order " + order.getId());
    }
    @Override
    public Order findById(String orderId) {
        System.out.println("  [MySQL] Fetching order " + orderId);
        return null; // placeholder
    }
}

class MongoOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        System.out.println("  [MongoDB] Saved order " + order.getId());
    }
    @Override
    public Order findById(String orderId) {
        System.out.println("  [MongoDB] Fetching order " + orderId);
        return null;
    }
}


// --- Concrete Notification Implementations ---

class EmailNotificationService implements NotificationService {
    @Override
    public void notify(String recipient, String message) {
        System.out.println("  [Email] To " + recipient + ": " + message);
    }
}

class SMSNotificationService implements NotificationService {
    @Override
    public void notify(String recipient, String message) {
        System.out.println("  [SMS] To " + recipient + ": " + message);
    }
}

class SlackNotificationService implements NotificationService {
    @Override
    public void notify(String recipient, String message) {
        System.out.println("  [Slack] To #" + recipient + ": " + message);
    }
}


// --- Concrete Payment Implementations ---

class StripePaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult charge(double amount, String customerId) {
        System.out.println("  [Stripe] Charging ₹" + amount);
        return PaymentResult.success("TXN_STRIPE_" + System.currentTimeMillis());
    }
}

class RazorpayPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult charge(double amount, String customerId) {
        System.out.println("  [Razorpay] Charging ₹" + amount);
        return PaymentResult.success("TXN_RZPY_" + System.currentTimeMillis());
    }
}


// ── Step 3: High-level module depends ONLY on abstractions ──────────────────

/**
 * OrderService is the HIGH-LEVEL module (business policy).
 * 
 * NOTICE: It references ONLY interfaces — not MySQL, not Gmail, not Stripe.
 * It has NO IDEA which database, notification system, or payment processor
 * it's using. And it doesn't need to know.
 * 
 * This class is CLOSED for modification. Switching from MySQL to MongoDB
 * requires ZERO changes here.
 */
class OrderService {

    // Dependencies are ABSTRACTIONS (interfaces), not concrete classes
    private final OrderRepository repository;
    private final NotificationService notificationService;
    private final PaymentProcessor paymentProcessor;

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  CONSTRUCTOR INJECTION — The most common DI technique          ║
    // ║  Dependencies are provided FROM OUTSIDE, not created inside.   ║
    // ║  The caller decides WHICH implementations to use.              ║
    // ╚═════════════════════════════════════════════════════════════════╝
    public OrderService(
            OrderRepository repository,
            NotificationService notificationService,
            PaymentProcessor paymentProcessor) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.paymentProcessor = paymentProcessor;
    }

    /**
     * Core business logic — pure policy, no infrastructure details.
     * This method reads like a BUSINESS REQUIREMENT, not like code.
     */
    public void placeOrder(Order order) {
        System.out.println("\n📦 Processing order: " + order.getItem());

        // Step 1: Charge customer
        PaymentResult result = paymentProcessor.charge(
            order.getPrice(), 
            order.getCustomerEmail()
        );

        if (result.isSuccess()) {
            // Step 2: Update order status and save
            order.setStatus(OrderStatus.PAID);
            repository.save(order);

            // Step 3: Notify customer
            notificationService.notify(
                order.getCustomerEmail(),
                "Order confirmed! " + order.getItem() + " — Txn: " + result.getTransactionId()
            );

            System.out.println("✅ Order placed successfully!");
        } else {
            // Step 2b: Handle failure
            order.setStatus(OrderStatus.FAILED);
            repository.save(order);

            notificationService.notify(
                order.getCustomerEmail(),
                "Payment failed: " + result.getFailureReason()
            );

            System.out.println("❌ Order failed: " + result.getFailureReason());
        }
    }
}

// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  THE DEPENDENCY ARROW FLIPPED!                                          ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//   BEFORE (bad — high-level depends on low-level):
//
//     OrderServiceBAD ──→ MySQLDatabase
//                    ──→ GmailService
//                    ──→ StripePayment
//
//   AFTER (good — both depend on abstractions):
//
//     OrderService ──→ OrderRepository (interface) ←── MySQLOrderRepository
//                 ──→ NotificationService (interface) ←── EmailNotificationService
//                 ──→ PaymentProcessor (interface) ←── StripePaymentProcessor
//
//   The arrows from the concrete classes point UPWARD toward the abstraction.
//   That's the "inversion" — dependencies flow toward abstractions, not details.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧪 THE KILLER BENEFIT: Testability with Mock Objects                   ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

// Because OrderService depends on interfaces, we can inject FAKES for testing.
// No real database. No real payment gateway. No real emails sent.

class FakeOrderRepository implements OrderRepository {
    // Stores orders in memory — perfect for testing
    private Map<String, Order> store = new HashMap<>();

    @Override
    public void save(Order order) { store.put(order.getId(), order); }

    @Override
    public Order findById(String id) { return store.get(id); }

    // Test helper — verify what was saved
    public Order getLastSaved(String id) { return store.get(id); }
}

class FakeNotificationService implements NotificationService {
    // Captures notifications for assertion
    private List<String> sentMessages = new ArrayList<>();

    @Override
    public void notify(String recipient, String message) {
        sentMessages.add(recipient + ": " + message);
    }

    // Test helper — verify what was sent
    public List<String> getSentMessages() { return sentMessages; }
}

class FakePaymentProcessor implements PaymentProcessor {
    private boolean shouldSucceed;

    public FakePaymentProcessor(boolean shouldSucceed) {
        this.shouldSucceed = shouldSucceed;
    }

    @Override
    public PaymentResult charge(double amount, String customerId) {
        if (shouldSucceed) return PaymentResult.success("FAKE_TXN_123");
        return PaymentResult.failure("Insufficient funds");
    }
}

// --- Example Test (without any testing framework, just logic) ---
class OrderServiceTest {

    public static void testSuccessfulOrder() {
        // Arrange — inject fakes
        FakeOrderRepository repo = new FakeOrderRepository();
        FakeNotificationService notifier = new FakeNotificationService();
        FakePaymentProcessor payment = new FakePaymentProcessor(true); // will succeed

        OrderService service = new OrderService(repo, notifier, payment);

        // Act
        Order order = new Order("ORD-001", "Laptop", 75000, "yash@test.com");
        service.placeOrder(order);

        // Assert
        Order saved = repo.getLastSaved("ORD-001");
        assert saved.getStatus() == OrderStatus.PAID : "Order should be PAID";
        assert notifier.getSentMessages().size() == 1 : "One notification should be sent";
        System.out.println("✅ testSuccessfulOrder PASSED\n");
    }

    public static void testFailedPayment() {
        // Arrange — payment will FAIL
        FakeOrderRepository repo = new FakeOrderRepository();
        FakeNotificationService notifier = new FakeNotificationService();
        FakePaymentProcessor payment = new FakePaymentProcessor(false); // will fail

        OrderService service = new OrderService(repo, notifier, payment);

        // Act
        Order order = new Order("ORD-002", "Phone", 50000, "yash@test.com");
        service.placeOrder(order);

        // Assert
        Order saved = repo.getLastSaved("ORD-002");
        assert saved.getStatus() == OrderStatus.FAILED : "Order should be FAILED";
        assert notifier.getSentMessages().get(0).contains("failed") : "Should notify failure";
        System.out.println("✅ testFailedPayment PASSED\n");
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — See DIP + DI in action                                      ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson4Main {
    public static void main(String[] args) {

        // ── PRODUCTION Configuration ──
        // Assemble with REAL implementations
        System.out.println("====== PRODUCTION MODE ======");
        OrderService prodService = new OrderService(
            new MySQLOrderRepository(),        // Real DB
            new EmailNotificationService(),    // Real email
            new StripePaymentProcessor()       // Real payment
        );

        Order order1 = new Order("ORD-001", "Laptop", 75000, "yash@company.com");
        prodService.placeOrder(order1);


        // ── SWITCHING INFRASTRUCTURE — Zero changes to OrderService ──
        System.out.println("\n====== AFTER MIGRATION ======");
        OrderService migratedService = new OrderService(
            new MongoOrderRepository(),        // Switched DB!
            new SlackNotificationService(),    // Switched notification!
            new RazorpayPaymentProcessor()     // Switched payment!
        );

        Order order2 = new Order("ORD-002", "Keyboard", 3000, "yash@company.com");
        migratedService.placeOrder(order2);
        // OrderService code: UNTOUCHED. That's DIP.


        // ── TEST Configuration ──
        System.out.println("\n====== TEST MODE ======");
        OrderServiceTest.testSuccessfulOrder();
        OrderServiceTest.testFailedPayment();
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (complete after all SOLID lessons)                 ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── model/
// │   ├── Employee.java
// │   ├── Order.java                     ← Entity
// │   ├── OrderStatus.java               ← Enum
// │   └── PaymentResult.java             ← Value Object
// │
// ├── capability/                         (from Lesson 3 — ISP)
// │   ├── Workable.java
// │   ├── Attendable.java
// │   └── ...
// │
// ├── repository/                         ← Abstraction + Implementations
// │   ├── OrderRepository.java           ← INTERFACE (abstraction)
// │   ├── MySQLOrderRepository.java      ← Concrete
// │   └── MongoOrderRepository.java      ← Concrete
// │
// ├── payment/                            ← Abstraction + Implementations
// │   ├── PaymentProcessor.java          ← INTERFACE
// │   ├── StripePaymentProcessor.java    ← Concrete
// │   └── RazorpayPaymentProcessor.java  ← Concrete
// │
// ├── notification/                       ← Abstraction + Implementations
// │   ├── NotificationService.java       ← INTERFACE
// │   ├── EmailNotificationService.java
// │   ├── SMSNotificationService.java
// │   └── SlackNotificationService.java
// │
// ├── service/
// │   ├── OrderService.java              ← HIGH-LEVEL (depends on interfaces only)
// │   └── BonusCalculator.java           (from Lesson 2)
// │
// ├── strategy/                           (from Lesson 2)
// ├── factory/                            (from Lesson 2)
// │
// ├── test/                               ← Test doubles
// │   ├── FakeOrderRepository.java
// │   ├── FakeNotificationService.java
// │   ├── FakePaymentProcessor.java
// │   └── OrderServiceTest.java
// │
// └── Lesson4Main.java


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧠 THREE TYPES OF DEPENDENCY INJECTION                                 ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// 1. CONSTRUCTOR INJECTION (✅ preferred — what we used above)
//    Dependencies provided via constructor. Object is fully ready after creation.
//
//      class OrderService {
//          private final OrderRepository repo;  // final = can't change after init
//          public OrderService(OrderRepository repo) { this.repo = repo; }
//      }
//
//    WHY PREFERRED:
//    - Dependencies are explicit (visible in constructor signature)
//    - Object is NEVER in an incomplete state
//    - Fields can be final (immutable, thread-safe)
//    - Easy to see when a class has too many dependencies (constructor gets long)
//
//
// 2. SETTER INJECTION (⚠️ use sparingly — for optional dependencies)
//    Dependencies set after construction via setters.
//
//      class ReportService {
//          private NotificationService notifier;  // optional
//          public void setNotifier(NotificationService n) { this.notifier = n; }
//      }
//
//    WHEN TO USE: When a dependency is truly OPTIONAL (the class works without it).
//    DANGER: Object can exist in an incomplete state. Null checks needed.
//
//
// 3. INTERFACE INJECTION (rare — the dependency provides the injector)
//    The interface itself defines the injection method.
//
//      interface NotificationAware {
//          void injectNotificationService(NotificationService service);
//      }
//
//    WHEN TO USE: Almost never in modern Java. Mostly historical.
//
//
// IN INTERVIEWS: Always default to Constructor Injection unless asked otherwise.
// In Spring Boot, @Autowired on constructors is the recommended approach.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🎓 SOLID COMPLETE — How all 5 work together                            ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//  ┌──────────┬───────────────────────────────────────────────────────────┐
//  │ Principle │ What it gives you                                        │
//  ├──────────┼───────────────────────────────────────────────────────────┤
//  │ SRP      │ Small, focused classes (1 reason to change)              │
//  │ OCP      │ Add features without modifying existing code             │
//  │ LSP      │ Safe substitution — subclasses honor parent contracts    │
//  │ ISP      │ Lean interfaces — no forced empty implementations        │
//  │ DIP      │ Loose coupling — swap implementations freely             │
//  └──────────┴───────────────────────────────────────────────────────────┘
//
//  TOGETHER they produce code that is:
//    → Easy to TEST    (inject fakes via DIP)
//    → Easy to EXTEND  (new strategies via OCP)
//    → Easy to READ    (small classes via SRP)
//    → Safe to REUSE   (clean contracts via LSP + ISP)
//    → Safe to CHANGE  (loose coupling via DIP)
//
//  If an interviewer asks "What's the point of SOLID?" — 
//  the answer is: "Maintainable, testable, extensible code."
