package gr.unipi.eshop.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializes Spring Session attributes as JSON in Redis instead of the default JDK serialization.
 * <p>
 * JDK serialization (ObjectInputStream) is a known RCE-gadget vector; JSON avoids invoking arbitrary
 * deserialization callbacks. The cart and pending order live in the database, so the only object stored
 * in the session is the Spring Security context — and Spring Security ships dedicated Jackson
 * deserializers for it (registered by SecurityJacksonModules), so no global default typing /
 * PolymorphicTypeValidator is involved.
 * <p>
 * SecurityJacksonModules registers mix-ins/deserializers for Spring Security types (SecurityContextImpl,
 * UsernamePasswordAuthenticationToken, User, ...), which are immutable / have no default constructor
 * and cannot otherwise be reconstructed by Jackson.
 * <p>
 * docs: spring-session/reference/configuration/redis.html#serializing-session-using-json
 */
@NullMarked
@Configuration
public class SessionConfig implements BeanClassLoaderAware {

    private ClassLoader loader = getClass().getClassLoader();

    // Bean name MUST be springSessionDefaultRedisSerializer to override Spring Session's default.
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new JacksonJsonRedisSerializer<>(objectMapper(), Object.class);
    }

    private JsonMapper objectMapper() {
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(loader))
                .build();
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}
