package ao.skool.common.security;

/**
 * Canonical role names. Kept as constants so annotations can reference them by symbol,
 * and there's exactly one place to add a role.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String DIRECTOR = "DIRECTOR";
    public static final String SECRETARY = "SECRETARY";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";
    public static final String GUARDIAN = "GUARDIAN";
    public static final String MINISTRY = "MINISTRY";

    private Roles() {}
}
