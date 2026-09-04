package com.furimeo.gitkoo.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/** Watchers. Same shape as stars; separate because the two mean different things. */
public interface WatcherRepository extends CrudRepository<RepositoryWatcher, Long> {

    int countByRepositoryId(Long repositoryId);

    boolean existsByRepositoryIdAndUserId(Long repositoryId, Long userId);

    @Modifying
    @Query("DELETE FROM repository_watchers WHERE repository_id = :repositoryId AND user_id = :userId")
    void deleteWatch(@Param("repositoryId") Long repositoryId, @Param("userId") Long userId);

    @Query("SELECT user_id FROM repository_watchers WHERE repository_id = :repositoryId")
    List<Long> watcherIds(@Param("repositoryId") Long repositoryId);
}
