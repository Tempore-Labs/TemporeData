package org.temporedata.api.base.constants;

/**
 * Shared HTTP / security constants.
 */
public final class HttpConstants {

    public static final String AUTH_HEADER = "Authorization";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String LOGIN_PATH = "/api/auth/login";

    private HttpConstants() {
    }
}