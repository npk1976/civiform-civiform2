package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import junitparams.JUnitParamsRunner;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.mvc.Result;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramImageControllerTest extends ResetPostgres {

  private ProgramService programService;
  private AdminProgramImageController controller;

  @Before
  public void setup() {
    programService = instanceOf(ProgramService.class);
    controller = instanceOf(AdminProgramImageController.class);
  }

  @Test
  public void index_ok_get() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result = controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Manage program image");
    assertThat(contentAsString(result)).contains("test name");
    assertThat(contentAsString(result)).contains("Image description");
  }

  @Test
  public void index_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(
            () -> controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void index_missingProgram_throws() {
    assertThatThrownBy(
            () ->
                controller.index(addCSRFToken(fakeRequest().method("GET")).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void index_programHasDescription_displayed() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "fake summary description"))
            .build();

    Result result = controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("fake summary description");
  }

  @Test
  public void updateDescription_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(
            () ->
                controller.updateDescription(
                    addCSRFToken(fakeRequest().method("POST")).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateDescription_missingProgram_throws() {
    assertThatThrownBy(
            () ->
                controller.updateDescription(
                    addCSRFToken(fakeRequest().method("POST")).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateDescription_createsNewDescription()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "fake description")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("fake description");
  }

  @Test
  public void updateDescription_editsExistingDescription()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "first description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "second description")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("second description");
  }

  @Test
  public void updateDescription_editExisting_preservesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "US description",
                    Locale.FRENCH,
                    "French description",
                    Locale.ITALIAN,
                    "Italian description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "new US description")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("new US description");
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.FRENCH))
        .isEqualTo("French description");
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.ITALIAN))
        .isEqualTo("Italian description");
  }

  @Test
  public void updateDescription_empty_removesDescription() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_blank_removesDescription() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "    ")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_empty_removesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "US description",
                    Locale.FRENCH,
                    "French description",
                    Locale.ITALIAN,
                    "Italian description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_nonEmpty_toastsSuccess() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "fake description")))
                .build(),
            program.id);

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("set to fake description");
  }

  @Test
  public void updateDescription_empty_toastsSuccess() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id);

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("removed");
  }
}
