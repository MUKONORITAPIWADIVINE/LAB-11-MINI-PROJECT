/**
 * Contract for anything that can be lent out and handed back.
 * Abstraction: callers know WHAT can be done, not HOW each item does it.
 */
public interface Borrowable {
    /** Lends this item to the member and returns the Loan that records it. */
    Loan borrow(Member member) throws ItemNotAvailableException, MemberLimitExceededException;

    /** Returns this item to the library. */
    void giveBack();
}
