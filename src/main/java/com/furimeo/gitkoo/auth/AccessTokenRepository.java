package com.furimeo.gitkoo.auth;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AccessToken} (see docs/).
 */
public interface AccessTokenRepository extends CrudRepository<AccessToken, Long> {

    Optional<AccessToken> findByTokenHash(String tokenHash);
}
