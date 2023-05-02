package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.INTAKE_FORM_ENABLED;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.typesafe.sslconfig.ssl.FakeChainedKeyStore;
import featureflags.FeatureFlags;
import forms.ProgramForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LabelTag;
import models.DisplayMode;
import models.TrustedIntermediaryGroup;
import modules.MainModule;
import play.mvc.Http.Request;
import repository.UserRepository;
import services.LocalizedStrings;
import services.Path;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.LocalizedQuestionOption;
import views.BaseHtmlView;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramFormBuilder extends BaseHtmlView {

  private final FeatureFlags featureFlags;
  private final String baseUrl;
  private final UserRepository userRepository;

  @Inject
  ProgramFormBuilder(Config configuration, FeatureFlags featureFlags, UserRepository userRepository) {
    this.featureFlags = featureFlags;
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.userRepository = checkNotNull(userRepository);
  }

  /** Builds the form using program form data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramForm program, boolean editExistingProgram) {
    return buildProgramForm(
        request,
        program.getAdminName(),
        program.getAdminDescription(),
        program.getLocalizedDisplayName(),
        program.getLocalizedDisplayDescription(),
        program.getExternalLink(),
        program.getLocalizedConfirmationMessage(),
        program.getDisplayMode(),
        program.getIsCommonIntakeForm(),
        editExistingProgram);
  }

  /** Builds the form using program definition data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramDefinition program, boolean editExistingProgram) {
    return buildProgramForm(
        request,
        program.adminName(),
        program.adminDescription(),
        program.localizedName().getDefault(),
        program.localizedDescription().getDefault(),
        program.externalLink(),
        program.localizedConfirmationMessage().getDefault(),
        program.displayMode().getValue(),
        program.programType().equals(ProgramType.COMMON_INTAKE_FORM),
        editExistingProgram);
  }

  private FormTag buildProgramForm(
      Request request,
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String confirmationSceen,
      String displayMode,
      Boolean isCommonIntakeForm,
      boolean editExistingProgram) {
    FormTag formTag = form().withMethod("POST").withId("program-details-form");
    formTag.with(
        requiredFieldsExplanationContent(),
        h2("Visible to applicants").withClasses("py-2", "mt-6", "font-semibold"),
        FieldWithLabel.input()
            .setId("program-display-name-input")
            .setFieldName("localizedDisplayName")
            .setLabelText("Enter the publicly displayed name for this program")
            .setRequired(true)
            .setValue(displayName)
            .getInputTag(),
        FieldWithLabel.textArea()
            .setId("program-display-description-textarea")
            .setFieldName("localizedDisplayDescription")
            .setLabelText("Describe this program for the public")
            .setRequired(true)
            .setValue(displayDescription)
            .getTextareaTag(),
        programUrlField(adminName, editExistingProgram),
        FieldWithLabel.input()
            .setId("program-external-link-input")
            .setFieldName("externalLink")
            .setLabelText("Link to program website")
            .setValue(externalLink)
            .getInputTag(),
        FieldWithLabel.textArea()
            .setId("program-confirmation-message-textarea")
            .setFieldName("localizedConfirmationMessage")
            .setLabelText(
                "A custom message that will be shown on the confirmation page after an application"
                    + " has been submitted. You can use this message to explain next steps of the"
                    + " application process and/or highlight other programs to apply for.")
            .setValue(confirmationSceen)
            .getTextareaTag(),
        h2("Visible to administrators only").withClasses("py-2", "mt-6", "font-semibold"),
        // TODO(#2618): Consider using helpers for grouping related radio controls.
        fieldset()
            .with(
                legend("Program visibility")
                    .withClass(BaseStyles.INPUT_LABEL)
                    .with(ViewUtils.requiredQuestionIndicator()),
                FieldWithLabel.radio()
                    .setId("program-display-mode-public")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Publicly visible")
                    .setValue(DisplayMode.PUBLIC.getValue())
                    .setChecked(displayMode.equals(DisplayMode.PUBLIC.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-hidden")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText(
                        "Hide from applicants. Only individuals with the unique program link can"
                            + " access this program")
                    .setValue(DisplayMode.HIDDEN_IN_INDEX.getValue())
                    .setChecked(displayMode.equals(DisplayMode.HIDDEN_IN_INDEX.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-ti-only")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Trusted Intermediaries ONLY")
                    .setValue(DisplayMode.TI_ONLY.getValue())
                    .setChecked(displayMode.equals(DisplayMode.TI_ONLY.getValue()))
                    .getRadioTag()),
      showTISelectionList(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program note for administrative use only")
            .setRequired(true)
            .setValue(adminDescription)
            .getTextareaTag());
    if (featureFlags.getFlagEnabled(request, INTAKE_FORM_ENABLED)) {
      formTag
          .with(
              FieldWithLabel.checkbox()
                  .setId("common-intake-checkbox")
                  .setFieldName("isCommonIntakeForm")
                  .setLabelText("Set program as pre-screener")
                  .addStyleClass("border-none")
                  .setValue("true")
                  .setChecked(isCommonIntakeForm)
                  .getCheckboxTag()
                  .with(
                      span(ViewUtils.makeSvgToolTip(
                              "You can set one program as the ‘pre-screener’. This will pin the"
                                  + " program card to the top of the programs and services page"
                                  + " while moving other program cards below it.",
                              Icons.INFO))
                          .withClass("ml-2")))
          .with(
              // Hidden checkbox used to signal whether or not the user has confirmed they want to
              // change which program is marked as the common intake form.
              FieldWithLabel.checkbox()
                  .setId("confirmed-change-common-intake-checkbox")
                  .setFieldName("confirmedChangeCommonIntakeForm")
                  .setValue("false")
                  .setChecked(false)
                  .addStyleClass("hidden")
                  .getCheckboxTag());
    }
    formTag.with(
        submitButton("Save")
            .withId("program-update-button")
            .withClasses(ButtonStyles.SOLID_BLUE, "mt-6"));

    return formTag;
  }

  private DomContent showTISelectionList() {
    List<TrustedIntermediaryGroup> tiGroups = userRepository.listTrustedIntermediaryGroups();
    DivTag tiSelectionRenderer =
      div()
        // Hidden input that's always selected to allow for clearing multi-select data.
        .with(
          input()
            .withType("checkbox")
            /**
             * For multi-select, we must append {@code []} to the field name so that the Play
             * framework allows multiple form keys with the same value. For more information, see
             * https://www.playframework.com/documentation/2.8.x/JavaFormHelpers#Handling-repeated-values
             */
            .withName("Select Trusted Intermediaries"+ Path.ARRAY_SUFFIX)
            .withValue("")
            .withCondChecked(true)
            .withClasses(ReferenceClasses.RADIO_DEFAULT, "hidden"))
        .with(
          tiGroups.stream()
            .map(
              option ->
                renderCheckboxOption(
                  option.getName(),
                  option.id)));

    return tiSelectionRenderer;
  }
  private DivTag renderCheckboxOption(
    String tiName, Long tiId) {
    String id = "checkbox-" + "-" + option.id();
    LabelTag labelTag =
      label()
        .withClasses(
          ReferenceClasses.RADIO_OPTION,
          BaseStyles.CHECKBOX_LABEL,
          true ? BaseStyles.BORDER_SEATTLE_BLUE : "")
        .with(
          input()
            .withId(id)
            .withType("checkbox")
            .withName(tiName)
            .withValue(String.valueOf(tiId))
           // .withCondChecked(isSelected)
          //  .condAttr("aria-invalid", "true")
           // .condAttr( "aria-required", "true")
            .withClasses(
              StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.CHECKBOX)));
          //span(option.optionText()).withClasses(ReferenceClasses.MULTI_OPTION_VALUE));

    return div()
      .withClasses(ReferenceClasses.MULTI_OPTION_QUESTION_OPTION, "my-2", "relative")
      .with(labelTag);
  }

  private DomContent programUrlField(String adminName, boolean editExistingProgram) {
    if (editExistingProgram) {
      String programUrl =
          baseUrl
              + controllers.applicant.routes.RedirectController.programBySlug(
                      MainModule.SLUGIFIER.slugify(adminName))
                  .url();
      return div()
          .withClass("mb-2")
          .with(
              p("The URL for this program. This value can't be changed")
                  .withClasses(BaseStyles.INPUT_LABEL),
              p(programUrl).withClasses(BaseStyles.FORM_FIELD));
    }
    return FieldWithLabel.input()
        .setId("program-name-input")
        .setFieldName("adminName")
        .setLabelText(
            "Enter an identifier that will be used in this program's applicant-facing URL. This"
                + " value can't be changed later. Aim to keep it short so it's easy to share. Use"
                + " a dash between each word")
        .setRequired(true)
        .setValue(adminName)
        .getInputTag();
  }

  protected Modal buildConfirmCommonIntakeChangeModal(String existingCommonIntakeFormDisplayName) {
    DivTag content =
        div()
            .withClasses("flex-row", "space-y-6")
            .with(
                p("The pre-screener will be updated from ")
                    .with(span(existingCommonIntakeFormDisplayName).withClass("font-bold"))
                    .withText(" to the current program."))
            .with(p("Would you like to confirm the change?"))
            .with(
                div()
                    .withClasses("flex")
                    .with(div().withClasses("flex-grow"))
                    .with(
                        submitButton("Confirm")
                            .withForm("program-details-form")
                            .withId("confirm-common-intake-change-button")
                            .withClasses(ButtonStyles.SOLID_BLUE, "cursor-pointer")));
    return Modal.builder()
        .setModalId("confirm-common-intake-change")
        .setContent(content)
        .setModalTitle("Confirm pre-screener change?")
        .setDisplayOnLoad(true)
        .setWidth(Width.THIRD)
        .build();
  }
}
