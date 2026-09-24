package microservices.ecommerce.users.application.ports.out;

/** Generates high-entropy opaque tokens and the one-way hash under which they are stored. */
public interface OpaqueTokenGenerator {

    /** A new random, URL-safe token value to hand to the client. */
    String generate();

    /** Deterministic one-way hash used for storage and lookup. */
    String hash(String rawToken);
}
