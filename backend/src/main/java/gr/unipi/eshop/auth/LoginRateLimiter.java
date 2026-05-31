package gr.unipi.eshop.auth;

import gr.unipi.eshop.shared.LogFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int    USERNAME_THRESHOLD = 10;
    private static final int    IP_THRESHOLD       = 20;
    private static final Duration WINDOW           = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public void checkLimit(String username, String clientIp) {
        var userFailures = getCount(userKey(username));
        var ipFailures   = getCount(ipKey(clientIp));

        if (userFailures >= USERNAME_THRESHOLD || ipFailures >= IP_THRESHOLD) {
            log.atWarn()
                    .addKeyValue(LogFields.Key.EVENT, LogFields.Event.LOGIN_THROTTLED)
                    .addKeyValue(LogFields.Key.USER, username)
                    .log("login throttled username={} userFailures={} ipFailures={}",
                            username, userFailures, ipFailures);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again in 15 minutes."
            );
        }
    }

    public void onFailure(String username, String clientIp) {
        increment(userKey(username));
        increment(ipKey(clientIp));
    }

    public void onSuccess(String username) {
        // Reset per-username counter — a successful login clears the slate
        redisTemplate.delete(userKey(username));
    }

    private long getCount(String key) {
        var value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private void increment(String key) {
        var count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // First failure in this window — set the expiry
            redisTemplate.expire(key, WINDOW);
        }
    }

    private static String userKey(String username) {
        return "login:fail:user:" + username.toLowerCase();
    }

    private static String ipKey(String ip) {
        return "login:fail:ip:" + ip;
    }
}
