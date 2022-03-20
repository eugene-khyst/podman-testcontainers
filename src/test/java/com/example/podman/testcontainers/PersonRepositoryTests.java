package com.example.podman.testcontainers;

import com.example.podman.testcontainers.entity.Person;
import com.example.podman.testcontainers.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

@Slf4j
class PersonRepositoryTests extends BaseIntegrationTest {

  @Autowired PersonRepository personRepository;

  @Test
  void shouldFindByLastName() {
    personRepository.save(new Person(null, "Harry", "Callahan")).subscribe();

    personRepository
        .findByLastName("Callahan")
        .doOnNext(person -> log.info("Person found with findByLastName(\"Callahan\"): {}", person))
        .as(StepVerifier::create)
        .expectNextMatches(
            person ->
                person.id() != null
                    && "Harry".equals(person.firstName())
                    && "Callahan".equals(person.lastName()))
        .verifyComplete();
  }
}
