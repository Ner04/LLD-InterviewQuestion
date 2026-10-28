# LLD — Interview Questions

A collection of **Low-Level Design (LLD) / machine-coding** problems implemented in Java.
Each problem is a self-contained file with its own `main()` demo that exercises the core
flows and edge cases.

## Tech stack

- **Language:** Java 21
- **Build:** Maven (`pom.xml`, `maven.compiler.release = 21`)
- **No external dependencies** — pure JDK (collections, concurrency utilities, `java.time`).

## Project layout

```
src/main/java/
├── ParkingLotSystem.java       # Parking Lot
├── Splitwise.java              # Expense sharing (Splitwise)
├── TaskSchedulingSystem.java   # Priority task scheduler
├── FoodDeliverySystem.java     # Food delivery (Zomato-style)
├── Main.java                   # Elevator — basic nearest-car controller
├── ElevatorSystem.java         # Elevator — cost-based dispatch controller
├── vendingMachineSystem.java   # Vending Machine
├── EventSchedularSystem.java   # Calendar / event scheduler with priority conflict resolution
└── ElevatorDispatchSystem.java # Scratch/WIP (fully commented out — see note below)
```

> **Heads-up — class-name collision.** `Main.java` and `ElevatorSystem.java` both declare
> top-level types with the same names (`Elevator`, `ElevatorController`, `ElevatorSystem`,
> `Direction`, `ElevatorStatus`). Java does **not** allow two files in the same package to
> define the same top-level class, so **they cannot be compiled together** in one `mvn compile`.
> Keep only one of the two on the classpath at a time (or move one into its own package)
> before compiling. See the run notes below.

## How to run

Each file has a `public` class with a `main()` method. Because the two elevator files clash
(see above), compile them separately rather than running `mvn compile` over the whole tree.

```bash
# Compile and run a single design in isolation (recommended)
javac -d /tmp/out src/main/java/ParkingLotSystem.java      && java -cp /tmp/out ParkingLotSystem
javac -d /tmp/out src/main/java/Splitwise.java             && java -cp /tmp/out Splitwise
javac -d /tmp/out src/main/java/TaskSchedulingSystem.java  && java -cp /tmp/out TaskSchedulingSystem
javac -d /tmp/out src/main/java/FoodDeliverySystem.java    && java -cp /tmp/out FoodDeliverySystem
javac -d /tmp/out src/main/java/vendingMachineSystem.java  && java -cp /tmp/out vendingMachineSystem
javac -d /tmp/out src/main/java/EventSchedularSystem.java  && java -cp /tmp/out EventSchedularSystem

# Elevators — compile only ONE of these at a time
javac -d /tmp/out src/main/java/Main.java                  && java -cp /tmp/out Main            # basic
javac -d /tmp/out src/main/java/ElevatorSystem.java        && java -cp /tmp/out ElevatorSystem  # cost-based
```

---

## Designs

### 1. Parking Lot — `ParkingLotSystem.java`

**Design pattern:** Strategy (for spot allocation).

- **Entities:** `Vehicle`, `ParkingSpot`, `ParkingLevel`, `ParkingTicket`, `ParkingLotService`.
- **Enums:** `VehicleType` (CAR, MOTORCYCLE, TRUCK), `SpotType` (MOTORCYCLE, COMPACT, LARGE).
- **Spot fitment:** `ParkingSpot.canFit()` maps each vehicle type to its allowed spot type.
- **Strategy:** `ParkingSpotAllocationStrategy` interface with `FirstAvailableSpotFacility`
  implementation — easy to swap for nearest-spot or best-fit later.
- **Ticketing:** `ParkingTicket` is issued on entry with a UUID and entry time; tracked in
  `activeTickets` (by ticket id) and `vehicleTickets` (by vehicle number).
- **Correctness:** rejects `null` vehicles, prevents parking the same vehicle twice, and
  rejects reuse of an already-exited ticket.
- **Thread safety:** spot mutation (`parkVehicle` / `removeVehicle` / `isAvailable`) is
  `synchronized`, and the service methods are `synchronized`.
- **`Vehicle`** overrides `equals()` / `hashCode()` on the vehicle number.

### 2. Splitwise — `Splitwise.java`

**Design pattern:** Strategy (for splitting logic).

- **Entities:** `User`, `Group`, `Expense`, `Split`, `Balance`, `SplitwiseService`.
- **Enums:** `SplitType` (EQUAL, EXACT, PERCENTAGE).
- **Money:** all amounts stored as `long` **paise** to avoid floating-point rounding errors.
- **Strategy:** `SplitStrategy` interface; `EqualSplitStrategy` divides the total evenly and
  distributes the remainder paise-by-paise so the splits always sum back to the total.
- **Balances:** nested map `debtorId -> creditorId -> amount` for quick lookups of who owes
  whom.
- **Settlement:** `Balance.reduceAmount()` validates the settlement amount (positive and not
  more than outstanding).
- **Correctness:** rejects invalid amounts and duplicate users; `Group` uses a `Set` and
  exposes members as an unmodifiable view.
- **Thread safety:** `SplitwiseService.addUser()` is `synchronized`.

### 3. Task Scheduler — `TaskSchedulingSystem.java`

**Design pattern:** Producer–Consumer (with a priority queue and a worker thread pool).

- **Entities:** `ScheduledTask`, `TaskScheduler`, `TaskDispatcher`, plus a `Task` functional
  interface.
- **Enums:** `Priority` (HIGH, MEDIUM, LOW), `TaskType` (ONE_TIME, RECURRING),
  `TaskStatus` (SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED).
- **Ordering:** `ScheduledTask implements Comparable` — ordered by execution time, then by
  priority on ties; held in a `PriorityQueue`.
- **Dispatcher loop:** waits when the queue is empty, peeks the earliest task, waits until it
  is due, then polls and hands it to an `ExecutorService` so the dispatcher stays free.
- **Recurring tasks:** re-scheduled for `now + interval` after each run.
- **Fault isolation:** a task throwing an exception is caught and marked `FAILED` — one
  failure never stops the others.
- **Cancellation:** `cancelTask()` removes a task from the queue and marks it `CANCELLED`;
  tasks cancelled between dispatch and run are skipped.
- **Thread safety:** queue access is guarded by `synchronized (queue)` with
  `wait()` / `notifyAll()`; `executionTime` and `status` are `volatile`.
- **Shutdown:** graceful `stop()` wakes and interrupts the dispatcher, then drains the pool.

### 4. Food Delivery — `FoodDeliverySystem.java`

**Design pattern:** State transitions on `Order` (state-machine style).

- **Entities:** `Customer`, `Address`, `Location`, `Restaurant`, `Menu`, `MenuItem`,
  `Cart`, `CartItem`, `Order`, `DeliveryPartner`.
- **Enums:** `OrderStatus` (PLACED → ACCEPTED → PREPARING → READY_FOR_PICKUP →
  OUT_FOR_DELIVERY → DELIVERED, plus REJECTED / CANCELLED), `PaymentStatus`,
  `DeliveryStatus`, `PartnerStatus` (AVAILABLE, RESERVED, OFFLINE).
- **Cart rule:** a cart may only hold items from a single restaurant; merges quantities on
  repeat adds and rejects unavailable items.
- **Order lifecycle:** `Order.updateStatus()` is `synchronized` and blocks updates to a
  completed/cancelled/rejected order.
- **Driver assignment:** `assignDeliveryPartner()` only succeeds when the order is
  `READY_FOR_PICKUP` and no partner is assigned yet — prevents double assignment.
- **Reject-and-skip:** a `DeliveryPartner` keeps a `rejectedOrderIds` set so a rejected order
  is never re-offered to the same driver.
- **Live tracking:** partner `location` is `volatile` and updatable.
- **Validation:** `Location` validates latitude/longitude ranges; `MenuItem` rejects negative
  prices.
- **Thread safety:** `ConcurrentHashMap` for menus/addresses and `synchronized` methods on
  `Cart`, `Order`, and `DeliveryPartner`.

### 5. Elevator (basic) — `Main.java`

**Design pattern:** Strategy-style controller (nearest-car selection).

- **Entities:** `Request`, `Elevator`, `ElevatorController`, `ElevatorSystem`.
- **Enums:** `ElevatorStatus` (IDLE, MOVING, STOPPED), `Direction` (UP, DOWN, IDLE).
- **Request:** derives its direction from source vs. destination floor and rejects
  same-floor requests.
- **Movement:** `Elevator.move()` steps one floor at a time — first toward the pickup
  (source) floor, then toward the destination — using a `headingToDestination` flag and a
  FIFO request queue.
- **Scheduling:** `ElevatorController.selectElevator()` picks the nearest idle elevator to
  the request's source floor.
- **Simulation:** `ElevatorSystem.step()` advances every elevator by one tick;
  `displayStatus()` prints each elevator's floor, direction, and status.

### 6. Elevator (cost-based dispatch) — `ElevatorSystem.java`

**Design pattern:** Strategy-style controller with a **dispatch-cost** scoring function.

A second, more realistic take on the elevator problem. Instead of just picking the nearest
idle car, the controller scores every eligible car and assigns the cheapest.

- **Entities:** `Elevator`, `ElevatorController`, `ElevatorSystem` (demo).
- **Enums:** `Direction` (UP, DOWN, IDLE), `ElevatorStatus` (IDLE, MOVING).
- **Per-car floor factor `F`:** each elevator has its own configurable `floorFactor`.
- **Admission rules (`canAccept`)** — a car accepts a hall call only if **any** holds:
  - the car is idle, **or**
  - the call floor is at least `F` floors away from the current floor, **or**
  - the call floor lies strictly beyond the car's next stop in its current direction.
  A moving car rejects calls whose direction doesn't match its own travel direction.
- **Stops as a `TreeSet<Integer>`:** ordered stop set enables efficient
  `higher` / `lower` / `ceiling` / `floor` lookups for "next stop" logic.
- **Dispatch cost (`calculateCost`):** `1` per floor to reach the call, plus `10` for each
  existing stop sitting between the car and the call floor; `ElevatorController` picks the
  lowest-cost eligible car (lowest-cost-first scan).
- **Movement:** `moveOnefloor()` advances one floor toward the nearest stop in the current
  direction, reverses when the direction is exhausted, and drops to `IDLE` when no stops
  remain.
- **Two request paths:** `requestFloor()` (in-car destination button) and
  `addHallCall()` (external up/down call, gated by `canAccept`).
- **Thread safety:** all car state mutations are `synchronized`.

### 7. Vending Machine — `vendingMachineSystem.java`

A minimal vending-machine model focused on product catalog and inventory.

- **Entities:** `Product`, `Inventory`, `vendingMachine`.
- **Inventory:** `Map<Product, Integer>` tracking stock; `addProduct()` accumulates quantity,
  `isAvailable()` checks stock, and `reduceQuantity()` decrements on a successful dispense.
- **Selection:** `vendingMachine.selectProduct()` rejects out-of-stock items and otherwise
  confirms the selection.
- **Scope:** intentionally small — a natural starting point to extend with coin/payment
  handling, change calculation, and a proper state machine (IDLE → HAS_MONEY → DISPENSING).

### 8. Event Scheduler — `EventSchedularSystem.java`

**Design pattern:** role-priority conflict resolution over a shared calendar.

- **Entities:** `eventUser`, `Event`, `CalendarService`.
- **Enums:** `Role` (DEVELOPER < MANAGER < DIRECTOR < VP), each carrying a numeric priority.
- **Registration & session:** `register()` creates users; `login()` sets the current user and
  guards scheduling/listing operations.
- **Scheduling:** `scheduleEvent()` validates the logged-in user, non-null start/end times,
  and that every participant is a known user.
- **Overlap detection:** `Event.overLaps()` uses the standard half-open interval test
  (`startA < endB && startB < endA`).
- **Conflict resolution:** `chooseWinner()` prefers an event owned by the current user,
  otherwise the event whose scheduler holds the higher `Role` priority.
- **Listing:** `listEvents()` shows only events the logged-in user schedules or participates in.

---

## Notes

- Each file is intentionally standalone so it can be read, run, and discussed in isolation
  during an interview.
- `Splitwise.java` focuses on the domain model and split strategy; the service layer
  (`SplitwiseService`) is the extension point for wiring up expense recording and balance
  updates.
- `ElevatorDispatchSystem.java` is **scratch/WIP** — its contents are entirely commented out
  (an earlier draft of the cost-based dispatcher that superseded it in `ElevatorSystem.java`).
  It is kept for reference only and compiles to an empty class.
