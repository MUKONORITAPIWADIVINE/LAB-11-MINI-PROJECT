/**
 * Factory pattern: callers (e.g. the CSV loader and the menu) ask for an item by
 * type name and never hard-code the concrete classes.
 */
public final class ItemFactory {
    private ItemFactory() { }

    public static LibraryItem create(String type, String id, String title, String extra) {
        return switch (type.trim().toUpperCase()) {
            case "BOOK" -> new Book(id, title, extra);
            case "DVD" -> {
                try {
                    yield new Dvd(id, title, Integer.parseInt(extra.trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Duration must be a whole number of minutes.");
                }
            }
            default -> throw new IllegalArgumentException("Unknown item type: " + type + " (use BOOK or DVD)");
        };
    }
}
