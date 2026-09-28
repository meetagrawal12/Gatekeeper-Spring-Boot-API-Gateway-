package in.coderarmy.gatekeeper.management.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force guard: 5 failures within 10 minutes locks the username
 * for the rest of that window. For multi-instance deployments move this to Redis.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private record Attempts(int count, Instant windowStart) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempts a = attempts.get(key(username));
        if (a == null) {
            return false;
        }
        if (a.windowStart().plus(WINDOW).isBefore(Instant.now())) {
            attempts.remove(key(username));
            return false;
        }
        return a.count() >= MAX_FAILURES;
    }

    public void failed(String username) {
        attempts.merge(key(username), new Attempts(1, Instant.now()),
                (old, ignored) -> old.windowStart().plus(WINDOW).isBefore(Instant.now())
                        ? new Attempts(1, Instant.now())
                        : new Attempts(old.count() + 1, old.windowStart()));
    }

    public void succeeded(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username.toLowerCase();
    }
}
