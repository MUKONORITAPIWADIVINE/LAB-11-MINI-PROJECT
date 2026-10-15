/** A DVD: shorter 7-day loan. */
public class Dvd extends LibraryItem {
    private final int durationMinutes;

    public Dvd(String id, String title, int durationMinutes) {
        super(id, title);
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }
        this.durationMinutes = durationMinutes;
    }

    @Override public int loanPeriodDays() { return 7; }
    @Override public String getType() { return "DVD"; }
    @Override public String getExtra() { return String.valueOf(durationMinutes); }
    public int getDurationMinutes() { return durationMinutes; }

    @Override
    public String toString() { return super.toString() + ", " + durationMinutes + " min"; }
}
