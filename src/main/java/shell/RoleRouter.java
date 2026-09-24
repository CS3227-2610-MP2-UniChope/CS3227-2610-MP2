package shell;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import model.user.Role;
import model.user.User;

/** Every role must have a destination. No default fall-through to another role. */
public final class RoleRouter {
    private final Map<Role, RoleView> views;

    public RoleRouter(Map<Role, RoleView> views) {
        this.views = new EnumMap<>(Role.class);
        this.views.putAll(views);
        for (Role role : Role.values()) {
            Objects.requireNonNull(this.views.get(role), "Missing view for " + role);
        }
    }

    public RoleView route(User user) {
        Objects.requireNonNull(user, "user");
        if (!user.isActive()) { throw new IllegalArgumentException("Account is inactive"); }
        return views.get(user.role());
    }
}
