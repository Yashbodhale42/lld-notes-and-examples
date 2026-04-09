// ============================================================================
// LLD LESSON 3: Liskov Substitution Principle (LSP) + 
//               Interface Segregation Principle (ISP)
// ============================================================================
//
// These two principles work together to answer:
//   LSP → "When is inheritance SAFE to use?"
//   ISP → "How do I design interfaces that don't TRAP implementors?"
//
// These are the two most commonly VIOLATED principles in real codebases,
// and interviewers love testing your understanding of them.
// ============================================================================


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 1: LISKOV SUBSTITUTION PRINCIPLE (LSP)                            ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// RULE: "If class B extends class A, you should be able to replace A with B
//        ANYWHERE in the code without breaking behavior."
//
// In plain terms: A subclass must HONOR the contract of its parent.
// If the parent promises something, the child can't break that promise.
//
// The classic trap: Rectangle → Square
// ============================================================================


// ── THE CLASSIC VIOLATION: Rectangle & Square ───────────────────────────────

// Seems logical: "A square IS a rectangle" (true in math).
// But in code, this inheritance BREAKS things.

import java.util.List;

class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return width * height;
    }
}

// ❌ BAD: Square overrides setters to enforce its constraint
class Square extends Rectangle {

    @Override
    public void setWidth(int width) {
        // A square must have equal sides, so we set BOTH
        this.width = width;
        this.height = width;  // ⚠️ SURPRISE! Setting width also changes height
    }

    @Override
    public void setHeight(int height) {
        this.width = height;  // ⚠️ SURPRISE! Setting height also changes width
        this.height = height;
    }
}

// Now look at this innocent method that works with ANY Rectangle:
class AreaCalculator {
    
    public void printArea(Rectangle rect) {
        rect.setWidth(5);
        rect.setHeight(3);
        
        // For a Rectangle: 5 * 3 = 15 ← EXPECTED
        // For a Square:    3 * 3 = 9  ← BROKEN! setHeight(3) overwrote width!
        
        System.out.println("Expected: 15, Got: " + rect.getArea());
        // If someone passes a Square here, the behavior BREAKS.
        // That's an LSP violation.
    }
}

// WHY IT'S BROKEN:
// Rectangle's contract says: "setWidth changes width, setHeight changes height,
// and these are INDEPENDENT operations."
// Square violates that contract — setting one dimension secretly changes the other.
// Any code that relies on the parent's contract will produce wrong results.


// ── ✅ THE FIX: Don't force inheritance where behavior differs ──────────────

// Option 1: Use a common interface instead of inheritance

interface Shape {
    int getArea();
    String describe();
}

class RectangleFixed implements Shape {
    private final int width;   // Made immutable — no surprising mutations
    private final int height;

    public RectangleFixed(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getArea() { return width * height; }

    @Override
    public String describe() { return "Rectangle(" + width + "x" + height + ")"; }
}

class SquareFixed implements Shape {
    private final int side;

    public SquareFixed(int side) {
        this.side = side;
    }

    @Override
    public int getArea() { return side * side; }

    @Override
    public String describe() { return "Square(" + side + ")"; }
}

// Now Rectangle and Square are SIBLINGS (both implement Shape),
// not parent-child. No broken contracts. No surprises.
// Any code that accepts Shape works correctly with both. ✅


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  LSP IN OUR EMPLOYEE SYSTEM — A Realistic Example                       ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

// From Lesson 2, let's say we have a base class:

abstract class BaseEmployee {
    protected String name;
    protected double salary;

    public abstract double calculatePay();
    public abstract void clockIn();    // Record attendance
    public abstract void clockOut();   // Record departure
}

class FullTimeEmployee extends BaseEmployee {
    @Override
    public double calculatePay() { return salary; }  // Monthly salary ✅

    @Override
    public void clockIn() { System.out.println(name + " clocked in at 9 AM"); }

    @Override
    public void clockOut() { System.out.println(name + " clocked out at 6 PM"); }
}

// ❌ BAD: Contractor doesn't have "clock in/out" — they bill by deliverables
class ContractorBAD extends BaseEmployee {
    @Override
    public double calculatePay() { return salary; }

    @Override
    public void clockIn() {
        // Contractor doesn't clock in... what do we do?
        throw new UnsupportedOperationException("Contractors don't clock in!");
        // ⚠️ LSP VIOLATION: Any code calling baseEmployee.clockIn() will CRASH
        // if it receives a Contractor. The parent promised clockIn() works.
    }

    @Override
    public void clockOut() {
        throw new UnsupportedOperationException("Contractors don't clock out!");
    }
}

// THE RED FLAG: If your subclass throws UnsupportedOperationException
// for a method it inherits, that's almost ALWAYS an LSP violation.
// The subclass is saying "I can't do what my parent promised I could do."


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  PART 2: INTERFACE SEGREGATION PRINCIPLE (ISP)                          ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// RULE: "No class should be forced to implement methods it doesn't use."
//
// ISP is often the SOLUTION to LSP violations.
// If a subclass can't fulfill all of the parent's methods,
// the parent interface is TOO BIG. Split it.
// ============================================================================


// ── ❌ BAD: One fat interface that forces unnecessary implementations ────────

interface WorkerBAD {
    void work();
    void clockIn();
    void clockOut();
    void submitTimesheet();
    void attendMeeting();
    double calculatePay();
    void applyForLeave();
    void submitExpenseReport();
}

// A full-time employee can implement ALL of these. Fine.
// But what about:
//   - A Contractor?     → No clockIn, no leave, no timesheet
//   - An Intern?        → No expense reports, limited leave
//   - A Freelancer?     → No meetings, no clockIn, no leave
//
// They're all FORCED to implement methods that don't apply to them.
// Result: methods that throw exceptions or return dummy values. = LSP violation.


// ── ✅ GOOD: Split into small, focused interfaces ───────────────────────────

// Each interface represents ONE capability. Classes implement ONLY what applies.

/**
 * Core capability: every worker type can do work and get paid.
 */
interface Workable {
    void work();
    double calculatePay();
}

/**
 * Capability: tracking attendance via clock in/out.
 * Only for on-site or time-tracked workers.
 */
interface Attendable {
    void clockIn();
    void clockOut();
    void submitTimesheet();
}

/**
 * Capability: eligible for company leave policy.
 */
interface LeaveEligible {
    void applyForLeave();
    int getRemainingLeaves();
}

/**
 * Capability: can submit expense reports for reimbursement.
 */
interface ExpenseReportable {
    void submitExpenseReport();
}

/**
 * Capability: participates in company meetings.
 */
interface MeetingParticipant {
    void attendMeeting();
    void scheduleMeeting(String title);
}


// ── Now each worker type picks ONLY what it needs ───────────────────────────

/**
 * Full-time employee: has ALL capabilities.
 * Implements every interface — and can genuinely fulfill each one. ✅
 */
class FullTimeWorker implements Workable, Attendable, LeaveEligible, 
                                 ExpenseReportable, MeetingParticipant {
    private String name;
    private double salary;
    private int leaves = 24;

    public FullTimeWorker(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    @Override public void work() { System.out.println(name + " working on sprint tasks"); }
    @Override public double calculatePay() { return salary; }
    @Override public void clockIn() { System.out.println(name + " clocked in"); }
    @Override public void clockOut() { System.out.println(name + " clocked out"); }
    @Override public void submitTimesheet() { System.out.println(name + " timesheet submitted"); }
    @Override public void applyForLeave() { leaves--; System.out.println(name + " applied for leave"); }
    @Override public int getRemainingLeaves() { return leaves; }
    @Override public void submitExpenseReport() { System.out.println(name + " expense submitted"); }
    @Override public void attendMeeting() { System.out.println(name + " attending standup"); }
    @Override public void scheduleMeeting(String t) { System.out.println(name + " scheduled: " + t); }
}


/**
 * Contractor: works and gets paid, but no attendance tracking,
 * no leave, no expense reports.
 * Only implements what genuinely applies. No dummy methods. No exceptions. ✅
 */
class Contractor implements Workable {
    private String name;
    private double hourlyRate;
    private int hoursWorked;

    public Contractor(String name, double hourlyRate) {
        this.name = name;
        this.hourlyRate = hourlyRate;
    }

    public void logHours(int hours) { this.hoursWorked += hours; }

    @Override public void work() { System.out.println(name + " delivering milestone"); }
    @Override public double calculatePay() { return hourlyRate * hoursWorked; }
    
    // NO clockIn(). NO applyForLeave(). NO submitExpenseReport().
    // Because this class was never forced to implement them. 
    // No UnsupportedOperationException. No lies. Clean contract. ✅
}


/**
 * Intern: works, tracks attendance, but no expense reports, limited meetings.
 */
class Intern implements Workable, Attendable {
    private String name;
    private double stipend;

    public Intern(String name, double stipend) {
        this.name = name;
        this.stipend = stipend;
    }

    @Override public void work() { System.out.println(name + " working on training tasks"); }
    @Override public double calculatePay() { return stipend; }
    @Override public void clockIn() { System.out.println(name + " clocked in"); }
    @Override public void clockOut() { System.out.println(name + " clocked out"); }
    @Override public void submitTimesheet() { System.out.println(name + " timesheet submitted"); }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🔗 HOW ISP FIXES LSP                                                  ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// BEFORE (ISP violation → causes LSP violation):
//   Contractor implements WorkerBAD → forced to implement clockIn()
//   → throws UnsupportedOperationException → BREAKS any code calling clockIn()
//
// AFTER (ISP applied → LSP is automatically satisfied):
//   Contractor implements ONLY Workable → never promises clockIn()
//   → code that needs attendance uses Attendable type, Contractor never appears
//   → no broken promises, no crashes, no surprises
//
// LESSON: ISP prevents LSP violations by ensuring classes never
// promise more than they can deliver.


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🚀 MAIN — Putting it all together with type-safe usage                 ║
// ╚═══════════════════════════════════════════════════════════════════════════╝

public class Lesson3Main {
    public static void main(String[] args) {

        FullTimeWorker yash = new FullTimeWorker("Yash", 100000);
        Contractor alex = new Contractor("Alex", 5000);
        alex.logHours(40);
        Intern riya = new Intern("Riya", 20000);

        // --- Process payroll for ALL workers ---
        // We use the Workable interface — works for everyone ✅
        System.out.println("=== PAYROLL ===");
        List<Workable> allWorkers = List.of(yash, alex, riya);
        for (Workable w : allWorkers) {
            System.out.println("Pay: ₹" + w.calculatePay());
        }

        // --- Process attendance for ONLY attendable workers ---
        // Contractor is NOT in this list — and that's correct ✅
        System.out.println("\n=== ATTENDANCE ===");
        List<Attendable> attendableWorkers = List.of(yash, riya);
        for (Attendable a : attendableWorkers) {
            a.clockIn();
        }
        // alex.clockIn() → COMPILE ERROR, not runtime crash.
        // The type system PREVENTS the mistake. That's the power of ISP.

        // --- Leave management for ONLY eligible workers ---
        System.out.println("\n=== LEAVE ===");
        List<LeaveEligible> leaveEligible = List.of(yash);
        for (LeaveEligible le : leaveEligible) {
            le.applyForLeave();
            System.out.println("Remaining: " + le.getRemainingLeaves());
        }
        // riya (Intern) and alex (Contractor) can't apply for leave.
        // Not because of an exception — because the TYPE doesn't allow it.
    }
}


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  📁 PROJECT STRUCTURE (updated from Lesson 2)                           ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// src/
// ├── model/
// │   └── Employee.java
// │
// ├── capability/                         ← NEW: Segregated interfaces
// │   ├── Workable.java
// │   ├── Attendable.java
// │   ├── LeaveEligible.java
// │   ├── ExpenseReportable.java
// │   └── MeetingParticipant.java
// │
// ├── worker/                             ← NEW: Worker implementations
// │   ├── FullTimeWorker.java            (implements all 5)
// │   ├── Contractor.java                (implements Workable only)
// │   └── Intern.java                    (implements Workable + Attendable)
// │
// ├── strategy/                           (from Lesson 2)
// ├── factory/                            (from Lesson 2)
// ├── repository/                         (from Lesson 1)
// ├── service/                            (from Lesson 1)
// ├── notification/                       (from Lesson 1)
// └── report/                             (from Lesson 1)


// ╔═══════════════════════════════════════════════════════════════════════════╗
// ║  🧠 INTERVIEW CHEAT SHEET: LSP + ISP                                   ║
// ╚═══════════════════════════════════════════════════════════════════════════╝
//
// ┌─────────────────────────────────────────────────────────────────────┐
// │  LSP RED FLAGS (things that signal a violation):                    │
// │                                                                     │
// │  🚩 Subclass throws UnsupportedOperationException                  │
// │  🚩 Subclass overrides a method to do nothing (empty body)         │
// │  🚩 Subclass overrides a method with surprising side effects       │
// │  🚩 Code uses instanceof checks to handle subclasses differently   │
// │  🚩 "This works for A but crashes for B" (both extend same parent)│
// └─────────────────────────────────────────────────────────────────────┘
//
// ┌─────────────────────────────────────────────────────────────────────┐
// │  ISP RED FLAGS (things that signal a violation):                    │
// │                                                                     │
// │  🚩 Interface has 7+ methods (probably doing too much)             │
// │  🚩 Some implementors leave methods empty or throw exceptions      │
// │  🚩 You see "// not applicable" comments in implementations        │
// │  🚩 Adding a method to interface forces changes in unrelated classes│
// │  🚩 Interface name contains "And" (e.g., WorkerAndPayable)        │
// └─────────────────────────────────────────────────────────────────────┘
//
// ┌─────────────────────────────────────────────────────────────────────┐
// │  THE FIX PATTERN:                                                   │
// │                                                                     │
// │  1. Identify which methods some implementors CAN'T fulfill         │
// │  2. Group methods by CAPABILITY (who genuinely needs them)         │
// │  3. Split into small interfaces (1 interface = 1 capability)       │
// │  4. Each class implements ONLY the interfaces it can truly fulfill │
// │  5. Use the SPECIFIC interface type in method signatures           │
// │     → processAttendance(Attendable worker)  NOT  process(Worker)   │
// └─────────────────────────────────────────────────────────────────────┘
//
// REAL-WORLD ISP EXAMPLES:
//   • java.util.Collection is well-segregated: Iterable → Collection → List
//   • JDBC: Connection, Statement, ResultSet (separate interfaces)
//   • Spring: ApplicationContext extends multiple small interfaces
//
// INTERVIEW QUESTION: "Design a Vehicle hierarchy for Car, Boat, Airplane"
//   BAD:  interface Vehicle { drive(); fly(); sail(); }
//         → Car.fly() throws exception 💥
//   GOOD: interface Drivable { drive(); }
//         interface Flyable  { fly(); }
//         interface Sailable { sail(); }
//         → Car implements Drivable
//         → Airplane implements Drivable, Flyable
//         → Amphibious implements Drivable, Sailable
