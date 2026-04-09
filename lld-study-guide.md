# Comprehensive Study Guide: Low-Level Design (LLD)

---

## 1. What is Low-Level Design?

Low-Level Design (LLD) is the phase of software design where the **internal logic of individual components** is defined in detail. While High-Level Design (HLD) answers *"What are the building blocks of the system?"*, LLD answers *"How does each building block work internally?"*

LLD translates the architectural blueprint into a **class-level, method-level, and interaction-level** specification that a developer can directly implement in code. It is the last design step before writing production code.

**Core focus areas of LLD:**

- Class structures, their attributes, and methods
- Relationships between classes (inheritance, composition, aggregation, dependency)
- Interface contracts and abstract classes
- Method signatures, input/output, and internal algorithms
- Interaction flows between objects (who calls whom, in what order)
- Data structures chosen for specific operations
- Error handling and edge case strategies
- Concurrency and thread-safety considerations (where applicable)

---

## 2. LLD vs. HLD — A Clear Distinction

| Dimension | High-Level Design (HLD) | Low-Level Design (LLD) |
|---|---|---|
| **Scope** | Entire system / subsystems | Individual modules / classes |
| **Audience** | Architects, tech leads, stakeholders | Developers, code reviewers |
| **Artifacts** | System architecture diagrams, tech stack decisions, data flow diagrams | Class diagrams, sequence diagrams, state diagrams, pseudocode |
| **Abstraction** | What services/modules exist and how they communicate | How each module is internally structured and coded |
| **Example** | "The Order Service talks to the Payment Service via REST" | "The `OrderProcessor` class has a `processOrder(Order o)` method that calls `PaymentGateway.charge()` and handles `PaymentFailedException`" |
| **Design patterns** | Architectural patterns (microservices, event-driven, layered) | OOP design patterns (Strategy, Observer, Factory, etc.) |

**Analogy:** HLD is the floor plan of a building (rooms, hallways, exits). LLD is the electrical wiring diagram and plumbing layout inside each room.

---

## 3. Where LLD Fits in the Software Development Lifecycle

```
Requirements → HLD (Architecture) → LLD (Detailed Design) → Implementation → Testing → Deployment
                    ▲                       ▲
                    │                       │
              Broad decisions        Granular decisions
              (services, DBs,       (classes, methods,
               protocols)            data structures)
```

LLD is the **bridge between architecture and code**. Without it, developers jump from abstract architecture directly to coding, which leads to inconsistent implementations, duplicated logic, and designs that violate SOLID principles. LLD forces you to think through the design *before* typing code.

---

## 4. Core Principles That Drive Good LLD

### 4.1 SOLID Principles

These are the backbone of any well-crafted LLD:

**S — Single Responsibility Principle (SRP)**
Every class should have exactly one reason to change. If your `UserService` is handling authentication, profile updates, *and* email notifications, it has too many responsibilities.

**O — Open/Closed Principle (OCP)**
Classes should be open for extension but closed for modification. Use interfaces and abstract classes so new behavior can be added without touching existing, tested code.

**L — Liskov Substitution Principle (LSP)**
A subclass must be usable wherever its parent class is expected, without breaking behavior. The classic violation: `Square extends Rectangle` where `setWidth()` unexpectedly also sets height.

**I — Interface Segregation Principle (ISP)**
Clients should not be forced to depend on interfaces they don't use. Prefer small, focused interfaces over large, monolithic ones.

**D — Dependency Inversion Principle (DIP)**
High-level modules should depend on abstractions, not on concrete implementations. This is the foundation of dependency injection.

### 4.2 Other Key Principles

- **DRY (Don't Repeat Yourself):** Eliminate duplicated logic by extracting it into shared methods or utility classes.
- **KISS (Keep It Simple, Stupid):** Prefer the simplest design that meets the requirements. Over-engineering is a real danger in LLD.
- **YAGNI (You Aren't Gonna Need It):** Don't add classes, methods, or abstractions for hypothetical future requirements.
- **Composition over Inheritance:** Favor "has-a" relationships over "is-a" when building flexible designs. Inheritance creates tight coupling; composition allows runtime flexibility.
- **Law of Demeter:** A method should only call methods on its own object, parameters passed to it, objects it creates, or its direct component objects — not on objects returned by other calls (avoid `a.getB().getC().doSomething()`).

---

## 5. Design Patterns — The Vocabulary of LLD

Design patterns are **reusable solutions to recurring design problems**. They are the most important topic in LLD, especially for interviews.

### 5.1 Creational Patterns (Object Creation)

| Pattern | Problem It Solves | Key Idea |
|---|---|---|
| **Singleton** | Ensure only one instance of a class exists | Private constructor + static `getInstance()` method |
| **Factory Method** | Decouple object creation from usage | A method returns an object whose concrete type is decided at runtime |
| **Abstract Factory** | Create families of related objects | A factory of factories — returns related products without specifying concrete classes |
| **Builder** | Construct complex objects step by step | Separate construction logic from representation; chained method calls |
| **Prototype** | Create objects by cloning an existing instance | Useful when object creation is expensive; implement `clone()` |

### 5.2 Structural Patterns (Object Composition)

| Pattern | Problem It Solves | Key Idea |
|---|---|---|
| **Adapter** | Make incompatible interfaces work together | A wrapper that translates one interface to another |
| **Decorator** | Add behavior to objects dynamically | Wrap an object with another object that adds functionality |
| **Facade** | Simplify a complex subsystem | A single, unified interface that hides internal complexity |
| **Proxy** | Control access to an object | A stand-in that intercepts calls (lazy loading, caching, access control) |
| **Composite** | Treat individual objects and groups uniformly | A tree structure where leaves and composites share the same interface |
| **Bridge** | Decouple abstraction from implementation | Two separate hierarchies that vary independently |
| **Flyweight** | Share objects to reduce memory usage | Cache and reuse immutable objects (e.g., character objects in a text editor) |

### 5.3 Behavioral Patterns (Object Interaction)

| Pattern | Problem It Solves | Key Idea |
|---|---|---|
| **Strategy** | Switch algorithms at runtime | Define a family of algorithms, encapsulate each one, make them interchangeable |
| **Observer** | Notify multiple objects when state changes | Publishers push updates to subscribed listeners |
| **Command** | Encapsulate a request as an object | Enables undo/redo, queuing, and logging of operations |
| **State** | Change behavior when internal state changes | Replace conditionals with polymorphic state objects |
| **Template Method** | Define the skeleton of an algorithm, let subclasses fill in steps | Abstract class with concrete and abstract methods |
| **Chain of Responsibility** | Pass a request along a chain of handlers | Each handler decides to process or pass the request forward |
| **Iterator** | Traverse a collection without exposing its internals | Standard `hasNext()` / `next()` interface |
| **Mediator** | Reduce coupling between many-to-many objects | A central object coordinates communication |

---

## 6. LLD Artifacts and Diagrams

### 6.1 Class Diagram

The most fundamental LLD artifact. It shows classes, their attributes, methods, and relationships.

```
┌──────────────────────┐
│      <<interface>>    │
│     PaymentGateway    │
├──────────────────────┤
│ + charge(amount): bool│
│ + refund(txnId): bool │
└──────────┬───────────┘
           │ implements
    ┌──────┴───────┐
    │              │
┌───▼────┐   ┌────▼─────┐
│ Stripe  │   │ PayPal   │
│ Gateway │   │ Gateway  │
├─────────┤   ├──────────┤
│-apiKey  │   │-clientId │
├─────────┤   ├──────────┤
│+charge()│   │+charge() │
│+refund()│   │+refund() │
└─────────┘   └──────────┘
```

**Relationships to show:**
- Association (→): "uses"
- Aggregation (◇→): "has" (weak ownership)
- Composition (◆→): "owns" (strong ownership, lifecycle dependency)
- Inheritance (△→): "is-a"
- Dependency (--→): "depends on" (temporary/parameter usage)

### 6.2 Sequence Diagram

Shows **time-ordered interactions** between objects for a specific use case.

```
User        OrderService     InventoryService    PaymentGateway     NotificationService
 │              │                   │                  │                    │
 │──placeOrder()─▶                  │                  │                    │
 │              │──checkStock()────▶│                  │                    │
 │              │◀─ stockAvailable──│                  │                    │
 │              │──charge()────────────────────────────▶│                    │
 │              │◀─ paymentSuccess──────────────────────│                    │
 │              │──sendConfirmation()──────────────────────────────────────▶│
 │◀─orderConfirmed──│                                                      │
```

### 6.3 State Diagram

Shows how an object transitions between states based on events.

```
                    ┌─────────┐
     ──create()───▶│ PENDING  │
                    └────┬────┘
                         │ pay()
                    ┌────▼────┐
                    │  PAID   │
                    └────┬────┘
                         │ ship()
                    ┌────▼────┐    cancel()    ┌───────────┐
                    │ SHIPPED ├───────────────▶│ CANCELLED │
                    └────┬────┘                └───────────┘
                         │ deliver()
                    ┌────▼─────┐
                    │DELIVERED │
                    └──────────┘
```

### 6.4 Other Useful Diagrams

- **Activity Diagram:** Flowchart of a process with decision points and parallel paths.
- **Component Diagram:** Shows internal structure of a single module and its dependencies.
- **ER Diagram (at detail level):** Table-level schema design with column types, indexes, and constraints.

---

## 7. A Worked Example — Parking Lot System (Interview Classic)

### Step 1: Identify Entities
Vehicle, ParkingSpot, ParkingLot, Ticket, ParkingFloor, EntryPanel, ExitPanel, Payment

### Step 2: Define Classes and Relationships

```
Vehicle (abstract)
├── Car
├── Truck
├── Motorcycle

ParkingSpot (abstract)
├── CompactSpot
├── LargeSpot
├── MotorcycleSpot
  - isAvailable: boolean
  - vehicle: Vehicle
  - assignVehicle(v): void
  - removeVehicle(): void

ParkingFloor
  - spots: List<ParkingSpot>
  - getAvailableSpot(type): ParkingSpot

ParkingLot (Singleton)
  - floors: List<ParkingFloor>
  - entryPanels: List<EntryPanel>
  - exitPanels: List<ExitPanel>
  - addFloor(), isFull(): bool

Ticket
  - entryTime: DateTime
  - spot: ParkingSpot
  - vehicle: Vehicle
  - paymentStatus: PaymentStatus

Payment (Strategy Pattern)
├── CashPayment
├── CardPayment
├── UPIPayment
  - pay(amount): boolean
```

### Step 3: Identify Patterns Used
- **Singleton** for `ParkingLot` (one instance for the entire system)
- **Strategy** for `Payment` (interchangeable payment methods)
- **Factory** for creating the right `ParkingSpot` based on vehicle type
- **Observer** for notifying display boards when spot availability changes

---

## 8. Common LLD Interview Problems

These are the most frequently asked problems. For each, practice identifying classes, relationships, patterns, and drawing a class diagram:

**Tier 1 — Must Prepare:**
1. Parking Lot System
2. Elevator System
3. Library Management System
4. Tic-Tac-Toe / Chess
5. BookMyShow (Movie Ticket Booking)
6. ATM Machine
7. Vending Machine

**Tier 2 — Frequently Asked:**
8. Snake and Ladder Game
9. Hotel Management System
10. Online Shopping (Amazon-like cart + order flow)
11. URL Shortener (internal class design, not system design)
12. File System (Composite pattern)
13. Splitwise (expense sharing)
14. Notification Service

**Tier 3 — Advanced:**
15. Ride-Sharing System (Uber/Ola)
16. Cricket Scoreboard
17. Logging Framework
18. Cache System (LRU Cache)
19. Rate Limiter
20. Pub-Sub Messaging System

---

## 9. Best Practices for Effective LLD

1. **Start from requirements, not from code.** List functional requirements, then identify nouns (entities/classes) and verbs (methods/actions) from them.

2. **Apply SOLID from the start.** It's far cheaper to design with SOLID than to refactor toward it later.

3. **Use interfaces to define contracts.** This makes your design testable and extensible. Code to interfaces, not implementations.

4. **Pick design patterns deliberately, not decoratively.** A pattern should solve a specific problem in your design. Don't use Observer just because you know it — use it because you genuinely have a publisher-subscriber relationship.

5. **Keep classes small and focused.** If you struggle to name a class or its name contains "And" or "Manager" with too many responsibilities, it probably violates SRP.

6. **Think about extensibility at boundary points.** Where are requirements most likely to change? Those are the places to introduce abstractions.

7. **Handle edge cases in your design.** Concurrency (two users booking the last spot), error states (payment failure mid-transaction), and boundary conditions should be explicit in your design.

8. **Document trade-offs.** If you chose composition over inheritance, or a HashMap over a TreeMap, note *why*. This is especially valued in interviews.

---

## 10. Common Challenges in LLD

- **Over-engineering:** Adding layers of abstraction and patterns for simple problems. A CRUD module doesn't need the Abstract Factory pattern.
- **Analysis paralysis:** Spending too long deciding the "perfect" class hierarchy. Start with a working design and iterate.
- **Leaky abstractions:** Exposing internal implementation details through public interfaces. If your `ParkingFloor` returns a raw `HashMap<Integer, ParkingSpot>`, consumers are now coupled to HashMap.
- **Ignoring concurrency:** Many LLD problems (parking lot, ticket booking, cache) have concurrent access scenarios. If you don't address thread safety (locks, synchronized blocks, ConcurrentHashMap), your design is incomplete.
- **Tight coupling:** Classes that know too much about each other become impossible to change independently. The fix is almost always: add an interface between them.
- **Inconsistent granularity:** Some parts of the design are at method-level detail while others are still hand-wavy. Maintain consistent depth.

---

## 11. Interview Tips for LLD Rounds

1. **Clarify requirements first (2-3 minutes).** Ask about scope, scale, and what features are in/out. This shows maturity.

2. **Identify entities and relationships (5 minutes).** List the core classes, their key attributes, and how they relate.

3. **Apply design patterns with justification.** Don't just say "I'll use Strategy." Say *"Payment can happen via card, cash, or UPI — these are interchangeable algorithms, so Strategy pattern fits here."*

4. **Draw a class diagram.** Even a rough one on a whiteboard demonstrates structured thinking.

5. **Walk through a sequence diagram for the main use case.** Show how objects collaborate to fulfill the primary scenario.

6. **Discuss extensibility.** Proactively say: *"If we need to add a new vehicle type tomorrow, we just extend `Vehicle` and add a new `ParkingSpot` subclass — no existing code changes."*

7. **Mention trade-offs.** Interviewers love hearing: *"I chose X over Y because of Z, but the downside is..."*

8. **Write key method signatures.** You don't need full code, but showing `public Ticket issueTicket(Vehicle v, ParkingFloor floor)` with clear input/output signals implementation readiness.

---

## 12. Recommended Study Path

**Week 1-2:** Master SOLID principles with code examples. Practice identifying violations in existing code.

**Week 3-4:** Learn all 23 GoF design patterns. Focus on the 10-12 most common ones listed above. Implement each in your preferred language.

**Week 5-6:** Solve 4-5 classic LLD problems end-to-end (class diagram → sequence diagram → code skeleton).

**Week 7-8:** Practice under timed conditions (45 minutes per problem). Get peer feedback or review solutions online.

**Recommended Resources:**
- *Head First Design Patterns* by Freeman & Robson (beginner-friendly)
- *Design Patterns: Elements of Reusable OO Software* by Gang of Four (reference)
- *Clean Code* and *Clean Architecture* by Robert C. Martin
- GitHub repositories with LLD solutions (search "awesome-low-level-design")
- Practice on platforms like interviewready.io or educative.io LLD courses

---

*This guide covers the essential theory, patterns, and practice strategy needed to understand and apply Low-Level Design effectively — both in production software and in technical interviews.*
