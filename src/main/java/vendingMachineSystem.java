import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Product{
    private final String id;
    private final String name;
    private final int price;


    Product (String name, int price) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }
}

class Inventory{
    private final Map<Product,Integer> products = new HashMap<>();
    public void addProduct(Product product,int quantity){
        products.put(product,products.getOrDefault(product,0)+quantity);
    }
    public boolean isAvailable(Product product){
        return products.getOrDefault(product,0) >0;
    }
    public void reduceQuantity(Product product){
        if(isAvailable(product)){
            products.put(product,products.get(product)-1);
        }
    }
}

class vendingMachine{
    private final Inventory inventory;


    vendingMachine(Inventory inventory) {
        this.inventory = inventory;
    }

    public void selectProduct(Product product){
        if(!inventory.isAvailable(product)){
            System.out.println("Product is out of stock");
            return;
        }
        System.out.println("Selected: "+ product.getName());
    }
}

public class vendingMachineSystem {
    public static void main(String[] args) {

        Product coke = new Product("Coke",40);
        Product chips = new Product("lays", 30);

        Inventory inventory = new Inventory();
        inventory.addProduct(coke,5);
        inventory.addProduct(chips,3);

        vendingMachine machine = new vendingMachine(inventory);

        machine.selectProduct(coke);
        machine.selectProduct(chips);
    }
}
