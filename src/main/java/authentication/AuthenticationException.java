package authentication;

/** Deliberately generic error shown for every unsuccessful login attempt. */
public final class AuthenticationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException() { super("Email or password is incorrect."); }
}
