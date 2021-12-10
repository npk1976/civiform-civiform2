package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.typesafe.config.Config;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mockito.Mockito;
import play.Environment;
import services.cloud.StorageClient;

/**
 * SimpleStorage provides methods to create federated links for users of CiviForm to upload and
 * download files directly to and from AWS Simple Storage Service (S3).
 */
@Singleton
public class BlobStorage implements StorageClient {

  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final String AZURE_CONTAINER_CONF_PATH = "azure.blob.container";
  public static final Duration AZURE_SAS_TOKEN_DURATION = Duration.ofMinutes(10);
  public static final Duration AZURE_USER_DELEGATION_KEY_DURATION = Duration.ofMinutes(60);

  private final Credentials credentials;
  private final String container;
  private final Client client;
  private final String accountName;
  private final String blobEndpoint;


  @Inject
  public BlobStorage(
      Credentials credentials,
      Config config,
      Environment environment) {

    this.credentials = checkNotNull(credentials);
    this.container = checkNotNull(config).getString(AZURE_CONTAINER_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);
    this.blobEndpoint = String.format("https://%s.blob.core.windows.net", accountName);

    // TODO: Set to NullClient for test environment
    if (environment.isDev()) {
      client = new AzuriteClient(config);
    } else if (environment.isDev()) {
      client = new NullClient();
    } else {
      client = new AzureBlobClient();
    }
  }

  @Override
  public URL getPresignedUrl(String fileName) {
    String signedUrl = client.getSasUrl(fileName);
    try {
      return new URL(signedUrl);
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BlobStorageUploadRequest getSignedUploadRequest(String fileName,
      String successRedirectActionLink) {
    BlobStorageUploadRequest.Builder builder = BlobStorageUploadRequest.builder()
        .setFileName(fileName)
        .setAccountName(accountName)
        .setContainerName(container)
        .setSasUrl(client.getSasUrl(fileName))
        .setSuccesstActionRedirect(successRedirectActionLink);
    return builder.build();
  }

  interface Client {

    String getSasUrl(String fileName);

  }

  // TODO: Create a Null client for mocking
  static class NullClient implements Client {

    BlobServiceClient blobServiceClient;
    BlobContainerClient blobContainerClient;
    BlobClient blobClient;

    // Test UserDelegationKey that is currently valid.
    UserDelegationKey testUserDelegationKey =
        new UserDelegationKey()
            .setValue("testUserDelegationKey")
            .setSignedObjectId("objectId")
            .setSignedTenantId("signedTenantVersion")
            .setSignedStart(OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).minusDays(1))
            .setSignedExpiry(OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).plusYears(1));

    NullClient() {
      this.blobServiceClient = Mockito.mock(BlobServiceClient.class);
      this.blobContainerClient = Mockito.mock(BlobContainerClient.class);
      this.blobClient = Mockito.mock(BlobClient.class);

      when(blobServiceClient.getUserDelegationKey(any(),
          any())).thenReturn(testUserDelegationKey);
      when(blobClient.getBlobUrl()).thenReturn("www.bloblurl.com");
      when(blobClient.generateSas(any())).thenReturn("sas");
    }

    // If we want to test with expired or not yet ready UserDelegationKeys to make sure it works
    // as intended.
    public void setTestUserDelegationKey(UserDelegationKey newUserDelegationKey) {
      testUserDelegationKey = newUserDelegationKey;
    }

    public String getSasQueryParameters(String fileName) {
      return String.format("%s?%s", fileName, "sas");
    }

    public BlobServiceClient getBlobServiceClient() {
      return blobServiceClient;
    }

    @Override
    public String getSasUrl(String fileName) {
      return null;
    }
  }

  class AzureBlobClient implements Client {

    private final BlobServiceClient blobServiceClient;
    private UserDelegationKey userDelegationKey;

    AzureBlobClient() {
      blobServiceClient = new BlobServiceClientBuilder()
          .endpoint(blobEndpoint)
          .credential(credentials.getCredentials())
          .buildClient();
      userDelegationKey = getUserDelegationKey();
    }

    private UserDelegationKey getUserDelegationKey() {
      OffsetDateTime tokenExpiration = OffsetDateTime.now().plus(AZURE_SAS_TOKEN_DURATION);
      OffsetDateTime keyExpiration = userDelegationKey.getSignedExpiry();
      if (userDelegationKey == null || keyExpiration.isBefore(tokenExpiration)) {
        userDelegationKey = blobServiceClient.getUserDelegationKey(
            OffsetDateTime.now().minus(Duration.ofMinutes(5)),
            OffsetDateTime.now().plus(AZURE_USER_DELEGATION_KEY_DURATION));
      }
      return userDelegationKey;
    }
    // userDelegationKey = blobServiceClient.getUserDelegationKey(
    //     OffsetDateTime.now(ZoneId.of("America/Los_Angeles")), OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).plus(AZURE_USER_DELEGATION_KEY_DURATION));

    // private UserDelegationKey getUserDelegationKey() {
    //   if (userDelegationKey.getSignedExpiry().isBefore(OffsetDateTime.now(ZoneId.of("America/Los_Angeles")))) {
    //     userDelegationKey = blobServiceClient.getUserDelegationKey(
    //         OffsetDateTime.now(ZoneId.of("America/Los_Angeles")),
    //         OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).plus(AZURE_USER_DELEGATION_KEY_DURATION));
    //   }
    //   return userDelegationKey;
    // }

    @Override
    public String getSasUrl(String filePath) {
      BlobClient blobClient = blobServiceClient
          .getBlobContainerClient(container)
          .getBlobClient(filePath);

      BlobSasPermission blobSasPermission = new BlobSasPermission()
          .setReadPermission(true)
          .setWritePermission(true);

      BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(
          OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).plus(AZURE_SAS_TOKEN_DURATION),
          blobSasPermission)
          .setProtocol(SasProtocol.HTTPS_ONLY);

      String sas = blobClient.generateUserDelegationSas(signatureValues, getUserDelegationKey());
      return String.format("%s?%s", blobClient.getBlobUrl(), sas);
    }

  }

  class AzuriteClient implements Client {
    // Using Account SAS because Azurite emulator does not support User Delegation SAS yet
    // See https://github.com/Azure/Azurite/issues/656

    private static final String AZURE_LOCAL_CONNECTION_STRING_CONF_PATH = "azure.local.connection";

    private final String connectionString;
    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;

    AzuriteClient(Config config) {
      this.connectionString = checkNotNull(config)
          .getString(AZURE_LOCAL_CONNECTION_STRING_CONF_PATH);
      this.blobServiceClient = new BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient();
      this.blobContainerClient = blobServiceClient.getBlobContainerClient(container);
      if (!blobContainerClient.exists()) {
        blobContainerClient.create();
      }

    }

    @Override
    public String getSasUrl(String filePath) {

      BlobClient blobClient = blobContainerClient.getBlobClient(filePath);

      BlobSasPermission blobSasPermission = new BlobSasPermission()
          .setReadPermission(true)
          .setWritePermission(true)
          .setListPermission(true);

      BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(
          OffsetDateTime.now(ZoneId.of("America/Los_Angeles")).plus(AZURE_SAS_TOKEN_DURATION),
          blobSasPermission)
          .setProtocol(SasProtocol.HTTPS_ONLY);

      String sas = blobClient.generateSas(signatureValues);
      return String.format("%s?%s", blobClient.getBlobUrl(), sas);
    }


  }
}
