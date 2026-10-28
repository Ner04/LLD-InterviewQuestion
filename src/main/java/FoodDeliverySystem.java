
/*
functional requirement
1. Browser the restraunts and their menus
2. Add items from one restaurant to a cart
3. place an order and pay
4. Restaurant accepts or decline the request
5. assigns the delivery partner
6. Driver accepts the request or rejects( if the driver rejects the request goes to the next and not showing again to the current)
7. tracking order status .
8. delivery partners live location
9. send notification to the user

Non-functional Requirement
1. avoid double payement
2. thread safety( Multiple requests/order may assign or accept the same request by delivery)
3. state tranisition( every order will have stage

core entities
0. ZomatoService
1. Customer
2. Restaurant
3. Menu
4. MenuItem
5. Cart / CartItems
6. Order
7. Payment
8. DeliveryPartner
9. Delivery
* */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

enum PartnerStatus{
    AVAILABLE , RESERVED , OFFLINE
}

enum DeliveryStatus{
    ASSIGNED , PICK_UP , DELIVERED , CANCELLED
}

enum OrderStatus{
    PLACED , ACCEPTED , PREPARING , READY_FOR_PICKUP, REJECTED , CANCELLED , DELIVERED , OUT_FOR_DELIVERY
}

enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
class Location{
    private double latitude;
    private double longitude;

    public Location(double latitude, double longitude) {
        if(latitude<-90 || latitude>90){
            throw new IllegalArgumentException("Invalid latitude");
        }
        if(longitude <-180 || longitude>180){
            throw new IllegalArgumentException("Invalid longitude");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public double getLatitude() {
        return latitude;
    }

}

class Address {
    private final String addressId;
    private final String fullAddress;
    private final Location location;

    public Address(String addressId, String fullAddress, Location location) {
        this.addressId = addressId;
        this.fullAddress = fullAddress;
        this.location = location;
    }

    public String getAddressId() {
        return addressId;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public Location getLocation() {
        return location;
    }
}

class Customer {
    private final String id;
    private final String name;
    private Map<String, Address> map = new ConcurrentHashMap<>();

    public Customer(String name, String rahul) {
        this.id =  UUID.randomUUID().toString(); ;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Address getAddress(String addressId) {
        return map.get(addressId);
    }

    public void removeAddress(String addressId) {
        map.remove(addressId);
    }

}

class MenuItem {
    private final String id;
    private String name;
    private double price;
    private boolean available;

    public MenuItem(String name, double price) {
        this.id =  UUID.randomUUID().toString(); ;
        this.name = name;
        this.price = price;
        this.available=true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }


    public void updatePrice (double price){
        if(price<0){
            throw new IllegalArgumentException("price can't be negative");
        }
        this.price = price;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}

class Menu {
    private final String id;
    private final Map<String, MenuItem> menuItems;

    public Menu() {
        this.id = UUID.randomUUID().toString();
        this.menuItems = new ConcurrentHashMap<>();
    }

    public void addMenuItem(MenuItem item){
        menuItems.put(item.getId(), item);
    }
    public void removeMenuItem(String itemId) {
        menuItems.remove(itemId);
    }

    public MenuItem getMenuItem(String itemId) {
        return menuItems.get(itemId);
    }

    public Map<String, MenuItem> getMenuItems() {
        return Map.copyOf(menuItems);
    }

    public void displayMenu() {
        for (MenuItem item : menuItems.values()) {
            if (item.isAvailable()) {
                System.out.println(
                        item.getName() + " - ₹" + item.getPrice()
                );
            }
        }
    }

    public String getId() {
        return id;
    }
}

class Restaurant{
    private final String id;
    private final String name;
    private final Location location;
    private final Menu menu;

    public Restaurant(Menu menu, Location location, String name) {
        this.id = UUID.randomUUID().toString();
        this.menu = menu;
        this.location = location;
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public Menu getMenu() {
        return menu;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void acceptOrder (Order order){
        order.updateStatus(OrderStatus.ACCEPTED);
        System.out.println(name+ "accepted order" + order.getId());
    }
    public void rejectOrder(Order order){
        order.updateStatus(OrderStatus.REJECTED);
        System.out.println(name+"rejected order"+order.getId());
    }

}

class CartItem {

    private final MenuItem menuItem;
    private int quantity;

    public CartItem(MenuItem menuItem, int quantity) {
        if (menuItem == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.quantity = quantity;
    }

    public double getSubtotal() {
        return menuItem.getPrice() * quantity;
    }
}

class Order {
    private final String id;
    private final Customer customer;
    private final Restaurant restaurant;
    private final Cart cart;
    private DeliveryPartner deliveryPartner;
    private OrderStatus status;

    public Order(Cart cart, Restaurant restaurant, Customer customer) {
        this.id = UUID.randomUUID().toString();
        this.cart = cart;
        this.restaurant = restaurant;
        this.customer = customer;
        this.status = OrderStatus.PLACED;
    }

    public synchronized void updateStatus ( OrderStatus newStatus){
        if(status == OrderStatus.CANCELLED || status == OrderStatus.DELIVERED || status == OrderStatus.REJECTED){
            throw  new IllegalStateException("cannot update a completed order");
        }
        this.status = newStatus;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Cart getCart() {
        return cart;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getId() {
        return id;
    }

    public synchronized boolean assignDeliveryPartner(
            DeliveryPartner partner) {

        if (partner == null || this.deliveryPartner != null) {
            return false;
        }

        if (status != OrderStatus.READY_FOR_PICKUP) {
            return false;
        }

        deliveryPartner = partner;
        status = OrderStatus.OUT_FOR_DELIVERY;

        return true;
    }
}

class Cart {

    private final String id;
    private String restaurantId;
    private final Map<String, CartItem> items = new HashMap<>();

    public Cart() {
        this.id = UUID.randomUUID().toString();
    }

    public synchronized void addItem(
            Restaurant restaurant, MenuItem item, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        if (!item.isAvailable()) {
            throw new IllegalStateException("Item is unavailable");
        }

        if (restaurantId != null
                && !restaurantId.equals(restaurant.getId())) {
            throw new IllegalStateException(
                    "Cart can contain items from one restaurant only");
        }

        restaurantId = restaurant.getId();

        CartItem existing = items.get(item.getId());

        if (existing != null) {
            existing.updateQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(item.getId(), new CartItem(item, quantity));
        }
    }

    public synchronized void removeItem(String itemId) {
        items.remove(itemId);

        if (items.isEmpty()) {
            restaurantId = null;
        }
    }

    public synchronized double getTotal() {
        double total = 0;

        for (CartItem item : items.values()) {
            total += item.getSubtotal();
        }

        return total;
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized Map<String, CartItem> getItems() {
        return Map.copyOf(items);
    }

    public synchronized String getRestaurantId() {
        return restaurantId;
    }

    public String getId() {
        return id;
    }
}

class DeliveryPartner{
    private final String id;
    private final String name;

    private volatile Location location;
    private PartnerStatus status;

    private final Set<String> rejectedOrderIds = new HashSet<>();

    public DeliveryPartner(Location location, String name) {
        this.id = UUID.randomUUID().toString();
        this.location = location;
        this.name = name;
        this.status= PartnerStatus.AVAILABLE;
    }

    public synchronized boolean acceptOrder (Order order){
        if(status != PartnerStatus.AVAILABLE){
            return false;
        }

        if(rejectedOrderIds.contains(order.getId())){
            return false;
        }

        if(order.getStatus() != OrderStatus.READY_FOR_PICKUP){
            return  false;
        }

        // Assign this driver to the order
        order.assignDeliveryPartner(this);

        status = PartnerStatus.RESERVED;

        return true;
    }

    public synchronized void rejectOrder(Order order) {
        rejectedOrderIds.add(order.getId());
    }

    public synchronized boolean hasRejected(String orderId) {
        return rejectedOrderIds.contains(orderId);
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public synchronized void updateStatus(PartnerStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public synchronized PartnerStatus getStatus() {
        return status;
    }
    }



public class FoodDeliverySystem {

    public static void main(String[] args) {

        // 1. Create restaurant and menu
        Location restaurantLocation = new Location(12.9352, 77.6245);
        Menu menu = new Menu();

        Restaurant restaurant =
                new Restaurant(menu, restaurantLocation, "Food Palace");

        MenuItem biryani = new MenuItem("Chicken Biryani", 280.0);
        MenuItem paneer = new MenuItem("Paneer Tikka", 220.0);

        restaurant.getMenu().addMenuItem(biryani);
        restaurant.getMenu().addMenuItem(paneer);

        System.out.println("=== RESTAURANT MENU ===");
        restaurant.getMenu().displayMenu();

        // 2. Create customer
        Customer customer = new Customer("C101", "Rahul");

        Address home = new Address(
                "A101",
                "Koramangala, Bengaluru",
                new Location(12.9350, 77.6240)
        );

        // 3. Create cart and add items
        Cart cart = new Cart();

        cart.addItem(restaurant, biryani, 2);
        cart.addItem(restaurant, paneer, 1);

        System.out.println("\n=== CART ===");
        System.out.println("Customer: " + customer.getName());
        System.out.println("Cart total: Rs. " + cart.getTotal());

        // 4. Place order
        Order order = new Order(cart, restaurant, customer);

        System.out.println("\n=== ORDER PLACED ===");
        System.out.println("Order ID: " + order.getId());
        System.out.println("Order status: " + order.getStatus());

        // 5. Restaurant accepts the order
        restaurant.acceptOrder(order);

        // 6. Restaurant prepares the order
        order.updateStatus(OrderStatus.PREPARING);
        order.updateStatus(OrderStatus.READY_FOR_PICKUP);

        System.out.println("\nOrder ready for pickup: "
                + order.getStatus());

        // 7. First driver rejects the order
        DeliveryPartner amit = new DeliveryPartner(
                new Location(12.9360, 77.6250),
                "Amit"
        );

        DeliveryPartner rahul = new DeliveryPartner(
                new Location(12.9370, 77.6260),
                "Rahul"
        );

        amit.rejectOrder(order);

        System.out.println("\n=== DRIVER ASSIGNMENT ===");

        boolean amitAccepted = amit.acceptOrder(order);
        System.out.println("Amit accepted: " + amitAccepted);

        // 8. Next driver accepts
        boolean rahulAccepted = rahul.acceptOrder(order);
        System.out.println("Rahul accepted: " + rahulAccepted);

        System.out.println("Order status: " + order.getStatus());

        // 9. Test duplicate assignment
        DeliveryPartner thirdDriver = new DeliveryPartner(
                new Location(12.9380, 77.6270),
                "Priya"
        );

        boolean thirdAccepted = thirdDriver.acceptOrder(order);
        System.out.println("Priya accepted same order: " + thirdAccepted);

        // 10. Update live location
        rahul.updateLocation(new Location(12.9400, 77.6300));

        System.out.println("\n=== LIVE TRACKING ===");
        System.out.println("Driver: " + rahul.getName());
        System.out.println("Latitude: "
                + rahul.getLocation().getLatitude());
        System.out.println("Longitude: "
                + rahul.getLocation().getLongitude());

        System.out.println("\nAll tests completed.");
    }
}