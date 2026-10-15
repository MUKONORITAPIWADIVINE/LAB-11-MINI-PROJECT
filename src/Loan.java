import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Records that one member borrowed one item on a given date. Immutable. */
public class Loan {
    private final Member member;
    private final LibraryItem item;
    private final LocalDate borrowDate;

    public Loan(Member member, LibraryItem item, LocalDate borrowDate) {
        if (member == null || item == null || borrowDate == null) {
            throw new IllegalArgumentException("Loan needs a member, an item and a date.");
        }
        this.member = member;
        this.item = item;
        this.borrowDate = borrowDate;
    }

    public Member getMember() { return member; }
    public LibraryItem getItem() { return item; }
    public LocalDate getBorrowDate() { return borrowDate; }

    /** Polymorphism at work: the due date depends on the item's own loan period. */
    public LocalDate getDueDate() { return borrowDate.plusDays(item.loanPeriodDays()); }

    public boolean isOverdue(LocalDate today) { return today.isAfter(getDueDate()); }

    public long daysOverdue(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(getDueDate(), today));
    }

    @Override
    public String toString() {
        return String.format("'%s' -> %s (borrowed %s, due %s)",
                item.getTitle(), member.getName(), borrowDate, getDueDate());
    }
}
