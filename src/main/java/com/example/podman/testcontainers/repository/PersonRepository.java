package com.example.podman.testcontainers.repository;

import com.example.podman.testcontainers.entity.Person;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PersonRepository extends ReactiveCrudRepository<Person, Long> {

    @Query("SELECT * FROM person WHERE last_name = :lastname")
    Flux<Person> findByLastName(String lastName);
}
