package org.temporedata.modules.ops.git.provider;

/**
 * Ops provider SPI (P2-10). Implemented for GitHub / GitLab; adapters degrade
 * gracefully when no access token is configured.
 */
public interface OpsProvider {

    String providerId();

    String providerName();

    String authorizeUrl(String callbackUrl);

    /**
     * Verify connectivity and that the given repo exists.
     */
    boolean testConnection(String repoRef, String branch, String token);

    String repoUrl(String repoRef);

    /**
     * Read a raw file from the repo at the given branch.
     */
    String getFile(String repoRef, String branch, String path, String token);

    /**
     * Register a push webhook; returns the webhook id.
     */
    String createWebhook(String repoRef, String token, String webhookUrl, String secret);

    void deleteWebhook(String repoRef, String token, String webhookId);

    /**
     * Short human hint of the latest commit on a branch (or empty).
     */
    String latestCommitHint(String repoRef, String branch, String token);
}