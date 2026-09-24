package microservices.ecommerce.users.application.ports.out;

import microservices.ecommerce.users.core.entities.User;

public interface TokenIssuer {
    /** Issues a signed, short-lived access token for the user (subject = user id, claim {@code roles}). */
    String issue(User user);

    /** Lifetime of the access tokens issued by {@link #issue(User)}, in seconds. */
    long accessTokenTtlSeconds();
}
