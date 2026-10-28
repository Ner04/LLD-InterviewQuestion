import java.time.LocalDateTime;
import java.util.*;

enum Role{
    DEVELOPER(1),MANAGER(2),DIRECTOR(3),VP(4);
    private final int priority;
    Role( int priority) {
        this.priority = priority;
    }
    public int getPriority() {
        return priority;
    }
}


class eventUser {
    private final String id;
    private final String name;
    private final Role role;

    eventUser(String name, Role role) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public Role getRole() {
        return role;
    }
}

class Event{
     private final String id;
     private final String name;
     private final LocalDateTime startDateTime;
     private final LocalDateTime endDateTime;
     private final String schedulerId;
     private final Set<String> participantIds;

    Event(String name, LocalDateTime startDateTime, LocalDateTime endDateTime, String schedulerId, Set<String> participantIds) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.schedulerId = schedulerId;
        this.participantIds = new HashSet<>(participantIds);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public String getSchedulerId() {
        return schedulerId;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public Set<String> getParticipantIds() {
        return participantIds;
    }

    public boolean overLaps(Event other){
        return startDateTime.isBefore(other.endDateTime) && other.startDateTime.isBefore(endDateTime);
    }
    public boolean involvesUser(String userId){
        return  schedulerId.equals(userId) || participantIds.contains(userId);
    }

    public void display(){
        System.out.println(name + " | " + startDateTime + " - " + endDateTime+ " | Scheduled by: " + schedulerId + " | particpants: " + participantIds );
    }
}


class CalendarService{
    private final Map<String,eventUser> users = new HashMap<>();
    private final List<Event> events = new ArrayList<>();
    private eventUser loggedInUser;

   public eventUser register(String name,Role role){
       eventUser user = new eventUser(name,role);
       users.put(user.getId(), user);
       System.out.println("Registered: " + name + "|ID:" + user.getId());

       return user;
   }

   public void login (String userId){
       eventUser user = users.get(userId);
       if(user == null){
           throw new IllegalArgumentException("User not found");
       }

       loggedInUser = user;
       System.out.println("LoggedIn: "+ user.getName());
   }

   public Event scheduleEvent(String name , LocalDateTime start , LocalDateTime end , Set<String> participantIds){
       if ( loggedInUser == null){
           throw new IllegalStateException("please login first");
       }
       if(start == null || end == null){
           throw new IllegalArgumentException("Invalid event time");
       }

       for(String id:participantIds){
           if(!users.containsKey(id)){
               throw new IllegalArgumentException("unknown participants: "+ id);
           }
       }

       Event newEvent = new Event(name, start , end, loggedInUser.getId(),participantIds);
       events.add(newEvent);
       return newEvent;
   }

   public void listEvents(){
       if(loggedInUser == null){
           throw new IllegalStateException("please login First");
       }
        System.out.println("\n events for" + loggedInUser.getName());
       for( Event event:events){
           if(event.involvesUser(loggedInUser.getId())){
               event.display();
           }
       }
    }

    private void resolveConflicts(Event newEvent) {
        for (Event existing : events) {
            if (newEvent.overLaps(existing)) {
                System.out.println("Conflict detected: "
                        + newEvent.getName() + " vs "
                        + existing.getName());

                Event winner = chooseWinner(newEvent, existing);

                System.out.println("Higher priority: "
                        + winner.getName());
            }
        }
    }

   private Event chooseWinner( Event first , Event second){
   String currentUserId = loggedInUser.getId();
   boolean firstCreateByCurrentUser = first.getSchedulerId().equals(currentUserId);
   boolean secondCreatedByCurrentUser = second.getSchedulerId().equals(currentUserId);

   if((firstCreateByCurrentUser) && (!secondCreatedByCurrentUser)){
       return first;
   }
   if(!firstCreateByCurrentUser && secondCreatedByCurrentUser){
       return second;
   }
   eventUser firstScheduler = users.get(first.getSchedulerId());
   eventUser secondScheduler = users. get(second.getSchedulerId());

   if(firstScheduler.getRole().getPriority() >=secondScheduler.getRole().getPriority()){
       return first;
   }
   return  second;
   }
}


public class EventSchedularSystem {
    public static void main(String[] args) {

        CalendarService service = new CalendarService();

        // 1. Register users
        eventUser developer =
                service.register("Rahul", Role.DEVELOPER);

        eventUser manager =
                service.register("Priya", Role.MANAGER);

        eventUser vp =
                service.register("Amit", Role.VP);

        // 2. Login as manager
        service.login(manager.getId());

        // 3. Schedule the first event
        Event meeting1 = service.scheduleEvent(
                "Team Meeting",
                LocalDateTime.of(2026, 10, 28, 10, 0),
                LocalDateTime.of(2026, 10, 28, 11, 0),
                new HashSet<>(Arrays.asList(developer.getId()))
        );

        // 4. Schedule an overlapping event
        Event meeting2 = service.scheduleEvent(
                "Architecture Review",
                LocalDateTime.of(2026, 10, 28, 10, 30),
                LocalDateTime.of(2026, 10, 28, 11, 30),
                new HashSet<>(Arrays.asList(vp.getId()))
        );

        // 5. Display events relevant to the logged-in user
        service.listEvents();
    }
}
