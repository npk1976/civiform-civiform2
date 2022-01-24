package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import controllers.dev.AwsLocalstackFileStorageStrategy;
import controllers.dev.AzuriteFileStorageStrategy;
import controllers.dev.CloudEmulatorFileStorageStrategy;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.StorageServiceName;
import views.AwsFileUploadStrategy;
import views.FileUploadStrategy;
import views.dev.AwsStorageDevViewStrategy;
import views.dev.AzureStorageDevViewStrategy;
import views.dev.CloudStorageDevViewStrategy;

/** Configures and initializes the classes for interacting with file storage backends. */
public class CloudStorageModule extends AbstractModule {

  private static final String AZURE_STORAGE_CLASS_NAME = "services.cloud.azure.BlobStorage";
  private static final String AWS_STORAGE_CLASS_NAME = "services.cloud.aws.SimpleStorage";

  private final Environment environment;
  private final Config config;

  public CloudStorageModule(Environment environment, Config config) {
    this.environment = checkNotNull(environment);
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    String className = AWS_STORAGE_CLASS_NAME;

    try {
      String storageProvider = checkNotNull(config).getString("cloud.storage");
      className = getStorageProviderClassName(storageProvider);
      bindCloudStorageStrategy(storageProvider);
    } catch (ConfigException ex) {
      // Ignore missing config and default to S3 for now
    }

    try {
      Class<? extends StorageClient> boundClass =
          environment.classLoader().loadClass(className).asSubclass(StorageClient.class);
      bind(StorageClient.class).to(boundClass);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(
          String.format("Failed to load storage client class: %s", className));
    }
  }

  private void bindCloudStorageStrategy(String storageProvider) {
    // Defaulting to this for now because prod file upload to Azure blob storage hasn't been
    // implemented yet.
    bind(FileUploadStrategy.class).to(AwsFileUploadStrategy.class);
    StorageServiceName storageServiceName = StorageServiceName.forString(storageProvider).get();

    switch (storageServiceName) {
      case AZURE_BLOB:
        bind(CloudEmulatorFileStorageStrategy.class).to(AzuriteFileStorageStrategy.class);
        bind(CloudStorageDevViewStrategy.class).to(AzureStorageDevViewStrategy.class);
        return;
      case AWS_S3:
      default:
        bind(CloudEmulatorFileStorageStrategy.class).to(AwsLocalstackFileStorageStrategy.class);
        bind(CloudStorageDevViewStrategy.class).to(AwsStorageDevViewStrategy.class);
    }
  }

  private String getStorageProviderClassName(String storageProvider) {
    StorageServiceName storageServiceName = StorageServiceName.forString(storageProvider).get();

    switch (storageServiceName) {
      case AZURE_BLOB:
        return AZURE_STORAGE_CLASS_NAME;
      case AWS_S3:
      default:
        return AWS_STORAGE_CLASS_NAME;
    }
  }
}
