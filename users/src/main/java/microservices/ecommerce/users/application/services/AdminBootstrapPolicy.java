package microservices.ecommerce.users.application.services;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grants {@code ADMIN} to accounts whose e-mail is listed in the {@code ADMIN_EMAILS} configuration
 * (comma-separated, case-insensitive). There are no hardcoded admin credentials: an operator registers a
 * normal account and lists its e-mail; the role is added on registration, login or refresh and persisted.
 * <p>
 * E-mails are not verified by this service, so list only addresses whose accounts already exist (or that
 * you register yourself before exposing the API). Removing an address does not demote the account; see
 * the README for how to revoke the role.
 */
public class AdminBootstrapPolicy {

    private final Set<String> adminEmails;

    public AdminBootstrapPolicy(Collection<String> adminEmails) {
        this.adminEmails = adminEmails == null ? Set.of() : adminEmails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isBootstrapAdmin(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
