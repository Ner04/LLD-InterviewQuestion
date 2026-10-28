import java.util.*;

enum ElevatorStatus{
    IDLE, MOVING, STOPPED
}
enum  Direction{
    UP , DOWN ,IDLE
}

class Request{
    private final int sourceFloor;
    private final int destinationFloor;
    private final Direction direction;

    public Request(int sourceFloor, int destinationFloor) {
        if(sourceFloor == destinationFloor){
            throw new IllegalArgumentException("Source and Destination cant be same");
        }

        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        this.direction = destinationFloor > sourceFloor?Direction.UP:Direction.DOWN;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

}


class Elevator {

    private final int elevatorId;
    private int currentFloor;
    private Direction direction;
    private ElevatorStatus status;

    private final Queue<Request> requests;
    private boolean headingToDestination;

    public Elevator(int elevatorId, int startingFloor) {
        this.elevatorId = elevatorId;
        this.currentFloor = startingFloor;
        this.direction = Direction.IDLE;
        this.status = ElevatorStatus.IDLE;
        this.requests = new LinkedList<>();
        this.headingToDestination = false;
    }

    public void addRequest(Request request) {
        requests.offer(request);
    }

    public void move() {
        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            status = ElevatorStatus.IDLE;
            headingToDestination = false;
            return;
        }

        Request request = requests.peek();

        int targetFloor = headingToDestination
                ? request.getDestinationFloor()
                : request.getSourceFloor();

        if (currentFloor < targetFloor) {
            direction = Direction.UP;
            status = ElevatorStatus.MOVING;
            currentFloor++;
        } else if (currentFloor > targetFloor) {
            direction = Direction.DOWN;
            status = ElevatorStatus.MOVING;
            currentFloor--;
        } else {
            status = ElevatorStatus.STOPPED;

            if (!headingToDestination) {
                // Passenger has reached the pickup floor
                headingToDestination = true;
            } else {
                // Passenger has reached the destination
                requests.poll();
                headingToDestination = false;

                if (requests.isEmpty()) {
                    direction = Direction.IDLE;
                    status = ElevatorStatus.IDLE;
                }
            }
        }
    }

    public int getElevatorId() {
        return elevatorId;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public ElevatorStatus getStatus() {
        return status;
    }

    public boolean hasRequests() {
        return !requests.isEmpty();
    }
}


class ElevatorController {
    public Elevator selectElevator(List<Elevator> elevators, Request request) {

        Elevator bestElevator = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getStatus() == ElevatorStatus.IDLE && !elevator.hasRequests()) {
                int distance = Math.abs(
                        elevator.getCurrentFloor() - request.getSourceFloor()
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    bestElevator = elevator;
                }
            }
        }

        if (bestElevator == null) {
            for (Elevator elevator : elevators) {
                int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());

                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }
        return bestElevator;
    }

}
class ElevatorSystem{
    private final List<Elevator> elevators;
    private final ElevatorController controller;


    public ElevatorSystem(int numberOfElevators , int startingFloor) {
         elevators = new ArrayList<>();
         controller = new ElevatorController();

         for(int i =1 ;i<=numberOfElevators;i++){
             elevators.add(new Elevator(i , startingFloor));
         }
    }

    public void requestElevator(int sourceFloor , int destinationFloor){
        Request request = new Request(sourceFloor,destinationFloor);
        Elevator elevator = controller.selectElevator(elevators,request);

        if(elevator == null ){
            System.out.println("No elevator available");
            return;
        }

        elevator.addRequest(request);

        System.out.println("Request assigned to Elevator "+ elevator.getElevatorId());
    }

    public void step() {
        for(Elevator elevator : elevators){
            elevator.move();
        }
    }
    public void displayStatus(){
        for(Elevator elevator :elevators){
            System.out.println("Elevator" + elevator.getElevatorId()
                  + "| Floor: " + elevator.getCurrentFloor()
                  + "| Direction:"+ elevator.getDirection()
                  + "| Status:"+ elevator.getStatus());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        ElevatorSystem system = new ElevatorSystem(3, 0);
        System.out.println("Intial Status");
        system.displayStatus();

        System.out.println("\nNew Request: ");
        system.requestElevator(2,7);
        system.requestElevator(2,6);
        system.requestElevator(1,4);
        system.requestElevator(6,2);
        system.requestElevator(4,1);
        system.requestElevator(0,9);

        for (int i=1;i<=5;i++){
            System.out.println("\nStep "+ i);
            system.step();
            system.displayStatus();
        }


    }
}
