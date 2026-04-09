// ============================================================================
// LLD LESSON 5: Factory Pattern + Builder Pattern
// ============================================================================
//
// Both patterns solve OBJECT CREATION problems, but for different reasons:
//
//   FACTORY → "I need an object, but I don't know (or shouldn't decide)
//              which CONCRETE TYPE to create."
//
//   BUILDER → "I know which type I need, but the object is COMPLEX
//              with many optional fields and configurations."
//
// We'll cover three factory variants, then the Builder pattern.
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 1: SIMPLE FACTORY (not a GoF pattern, but widely used)            ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// Scenario: A notification system that creates the right notifier based on
// user preference. The CALLER shouldn't know which class to instantiate.

// ── The interface (from Lesson 1 & 4 — DIP in action) ──────────────────────

interface Notification {
    void send(String recipient, String message);
    String getChannel();  // for logging
}

class EmailNotification implements Notification {
    @Override
    public void send(String recipient, String message) {
        System.out.println("📧 [Email] To: " + recipient + " → " + message);
    }
    @Override
    public String getChannel() { return "EMAIL"; }
}

class SMSNotification implements Notification {
    @Override
    public void send(String recipient, String message) {
        System.out.println("📱 [SMS] To: " + recipient + " → " + message);
    }
    @Override
    public String getChannel() { return "SMS"; }
}

class PushNotification implements Notification {
    @Override
    public void send(String recipient, String message) {
        System.out.println("🔔 [Push] To: " + recipient + " → " + message);
    }
    @Override
    public String getChannel() { return "PUSH"; }
}

class SlackNotification implements Notification {
    @Override
    public void send(String recipient, String message) {
        System.out.println("💬 [Slack] To: #" + recipient + " → " + message);
    }
    @Override
    public String getChannel() { return "SLACK"; }
}


// ── ❌ WITHOUT Factory — caller must know all concrete classes ──────────────

class NotificationSenderBAD {
    
    public void notifyUser(String type, String recipient, String message) {
        // The CALLER is forced to know every concrete class
        // Adding WhatsApp = modifying this method (OCP violation!)
        Notification notification;
        
        if (type.equals("EMAIL"))      notification = new EmailNotification();
        else if (type.equals("SMS"))   notification = new SMSNotification();
        else if (type.equals("PUSH"))  notification = new PushNotification();
        else throw new IllegalArgumentException("Unknown type: " + type);
        
        notification.send(recipient, message);
    }
    // Problem: This if-else chain will exist in EVERY place that creates notifications.
    // 10 places in codebase = 10 places to update when you add WhatsApp.
}


// ── ✅ SIMPLE FACTORY — Centralizes creation logic in one place ─────────────

/**
 * Simple Factory: A single class with a static/instance method that
 * creates and returns the right object based on input.
 * 
 * NOT a GoF pattern, but the most common "factory" you'll see in practice.
 * Think of it as a centralized "if-else for object creation."
 */
class NotificationFactory {

    // Option A: if-else based (simple, fine for small number of types)
    public static Notification createNotification(String type) {
        switch (type.toUpperCase()) {
            case "EMAIL": return new EmailNotification();
            case "SMS":   return new SMSNotification();
            case "PUSH":  return new PushNotification();
            case "SLACK": return new SlackNotification();
            default:
                throw new IllegalArgumentException("Unknown notification type: " + type);
        }
    }

    // Option B: Registry-based (better for many types, OCP-friendly)
    // Same idea as BonusStrategyFactory from Lesson 2
    private static final Map<String, Supplier<Notification>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("EMAIL", EmailNotification::new);
        REGISTRY.put("SMS",   SMSNotification::new);
        REGISTRY.put("PUSH",  PushNotification::new);
        REGISTRY.put("SLACK", SlackNotification::new);
    }

    public static Notification create(String type) {
        Supplier<Notification> supplier = REGISTRY.get(type.toUpperCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown notification type: " + type);
        }
        return supplier.get();  // Creates a NEW instance each time
    }

    // ✅ To add WhatsApp: one line → REGISTRY.put("WHATSAPP", WhatsAppNotification::new);
    // No switch/if-else to modify. Existing code untouched.
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 2: FACTORY METHOD PATTERN (GoF — uses inheritance)                ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// Key difference from Simple Factory:
//   Simple Factory  → ONE class with a method that picks the type
//   Factory Method  → An ABSTRACT method that SUBCLASSES override to create objects
//
// Use when: Different "contexts" need different objects, and you want each
// context to define its own creation logic.
//
// Scenario: A document processing system. Different document types need
// different parsers, validators, and renderers.

// ── The products (what gets created) ────────────────────────────────────────

interface Document {
    void open();
    void save();
    String getType();
}

class PDFDocument implements Document {
    private String content;
    @Override public void open() { System.out.println("Opening PDF with Adobe renderer"); }
    @Override public void save() { System.out.println("Saving as .pdf"); }
    @Override public String getType() { return "PDF"; }
}

class WordDocument implements Document {
    private String content;
    @Override public void open() { System.out.println("Opening DOCX with Word parser"); }
    @Override public void save() { System.out.println("Saving as .docx"); }
    @Override public String getType() { return "WORD"; }
}

class SpreadsheetDocument implements Document {
    private String content;
    @Override public void open() { System.out.println("Opening XLSX with grid renderer"); }
    @Override public void save() { System.out.println("Saving as .xlsx"); }
    @Override public String getType() { return "SPREADSHEET"; }
}


// ── The Factory Method pattern ──────────────────────────────────────────────

/**
 * ABSTRACT CREATOR: Defines the "factory method" that subclasses must implement.
 * Also contains shared logic that uses the product (template method style).
 */
abstract class DocumentProcessor {

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  THIS is the Factory Method — abstract, subclasses decide      ║
    // ║  which concrete Document to create.                            ║
    // ╚═════════════════════════════════════════════════════════════════╝
    protected abstract Document createDocument();

    /**
     * Shared workflow that ALL processors follow.
     * The specific Document type is determined by the subclass.
     * This method doesn't know (or care) whether it's PDF or Word.
     */
    public void processDocument() {
        Document doc = createDocument();  // Factory method call — subclass decides the type
        System.out.println("Processing " + doc.getType() + " document...");
        doc.open();
        // ... do processing ...
        doc.save();
        System.out.println("Done processing " + doc.getType() + "\n");
    }
}

// CONCRETE CREATORS: Each one creates a different product

class PDFProcessor extends DocumentProcessor {
    @Override
    protected Document createDocument() {
        return new PDFDocument();  // This subclass creates PDFs
    }
}

class WordProcessor extends DocumentProcessor {
    @Override
    protected Document createDocument() {
        return new WordDocument();  // This subclass creates Word docs
    }
}

class SpreadsheetProcessor extends DocumentProcessor {
    @Override
    protected Document createDocument() {
        return new SpreadsheetDocument();  // This subclass creates spreadsheets
    }
}

// USAGE:
// DocumentProcessor processor = new PDFProcessor();
// processor.processDocument();  // Creates PDF internally, processes it
//
// To add PowerPoint: Create PowerPointDocument + PowerPointProcessor.
// ZERO changes to DocumentProcessor or any existing processor. OCP ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 3: ABSTRACT FACTORY (Factory of Factories)                        ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// Use when: You need to create FAMILIES of related objects that must be
// used together. Objects from different families shouldn't be mixed.
//
// Scenario: A cross-platform UI toolkit. Each platform (Windows, Mac, Linux)
// has its own Button, Checkbox, and TextField — but they must be consistent.
// You can't mix a Windows Button with a Mac Checkbox.

// ── Product interfaces (what gets created) ──────────────────────────────────

interface Button {
    void render();
    void onClick(Runnable action);
}

interface Checkbox {
    void render();
    boolean isChecked();
}

interface TextField {
    void render();
    String getText();
}


// ── Windows family ──────────────────────────────────────────────────────────

class WindowsButton implements Button {
    @Override public void render() { System.out.println("[Windows] ┌──Button──┐"); }
    @Override public void onClick(Runnable action) { action.run(); }
}

class WindowsCheckbox implements Checkbox {
    @Override public void render() { System.out.println("[Windows] ☑ Checkbox"); }
    @Override public boolean isChecked() { return true; }
}

class WindowsTextField implements TextField {
    @Override public void render() { System.out.println("[Windows] |___TextField___|"); }
    @Override public String getText() { return "windows-input"; }
}


// ── Mac family ──────────────────────────────────────────────────────────────

class MacButton implements Button {
    @Override public void render() { System.out.println("[Mac] (  Button  )"); }
    @Override public void onClick(Runnable action) { action.run(); }
}

class MacCheckbox implements Checkbox {
    @Override public void render() { System.out.println("[Mac] ✓ Checkbox"); }
    @Override public boolean isChecked() { return true; }
}

class MacTextField implements TextField {
    @Override public void render() { System.out.println("[Mac] ╭──TextField──╮"); }
    @Override public String getText() { return "mac-input"; }
}


// ── Abstract Factory interface ──────────────────────────────────────────────

/**
 * The ABSTRACT FACTORY: creates a FAMILY of related products.
 * Each method returns an interface type — the caller never sees concrete classes.
 */
interface UIComponentFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField();
}

// ── Concrete factories (one per family) ─────────────────────────────────────

class WindowsUIFactory implements UIComponentFactory {
    @Override public Button createButton() { return new WindowsButton(); }
    @Override public Checkbox createCheckbox() { return new WindowsCheckbox(); }
    @Override public TextField createTextField() { return new WindowsTextField(); }
}

class MacUIFactory implements UIComponentFactory {
    @Override public Button createButton() { return new MacButton(); }
    @Override public Checkbox createCheckbox() { return new MacCheckbox(); }
    @Override public TextField createTextField() { return new MacTextField(); }
}


// ── Application uses the factory — completely platform-agnostic ─────────────

class LoginForm {
    private Button loginButton;
    private TextField usernameField;
    private TextField passwordField;
    private Checkbox rememberMe;

    /**
     * The form doesn't know if it's on Windows or Mac.
     * It just asks the factory for components. ALL components come from
     * the SAME family — consistency guaranteed.
     */
    public LoginForm(UIComponentFactory factory) {
        this.usernameField = factory.createTextField();
        this.passwordField = factory.createTextField();
        this.rememberMe = factory.createCheckbox();
        this.loginButton = factory.createButton();
    }

    public void render() {
        System.out.println("=== Login Form ===");
        usernameField.render();
        passwordField.render();
        rememberMe.render();
        loginButton.render();
        System.out.println();
    }
}

// USAGE:
// UIComponentFactory factory = new MacUIFactory();
// LoginForm form = new LoginForm(factory);
// form.render();  // All Mac components — consistent look & feel
//
// Switch to Windows: just change the factory. LoginForm code = UNCHANGED.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 4: BUILDER PATTERN                                                ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// PROBLEM: Constructors with many parameters, especially optional ones.
//
// new User("Yash", "yash@co.com", 28, "Bangalore", "ENGINEERING", 
//          true, false, "IST", "dark", null, null, 3, "premium");
// 
// Which boolean is which? What are the nulls? This is unreadable.
//
// BUILDER solves this with:
//   1. Method chaining (fluent API): .name("Yash").email("yash@co.com")
//   2. Only set what you need — everything else gets sensible defaults
//   3. Validation at build time — object is always in a valid state

// ── ❌ BAD: Telescoping Constructor Anti-Pattern ────────────────────────────

class UserBAD {
    private String name;
    private String email;
    private int age;
    private String city;
    private String department;
    private boolean isActive;
    private boolean isAdmin;
    private String timezone;
    private String theme;
    private String phone;     // optional
    private String avatar;    // optional
    private int loginCount;
    private String plan;

    // Constructor 1: required fields only
    public UserBAD(String name, String email) {
        this(name, email, 0, null, null, true, false, "UTC", "light", null, null, 0, "free");
    }

    // Constructor 2: required + some optional
    public UserBAD(String name, String email, int age, String city) {
        this(name, email, age, city, null, true, false, "UTC", "light", null, null, 0, "free");
    }

    // Constructor 3: all fields — NIGHTMARE to call correctly
    public UserBAD(String name, String email, int age, String city, String department,
                   boolean isActive, boolean isAdmin, String timezone, String theme,
                   String phone, String avatar, int loginCount, String plan) {
        this.name = name;
        this.email = email;
        this.age = age;
        // ... 10 more assignments
        // WHO CAN READ THIS AT THE CALL SITE?
    }
}

// Calling code:
// new UserBAD("Yash", "yash@co.com", 28, "Bangalore", "ENGINEERING",
//             true, false, "IST", "dark", null, null, 0, "premium");
// ↑ What is 'true'? What is 'false'? What are the nulls? Impossible to read.


// ── ✅ GOOD: Builder Pattern ────────────────────────────────────────────────

/**
 * User is IMMUTABLE — all fields are final.
 * The ONLY way to create a User is through the Builder.
 * This guarantees every User object is in a valid state.
 */
class User {
    // All fields are final — set once during construction, never changed
    private final String name;          // required
    private final String email;         // required
    private final int age;              // optional, default 0
    private final String city;          // optional
    private final String department;    // optional
    private final boolean isActive;     // optional, default true
    private final boolean isAdmin;      // optional, default false
    private final String timezone;      // optional, default "UTC"
    private final String theme;         // optional, default "light"
    private final String phone;         // optional
    private final String avatar;        // optional
    private final int loginCount;       // optional, default 0
    private final String plan;          // optional, default "free"

    // PRIVATE constructor — only Builder can call this
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.age = builder.age;
        this.city = builder.city;
        this.department = builder.department;
        this.isActive = builder.isActive;
        this.isAdmin = builder.isAdmin;
        this.timezone = builder.timezone;
        this.theme = builder.theme;
        this.phone = builder.phone;
        this.avatar = builder.avatar;
        this.loginCount = builder.loginCount;
        this.plan = builder.plan;
    }

    // Getters only — no setters (immutable)
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    public String getCity() { return city; }
    public String getDepartment() { return department; }
    public boolean isActive() { return isActive; }
    public boolean isAdmin() { return isAdmin; }
    public String getTimezone() { return timezone; }
    public String getTheme() { return theme; }
    public String getPhone() { return phone; }
    public String getAvatar() { return avatar; }
    public int getLoginCount() { return loginCount; }
    public String getPlan() { return plan; }

    @Override
    public String toString() {
        return "User{name='" + name + "', email='" + email + "', city='" + city
            + "', dept='" + department + "', plan='" + plan + "', admin=" + isAdmin + "}";
    }

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  THE BUILDER — Static inner class                              ║
    // ╚═════════════════════════════════════════════════════════════════╝
    
    public static class Builder {
        // Required fields — set in constructor
        private final String name;
        private final String email;

        // Optional fields — sensible defaults
        private int age = 0;
        private String city = null;
        private String department = null;
        private boolean isActive = true;
        private boolean isAdmin = false;
        private String timezone = "UTC";
        private String theme = "light";
        private String phone = null;
        private String avatar = null;
        private int loginCount = 0;
        private String plan = "free";

        /**
         * Builder constructor takes ONLY required fields.
         * Everything else is optional with defaults.
         */
        public Builder(String name, String email) {
            // Validate required fields immediately
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name is required");
            if (email == null || !email.contains("@"))
                throw new IllegalArgumentException("Valid email is required");
            this.name = name;
            this.email = email;
        }

        // Each setter returns 'this' — enables method chaining
        public Builder age(int age) {
            if (age < 0 || age > 150) throw new IllegalArgumentException("Invalid age");
            this.age = age;
            return this;     // ← Returns 'this' for chaining
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder isAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public Builder plan(String plan) {
            this.plan = plan;
            return this;
        }

        /**
         * build() — Final validation + create the immutable object.
         * This is where CROSS-FIELD validation happens.
         */
        public User build() {
            // Cross-field validation — can't do this in a constructor easily
            if (isAdmin && plan.equals("free")) {
                throw new IllegalStateException("Admins must have a premium plan");
            }
            return new User(this);
        }
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — Everything in action                                         ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson5Main {
    public static void main(String[] args) {

        // ── SIMPLE FACTORY ──
        System.out.println("====== SIMPLE FACTORY ======\n");
        Notification n1 = NotificationFactory.create("EMAIL");
        Notification n2 = NotificationFactory.create("SLACK");
        n1.send("yash@company.com", "Your PR was approved");
        n2.send("dev-team", "Deployment complete");

        // ── FACTORY METHOD ──
        System.out.println("\n====== FACTORY METHOD ======\n");
        DocumentProcessor pdfProc = new PDFProcessor();
        DocumentProcessor wordProc = new WordProcessor();
        pdfProc.processDocument();    // Internally creates PDFDocument
        wordProc.processDocument();   // Internally creates WordDocument

        // ── ABSTRACT FACTORY ──
        System.out.println("====== ABSTRACT FACTORY ======\n");
        
        // Simulate detecting the OS
        String os = "MAC"; // could come from System.getProperty("os.name")
        UIComponentFactory uiFactory = os.equals("MAC") 
            ? new MacUIFactory() 
            : new WindowsUIFactory();
        
        LoginForm form = new LoginForm(uiFactory);
        form.render();  // All components from the SAME family

        // ── BUILDER PATTERN ──
        System.out.println("====== BUILDER PATTERN ======\n");

        // Minimal user — only required fields
        User basic = new User.Builder("Riya", "riya@example.com")
            .build();
        System.out.println("Basic:  " + basic);

        // Fully configured user — readable, self-documenting
        User admin = new User.Builder("Yash", "yash@company.com")
            .age(28)
            .city("Bangalore")
            .department("ENGINEERING")
            .isAdmin(true)
            .plan("premium")           // Required because isAdmin=true
            .timezone("IST")
            .theme("dark")
            .build();
        System.out.println("Admin:  " + admin);

        // Compare readability:
        // BUILDER: .isAdmin(true).plan("premium")     ← Crystal clear
        // CONSTRUCTOR: true, false, "IST", "dark"...   ← What is what??

        // Cross-field validation in action:
        try {
            User invalid = new User.Builder("Hack", "hack@evil.com")
                .isAdmin(true)
                .plan("free")   // ❌ Admin + free plan = invalid
                .build();
        } catch (IllegalStateException e) {
            System.out.println("\n⛔ Validation caught: " + e.getMessage());
        }
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧠 WHEN TO USE WHICH? — Interview Decision Chart                      ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// ┌──────────────────────┬──────────────────────────────────────────────────┐
// │ Pattern              │ Use When...                                      │
// ├──────────────────────┼──────────────────────────────────────────────────┤
// │ Simple Factory       │ One method picks from several concrete types     │
// │                      │ based on a parameter (string, enum).             │
// │                      │ Ex: NotificationFactory.create("SMS")            │
// ├──────────────────────┼──────────────────────────────────────────────────┤
// │ Factory Method       │ Subclasses need to decide which object to create.│
// │                      │ The parent defines the workflow; child picks     │
// │                      │ the product. Uses INHERITANCE.                   │
// │                      │ Ex: PDFProcessor vs WordProcessor                │
// ├──────────────────────┼──────────────────────────────────────────────────┤
// │ Abstract Factory     │ Need FAMILIES of related objects that must be    │
// │                      │ used together. Prevents mixing incompatible      │
// │                      │ products. Uses COMPOSITION.                      │
// │                      │ Ex: WindowsUI vs MacUI (button + checkbox + ...) │
// ├──────────────────────┼──────────────────────────────────────────────────┤
// │ Builder              │ Object has many fields (especially optional).    │
// │                      │ Constructors become unreadable. Need validation  │
// │                      │ at creation time. Want IMMUTABLE objects.        │
// │                      │ Ex: User, HttpRequest, Query, Config             │
// └──────────────────────┴──────────────────────────────────────────────────┘
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  REAL-WORLD EXAMPLES YOU ALREADY USE                                    ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// SIMPLE FACTORY:
//   • Calendar.getInstance()          → returns GregorianCalendar or BuddhistCalendar
//   • NumberFormat.getInstance(locale) → returns locale-specific formatter
//   • LoggerFactory.getLogger(class)  → SLF4J logger factory
//
// FACTORY METHOD:
//   • Collection.iterator()           → ArrayList returns ArrayIterator, 
//                                       LinkedList returns LinkedIterator
//   • HttpServletResponse.getWriter() → subclasses create specific writers
//
// ABSTRACT FACTORY:
//   • JDBC DriverManager              → MySQL driver creates MySQL Connection, Statement
//                                       Postgres driver creates Postgres Connection, Statement
//   • javax.xml.parsers               → DocumentBuilderFactory (DOM vs SAX family)
//
// BUILDER:
//   • StringBuilder                   → .append().append().toString()
//   • HttpRequest.newBuilder()        → .uri().header().POST().build()
//   • Lombok @Builder                 → auto-generates builder at compile time
//   • Stream.builder()                → .add().add().build()
//   • Retrofit / OkHttp               → OkHttpClient.Builder()
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (updated)                                         ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── model/
// │   ├── Employee.java
// │   ├── Order.java
// │   ├── User.java                       ← Contains inner Builder class
// │   └── ...
// │
// ├── factory/
// │   ├── NotificationFactory.java        ← Simple Factory
// │   └── BonusStrategyFactory.java       (from Lesson 2)
// │
// ├── document/
// │   ├── Document.java                   ← Product interface
// │   ├── PDFDocument.java
// │   ├── WordDocument.java
// │   ├── DocumentProcessor.java          ← Abstract creator (Factory Method)
// │   ├── PDFProcessor.java               ← Concrete creator
// │   └── WordProcessor.java              ← Concrete creator
// │
// ├── ui/
// │   ├── Button.java                     ← Product interface
// │   ├── Checkbox.java                   ← Product interface
// │   ├── TextField.java                  ← Product interface
// │   ├── UIComponentFactory.java         ← Abstract Factory interface
// │   ├── WindowsUIFactory.java           ← Concrete factory
// │   ├── MacUIFactory.java               ← Concrete factory
// │   └── LoginForm.java                  ← Client
// │
// ├── notification/
// │   ├── Notification.java               ← Interface
// │   ├── EmailNotification.java
// │   ├── SMSNotification.java
// │   ├── PushNotification.java
// │   └── SlackNotification.java
// │
// ├── (all previous packages from Lessons 1-4)
// └── Lesson5Main.java
