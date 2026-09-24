package model;

/** Shared domain validation; models remain independent of JavaFX and persistence. */
public final class Validation {
    private Validation() { }

    public static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    public static String email(String value) {
        String email = text(value, "email");
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        return email;
    }
}
