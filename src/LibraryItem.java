import java.time.LocalDate;

/**
 * Abstract parent of everything the library lends (Book, Dvd).
 * Inheritance + abstraction: shared state and behaviour live here,
 * while loanPeriodDays() is left for each subclass to define.
 */
public abstract class LibraryItem implements Borrowable {
    private final String id;
    private final String title;
    private boolean available = true;
    private Loan currentLoan;               // null when the item is on the shelf

    protected LibraryItem(String id, String title) {
        this.id = Validation.requireText(id, "Item id");
        this.title = Validation.requireText(title, "Title");
    }

    /** Each kind of item has its own loan period (polymorphism). */
    public abstract int loanPeriodDays();

    /** Short label used in listings and in the CSV file ("BOOK" or "DVD"). */
    public abstract String getType();

    /** The one extra CSV field that belongs to the subclass (author, duration...). */
    public abstract String getExtra();

    @Override
    public Loan borrow(Member member) throws ItemNotAvailableException, MemberLimitExceededException {
        return borrow(member, LocalDate.now());
    }

    /** Package-private so the library can restore saved loans with their original date. */
    Loan borrow(Member member, LocalDate borrowDate)
            throws ItemNotAvailableException, MemberLimitExceededException {
        if (!available) {
            throw new ItemNotAvailableException("'" + title + "' is already on loan.");
        }
        if (member.getLoanCount() >= Member.MAX_LOANS) {
            throw new MemberLimitExceededException(member.getName()
                    + " already has " + Member.MAX_LOANS + " loans (the maximum).");
        }
        Loan loan = new Loan(member, this, borrowDate);
        available = false;
        currentLoan = loan;
        member.addLoan(loan);
        return loan;
    }

    @Override
    public void giveBack() {
        if (available) {
            throw new IllegalStateException("'" + title + "' is not on loan.");
        }
        currentLoan.getMember().removeLoan(currentLoan);
        currentLoan = null;
        available = true;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isAvailable() { return available; }
    public Loan getCurrentLoan() { return currentLoan; }

    @Override
    public String toString() {
        return String.format("[%s] %-6s %-28s %s (loan: %d days)",
                getType(), id, title, available ? "available" : "ON LOAN", loanPeriodDays());
    }
}
