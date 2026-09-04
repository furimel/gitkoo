package com.furimeo.gitkoo.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One topic chip on a repository. */
@Table("repository_topics")
public class RepositoryTopic {

    @Id
    private Long id;

    private Long repositoryId;
    private String topic;

    public RepositoryTopic() {
    }

    public RepositoryTopic(Long repositoryId, String topic) {
        this.repositoryId = repositoryId;
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
