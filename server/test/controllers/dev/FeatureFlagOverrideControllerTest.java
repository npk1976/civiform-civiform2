package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class FeatureFlagOverrideControllerTest {

  private static final String FLAG_NAME = "flag";
  private Optional<Application> maybeApp = Optional.empty();
  private FeatureFlagOverrideController controller;

  @After
  public void stopApplication() {
    if (maybeApp.isPresent()) {
      Helpers.stop(maybeApp.get());
      maybeApp = Optional.empty();
    }
  }

  @Test
  public void disable_nonDevMode_fails() {
    // Setup
    setupControllerInMode(Mode.TEST);

    // Execute
    var result = controller.disable(fakeRequest().build(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void disable() {
    // Setup
    setupControllerInMode(Mode.DEV);

    // Execute
    var result = controller.disable(fakeRequest().build(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.session().get(FLAG_NAME)).hasValue("false");
  }

  @Test
  public void enable_nonDevMode_fails() {
    // Setup
    setupControllerInMode(Mode.TEST);

    // Execute
    var result = controller.enable(fakeRequest().build(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void enable() {
    // Setup
    setupControllerInMode(Mode.DEV);

    // Execute
    var result = controller.enable(fakeRequest().build(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.session().get(FLAG_NAME)).hasValue("true");
  }

  @Test
  public void index() {
    // Setup
    setupControllerInMode(Mode.DEV);

    // Execute
    var result = controller.index(fakeRequest().build());

    // Verify
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Overrides are allowed");
  }

  private void setupControllerInMode(Mode mode) {
    maybeApp = Optional.of(new GuiceApplicationBuilder().in(mode).build());
    controller = maybeApp.get().injector().instanceOf(FeatureFlagOverrideController.class);
  }

  @Test
  public void status() {
    setupControllerInMode(Mode.TEST);

    Result enabledResult =
        controller.status(fakeRequest().build(), "program_read_only_view_enabled");
    assertEquals("true", Helpers.contentAsString(enabledResult));

    Result diabledResult = controller.status(fakeRequest().build(), "intake_form_enabled");
    assertEquals("false", Helpers.contentAsString(diabledResult));

    Result noFeatureResult = controller.status(fakeRequest().build(), "no_flag_by_this_name");
    assertEquals("false", Helpers.contentAsString(noFeatureResult));
  }
}
