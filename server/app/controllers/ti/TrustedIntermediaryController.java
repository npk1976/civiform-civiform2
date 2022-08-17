package controllers.ti;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import services.PaginationInfo;
import services.ti.EmailAddressExistsException;
import services.ti.TIClientCreationResult;
import services.ti.TrustedIntermediaryService;
import views.applicant.TrustedIntermediaryDashboardView;

/**
 * Controller for handling methods for an trusted intermediary managing their clients and applying
 * to programs on behalf of them.
 */
public class TrustedIntermediaryController {

  private static final int PAGE_SIZE = 10;
  private final TrustedIntermediaryDashboardView tiDashboardView;
  private final ProfileUtils profileUtils;
  private final UserRepository userRepository;
  private final MessagesApi messagesApi;
  private final FormFactory formFactory;
  private final String baseUrl;
  private final TrustedIntermediaryService tiService;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      UserRepository userRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryDashboardView trustedIntermediaryDashboardView,
      Config config,
      TrustedIntermediaryService tiService) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiDashboardView = Preconditions.checkNotNull(trustedIntermediaryDashboardView);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
    this.baseUrl = Preconditions.checkNotNull(config).getString("base_url");
    this.tiService = tiService;
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result dashboard(Http.Request request, Optional<String> search, Optional<Integer> page) {
    if (page.isEmpty()) {
      return redirect(routes.TrustedIntermediaryController.dashboard(search, Optional.of(1)));
    }
    Optional<CiviFormProfile> civiformProfile = profileUtils.currentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    ImmutableList<Account> managedAccounts =
        trustedIntermediaryGroup.get().getManagedAccounts(search);
    PaginationInfo<Account> pageInfo =
        PaginationInfo.paginate(managedAccounts, PAGE_SIZE, page.get());

    return ok(
        tiDashboardView.render(
            trustedIntermediaryGroup.get(),
            civiformProfile.get().getApplicant().join().getApplicantData().getApplicantName(),
            pageInfo.getPageItems(),
            pageInfo.getPageCount(),
            pageInfo.getPage(),
            search,
            request,
            messagesApi.preferred(request)));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result addApplicant(Long id, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.currentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return unauthorized();
    }
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(request);
    TIClientCreationResult tiClientCreationResult;
    try {
      tiClientCreationResult = tiService.addNewClient(form, trustedIntermediaryGroup.get());
      if (tiClientCreationResult.isSuccessful()) {
        return redirect(
            routes.TrustedIntermediaryController.dashboard(
                /* search= */ Optional.empty(), /* page= */ Optional.empty()));
      }
    }
    // Only for EmailAddressException, the expection is thrown from the Service class and handled in
    // the controller.
    // For all other errors, the error is packaged in the TIClientResult object
    catch (EmailAddressExistsException e) {
      String trustedIntermediaryUrl = baseUrl + "/trustedIntermediaries";

      return redirectToDashboardWithError(
          "Email address already in use.  Cannot create applicant if an account already exists. "
              + " Direct applicant to sign in and go to"
              + " "
              + trustedIntermediaryUrl,
          form);
    }
    return redirectToDashboardWithError(
        getFormErrors(tiClientCreationResult.getForm().get()),
        tiClientCreationResult.getForm().get());
  }

  private String getFormErrors(Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    StringBuilder errorMessage = new StringBuilder();
    form.errors().stream()
        .forEach(
            validationError ->
                errorMessage.append(
                    System.getProperty("line.separator") + validationError.message()));
    return errorMessage.toString();
  }

  private Result redirectToDashboardWithError(
      String errorMessage, Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(Optional.empty(), Optional.of(1)).url())
        .flashing("error", errorMessage)
        .flashing("providedFirstName", form.value().get().getFirstName())
        .flashing("providedMiddleName", form.value().get().getMiddleName())
        .flashing("providedLastName", form.value().get().getLastName())
        .flashing("providedEmail", form.value().get().getEmailAddress())
        .flashing("providedDateOfBirth", form.value().get().getDob());
  }
}
