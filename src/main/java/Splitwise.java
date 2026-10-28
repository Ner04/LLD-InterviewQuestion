import java.util.*;

/**
Functional requirements (FR)

 1. Create users — each user has a unique ID and name.
 2. Create a group — add users to a group of friends or remove them .
 3. Add an expense — one user pays an amount on behalf of multiple group members.
 4. Split equally — divide the expense equally among the selected members.
 5. Show balances — display who owes whom and how much.
 6. Settle a debt — a user pays another user to reduce their outstanding balance.

Non-functional requirements (NFR)

 1. Thread safety	Prevent concurrent expenses from corrupting balances.
 2. Data consistency	An expense and all its balance changes must happen together.
 3. Correctness	Reject invalid amounts, users, and group members.
 4. Maintainability	Use a few focused classes with clear responsibilities.
 5. Performance	Use hash maps for quick user and balance lookups

 stack will be using


 Entities
 1. user
 2. Group
 3. expense
 4. Split
 5. Balance
 6. SplitStrategy
 7. EqualSplitStrategy

 **/

enum SplitType {
    EQUAL,EXACT,PERCENTAGE
}
class User {

    private final String id;
    private final String name;

    public User(String name, Role role) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

/*
*A set prevents duplicate members from being added to the group, assuming User implements appropriate equals() and hashCode() methods. Our current User class doesn't implement them yet, so two different User objects with the same ID could still be added. We'll address that if needed
* */
class Group{
    private  final  String id;
    private  final String name;
    private final Set<User> members;

    public Group(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.members = new HashSet<>();
    }

    public Set<User> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}

class Split{
    private final User user;
    private final long amountPaise;

    public Split(User user, long amountPaise) {
        this.user = user;
        this.amountPaise = amountPaise;
    }

    public User getUser() {
        return user;
    }

    public long getAmountPaise() {
        return amountPaise;
    }
}

class Expense{
    private final String id;
    private final String description;
    private final User paidBy;
    private final long totalAmountPaise;
    private final List<Split> splits;
    private final SplitType splitType;

    public Expense(SplitType splitType, List<Split> splits, long totalAmountPaise, User paidBy, String description) {
        this.id = UUID.randomUUID().toString();
        this.splitType = splitType;
        this.splits = splits;
        this.totalAmountPaise = totalAmountPaise;
        this.paidBy = paidBy;
        this.description = description;

    }

    public User getPaidBy() {
        return paidBy;
    }

    public long getTotalAmountPaise() {
        return totalAmountPaise;
    }

    public List<Split> getSplits() {
        return Collections.unmodifiableList(splits);
    }

    public SplitType getSplitType() {
        return splitType;
    }
}

class Balance{
    private final User debtor;
    private final User creditor;
    private long amountPaise;

    public Balance(long amountPaise, User creditor, User debtor) {
        this.amountPaise = amountPaise;
        this.creditor = creditor;
        this.debtor = debtor;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public User getCreditor() {
        return creditor;
    }

    public User getDebtor() {
        return debtor;
    }

    public void reduceAmount(long amountPaise) {
        if (amountPaise <= 0 || amountPaise > this.amountPaise) {
            throw new IllegalArgumentException("Invalid settlement amount");
        }

        this.amountPaise -= amountPaise;
    }
}

interface SplitStrategy{
    List<Split> calculateSplits(
            long totalAmountPaise,
            List<User> participants
    );
}


class EqualSplitStrategy implements SplitStrategy{
    @Override
    public List<Split> calculateSplits(long totalAmountPaise, List<User> participants) {
        if (totalAmountPaise <= 0 || participants.isEmpty()) {
            throw new IllegalArgumentException("Invalid expense");
        }

        long baseShare = totalAmountPaise / participants.size();
        long remainder = totalAmountPaise % participants.size();

        List<Split> splits = new ArrayList<>();

        for (int i = 0; i < participants.size(); i++) {
            long share = baseShare + (i < remainder ? 1 : 0);

            splits.add(new Split(participants.get(i), share));
        }
        return splits;
    }
}

class SplitwiseService {

    private final Map<String, User> users = new HashMap<>();

    private final Map<String, Group> groups = new HashMap<>();

    // debtorId -> creditorId -> amount owed in paise
    private final Map<String, Map<String, Long>> balances =
            new HashMap<>();

    private final List<Expense> expenses = new ArrayList<>();

    public synchronized void addUser(User user) {
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User already exists");
        }

        users.put(user.getId(), user);
    }
}

public class Splitwise {
}
