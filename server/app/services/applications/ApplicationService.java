package services.applications;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import models.Application;
import repository.ApplicationRepository;
import services.DeploymentType;
import services.program.ProgramDefinition;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/** The service responsible for mediating a program admin's access to the Application resource. */
public class ApplicationService {

  private final ApplicationRepository applicationRepository;

  @Inject
  ApplicationService(
      ApplicationRepository applicationRepository,
      Config configuration,
      DeploymentType deploymentType) {
    this.applicationRepository = checkNotNull(applicationRepository);

    checkNotNull(configuration);
    checkNotNull(deploymentType);
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<Application> getApplication(long applicationId, ProgramDefinition program) {
      Optional<Application> maybeApplication = applicationRepository.getApplication(applicationId).toCompletableFuture().join();
      if (maybeApplication.isEmpty()) {
        return Optional.empty();
      }
      Application application = maybeApplication.get();
      if (program.adminName().isEmpty()
        || !application
        .getProgram()
        .getProgramDefinition()
        .adminName()
        .equals(program.adminName())) {
        return Optional.empty();
      }
      return Optional.of(application);
  }
}
