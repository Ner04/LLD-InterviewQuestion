/*
## Requirements

- Users can call an elevator from any floor with a direction (**UP** or **DOWN**).
- Users inside an elevator can request any destination floor from multiple users.
- Each elevator has its own floor factor `F`, which is configurable and can differ between elevators.
- A call request is added to an elevator only if at least one of these conditions is true:
  - The elevator is idle.
  - The requested floor is at least `F` floors away from the elevator's current position.
  - The requested floor is strictly beyond the next stop in the elevator's current direction.
- A non-idle elevator rejects any call request whose direction does not match its current direction of travel.
- The controller tries every elevator, lowest cost first.
- Dispatch cost is calculated as:

  ```text
  1 per floor traveled to reach the requested floor
  + 10 per existing stop in the queue between the elevator's current position and the requested floor
*

entities

1. Direction enum ( uP and down)
2. ElevatorStatus ( IDle and moving)
3. elevator
4. ElevatorController
5. Main
* */


import java.util.*;

enum Direction{
    UP, DOWN , IDLE
}

enum ElevatorStatus{
    IDLE, MOVING
}


class Elevator{
    private final String id;
    private final int floorFactor;
    private int currentFloor;
    private  Direction direction = Direction.IDLE;
    private final TreeSet<Integer> stops= new TreeSet<>();


    Elevator(int floorFactor, int currentFloor) {
        this.currentFloor = currentFloor;
        this.id = UUID.randomUUID().toString();
        this.floorFactor = floorFactor;
    }
    public synchronized boolean isIdle(){
        return stops.isEmpty() && direction == Direction.IDLE;
    }
    public synchronized int getCurrentFloor(){
        return  currentFloor;
    }
    public  synchronized Direction getDirection(){
        return direction;
    }
    public String getId(){
        return id;
    }

    public synchronized boolean canAccept(int floor , Direction requestedDirection){
        if(isIdle()) return true;
        if(requestedDirection != direction) return  false;
        boolean farEnough = Math.abs(floor-currentFloor)>=floorFactor;
        Integer nextStop = direction == Direction.UP ? stops.higher(currentFloor):stops.lower(currentFloor);
        boolean beyondNextStop = nextStop != null && (direction ==Direction.UP ? floor > nextStop :floor<nextStop);

        return farEnough || beyondNextStop;
    }

    public synchronized boolean requestFloor(int floor){
        if(floor == currentFloor ) return true;
        Direction requestedDirection = floor>currentFloor ? Direction.UP: Direction.DOWN;

        if(direction != Direction.IDLE && requestedDirection != direction){
            return false;
        }
        stops.add(floor);
        if(direction == Direction.IDLE){
            direction = requestedDirection;
        }
        return true;

    }

    public synchronized void addHallCall(int floor , Direction requestedDirection){
        if(!canAccept(floor , requestedDirection)){
            throw new IllegalArgumentException("Elevator can't accept this call");
        }
        if(floor == currentFloor) return;
        boolean wasIdle = isIdle();
        stops.add(floor);
        if(wasIdle){
            direction=requestedDirection;
        }
    }
    private void UpdateDirection(){
        if(stops.isEmpty()){
            direction = Direction.IDLE;
        } else if (stops.first()>currentFloor) {
            direction = Direction.UP;
        } else if (stops.last()<currentFloor) {
            direction = Direction.DOWN;
        } else if (direction == Direction.IDLE) {
            direction = stops.first() >currentFloor ? Direction.UP:Direction.DOWN;
        }
    }

    public synchronized int calculateCost(int floor){
        int cost = Math.abs ( floor - currentFloor);
        for(int stop:stops){
            boolean between = floor >currentFloor ? stop >currentFloor&& stop <floor :stop< currentFloor && stop>floor;
            if(between){
                cost+=10;
            }
        }
        return cost;
    }
    public synchronized void moveOnefloor(){
        if(stops.isEmpty()){
            direction = Direction.IDLE;
            return;
        }
        Integer target = direction == Direction.DOWN ? stops.floor(currentFloor): stops.ceiling(currentFloor);
        if(target == null || target == currentFloor){
            target= direction == Direction.DOWN ? stops.lower(currentFloor):stops.higher(currentFloor);

        }
        if(target == null){
            direction = direction == Direction.UP? Direction.DOWN:Direction.UP;
            moveOnefloor();
            return;
        }
        currentFloor += Integer.compare(target,currentFloor);
        if(stops.remove(currentFloor)){
            System.out.println("Elevator :: "+ id + "stopped at floor" + currentFloor);

        }
        if( stops.isEmpty()){
            direction = Direction.IDLE;
        }
    }
    public synchronized String getStatus(){
        return "Elevator:: " + id + "|FLoor=" + currentFloor+"Direction="+ direction+"|stops= "+ stops;
    }
}


class ElevatorController{
    private  final List<Elevator> elevators;

    ElevatorController(List<Elevator> elevators) {
        this.elevators = elevators;
    }

    public synchronized void requestElevator(int floor , Direction direction)
    {
        Elevator bestElevator = null;
        int lowerCost = Integer.MAX_VALUE;

        for( Elevator elevator:elevators){
            if(!elevator.canAccept(floor,direction)){
                continue;
            }
            int cost = elevator.calculateCost(floor);
            if(cost<lowerCost){
                lowerCost = cost;
                bestElevator = elevator;
            }
        }

        if(bestElevator == null){
            System.out.println("No eligble elevator for floor" + floor);
            return;
        }
        bestElevator.addHallCall(floor,direction);
        System.out.println("Elevator ::" + bestElevator.getId()+"--assigned to floor--" + floor);

    }
}
public class ElevatorSystem {
    public static void main(String[] args) throws InterruptedException {
        Elevator e1 = new Elevator(2,0);
        Elevator e2 = new Elevator(3,5);
        List<Elevator> elevators = Arrays.asList(e1,e2);
        ElevatorController controller = new ElevatorController(elevators);
        controller.requestElevator(4, Direction.UP);
        controller.requestElevator(8 ,Direction.DOWN);
        e1.requestFloor(7);

        System.out.println(e1.getStatus());
        System.out.println(e2.getStatus());
        }
    }

