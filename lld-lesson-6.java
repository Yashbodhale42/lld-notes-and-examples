// ============================================================================
// LLD LESSON 6: Observer Pattern + Decorator Pattern
// ============================================================================
//
// OBSERVER → "When something happens, NOTIFY everyone who cares."
//            One-to-many dependency: when one object changes state,
//            all dependents are notified automatically.
//
// DECORATOR → "Add new behavior to an object WITHOUT modifying its class."
//             Wrap an object with another object that adds functionality.
//             Like layers of clothing — each layer adds something.
//
// Both patterns are about EXTENSION without MODIFICATION (OCP in action).
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 1: OBSERVER PATTERN                                               ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// Scenario: An e-commerce order system. When an order is placed, MANY things
// need to happen: send email, update inventory, notify warehouse, log analytics,
// send SMS to customer, update dashboard...
//
// The ORDER shouldn't know about ALL these systems. It should just say
// "Hey, I was placed!" and let interested parties react.


// ── ❌ BAD DESIGN — Order directly calls every dependent system ─────────────

class OrderServiceBAD {

    // Order "knows" about every system that cares about it.
    // Adding a new listener = modifying this class. OCP violation.
    // Tight coupling to every downstream system.

    private EmailService emailService;
    private InventoryService inventoryService;
    private AnalyticsService analyticsService;
    private SMSService smsService;
    private DashboardService dashboardService;
    // ... 10 more services next year?

    public void placeOrder(String item, double price) {
        System.out.println("Order placed: " + item);

        // Manually calling each dependent — tightly coupled
        emailService.sendOrderConfirmation(item, price);
        inventoryService.reduceStock(item, 1);
        analyticsService.trackPurchase(item, price);
        smsService.sendSMS("Order confirmed: " + item);
        dashboardService.updateRevenue(price);
        // ⚠️ Next month: "Add WhatsApp notification" → modify THIS class
        // ⚠️ Next quarter: "Add fraud detection" → modify THIS class AGAIN
    }
}

// PROBLEMS:
// 1. OrderService is coupled to 5+ unrelated services
// 2. Adding/removing a listener requires modifying OrderService
// 3. If AnalyticsService is slow, it blocks the entire order flow
// 4. Can't test OrderService without mocking ALL downstream services
// 5. OrderService has knowledge it shouldn't have (SRP violation)


// ── ✅ GOOD DESIGN — Observer Pattern ───────────────────────────────────────

// ── Step 1: Define the Event (what happened) ────────────────────────────────

/**
 * Event object — carries all data about what happened.
 * Observers receive this and extract what they need.
 * Using a class (not raw strings) makes it type-safe and extensible.
 */
class OrderEvent {
    private final String orderId;
    private final String item;
    private final double price;
    private final String customerEmail;
    private final long timestamp;

    public OrderEvent(String orderId, String item, double price, String customerEmail) {
        this.orderId = orderId;
        this.item = item;
        this.price = price;
        this.customerEmail = customerEmail;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public String getItem() { return item; }
    public double getPrice() { return price; }
    public String getCustomerEmail() { return customerEmail; }
    public long getTimestamp() { return timestamp; }
}


// ── Step 2: Define the Observer interface (who can listen) ──────────────────

/**
 * Any class that wants to react to order events implements this.
 * The Subject (OrderService) doesn't know which classes implement it.
 */
interface OrderEventListener {

    /**
     * Called when an order event occurs.
     * Each listener decides HOW to react.
     */
    void onOrderPlaced(OrderEvent event);

    /**
     * Identifier for logging/debugging — which listener is this?
     */
    String getListenerName();
}


// ── Step 3: Create concrete observers (each handles ONE concern) ────────────

class EmailListener implements OrderEventListener {
    @Override
    public void onOrderPlaced(OrderEvent event) {
        System.out.println("  📧 [Email] Sending confirmation to "
            + event.getCustomerEmail() + " for " + event.getItem());
    }
    @Override
    public String getListenerName() { return "EmailListener"; }
}

class InventoryListener implements OrderEventListener {
    @Override
    public void onOrderPlaced(OrderEvent event) {
        System.out.println("  📦 [Inventory] Reducing stock for: " + event.getItem());
    }
    @Override
    public String getListenerName() { return "InventoryListener"; }
}

class AnalyticsListener implements OrderEventListener {
    @Override
    public void onOrderPlaced(OrderEvent event) {
        System.out.println("  📊 [Analytics] Tracking purchase: "
            + event.getItem() + " — ₹" + event.getPrice());
    }
    @Override
    public String getListenerName() { return "AnalyticsListener"; }
}

class SMSListener implements OrderEventListener {
    @Override
    public void onOrderPlaced(OrderEvent event) {
        System.out.println("  📱 [SMS] Sending to customer: Order "
            + event.getOrderId() + " confirmed");
    }
    @Override
    public String getListenerName() { return "SMSListener"; }
}

class FraudDetectionListener implements OrderEventListener {
    @Override
    public void onOrderPlaced(OrderEvent event) {
        boolean suspicious = event.getPrice() > 100000; // simple rule
        if (suspicious) {
            System.out.println("  🚨 [Fraud] ALERT: High-value order "
                + event.getOrderId() + " — ₹" + event.getPrice());
        } else {
            System.out.println("  ✅ [Fraud] Order " + event.getOrderId() + " looks clean");
        }
    }
    @Override
    public String getListenerName() { return "FraudDetectionListener"; }
}


// ── Step 4: The Subject (Publisher) — manages and notifies listeners ────────

/**
 * EventManager: A reusable component that manages observer registration
 * and notification. Separated from OrderService so ANY service can use it.
 */
class EventManager {

    private final List<OrderEventListener> listeners = new ArrayList<>();

    public void subscribe(OrderEventListener listener) {
        listeners.add(listener);
        System.out.println("  [EventManager] Subscribed: " + listener.getListenerName());
    }

    public void unsubscribe(OrderEventListener listener) {
        listeners.remove(listener);
        System.out.println("  [EventManager] Unsubscribed: " + listener.getListenerName());
    }

    /**
     * Notify ALL registered listeners about the event.
     * Each listener reacts independently — they don't know about each other.
     */
    public void notifyAll(OrderEvent event) {
        System.out.println("  [EventManager] Notifying " + listeners.size() + " listeners...\n");
        for (OrderEventListener listener : listeners) {
            try {
                listener.onOrderPlaced(event);
            } catch (Exception e) {
                // One listener failing shouldn't crash others
                System.out.println("  ⚠️ [EventManager] " + listener.getListenerName()
                    + " failed: " + e.getMessage());
            }
        }
    }
}


/**
 * OrderService: The Subject/Publisher.
 * It only knows about EventManager — NOT about individual listeners.
 * 
 * Compare with OrderServiceBAD: 5+ direct dependencies → now just 1.
 */
class OrderService {
    private final EventManager eventManager;

    public OrderService(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void placeOrder(String orderId, String item, double price, String email) {
        // Core business logic — place the order
        System.out.println("\n🛒 Order " + orderId + " placed: " + item + " — ₹" + price);

        // Create event and publish — OrderService doesn't know WHO listens
        OrderEvent event = new OrderEvent(orderId, item, price, email);
        eventManager.notifyAll(event);

        System.out.println("\n✅ Order processing complete.\n");
    }
}

// WHAT CHANGED:
// - OrderService has 1 dependency (EventManager), not 5+
// - Adding WhatsApp notification = new class + one subscribe() call
// - OrderService is NEVER modified. OCP ✅
// - Each listener is independently testable. SRP ✅
// - Listeners don't know about each other. Low coupling ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 2: DECORATOR PATTERN                                              ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// PROBLEM: You have a base object and want to add features to it dynamically,
// without modifying its class and without creating a subclass for every
// possible combination.
//
// Scenario: A coffee shop ordering system.
//   Base: Espresso (₹120)
//   Add-ons: Milk (+₹30), Whipped Cream (+₹40), Chocolate (+₹50), Caramel (+₹35)
//
// With inheritance, you'd need:
//   EspressoWithMilk, EspressoWithChocolate, EspressoWithMilkAndChocolate,
//   EspressoWithMilkAndWhippedCream, EspressoWithMilkAndChocolateAndCaramel...
//   → Combinatorial EXPLOSION. 4 add-ons = 16 subclasses. 10 add-ons = 1024.
//
// With Decorator, you WRAP the base object with layers. Each layer adds ONE thing.


// ── Step 1: The Component interface (base contract) ─────────────────────────

interface Coffee {
    double getCost();
    String getDescription();
}


// ── Step 2: Concrete base components ────────────────────────────────────────

class Espresso implements Coffee {
    @Override
    public double getCost() { return 120.0; }
    @Override
    public String getDescription() { return "Espresso"; }
}

class HouseBlend implements Coffee {
    @Override
    public double getCost() { return 100.0; }
    @Override
    public String getDescription() { return "House Blend"; }
}

class Cappuccino implements Coffee {
    @Override
    public double getCost() { return 150.0; }
    @Override
    public String getDescription() { return "Cappuccino"; }
}


// ── Step 3: Abstract Decorator (the wrapper base) ───────────────────────────

/**
 * KEY INSIGHT: A Decorator IS-A Coffee AND HAS-A Coffee.
 * It implements the same interface, AND wraps another Coffee object.
 * This is what makes decoration stackable.
 */
abstract class CoffeeDecorator implements Coffee {

    protected final Coffee wrappedCoffee;  // The coffee being decorated

    public CoffeeDecorator(Coffee coffee) {
        this.wrappedCoffee = coffee;
    }

    // Default: delegate to wrapped object. Subclasses override to ADD behavior.
    @Override
    public double getCost() { return wrappedCoffee.getCost(); }

    @Override
    public String getDescription() { return wrappedCoffee.getDescription(); }
}


// ── Step 4: Concrete Decorators (each adds ONE feature) ─────────────────────

class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }
    @Override
    public double getCost() {
        return wrappedCoffee.getCost() + 30.0;      // Add milk cost
    }
    @Override
    public String getDescription() {
        return wrappedCoffee.getDescription() + " + Milk";  // Add to description
    }
}

class WhippedCreamDecorator extends CoffeeDecorator {
    public WhippedCreamDecorator(Coffee coffee) {
        super(coffee);
    }
    @Override
    public double getCost() { return wrappedCoffee.getCost() + 40.0; }
    @Override
    public String getDescription() { return wrappedCoffee.getDescription() + " + Whipped Cream"; }
}

class ChocolateDecorator extends CoffeeDecorator {
    public ChocolateDecorator(Coffee coffee) {
        super(coffee);
    }
    @Override
    public double getCost() { return wrappedCoffee.getCost() + 50.0; }
    @Override
    public String getDescription() { return wrappedCoffee.getDescription() + " + Chocolate"; }
}

class CaramelDecorator extends CoffeeDecorator {
    public CaramelDecorator(Coffee coffee) {
        super(coffee);
    }
    @Override
    public double getCost() { return wrappedCoffee.getCost() + 35.0; }
    @Override
    public String getDescription() { return wrappedCoffee.getDescription() + " + Caramel"; }
}

// Adding a new add-on (Vanilla) = ONE new class. No existing class changes. OCP ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🔥 DECORATOR IN A REAL SYSTEM — Request Processing Pipeline            ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// The coffee example teaches the structure, but THIS is where Decorator
// shines in production: wrapping service calls with cross-cutting concerns.

interface DataService {
    String fetchData(String key);
}

// ── Base implementation ─────────────────────────────────────────────────────

class DatabaseService implements DataService {
    @Override
    public String fetchData(String key) {
        // Simulate slow DB call
        System.out.println("    [DB] Fetching '" + key + "' from database...");
        return "data_for_" + key;
    }
}

// ── Abstract Decorator for DataService ──────────────────────────────────────

abstract class DataServiceDecorator implements DataService {
    protected final DataService wrapped;
    public DataServiceDecorator(DataService wrapped) { this.wrapped = wrapped; }
}

// ── Logging Decorator — adds logging around every call ──────────────────────

class LoggingDecorator extends DataServiceDecorator {
    public LoggingDecorator(DataService wrapped) { super(wrapped); }

    @Override
    public String fetchData(String key) {
        System.out.println("    [LOG] Request for key: " + key);
        long start = System.currentTimeMillis();

        String result = wrapped.fetchData(key);  // Delegate to wrapped service

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("    [LOG] Response for '" + key + "' in " + elapsed + "ms");
        return result;
    }
}

// ── Caching Decorator — returns cached result if available ──────────────────

class CachingDecorator extends DataServiceDecorator {
    private final Map<String, String> cache = new HashMap<>();

    public CachingDecorator(DataService wrapped) { super(wrapped); }

    @Override
    public String fetchData(String key) {
        if (cache.containsKey(key)) {
            System.out.println("    [CACHE] Hit for '" + key + "'");
            return cache.get(key);
        }
        System.out.println("    [CACHE] Miss for '" + key + "'");
        String result = wrapped.fetchData(key);  // Delegate to wrapped
        cache.put(key, result);                   // Store for next time
        return result;
    }
}

// ── Auth Decorator — checks permissions before allowing access ──────────────

class AuthDecorator extends DataServiceDecorator {
    private final Set<String> allowedKeys;

    public AuthDecorator(DataService wrapped, Set<String> allowedKeys) {
        super(wrapped);
        this.allowedKeys = allowedKeys;
    }

    @Override
    public String fetchData(String key) {
        if (!allowedKeys.contains(key)) {
            System.out.println("    [AUTH] ⛔ Access denied for key: " + key);
            throw new SecurityException("Unauthorized access to: " + key);
        }
        System.out.println("    [AUTH] ✅ Access granted for key: " + key);
        return wrapped.fetchData(key);
    }
}

// ── Retry Decorator — retries on failure ────────────────────────────────────

class RetryDecorator extends DataServiceDecorator {
    private final int maxRetries;

    public RetryDecorator(DataService wrapped, int maxRetries) {
        super(wrapped);
        this.maxRetries = maxRetries;
    }

    @Override
    public String fetchData(String key) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return wrapped.fetchData(key);
            } catch (Exception e) {
                attempts++;
                System.out.println("    [RETRY] Attempt " + attempts + " failed, retrying...");
            }
        }
        throw new RuntimeException("Failed after " + maxRetries + " retries");
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — Everything in action                                         ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson6Main {
    public static void main(String[] args) {

        // ══════════════════════════════════════════
        //  OBSERVER PATTERN DEMO
        // ══════════════════════════════════════════
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       OBSERVER PATTERN DEMO          ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // Setup: Create event manager and register listeners
        EventManager eventManager = new EventManager();
        
        EmailListener emailListener = new EmailListener();
        InventoryListener inventoryListener = new InventoryListener();
        AnalyticsListener analyticsListener = new AnalyticsListener();
        SMSListener smsListener = new SMSListener();
        FraudDetectionListener fraudListener = new FraudDetectionListener();

        eventManager.subscribe(emailListener);
        eventManager.subscribe(inventoryListener);
        eventManager.subscribe(analyticsListener);
        eventManager.subscribe(smsListener);
        eventManager.subscribe(fraudListener);

        // Create OrderService — it only knows about EventManager
        OrderService orderService = new OrderService(eventManager);

        // Place orders — all 5 listeners react automatically
        orderService.placeOrder("ORD-001", "Laptop", 75000, "yash@company.com");
        orderService.placeOrder("ORD-002", "Server Rack", 250000, "it@company.com");

        // Dynamically unsubscribe SMS — no change to OrderService
        System.out.println("--- Unsubscribing SMS ---");
        eventManager.unsubscribe(smsListener);
        orderService.placeOrder("ORD-003", "Mouse", 500, "yash@company.com");
        // ↑ Only 4 listeners react now. OrderService code: UNTOUCHED.


        // ══════════════════════════════════════════
        //  DECORATOR PATTERN DEMO — Coffee
        // ══════════════════════════════════════════
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      DECORATOR PATTERN — Coffee      ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // Start with base coffee, WRAP with add-ons
        Coffee order1 = new Espresso();
        System.out.println(order1.getDescription() + " → ₹" + order1.getCost());
        // Output: Espresso → ₹120.0

        // Wrap with Milk
        Coffee order2 = new MilkDecorator(new Espresso());
        System.out.println(order2.getDescription() + " → ₹" + order2.getCost());
        // Output: Espresso + Milk → ₹150.0

        // Stack multiple decorators — each wraps the previous
        Coffee order3 = new CaramelDecorator(
                            new WhippedCreamDecorator(
                                new ChocolateDecorator(
                                    new Espresso())));
        System.out.println(order3.getDescription() + " → ₹" + order3.getCost());
        // Output: Espresso + Chocolate + Whipped Cream + Caramel → ₹245.0

        // Double chocolate? Just wrap twice!
        Coffee order4 = new ChocolateDecorator(
                            new ChocolateDecorator(
                                new HouseBlend()));
        System.out.println(order4.getDescription() + " → ₹" + order4.getCost());
        // Output: House Blend + Chocolate + Chocolate → ₹200.0


        // ══════════════════════════════════════════
        //  DECORATOR PATTERN DEMO — Service Pipeline
        // ══════════════════════════════════════════
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║    DECORATOR — Service Pipeline      ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // Build the pipeline by WRAPPING layers:
        //   Auth → Cache → Logging → Database
        //
        // Request flows INWARD: Auth checks → Cache checks → Log → DB
        // Response flows OUTWARD: DB → Log → Cache stores → Auth passes
        
        Set<String> allowed = Set.of("user_profile", "settings", "dashboard");

        DataService service = new AuthDecorator(            // Layer 3 (outermost)
                                new CachingDecorator(       // Layer 2
                                    new LoggingDecorator(   // Layer 1
                                        new DatabaseService()  // Core
                                    )
                                ), allowed);

        System.out.println("--- First request (cache miss) ---");
        service.fetchData("user_profile");

        System.out.println("\n--- Second request (cache hit) ---");
        service.fetchData("user_profile");

        System.out.println("\n--- Unauthorized request ---");
        try {
            service.fetchData("admin_secrets");
        } catch (SecurityException e) {
            System.out.println("    Caught: " + e.getMessage());
        }

        // NOTICE: DatabaseService was NEVER modified. We added logging,
        // caching, auth, and retry WITHOUT touching the original class.
        // Each decorator is independently testable and reusable.
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧠 VISUAL: How Decorator Wrapping Works                                ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//  ┌─────────────────────────────────────────────────┐
//  │ AuthDecorator                                    │
//  │  ┌─────────────────────────────────────────┐    │
//  │  │ CachingDecorator                         │    │
//  │  │  ┌─────────────────────────────────┐    │    │
//  │  │  │ LoggingDecorator                 │    │    │
//  │  │  │  ┌─────────────────────────┐    │    │    │
//  │  │  │  │ DatabaseService (core)  │    │    │    │
//  │  │  │  └─────────────────────────┘    │    │    │
//  │  │  └─────────────────────────────────┘    │    │
//  │  └─────────────────────────────────────────┘    │
//  └─────────────────────────────────────────────────┘
//
//  Request:  Auth → Cache → Logging → Database
//  Response: Database → Logging → Cache → Auth
//
//  Each layer sees a DataService. Each layer IS a DataService.
//  That's the trick — same interface at every level.
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🔍 WHEN TO USE — Interview Decision Guide                              ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// OBSERVER — use when:
//   ✅ One event triggers reactions in MULTIPLE independent systems
//   ✅ Publishers and subscribers should be decoupled
//   ✅ New reactions may be added in the future without modifying the source
//   ✅ You see "when X happens, do Y AND Z AND W..."
//
//   Real-world: Event buses, pub/sub systems, UI event listeners,
//   Kafka consumers, webhook handlers, Spring @EventListener,
//   JavaScript addEventListener(), React useEffect watching state
//
//   Your n8n context: When a new submission email arrives, multiple
//   things happen (extract data, validate, triage, notify). That's Observer.
//
// DECORATOR — use when:
//   ✅ Add behavior to individual objects dynamically (not entire class)
//   ✅ Combinations of features would cause subclass explosion
//   ✅ Need to add cross-cutting concerns (logging, caching, auth, retry)
//   ✅ You see "same operation, but with optional extra steps around it"
//
//   Real-world: Java I/O streams (BufferedReader wraps FileReader wraps Reader),
//   middleware in Express.js, Spring interceptors, Python decorators (@login_required),
//   OkHttp interceptors, retry/timeout/logging wrappers
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ⚠️ OBSERVER vs DECORATOR — Don't confuse them                          ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//   OBSERVER: "Something happened → tell everyone who subscribed"
//             One source, MANY independent reactors.
//             Reactors DON'T wrap the source.
//
//   DECORATOR: "Do the same thing, but with extra steps"
//              One object WRAPPED in layers.
//              Each layer enhances the SAME operation.
//
//   OBSERVER = fan-out (1 → many)
//   DECORATOR = pipeline (input → layer → layer → layer → output)
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  💡 Java I/O — The Most Famous Decorator Chain                          ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//   // This is PURE Decorator pattern:
//   BufferedReader reader = new BufferedReader(      // Adds buffering
//                             new InputStreamReader( // Adds char decoding
//                               new FileInputStream( // Base: raw bytes
//                                 "data.txt")));
//
//   Each wrapper adds ONE capability around the same read() operation.
//   BufferedReader IS-A Reader AND HAS-A Reader. Classic Decorator.
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (updated)                                         ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── event/                              ← NEW: Observer infrastructure
// │   ├── OrderEvent.java                ← Event data
// │   ├── OrderEventListener.java        ← Observer interface
// │   └── EventManager.java              ← Publisher / Subject
// │
// ├── listener/                           ← NEW: Concrete observers
// │   ├── EmailListener.java
// │   ├── InventoryListener.java
// │   ├── AnalyticsListener.java
// │   ├── SMSListener.java
// │   └── FraudDetectionListener.java
// │
// ├── decorator/                          ← NEW: Decorator infrastructure
// │   ├── coffee/
// │   │   ├── Coffee.java               ← Component interface
// │   │   ├── Espresso.java             ← Concrete component
// │   │   ├── CoffeeDecorator.java      ← Abstract decorator
// │   │   ├── MilkDecorator.java        ← Concrete decorator
// │   │   ├── ChocolateDecorator.java
// │   │   └── CaramelDecorator.java
// │   └── service/
// │       ├── DataService.java           ← Component interface
// │       ├── DatabaseService.java       ← Concrete component
// │       ├── DataServiceDecorator.java  ← Abstract decorator
// │       ├── LoggingDecorator.java
// │       ├── CachingDecorator.java
// │       ├── AuthDecorator.java
// │       └── RetryDecorator.java
// │
// ├── (all previous packages from Lessons 1-5)
// └── Lesson6Main.java
