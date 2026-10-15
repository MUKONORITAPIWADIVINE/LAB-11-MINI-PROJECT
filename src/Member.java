import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A registered library member. Holds at most MAX_LOANS active loans. */
public class Member {
    public static final int MAX_LOANS = 3;

    private final String id;
    private final String name;
    private final List<Loan> loans = new ArrayList<>();

    public Member(String id, String name) {
        this.id = Validation.requireText(id, "Member id");
        this.name = Validation.requireText(name, "Name");
    }

    void addLoan(Loan loan) { loans.add(loan); }
    void removeLoan(Loan loan) { loans.remove(loan); }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getLoanCount() { return loans.size(); }
    /** Read-only view: outsiders cannot change the list behind our back. */
    public List<Loan> getLoans() { return Collections.unmodifiableList(loans); }

    @Override
    public String toString() {
        return String.format("Member %s - %s (%d/%d loans)", id, name, loans.size(), MAX_LOANS);
    }
}
