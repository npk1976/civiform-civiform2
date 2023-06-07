package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import services.applicant.ApplicantPersonalInfo.LoggedInRepresentation;

public class ApplicantPersonalInfoTest {

  private Messages messages;

  @Before
  public void setUp() {
    messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  }

  @Test
  public void getDisplayString_forGuest() {
    ApplicantPersonalInfo personalInfo = ApplicantPersonalInfo.ofGuestUser();

    assertThat(personalInfo.getDisplayString(messages)).isEqualTo("guest");
  }

  @Test
  public void getDisplayString_forLoggedInUser_emailOnly() {
    ApplicantPersonalInfo personalInfo =
        ApplicantPersonalInfo.ofLoggedInUser(
            LoggedInRepresentation.builder().setEmail("user@example.com").build());

    assertThat(personalInfo.getDisplayString(messages)).isEqualTo("user@example.com");
  }

  @Test
  public void getDisplayString_forLoggedInUser_nameOnly() {
    ApplicantPersonalInfo personalInfo =
        ApplicantPersonalInfo.ofLoggedInUser(
            LoggedInRepresentation.builder().setName("John Doe").build());

    assertThat(personalInfo.getDisplayString(messages)).isEqualTo("John Doe");
  }

  @Test
  public void getDisplayString_forLoggedInUser_nameAndEmailPrefersName() {
    ApplicantPersonalInfo personalInfo =
        ApplicantPersonalInfo.ofLoggedInUser(
            LoggedInRepresentation.builder()
                .setEmail("user@example.com")
                .setName("John Doe")
                .build());

    assertThat(personalInfo.getDisplayString(messages)).isEqualTo("John Doe");
  }
}
