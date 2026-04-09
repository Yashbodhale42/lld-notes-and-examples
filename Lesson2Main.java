// ============================================================================
// LLD LESSON 2: Open/Closed Principle (OCP) + Strategy Pattern
// ============================================================================
//
// RULE: "Classes should be OPEN for extension, CLOSED for modification."
//
// Translation: You should be able to ADD new behavior to the system
// WITHOUT editing existing, tested, working code.
//
// Why? Because every time you modify existing code, you risk breaking
// something that already works. Tested code should stay untouched.
//
// The STRATEGY PATTERN is the most common way to achieve OCP.
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  SCENARIO: Expanding Bonus Rules                                        ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// In Lesson 1, our BonusCalculator had ONE formula. But now requirements grow:
//
// - Engineering dept: bonus based on performance rating (existing logic)
// - Sales dept:       bonus based on revenue generated (commission-based)
// - HR dept:          flat 8% bonus regardless of rating
// - Interns:          no bonus
//
// Let's see how a naive developer handles this, then the OCP way.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ❌ BAD DESIGN — Modification every time a new department is added       ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

class BonusCalculatorBAD {

    public double calculateBonus(Employee employee) {
        String dept = employee.getDepartment();

        // Every new department = a new if/else block HERE.
        // This method keeps growing. You're MODIFYING tested code each time.

        if (dept.equals("ENGINEERING")) {
            int rating = employee.getPerformanceRating();
            if (rating >= 4) return employee.getSalary() * 0.20;
            if (rating >= 3) return employee.getSalary() * 0.10;
            return employee.getSalary() * 0.05;

        } else if (dept.equals("SALES")) {
            // Commission-based: 5% of revenue generated
            return employee.getRevenueGenerated() * 0.05;

        } else if (dept.equals("HR")) {
            return employee.getSalary() * 0.08;

        } else if (dept.equals("INTERN")) {
            return 0;

        }
        // ⚠️ 6 months later: "Add bonus logic for Marketing dept"
        // → You open THIS file, add another else-if, risk breaking Sales logic
        //   that was working fine for months. 
        //
        // ⚠️ 12 months later: 15 departments, 200-line method, nobody wants
        //   to touch this file. Welcome to legacy code.

        return 0;
    }
}

// PROBLEMS:
// 1. Violates OCP — you MODIFY this class for every new department
// 2. Violates SRP — one class knows the rules for ALL departments
// 3. Untestable in isolation — you can't test Sales logic without Engineering logic existing
// 4. Risky — changing HR bonus might accidentally break Engineering bonus
// 5. The if-else chain will grow forever


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ✅ GOOD DESIGN — Strategy Pattern (OCP in action)                      ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// THE KEY IDEA:
// Instead of one class with many if-else branches, we:
//   1. Define a STRATEGY INTERFACE (the contract)
//   2. Create one CONCRETE STRATEGY per variation
//   3. The context class (BonusCalculator) delegates to whatever strategy it receives
//
// New department? Create a new class. Existing classes stay UNTOUCHED.


// ── Step 1: Define the Strategy Interface ───────────────────────────────────

/**
 * This is the CONTRACT that all bonus strategies must follow.
 * Any class that implements this interface is a valid bonus strategy.
 * 
 * Think of this as a "plug" shape — any strategy that fits this shape
 * can be plugged into the system.
 */
interface BonusStrategy {
    
    /**
     * Calculate bonus for the given employee.
     * Each implementation defines its own formula.
     */
    double calculate(Employee employee);

    /**
     * Human-readable name for logging/debugging.
     */
    String getStrategyName();
}


// ── Step 2: Create Concrete Strategies (one per variation) ──────────────────

/**
 * Engineering bonus: performance-rating based.
 * This class knows ONLY about engineering bonus rules. Nothing else.
 */
class EngineeringBonusStrategy implements BonusStrategy {

    @Override
    public double calculate(Employee employee) {
        int rating = employee.getPerformanceRating();
        double salary = employee.getSalary();

        if (rating >= 4) return salary * 0.20;  // Top performers: 20%
        if (rating >= 3) return salary * 0.10;  // Good performers: 10%
        return salary * 0.05;                    // Baseline: 5%
    }

    @Override
    public String getStrategyName() {
        return "Engineering Performance-Based Bonus";
    }
}


/**
 * Sales bonus: commission-based on revenue generated.
 * Completely independent from Engineering logic.
 */
class SalesBonusStrategy implements BonusStrategy {

    private static final double COMMISSION_RATE = 0.05;      // 5% standard
    private static final double HIGH_PERFORMER_RATE = 0.08;  // 8% for top sellers
    private static final double HIGH_REVENUE_THRESHOLD = 500000;

    @Override
    public double calculate(Employee employee) {
        double revenue = employee.getRevenueGenerated();

        // Top sellers who brought in >5L get a higher commission rate
        if (revenue > HIGH_REVENUE_THRESHOLD) {
            return revenue * HIGH_PERFORMER_RATE;
        }
        return revenue * COMMISSION_RATE;
    }

    @Override
    public String getStrategyName() {
        return "Sales Commission-Based Bonus";
    }
}


/**
 * HR bonus: flat percentage, simple and predictable.
 */
class HRBonusStrategy implements BonusStrategy {

    private static final double FLAT_RATE = 0.08;  // 8% flat

    @Override
    public double calculate(Employee employee) {
        return employee.getSalary() * FLAT_RATE;
    }

    @Override
    public String getStrategyName() {
        return "HR Flat-Rate Bonus";
    }
}


/**
 * Intern bonus: no bonus.
 * Yes, even "no bonus" is a strategy — it makes the system consistent.
 * No special null checks needed anywhere.
 */
class InternBonusStrategy implements BonusStrategy {

    @Override
    public double calculate(Employee employee) {
        return 0;
    }

    @Override
    public String getStrategyName() {
        return "Intern (No Bonus)";
    }
}


// ── Step 3: The Context Class — Uses strategies, doesn't know their details ─

/**
 * BonusCalculator is now CLOSED for modification.
 * It doesn't know (or care) how many department types exist.
 * It just calls whatever strategy it's given.
 */
class BonusCalculator {

    // The strategy is INJECTED — not hardcoded
    private BonusStrategy strategy;

    public BonusCalculator(BonusStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Switch strategy at runtime if needed.
     * Example: an employee transfers from Engineering to Sales mid-year.
     */
    public void setStrategy(BonusStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculateBonus(Employee employee) {
        double bonus = strategy.calculate(employee);
        System.out.println("[" + strategy.getStrategyName() + "] "
            + employee.getName() + " → Bonus: ₹" + bonus);
        return bonus;
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🏭 BONUS: Strategy Factory — Maps department to strategy automatically ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// In real apps, you don't want the caller to manually pick the strategy.
// A factory does the mapping. This is a PREVIEW of the Factory pattern
// (full lesson coming in Lesson 5).

class BonusStrategyFactory {

    // Registry of strategies — add new ones here, nowhere else.
    private static final Map<String, BonusStrategy> STRATEGY_MAP = new HashMap<>();

    static {
        STRATEGY_MAP.put("ENGINEERING", new EngineeringBonusStrategy());
        STRATEGY_MAP.put("SALES",       new SalesBonusStrategy());
        STRATEGY_MAP.put("HR",          new HRBonusStrategy());
        STRATEGY_MAP.put("INTERN",      new InternBonusStrategy());
    }

    /**
     * Returns the right strategy for the department.
     * 
     * ✅ To add Marketing bonus: 
     *    1. Create MarketingBonusStrategy (new file)
     *    2. Add one line here: STRATEGY_MAP.put("MARKETING", new MarketingBonusStrategy())
     *    3. DONE. No existing strategy class is modified.
     */
    public static BonusStrategy getStrategy(String department) {
        BonusStrategy strategy = STRATEGY_MAP.get(department.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                "No bonus strategy registered for department: " + department
            );
        }
        return strategy;
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📝 Updated Employee class (building on Lesson 1)                       ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

class Employee {
    private String id;
    private String name;
    private String email;
    private double salary;
    private int performanceRating;
    private String department;
    private double revenueGenerated;  // relevant for Sales

    public Employee(String id, String name, String email, double salary, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.salary = salary;
        this.department = department;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getSalary() { return salary; }
    public int getPerformanceRating() { return performanceRating; }
    public String getDepartment() { return department; }
    public double getRevenueGenerated() { return revenueGenerated; }

    // --- Setters with validation ---
    public void setPerformanceRating(int rating) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be 1-5");
        this.performanceRating = rating;
    }

    public void setRevenueGenerated(double revenue) {
        if (revenue < 0)
            throw new IllegalArgumentException("Revenue cannot be negative");
        this.revenueGenerated = revenue;
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — See the full pattern in action                               ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson2Main {
    public static void main(String[] args) {

        // --- Create employees from different departments ---
        Employee yash = new Employee("E001", "Yash", "yash@co.com", 100000, "ENGINEERING");
        yash.setPerformanceRating(4);

        Employee priya = new Employee("E002", "Priya", "priya@co.com", 80000, "SALES");
        priya.setRevenueGenerated(600000);  // brought in 6L revenue

        Employee amit = new Employee("E003", "Amit", "amit@co.com", 70000, "HR");

        Employee intern = new Employee("E004", "Riya", "riya@co.com", 25000, "INTERN");


        // --- APPROACH 1: Manual strategy selection ---
        System.out.println("=== Manual Strategy Selection ===\n");
        
        BonusCalculator calc = new BonusCalculator(new EngineeringBonusStrategy());
        calc.calculateBonus(yash);      // Uses Engineering strategy

        calc.setStrategy(new SalesBonusStrategy());
        calc.calculateBonus(priya);     // Switched to Sales strategy at runtime!

        calc.setStrategy(new HRBonusStrategy());
        calc.calculateBonus(amit);      // Switched to HR strategy


        // --- APPROACH 2: Factory-driven (automatic selection) ---
        System.out.println("\n=== Factory-Driven Strategy Selection ===\n");

        // Process ANY employee — the factory picks the right strategy
        Employee[] allEmployees = { yash, priya, amit, intern };

        for (Employee emp : allEmployees) {
            BonusStrategy strategy = BonusStrategyFactory.getStrategy(emp.getDepartment());
            BonusCalculator calculator = new BonusCalculator(strategy);
            calculator.calculateBonus(emp);
        }

        // OUTPUT:
        // [Engineering Performance-Based Bonus] Yash → Bonus: ₹20000.0
        // [Sales Commission-Based Bonus] Priya → Bonus: ₹48000.0
        // [HR Flat-Rate Bonus] Amit → Bonus: ₹5600.0
        // [Intern (No Bonus)] Riya → Bonus: ₹0.0
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (updated from Lesson 1)                           ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── model/
// │   └── Employee.java
// │
// ├── repository/
// │   ├── EmployeeRepository.java         (from Lesson 1)
// │   └── MySQLEmployeeRepository.java    (from Lesson 1)
// │
// ├── service/
// │   ├── BonusCalculator.java            ← Context (uses strategy)
// │   └── EmployeeOnboardingService.java  (from Lesson 1)
// │
// ├── strategy/                            ← NEW PACKAGE
// │   ├── BonusStrategy.java              ← Interface (contract)
// │   ├── EngineeringBonusStrategy.java   ← Concrete strategy
// │   ├── SalesBonusStrategy.java         ← Concrete strategy
// │   ├── HRBonusStrategy.java            ← Concrete strategy
// │   └── InternBonusStrategy.java        ← Concrete strategy
// │
// ├── factory/                             ← NEW PACKAGE
// │   └── BonusStrategyFactory.java       ← Maps dept → strategy
// │
// ├── notification/                        (from Lesson 1)
// ├── report/                              (from Lesson 1)
// └── Lesson2Main.java
//
// KEY OBSERVATION: Adding "MarketingBonusStrategy" means:
//   → 1 new file in strategy/
//   → 1 new line in BonusStrategyFactory
//   → 0 changes to BonusCalculator, Employee, or any other class
//   That's OCP. ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧠 STRATEGY PATTERN — Mental Model                                     ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
//   ┌──────────────────┐         ┌──────────────────────┐
//   │  BonusCalculator │────────▶│  <<interface>>        │
//   │  (Context)       │ uses    │  BonusStrategy        │
//   │                  │         │  + calculate(emp)     │
//   └──────────────────┘         └──────────┬───────────┘
//                                           │ implements
//                          ┌────────────────┼────────────────┐
//                          │                │                │
//                   ┌──────▼──────┐  ┌──────▼──────┐  ┌─────▼───────┐
//                   │ Engineering │  │   Sales     │  │    HR       │
//                   │ Strategy    │  │  Strategy   │  │  Strategy   │
//                   └─────────────┘  └─────────────┘  └─────────────┘
//
//   The Context doesn't know which strategy it's using.
//   It just calls calculate(). The strategy handles the rest.
//   That's polymorphism doing the heavy lifting.
//
//
// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🔍 WHEN TO USE STRATEGY PATTERN (Interview Cheat Sheet)                ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// USE Strategy when you see:
//   ✅ Multiple algorithms/rules that do the same "type" of work differently
//   ✅ if-else or switch chains that pick behavior based on a "type" field
//   ✅ Behavior that needs to be swappable at runtime
//   ✅ Need to add new variations without modifying existing code
//
// REAL-WORLD examples of Strategy:
//   • Payment processing  → CreditCardPayment, UPIPayment, WalletPayment
//   • Sorting algorithms  → QuickSort, MergeSort, TimSort
//   • Compression         → ZipCompression, GzipCompression, RarCompression
//   • Authentication      → OAuthStrategy, JWTStrategy, SAMLStrategy
//   • Pricing/Discount    → FlatDiscount, PercentDiscount, BuyOneGetOne
//   • Notification        → EmailNotification, SMSNotification, PushNotification
//   • Route calculation   → ShortestPath, FastestRoute, ScenicRoute
//
// In interviews, if you see an if-else chain based on TYPE → Strategy pattern.
