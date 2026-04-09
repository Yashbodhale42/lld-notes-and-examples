// ============================================================================
// LLD LESSON 1: OOP Foundations & Single Responsibility Principle (SRP)
// ============================================================================
// 
// RULE: "A class should have only ONE reason to change."
//
// If your class handles user data AND sends emails AND logs to a file,
// then a change in email provider, log format, OR user schema all force
// you to modify the SAME class. That's 3 reasons to change = SRP violation.
//
// Let's see this in action with a real scenario: an Employee Management module.
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ❌ BAD DESIGN — The "God Class" that does everything                    ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

import java.util.ArrayList;
import java.util.List;

class EmployeeServiceBAD {
    
    // Problem: This ONE class has FOUR different responsibilities.
    // Any change in any of these areas forces this class to be modified.

    // Responsibility 1: Employee data management
    public void addEmployee(String name, String email, double salary) {
        // save to database
        System.out.println("INSERT INTO employees VALUES ('" + name + "', '" + email + "', " + salary + ")");
    }

    // Responsibility 2: Salary calculation (business logic)
    public double calculateBonus(double salary, int rating) {
        if (rating >= 4) return salary * 0.20;
        if (rating >= 3) return salary * 0.10;
        return salary * 0.05;
    }

    // Responsibility 3: Sending notifications
    public void sendWelcomeEmail(String email, String name) {
        // Imagine this uses SMTP, SendGrid, etc.
        System.out.println("Sending email to " + email + ": Welcome " + name + "!");
    }

    // Responsibility 4: Generating reports
    public String generatePaySlip(String name, double salary, double bonus) {
        return "PaySlip: " + name + " | Salary: " + salary + " | Bonus: " + bonus;
    }
}

// WHY IS THIS BAD?
// - If you switch from MySQL to MongoDB → you modify this class
// - If bonus logic changes → you modify this class
// - If you switch from email to Slack notifications → you modify this class
// - If payslip format changes → you modify this class
// 
// 4 different reasons to change = 4x the risk of introducing bugs.
// Also: how do you unit test calculateBonus() without a database connection?
// Answer: you can't easily. Everything is tangled together.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  ✅ GOOD DESIGN — Each class has exactly ONE responsibility              ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

// --- 1. ENTITY: Pure data representation (no logic, no side effects) ---
class Employee {
    private String id;
    private String name;
    private String email;
    private double salary;
    private int performanceRating;

    // Constructor
    public Employee(String id, String name, String email, double salary) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.salary = salary;
    }

    // Getters & Setters (encapsulation — external code can't directly touch fields)
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getSalary() { return salary; }
    public int getPerformanceRating() { return performanceRating; }
    
    public void setPerformanceRating(int rating) { 
        // Validation lives WITH the data it protects
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.performanceRating = rating; 
    }
}

// KEY INSIGHT: Employee class only changes if the employee DATA MODEL changes.
// That's ONE reason to change. ✅


// --- 2. REPOSITORY: Handles ONLY data persistence ---
// (This is where database operations live)

interface EmployeeRepository {
    // Why an interface? Because we might switch from MySQL to MongoDB later.
    // The rest of the code depends on this CONTRACT, not the implementation.
    void save(Employee employee);
    Employee findById(String id);
    List<Employee> findAll();
}

class MySQLEmployeeRepository implements EmployeeRepository {
    
    @Override
    public void save(Employee employee) {
        // Only database logic here. Nothing about emails or bonuses.
        System.out.println("INSERT INTO employees VALUES ('" 
            + employee.getId() + "', '" 
            + employee.getName() + "', '" 
            + employee.getEmail() + "', " 
            + employee.getSalary() + ")");
    }

    @Override
    public Employee findById(String id) {
        // SELECT * FROM employees WHERE id = ?
        System.out.println("Fetching employee with id: " + id);
        return new Employee(id, "John", "john@example.com", 50000); // placeholder
    }

    @Override
    public List<Employee> findAll() {
        System.out.println("Fetching all employees");
        return new ArrayList<>(); // placeholder
    }
}

// KEY INSIGHT: This class only changes if the DATABASE LAYER changes.
// Switch to MongoDB? Create MongoEmployeeRepository. Zero changes elsewhere. ✅


// --- 3. SERVICE: Business logic ONLY ---

class BonusCalculator {
    
    // Pure business logic. No database calls. No email sending. 
    // Easy to unit test — just pass in values and check the output.
    
    public double calculateBonus(Employee employee) {
        int rating = employee.getPerformanceRating();
        double salary = employee.getSalary();

        if (rating >= 4) return salary * 0.20;  // 20% for top performers
        if (rating >= 3) return salary * 0.10;  // 10% for good performers
        return salary * 0.05;                    // 5% baseline
    }
}

// KEY INSIGHT: This class only changes if the BONUS RULES change. ✅


// --- 4. NOTIFICATION: Handles ONLY sending messages ---

interface NotificationService {
    void sendNotification(String recipient, String message);
}

class EmailNotificationService implements NotificationService {
    
    @Override
    public void sendNotification(String recipient, String message) {
        // Only email logic here
        System.out.println("📧 Sending email to " + recipient + ": " + message);
    }
}

class SlackNotificationService implements NotificationService {
    
    @Override
    public void sendNotification(String recipient, String message) {
        // Only Slack logic here
        System.out.println("💬 Sending Slack to " + recipient + ": " + message);
    }
}

// KEY INSIGHT: Want to add WhatsApp notifications? Create a new class.
// No existing class needs to change. ✅


// --- 5. REPORT GENERATOR: Handles ONLY report formatting ---

class PaySlipGenerator {

    public String generate(Employee employee, double bonus) {
        return String.format(
            "===== PAY SLIP =====\n" +
            "Name   : %s\n" +
            "Salary : ₹%.2f\n" +
            "Bonus  : ₹%.2f\n" +
            "Total  : ₹%.2f\n" +
            "====================",
            employee.getName(),
            employee.getSalary(),
            bonus,
            employee.getSalary() + bonus
        );
    }
}

// KEY INSIGHT: Format changes (PDF, HTML, CSV) only affect this class. ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🔗 ORCHESTRATOR — Wires everything together                            ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

// This is the class that COORDINATES the single-responsibility classes.
// It delegates work; it doesn't DO the work itself.

class EmployeeOnboardingService {

    // Dependencies are INJECTED (not created internally) — this is DIP in action
    private final EmployeeRepository repository;
    private final NotificationService notificationService;
    private final BonusCalculator bonusCalculator;
    private final PaySlipGenerator paySlipGenerator;

    // Constructor Injection — the caller decides WHICH implementations to use
    public EmployeeOnboardingService(
            EmployeeRepository repository,
            NotificationService notificationService,
            BonusCalculator bonusCalculator,
            PaySlipGenerator paySlipGenerator) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.bonusCalculator = bonusCalculator;
        this.paySlipGenerator = paySlipGenerator;
    }

    public void onboardEmployee(Employee employee) {
        // Step 1: Save to DB (delegated to repository)
        repository.save(employee);

        // Step 2: Send welcome notification (delegated to notification service)
        notificationService.sendNotification(
            employee.getEmail(),
            "Welcome to the team, " + employee.getName() + "!"
        );

        System.out.println("✅ Employee " + employee.getName() + " onboarded successfully.");
    }

    public String processPayroll(String employeeId) {
        // Step 1: Fetch employee
        Employee emp = repository.findById(employeeId);

        // Step 2: Calculate bonus (delegated)
        double bonus = bonusCalculator.calculateBonus(emp);

        // Step 3: Generate payslip (delegated)
        return paySlipGenerator.generate(emp, bonus);
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — Putting it all together                                      ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson1Main {
    public static void main(String[] args) {

        // --- Assemble dependencies (in real apps, a DI framework does this) ---
        EmployeeRepository repo = new MySQLEmployeeRepository();
        NotificationService notifier = new EmailNotificationService();
        // ↑ Want Slack instead? Just change to: new SlackNotificationService();
        // NOTHING else in the codebase changes. That's the power of SRP + interfaces.

        BonusCalculator bonusCalc = new BonusCalculator();
        PaySlipGenerator paySlipGen = new PaySlipGenerator();

        EmployeeOnboardingService service = new EmployeeOnboardingService(
            repo, notifier, bonusCalc, paySlipGen
        );

        // --- Use the system ---
        Employee emp = new Employee("E001", "Yash", "yash@company.com", 80000);
        emp.setPerformanceRating(4);

        service.onboardEmployee(emp);

        String payslip = service.processPayroll("E001");
        System.out.println(payslip);
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (how this maps to packages/folders)               ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── model/
// │   └── Employee.java              ← Pure data entity
// │
// ├── repository/
// │   ├── EmployeeRepository.java    ← Interface (contract)
// │   └── MySQLEmployeeRepository.java  ← Concrete implementation
// │
// ├── service/
// │   ├── BonusCalculator.java       ← Business logic
// │   └── EmployeeOnboardingService.java  ← Orchestrator
// │
// ├── notification/
// │   ├── NotificationService.java   ← Interface
// │   ├── EmailNotificationService.java
// │   └── SlackNotificationService.java
// │
// ├── report/
// │   └── PaySlipGenerator.java      ← Report formatting
// │
// └── Lesson1Main.java               ← Entry point
//
// NOTICE: Each package has ONE concern. Each class has ONE job.
// This IS Low-Level Design in practice.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧪 WHY THIS MATTERS: Testability                                       ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// With the BAD design, to test bonus calculation you'd need:
//   - A running database
//   - An SMTP server
//   - The entire EmployeeServiceBAD class
//
// With the GOOD design:
//
//   @Test
//   void testHighPerformerBonus() {
//       Employee emp = new Employee("1", "Test", "t@t.com", 100000);
//       emp.setPerformanceRating(5);
//       BonusCalculator calc = new BonusCalculator();
//       assertEquals(20000.0, calc.calculateBonus(emp));
//   }
//
// No database. No email server. No dependencies. Just logic.
// THAT is the payoff of Single Responsibility.
