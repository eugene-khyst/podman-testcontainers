package com.example.podman.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class RedisIncrementTests extends BaseIntegrationTest {

  @Autowired ReactiveRedisTemplate<String, Long> redisTemplate;

  @Test
  void shouldIncrementKey() {
    Flux.range(0, 3)
        .flatMap(i -> redisTemplate.opsForValue().increment("mykey"))
        .as(StepVerifier::create)
        .expectNext(1L)
        .expectNext(2L)
        .expectNext(3L)
        .verifyComplete();
  }
}
