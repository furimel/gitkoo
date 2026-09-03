package com.furimeo.gitkoo.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface TeamRepository extends CrudRepository<Team, Long> {
    Optional<Team> findByName(String name);
    boolean existsByName(String name);
}
