package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import java.util.Locale;
import models.Applicant;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;
import support.TestQuestionBank;

public class ApplicantProgramsControllerTest extends WithMockedApplicantProfiles {

  private Applicant currentApplicant;
  private ApplicantProgramsController controller;

  @Before
  public void setUp() {
    resourceCreator().clearDatabase();
    controller = instanceOf(ApplicantProgramsController.class);
    currentApplicant = createApplicantWithMockedProfile();
  }

  @Test
  public void index_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .index(fakeRequest().build(), currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void index_withNoPrograms_returnsEmptyResult() {
    Result result =
        controller.index(fakeRequest().build(), currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).doesNotContain("program-card");
  }

  @Test
  public void index_withPrograms_returnsOnlyRelevantPrograms() {
    resourceCreator().insertProgram("one", LifecycleStage.ACTIVE);
    resourceCreator().insertProgram("two", LifecycleStage.ACTIVE);
    resourceCreator().insertProgram("three", LifecycleStage.DRAFT);

    Result result =
        controller.index(fakeRequest().build(), currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
    assertThat(contentAsString(result)).doesNotContain("three");
  }

  @Test
  public void index_withProgram_includesApplyButtonWithRedirect() {
    Program program = resourceCreator().insertProgram("program", LifecycleStage.ACTIVE);

    Result result =
        controller.index(fakeRequest().build(), currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.edit(currentApplicant.id, program.id).url());
  }

  @Test
  public void index_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        fakeRequest().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("obtener beneficios");
  }

  @Test
  public void index_missingTranslationForProgram_defaultsToEnglish() {
    resourceCreator()
        .insertProgram(
            Locale.forLanguageTag("es-US"), "A different language!", LifecycleStage.ACTIVE);
    resourceCreator()
        .insertProgram("English program", LifecycleStage.ACTIVE); // Missing translation

    // Set the PLAY_LANG cookie
    Http.Request request =
        fakeRequest().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A different language!");
    assertThat(contentAsString(result)).contains("English program");
  }

  @Test
  public void edit_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id + 1, 1L)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_withNewProgram_redirectsToFirstBlock() {
    Program program =
        ProgramBuilder.newProgram()
            .withBlock()
            .withQuestion(TestQuestionBank.applicantName())
            .build();

    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "1")
                .url());
  }

  @Test
  public void edit_redirectsToFirstIncompleteBlock() {
    QuestionDefinition colorQuestion =
        TestQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    Program program =
        ProgramBuilder.newProgram()
            .withBlock()
            .withQuestionDefinition(colorQuestion)
            .withBlock()
            .withQuestion(TestQuestionBank.applicantAddress())
            .build();
    // Answer the color question
    currentApplicant
        .getApplicantData()
        .putString(colorQuestion.getPath().join("text"), "forest green");
    currentApplicant.getApplicantData().putLong(colorQuestion.getLastUpdatedTimePath(), 12345L);
    currentApplicant.getApplicantData().putLong(colorQuestion.getProgramIdPath(), 456L);
    currentApplicant.save();

    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "2")
                .url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void edit_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    Program program = resourceCreator().insertProgram("My Program");

    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index(currentApplicant.id).url());
  }
}
