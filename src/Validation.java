/** Small helper so every class validates text the same way (no duplication). */
final class Validation {
    private Validation() { }

    /** Text must be non-blank and must not contain commas (commas separate CSV fields). */
    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be empty.");
        }
        if (value.contains(",")) {
            throw new IllegalArgumentException(field + " must not contain commas.");
        }
        return value.trim();
    }
}
