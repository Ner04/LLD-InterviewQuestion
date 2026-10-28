//import java.util.Map;
//import java.util.TreeSet;
//import java.util.UUID;
//
//enum Direction{
//    UP,DOWN,IDLE
//}
//
//class Elevator{
//    private final int id;
//    private final int floorFactor;
//    private int currentFloor;
//    private Direction direction;
//    private final TreeSet<Integer> stops = new TreeSet<>();
//
//    Elevator(int floorFactor, int currentFloor, Direction direction) {
//        this.currentFloor = currentFloor;
//        this.direction = Direction.IDLE;
//        this.id = Integer.parseInt(UUID.randomUUID().toString());
//        this.floorFactor = floorFactor;
//    }
//
//    boolean canAcceptCall(int floor , Direction requestDirection){
//        if(direction != Direction.IDLE && direction !=requestDirection)
//            return false;
//
//        if(direction == Direction.IDLE){
//            return true;
//        }
//        if (Math.abs(currentFloor - floor) >= floorFactor) return true;
//
//        Integer nextStop = direction == Direction.UP?stops.higher(currentFloor):stops.lower(currentFloor);
//
//        return  nextStop != null &&(direction == Direction.UP?floor>nextStop:floor<nextStop);
//    }
//    int calculateCost(int floor){
//        int cost = Math.abs(currentFloor - floor) && stop < Math.max(currentFloor,floor){
//            cost +=10;
//        }
//    }
//
//}
//
//public class ElevatorDispatchSystem {
//}
