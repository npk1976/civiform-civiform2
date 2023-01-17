package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.components.ToastMessage.ToastType.ALERT;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramSummaryView;
import views.components.ToastMessage;

/**
 * Controller for reviewing program responses for an applicant.
 *
 * <p>CAUTION: You must explicitly check the current profile so that an unauthorized user cannot
 * access another applicant's data!
 */
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.profileUtils = checkNotNull(profileUtils);
  }

  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    Optional<ToastMessage> banner =
        request.flash().get("banner").map(m -> new ToastMessage(m, ALERT));
    CompletionStage<Optional<String>> applicantStage = applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              ApplicantProgramSummaryView.Params params =
                  this.generateParamsBuilder(roApplicantProgramService)
                      .setApplicantId(applicantId)
                      .setApplicantName(applicantStage.toCompletableFuture().join())
                      .setBannerMessage(banner)
                      .setMessages(messagesApi.preferred(request))
                      .setProgramId(programId)
                      .setRequest(request)
                      .build();
              return ok(summaryView.render(params));
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> submit(Request request, long applicantId, long programId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenComposeAsync(
            v -> submitInternal(request, applicantId, programId), httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ApplicationOutOfDateException) {

                  String errorMsg = messagesApi.preferred(request).at(MessageKey.TOAST_APPLICATION_OUT_OF_DATE.getKeyName());
                  Call reviewPage =
                      routes.ApplicantProgramReviewController.review(applicantId, programId);
                  return redirect(reviewPage).flashing("error", errorMsg);
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }


  private ApplicantProgramSummaryView.Params.Builder generateParamsBuilder(
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryData();
    int totalBlockCount = roApplicantProgramService.getAllActiveBlocks().size();
    int completedBlockCount = roApplicantProgramService.getActiveAndCompletedInProgramBlockCount();
    String programTitle = roApplicantProgramService.getProgramTitle();

    return ApplicantProgramSummaryView.Params.builder()
        .setCompletedBlockCount(completedBlockCount)
        .setProgramTitle(programTitle)
        .setSummaryData(summaryData)
        .setTotalBlockCount(totalBlockCount);
  }

  private CompletionStage<Result> submitInternal(
      Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();

    CompletionStage<Application> submitApp =
        applicantService.submitApplication(applicantId, programId, submittingProfile);
    return submitApp
        .thenApplyAsync(
            application -> {
              Long applicationId = application.id;
              Call endOfProgramSubmission =
                  routes.RedirectController.considerRegister(
                      applicantId,
                      programId,
                      applicationId,
                      routes.ApplicantProgramsController.index(applicantId).url());
              return found(endOfProgramSubmission);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ApplicationSubmissionException) {
                  Call reviewPage =
                      routes.ApplicantProgramReviewController.review(applicantId, programId);
                  return found(reviewPage).flashing("banner", "Error saving application.");
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
