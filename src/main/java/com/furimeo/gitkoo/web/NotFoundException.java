package com.furimeo.gitkoo.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A requested user, repository, issue or pull request does not exist.
 *
 * <p>The controllers used to signal this with {@link IllegalArgumentException},
 * which Spring maps to 500. That was survivable while every repository URL required
 * a session, because stray requests were bounced to the login page before reaching a
 * controller. Once public repositories became browsable anonymously, every two
 * segment URL that is not a repository - a mistyped path, a bot probe, a favicon
 * request under the wrong prefix - reached the handler and produced a 500 with a
 * full stack trace in the log.
 *
 * <p>A missing thing is a 404, not a server error.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
