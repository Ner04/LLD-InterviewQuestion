import java.time.LocalDateTime;
import java.util.*;

enum VehicleType{
    CAR, MOTORCYCLE,TRUCK
}
enum SpotType{
MOTORCYCLE,COMPACT,LARGE
}

class Vehicle{
    private final String vehicleNumber;
    private final VehicleType vehicleType;

    public Vehicle(String vehicleNumber, VehicleType vehicleType) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj) return true;
        if (!(obj instanceof Vehicle))  return  false;

        Vehicle other = (Vehicle) obj;
        return vehicleNumber.equals(other.vehicleNumber);
    }

    @Override
    public int hashCode() {
        return vehicleNumber.hashCode();
    }
}

class ParkingSpot{
    private final String spotId;
    private final SpotType spotType;

    private  Vehicle parkedVehicle;
    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
    }

    public String getSpotId() {
        return spotId;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public synchronized boolean isAvailable (){
        return parkedVehicle == null;
    }

    public synchronized  boolean canFit(Vehicle vehicle){
        if (vehicle == null || parkedVehicle !=null){
            return false;
        }
        switch (vehicle.getVehicleType()) {
            case MOTORCYCLE:
                return spotType == SpotType.MOTORCYCLE;
            case CAR:
                return spotType == SpotType.COMPACT;
            case TRUCK:
                return spotType == SpotType.LARGE;
            default:
                return false;
        }
    }
    public  synchronized  boolean parkVehicle (Vehicle vehicle){
        if(!canFit(vehicle)){
            return  false;
        }
        parkedVehicle = vehicle;
        return true;
    }
    public synchronized boolean removeVehicle(String vehicleNumber){
        if (parkedVehicle == null || !parkedVehicle.getVehicleNumber().equals(vehicleNumber) ){
            return false;
        }
        parkedVehicle = null;
        return true;
    }
}

class ParkingLevel{

    private  final String levelId;
    private final List<ParkingSpot> spots;

    public ParkingLevel(String levelId, List<ParkingSpot> spots) {
        this.levelId = levelId;
        this.spots = spots;
    }

    public String getLevelId() {
        return levelId;
    }

    public List<ParkingSpot> getSpots() {
        return Collections.unmodifiableList(spots);
    }

    public int getAvailableSpotCount(){
        int count =0;

        for(ParkingSpot spot :spots){
            if(spot.isAvailable()){
                count++;
            }
        }
        return count;
    }
}

class ParkingTicket{
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot parkingSpot;
    private final LocalDateTime entryTime;

    public ParkingTicket(Vehicle vehicle, ParkingSpot parkingSpot, LocalDateTime entryTime) {
        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.parkingSpot = parkingSpot;
        this.entryTime = entryTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public String getTicketId() {
        return ticketId;
    }
}

//DesignPattern : Strategy
//first avaialble spot assigned , nearest spot and best fit
interface ParkingSpotAllocationStrategy{
    ParkingSpot findSpot(Vehicle vehicle , List<ParkingLevel>levels);
}

class FirstAvailableSpotFacility implements ParkingSpotAllocationStrategy{

    @Override
    public ParkingSpot findSpot(Vehicle vehicle, List<ParkingLevel> levels) {
        for (ParkingLevel level : levels){
            for(ParkingSpot spot: level.getSpots()){
                if(spot.canFit(vehicle)){
                    return spot;
                }
            }
        }
        return  null;
    }
}

class ParkingLotService {

    private final List<ParkingLevel> levels;
    private final ParkingSpotAllocationStrategy strategy;

    private final Map<String, ParkingTicket> activeTickets =
            new HashMap<>();

    private final Map<String, ParkingTicket> vehicleTickets =
            new HashMap<>();

    public ParkingLotService(
            List<ParkingLevel> levels,
            ParkingSpotAllocationStrategy strategy) {

        this.levels = new ArrayList<>(levels);
        this.strategy = strategy;
    }

    // Vehicle enters the parking lot.
    public synchronized ParkingTicket parkVehicle(Vehicle vehicle) {

        if (vehicle == null) {
            return null;
        }

        // Prevent the same vehicle from being parked twice.
        if (vehicleTickets.containsKey(vehicle.getVehicleNumber())) {
            return null;
        }

        ParkingSpot spot = strategy.findSpot(vehicle, levels);

        if (spot == null) {
            return null;
        }

        // Claim the spot before issuing a ticket.
        if (!spot.parkVehicle(vehicle)) {
            return null;
        }

        LocalDateTime entryTime = LocalDateTime.now();
        ParkingTicket ticket = new ParkingTicket(vehicle, spot,entryTime);

        activeTickets.put(ticket.getTicketId(), ticket);
        vehicleTickets.put(vehicle.getVehicleNumber(), ticket);

        return ticket;
    }

    // Vehicle exits using its ticket.
    public synchronized boolean exitVehicle(String ticketId) {

        ParkingTicket ticket = activeTickets.get(ticketId);

        if (ticket == null) {
            return false;
        }

        boolean released = ticket.getParkingSpot()
                .removeVehicle(
                        ticket.getVehicle().getVehicleNumber()
                );

        if (!released) {
            return false;
        }

        activeTickets.remove(ticketId);
        vehicleTickets.remove(
                ticket.getVehicle().getVehicleNumber()
        );

        return true;
    }

    // Total currently available spots across all levels.
    public synchronized int getAvailableSpotCount() {

        int count = 0;

        for (ParkingLevel level : levels) {
            count += level.getAvailableSpotCount();
        }

        return count;
    }

    // Return a snapshot of currently available spots.
    public synchronized List<ParkingSpot> getAvailableSpots() {

        List<ParkingSpot> available = new ArrayList<>();

        for (ParkingLevel level : levels) {
            for (ParkingSpot spot : level.getSpots()) {
                if (spot.isAvailable()) {
                    available.add(spot);
                }
            }
        }

        return Collections.unmodifiableList(available);
    }
}



public class ParkingLotSystem {

    public static void main(String[] args){ // 1. Create parking spots.
        ParkingSpot s1 = new ParkingSpot("M1", SpotType.MOTORCYCLE);
        ParkingSpot s2 = new ParkingSpot("C1", SpotType.COMPACT);
        ParkingSpot s3 = new ParkingSpot("L1", SpotType.LARGE);

        // 2. Create parking levels.
        ParkingLevel level1 = new ParkingLevel(
                "L1", Arrays.asList(s1, s2)
        );

        ParkingLevel level2 = new ParkingLevel(
                "L2", Arrays.asList(s3)
        );

        List<ParkingLevel> levels = Arrays.asList(level1, level2);

        // 3. Configure the allocation strategy.
        ParkingSpotAllocationStrategy strategy =
                new FirstAvailableSpotFacility();

        // 4. Create the parking lot service.
        ParkingLotService parkingLot =
                new ParkingLotService(levels, strategy);

        // TEST 1: Park a car.
        Vehicle car = new Vehicle("KA01AB1234", VehicleType.CAR);

        ParkingTicket ticket = parkingLot.parkVehicle(car);

        System.out.println("Car parked: " + (ticket != null));
        System.out.println("Assigned spot: "
                + (ticket != null
                ? ticket.getParkingSpot().getSpotId()
                : "None"));

        // TEST 2: Prevent the same vehicle from parking twice.
        ParkingTicket duplicate = parkingLot.parkVehicle(car);

        System.out.println("Duplicate rejected: " + (duplicate == null));

        // TEST 3: Park a motorcycle.
        Vehicle bike = new Vehicle("KA02CD5678", VehicleType.MOTORCYCLE);

        ParkingTicket bikeTicket = parkingLot.parkVehicle(bike);

        System.out.println("Motorcycle parked: " + (bikeTicket != null));

        // TEST 4: Check availability.
        System.out.println("Available spots: "
                + parkingLot.getAvailableSpotCount());

        // TEST 5: Exit the car.
        boolean exited = parkingLot.exitVehicle(ticket.getTicketId());

        System.out.println("Car exited: " + exited);

        // TEST 6: Reject reuse of an exited ticket.
        boolean secondExit = parkingLot.exitVehicle(ticket.getTicketId());

        System.out.println("Second exit rejected: " + !secondExit);

        // TEST 7: Check availability after exit.
        System.out.println("Available spots after exit: "
                + parkingLot.getAvailableSpotCount());

        // TEST 8: Reject an unsupported fit.
        ParkingSpot motorcycleSpot =
                new ParkingSpot("M2", SpotType.MOTORCYCLE);

        System.out.println("Truck fits motorcycle spot: "
                + motorcycleSpot.canFit(
                new Vehicle("KA03EF9999", VehicleType.TRUCK)
        ));
    }}


