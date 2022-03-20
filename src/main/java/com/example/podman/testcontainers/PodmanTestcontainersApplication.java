package com.example.podman.testcontainers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@SpringBootApplication
public class PodmanTestcontainersApplication {

  @Bean
  ReactiveRedisTemplate<String, Long> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    return new ReactiveRedisTemplate<>(
        factory,
        RedisSerializationContext.<String, Long>newSerializationContext(new JdkSerializationRedisSerializer())
            .key(new StringRedisSerializer())
            .value(new GenericToStringSerializer<>(Long.class))
            .build());
  }

  public static void main(String[] args) {
    SpringApplication.run(PodmanTestcontainersApplication.class, args);
  }
}
