package com.furimeo.gitkoo.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Stars.
 *
 * <p>Counts are {@code COUNT(*)} on an indexed column rather than a cached counter
 * column: free at this scale, and a counter cache is a drift bug waiting to happen.
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface StarRepository extends CrudRepository<RepositoryStar, Long> {

    int countByRepositoryId(Long repositoryId);

    boolean existsByRepositoryIdAndUserId(Long repositoryId, Long userId);

    @Modifying
    @Query("DELETE FROM repository_stars WHERE repository_id = :repositoryId AND user_id = :userId")
    void deleteStar(@Param("repositoryId") Long repositoryId, @Param("userId") Long userId);
}
