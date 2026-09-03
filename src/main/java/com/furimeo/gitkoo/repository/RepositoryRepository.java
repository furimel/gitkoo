package com.furimeo.gitkoo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jdbc.repository.query.Query;

/**
 * Spring Data JDBC repository for {@link Repository}.
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface RepositoryRepository extends CrudRepository<Repository, Long> {

    /** Finds a repository by its owner and name, e.g. {@code minh/pump}. */
    Optional<Repository> findByOwnerTypeAndOwnerIdAndName(
            @Param("ownerType") String ownerType, @Param("ownerId") Long ownerId, @Param("name") String name);

    /** Lists repositories owned by a user. */
    List<Repository> findByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
}
