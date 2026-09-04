package com.furimeo.gitkoo.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/** Topic chips on a repository. */
public interface TopicRepository extends CrudRepository<RepositoryTopic, Long> {

    List<RepositoryTopic> findByRepositoryIdOrderByTopic(Long repositoryId);

    @Modifying
    @Query("DELETE FROM repository_topics WHERE repository_id = :repositoryId")
    void deleteAllFor(@Param("repositoryId") Long repositoryId);
}
