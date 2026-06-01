package gr.unipi.eshop.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

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
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Number.class)
                .allowIfSubType(String.class);
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(loader, typeValidator))
                .build();
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}
