package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.ApiKey;
import org.apache.commons.net.util.SubnetUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.BadCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.apikey.ApiKeyService;

/**
 * Authenticator for API requests based on HTTP basic auth and backed by the {@link ApiKey}
 * resource. Background: https://en.wikipedia.org/wiki/Basic_access_authentication
 *
 * <p>When referenced by a request, {@code ApiKey}s are stored in the "api-keys" named cache, keyed
 * by their key ID. This allows controller code to load the key without the need for a subsequent
 * database query. This is necessary because pac4j creates profiles (which are what's made available
 * to controller code through the request object) in the next step, but we need the API key to
 * perform authentication (this step). The purpose of the cache is to avoid making multiple database
 * calls to retrieve the API key throughout the cycle of the request.
 *
 * <p>Note that at this layer, the request is authenticated, not authorized. All API requests that
 * reach a controller are already authenticated, but it is the controller's responsibility to check
 * that the resource being accessed is authorized for the request's API key.
 *
 * <p>The API key ID and secret are provided as the basic auth username and password, respectively.
 * To be valid, a request must reference a valid API key with its username credential, and further
 * that key must:
 *
 * <ul>
 *   <li>Not be retired.
 *   <li>Have an expiration date in the future.
 *   <li>Have a subnet that includes the IP address of the request.
 *   <li>Have a salted key secret that matches the salted password in the request's basic auth
 *       credentials.
 * </ul>
 */
public class ApiAuthenticator implements Authenticator {

  private static final Logger logger = LoggerFactory.getLogger(ApiAuthenticator.class);
  private final Provider<ApiKeyService> apiKeyService;

  @Inject
  public ApiAuthenticator(Provider<ApiKeyService> apiKeyService) {
    this.apiKeyService = checkNotNull(apiKeyService);
  }

  /**
   * Authenticates that an API request has a valid API key and is from a permitted IP address.
   * Throws a {@link BadCredentialsException} if not, causing a status-only HTTP response 401. The
   * exception messages are included in the server logs to aid in debugging and monitoring for
   * malicious use.
   */
  @Override
  public void validate(Credentials rawCredentials, WebContext context, SessionStore sessionStore) {
    if (!(rawCredentials instanceof UsernamePasswordCredentials)) {
      throw new RuntimeException("ApiAuthenticator must receive UsernamePasswordCredentials.");
    }

    // The terms "username" and "password" here may look a bit odd since API requests are not
    // associated with user accounts but rather API keys. They're used here because pac4j's
    // built-in support for basic auth uses those terms to identify the components of the
    // basic auth credentials. In this sense, the API key ID is the "username" and the secret
    // is the "password". An API key itself can be thought of as the "user account".
    UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) rawCredentials;
    String keyId = credentials.getUsername();

    // Cache the API key for quick lookup in the controller, also for subsequent requests.
    // We intentionally cache the empty optional rather than throwing here so that subsequent
    // requests with the invalid key do not put pressure on the database.
    Optional<ApiKey> maybeApiKey = apiKeyService.get().findByKeyIdWithCache(keyId);

    if (maybeApiKey.isEmpty()) {
      throwUnauthorized(context, "API key does not exist: " + keyId);
    }

    ApiKey apiKey = maybeApiKey.get();

    if (apiKey.isRetired()) {
      throwUnauthorized(context, "API key is retired: " + keyId);
    }

    if (apiKey.expiredAfter(Instant.now())) {
      throwUnauthorized(context, "API key is expired: " + keyId);
    }

    if (!isAllowedIp(apiKey, context)) {
      throwUnauthorized(
          context,
          String.format(
              "IP %s not in allowed range for key ID: %s", context.getRemoteAddr(), keyId));
    }

    String saltedCredentialsSecret = apiKeyService.get().salt(credentials.getPassword());
    if (!saltedCredentialsSecret.equals(apiKey.getSaltedKeySecret())) {
      throwUnauthorized(context, "Invalid secret for key ID: " + keyId);
    }
  }

  private boolean isAllowedIp(ApiKey apiKey, WebContext context) {
    return apiKey.getSubnetSet().stream()
        .map(SubnetUtils::new)
        // Setting this to true includes the network and broadcast addresses.
        // I.e. /31 and /32 will not be considered included in the subnetwork
        // if this is false.
        .peek(allowedSubnet -> allowedSubnet.setInclusiveHostCount(true))
        .anyMatch(allowedSubnet -> allowedSubnet.getInfo().isInRange(context.getRemoteAddr()));
  }

  private void throwUnauthorized(WebContext context, String cause) {
    logger.warn(
        String.format(
            "UnauthorizedApiRequest(resource: \"%s\", ip: \"%s\", cause: \"%s\")",
            context.getPath(), context.getRemoteAddr(), cause));

    throw new BadCredentialsException(cause);
  }
}
