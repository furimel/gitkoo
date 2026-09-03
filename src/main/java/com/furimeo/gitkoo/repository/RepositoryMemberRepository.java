package com.furimeo.gitkoo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RepositoryMemberRepository extends CrudRepository<RepositoryMember, Long> {
    List<RepositoryMember> findByRepositoryId(Long repositoryId);
    Optional<RepositoryMember> findByRepositoryIdAndUserId(Long repositoryId, Long userId);
}
