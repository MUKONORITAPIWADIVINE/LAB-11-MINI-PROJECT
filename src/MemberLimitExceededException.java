/** Checked exception: the member already holds the maximum number of loans. */
public class MemberLimitExceededException extends Exception {
    public MemberLimitExceededException(String message) { super(message); }
}
