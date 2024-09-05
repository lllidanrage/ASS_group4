import java.util.*;

import static java.lang.String.format;

public class MailRoom {
    public enum Mode {CYCLING, FLOORING}
    //Eventually change to a List of mailItems, Polymorphism/Protected Variation
    List<MailItem>[] waitingForDelivery;
    private final int numRobots;

    Queue<Robot> idleRobots;
    List<Robot> activeRobots;
    List<Robot> deactivatingRobots; // Don't treat a robot as both active and idle by swapping directly

    private Mode mode;

    //Already sounds Polymorphic, potential hint
    //Exit early
    public boolean someItems() {
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!waitingForDelivery[i].isEmpty()) {
                    return true;
            }
        }
        return false;
    }

    private int floorWithEarliestItem() {
        int floor = -1;
        int earliest = Simulation.now() + 1;
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!waitingForDelivery[i].isEmpty()) {
                int arrival = waitingForDelivery[i].getFirst().myArrival();
                if (earliest > arrival) {
                    floor = i;
                    earliest = arrival;
                }
            }
        }
        return floor;
    }
    //Need to change some of this code to incorporate for new floor and column robots
    MailRoom(int numFloors, int numRobots,Mode mode) {
        this.mode = mode;
        waitingForDelivery = new List[numFloors];
        for (int i = 0; i < numFloors; i++) {
            waitingForDelivery[i] = new LinkedList<>();
        }
        this.numRobots = numRobots;

        idleRobots = new LinkedList<>();

        if (this.mode == Mode.CYCLING) {
            for (int i = 0; i < numRobots; i++)
                idleRobots.add(new CyclingRobot(MailRoom.this));  // In mailroom, floor/room is not significant
            activeRobots = new ArrayList<>();
            deactivatingRobots = new ArrayList<>();
        }
    }

    void arrive(List<MailItem> items) {
        for (MailItem item : items) {
            waitingForDelivery[item.myFloor()-1].add(item);
            System.out.printf("Item: Time = %d Floor = %d Room = %d Weight = %d\n",
                    item.myArrival(), item.myFloor(), item.myRoom(), 0);
        }
    }

    public void tick() { // Simulation time unit
        if(this.mode == Mode.CYCLING){
            for (Robot activeRobot : activeRobots) {
                System.out.printf("About to tick: " + activeRobot.toString() + "\n"); activeRobot.tick();
            }
            robotDispatch();  // dispatch a robot if conditions are met
            // These are returning robots who shouldn't be dispatched in the previous step
            ListIterator<Robot> iter = deactivatingRobots.listIterator();
            while (iter.hasNext()) {  // In timestamp order
                Robot robot = iter.next();
                iter.remove();
                activeRobots.remove(robot);
                idleRobots.add(robot);
            }
            
        } else if (this.mode == Mode.FLOORING) {
            
        }

    }

    void robotDispatch() { // Can dispatch at most one robot; it needs to move out of the way for the next
        System.out.println("Dispatch at time = " + Simulation.now());
        // Need an idle robot and space to dispatch (could be a traffic jam)
        if (!idleRobots.isEmpty() && !Building.getBuilding().isOccupied(0,0)) {
            int fwei = floorWithEarliestItem();
            if (fwei >= 0) {  // Need an item or items to deliver, starting with earliest
                Robot robot = idleRobots.remove();
                loadRobot(fwei, robot);
                // Room order for left to right deliver.
                robot.sort();
                activeRobots.add(robot);
                System.out.println("Dispatch @ " + Simulation.now() +
                        " of Robot " + robot.getId() + " with " + robot.numItems() + " item(s)");
                robot.place(0, 0);
            }
        }
    }

    void robotReturn(Robot robot) {
        Building building = Building.getBuilding();
        int floor = robot.getFloor();
        int room = robot.getRoom();
        assert floor == 0 && room == building.NUMROOMS+1: format("robot returning from wrong place - floor=%d, room ==%d", floor, room);
        assert robot.isEmpty() : "robot has returned still carrying at least one item";
        building.remove(floor, room);
        deactivatingRobots.add(robot);
    }

    void loadRobot(int floor, Robot robot) {
        ListIterator<MailItem> iter = waitingForDelivery[floor].listIterator();
        while (iter.hasNext()) {  // In timestamp order
            MailItem item = iter.next();
            robot.add(item); //Hand it over
            iter.remove();
        }
    }

}
