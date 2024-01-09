package services.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.test.Helpers.fakeRequest;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.EditTiClientInfoForm;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import repository.AccountRepository;
import repository.SearchParameters;
import services.applicant.ApplicantData;
import services.applicant.exception.ApplicantNotFoundException;

public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  private AccountRepository repo;

  private TrustedIntermediaryService service;
  private FormFactory formFactory;
  private ProfileFactory profileFactory;
  private TrustedIntermediaryGroupModel tiGroup;
  private TrustedIntermediaryGroupModel tiGroup2;
  AccountModel testAccount;
  ApplicantModel testApplicant;

  @Before
  public void setup() {
    repo = instanceOf(AccountRepository.class);
    service = instanceOf(TrustedIntermediaryService.class);
    formFactory = instanceOf(FormFactory.class);
    profileFactory = instanceOf(ProfileFactory.class);
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    ApplicantModel managedApplicant2 = createApplicant();
    createTIWithMockedProfile(managedApplicant2);
    profileFactory.createFakeTrustedIntermediary();
    tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    tiGroup2 = repo.listTrustedIntermediaryGroups().get(1);
    testAccount = setupTiClientAccount("email2123", tiGroup);
    testApplicant = setTiClientApplicant(testAccount, "clientFirst", "2021-12-12");
  }

  @After
  public void teardown() {
    // Clean up accounts between tests
    tiGroup.getManagedAccounts().stream()
        .forEach(
            acct -> {
              acct.getApplicants().stream().forEach(app -> app.delete());
              acct.delete();
            });
    tiGroup2.getManagedAccounts().stream()
        .forEach(
            acct -> {
              acct.getApplicants().stream().forEach(app -> app.delete());
              acct.delete();
            });
    tiGroup.delete();
    tiGroup2.delete();
  }

  @Test
  public void addClient_withMissingDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message()).isEqualTo("Date of Birth required");
  }

  @Test
  public void addClient_withInvalidDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "1865-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth should be less than 150 years ago");
  }

  @Test
  public void addClient_withUnformattedDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "20-20-20")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth must be in MM/dd/yyyy format");
  }

  @Test
  public void addClient_withInvalidLastName() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "2022-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("lastName").get().message()).isEqualTo("Last name required");
  }

  @Test
  public void addClient_WithInvalidFirstName() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "2012-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("firstName").get().message()).isEqualTo("First name required");
  }

  @Test
  public void addClient_WithEmailAddressExistsError() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "First",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "sample@fake.com",
                        "dob",
                        "2012-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm1 =
        service.addNewClient(form, tiGroup);
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm2 =
        service.addNewClient(form, tiGroup);
    // The first form is successful
    assertThat(returnedForm1).isEqualTo(form);
    // The second form has the same emailAddress, so it errors
    assertThat(returnedForm2.error("emailAddress").get().message())
        .isEqualTo(
            "Email address already in use. Cannot create applicant if an account already exists.");
  }

  @Test
  public void addClient_WithEmptyEmailAddress() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "No",
                        "middleName",
                        "middle",
                        "lastName",
                        "Email",
                        "emailAddress",
                        "",
                        "dob",
                        "2011-11-11")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.errors()).isEmpty();
    AccountModel account =
        tiGroup.getManagedAccounts().stream()
            .filter(acct -> acct.getApplicantName().equals("Email, No"))
            .findFirst()
            .get();
    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2011-11-11");
    assertThat(account.getEmailAddress()).isNull();
  }

  @Test
  public void addClient_WithAllInformation() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "First",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "add1@fake.com",
                        "dob",
                        "2022-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm).isEqualTo(form);
    AccountModel account = repo.lookupAccountByEmail("add1@fake.com").get();

    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
  }

  @Test
  public void getManagedAccounts_SearchByDob() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email1", tiGroup);
    setupTiClientAccountWithApplicant("Second", "2022-07-08", "email2", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-12-12", "email3", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDateQuery(Optional.of("2022-12-12"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(1);
    assertThat(tiResult.getAccounts().get().get(0).getEmailAddress()).isEqualTo("email3");
  }

  @Test
  public void getManagedAccounts_SearchByName() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email10", tiGroup);
    setupTiClientAccountWithApplicant("Emily", "2022-07-08", "email20", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-07-10", "email30", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.of("Emily"))
            .setDateQuery(Optional.empty())
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(1);
    assertThat(tiResult.getAccounts().get().get(0).getEmailAddress()).isEqualTo("email20");
  }

  @Test
  public void getManagedAccounts_ExpectUnformattedDobException() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email11", tiGroup);
    setupTiClientAccountWithApplicant("Second", "2022-10-10", "email21", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-07-10", "email31", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDateQuery(Optional.of("22-22-22"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(tiGroup.getManagedAccounts().size());
    assertThat(tiResult.getErrorMessage().get())
        .isEqualTo("Please enter date in MM/dd/yyyy format");
  }

  @Test
  public void editTiClientInfo_AllPass_NameEmailUpdate() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("emailOld", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "emailAllPassEditClient",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, account.id);
    assertThat(returnForm).isEqualTo(form);
    AccountModel accountFinal = repo.lookupAccount(account.id).get();
    ApplicantModel applicantFinal = repo.lookupApplicantSync(applicant.id).get();

    assertThat(accountFinal.getTiNote()).isEqualTo("unitTest");
    assertThat(applicantFinal.getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
    assertThat(applicantFinal.getApplicantData().getPhoneNumber().get().toString())
        .isEqualTo("4259879090");
    assertThat(applicantFinal.getApplicantData().getApplicantName().get())
        .isEqualTo("ClientLast, clientFirst");
    assertThat(accountFinal.getEmailAddress()).isEqualTo("emailAllPassEditClient");
  }

  @Test
  public void editTiClientInfo_PhoneLengthValidationFail() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "42598790")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, testAccount.id);
    assertThat(returnForm.error("phoneNumber").get().message())
        .isEqualTo("A phone number must contain only 10 digits");
  }

  @Test
  public void editTiClientInfo_PhoneNumberNonDigitValidationFail()
      throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "42598790UI")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, testAccount.id);
    assertThat(returnForm.error("phoneNumber").get().message())
        .isEqualTo("A phone number must contain only digits");
  }

  @Test
  public void editTiClientInfo_PhoneNumberValidationFail() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "0000000000")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, testAccount.id);
    assertThat(returnForm.error("phoneNumber").get().message())
        .isEqualTo("This phone number is not valid");
  }

  @Test
  public void editTiClientInfo_DOBValidationFail() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email1123", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2040-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "42598790")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, account.id);
    assertThat(returnForm.error("dob").get().message())
        .isEqualTo("Date of Birth should be in the past");
    assertThat(applicant.getApplicantData().getDateOfBirth()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_FirstNameValidationFail() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email121", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2040-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "42598790")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, account.id);
    assertThat(returnForm.error("firstName").get().message()).isEqualTo("First name required");
    assertThat(applicant.getApplicantData().getDateOfBirth()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_LastNameValidationFail() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email121", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "",
                        "dob",
                        "2040-07-07",
                        "emailAddress",
                        "email2123",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "42598790")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<EditTiClientInfoForm> returnForm = service.updateClientInfo(form, tiGroup, account.id);
    assertThat(returnForm.error("lastName").get().message()).isEqualTo("Last name required");
    assertThat(applicant.getApplicantData().getDateOfBirth()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_throwsException() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "email21",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));
    Form<EditTiClientInfoForm> form =
        formFactory.form(EditTiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    assertThatThrownBy(() -> service.updateClientInfo(form, tiGroup, 1L))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID 1");
  }

  private void setupTiClientAccountWithApplicant(
      String firstName, String dob, String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(firstName, "", "Last");
    applicantData.setDateOfBirth(dob);
    applicant.save();
  }

  private AccountModel setupTiClientAccount(String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    return account;
  }

  private ApplicantModel setTiClientApplicant(AccountModel account, String firstName, String dob) {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(firstName, "", "Last");
    applicantData.setDateOfBirth(dob);
    applicant.save();
    account.save();
    return applicant;
  }
}
