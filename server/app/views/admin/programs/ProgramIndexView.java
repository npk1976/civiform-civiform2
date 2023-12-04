package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LiTag;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ProgramCardFactory;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final TranslationLocales translationLocales;
  private final ProgramCardFactory programCardFactory;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      ProgramCardFactory programCardFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.translationLocales = checkNotNull(translationLocales);
    this.programCardFactory = checkNotNull(programCardFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(
      ActiveAndDraftPrograms programs,
      ActiveAndDraftQuestions questions,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    if (profile.isPresent()) {
      layout.setAdminType(profile.get());
    }

    String pageTitle = "Program dashboard";

    // Revisit if we introduce internationalization because the word order could change in other
    // languages.
    String pageExplanation =
        "Create, edit and publish programs in "
            + settingsManifest.getWhitelabelCivicEntityShortName(request).get();
    Optional<Modal> maybePublishModal = maybeRenderPublishAllModal(programs, questions, request);
    Modal demographicsCsvModal = renderDemographicsCsvModal();

    ImmutableList<Long> universalQuestionIds =
        questions.getActiveAndDraftQuestions().stream()
            .filter(question -> question.isUniversal())
            .map(question -> question.getId())
            .collect(ImmutableList.toImmutableList());

    ImmutableList<Modal> publishSingleProgramModals =
        buildPublishSingleProgramModals(programs.getDraftPrograms(), universalQuestionIds, request);

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(
                        h1(pageTitle),
                        div().withClass("flex-grow"),
                        demographicsCsvModal
                            .getButton()
                            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2"),
                        renderNewProgramButton(),
                        maybePublishModal.isPresent() ? maybePublishModal.get().getButton() : null),
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(h2(pageExplanation)),
                div()
                    .withClasses("mt-10", "flex")
                    .with(
                        div().withClass("flex-grow"),
                        p("Sorting by most recently updated").withClass("text-sm")),
                div()
                    .withClass("mt-6")
                    .with(
                        each(
                            programs.getProgramNames().stream()
                                .map(
                                    name ->
                                        this.buildProgramCardData(
                                            programs.getActiveProgramDefinition(name),
                                            programs.getDraftProgramDefinition(name),
                                            request,
                                            profile,
                                            publishSingleProgramModals))
                                .sorted(
                                    ProgramCardFactory
                                        .programTypeThenLastModifiedThenNameComparator())
                                .map(
                                    cardData ->
                                        programCardFactory.renderCard(request, cardData)))));

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(pageTitle)
            .addMainContent(contentDiv)
            .addModals(demographicsCsvModal);

    if (settingsManifest.getUniversalQuestions(request)) {
      publishSingleProgramModals.stream()
          .forEach(
              (modal) -> {
                htmlBundle.addModals(modal);
              });
    }

    maybePublishModal.ifPresent(htmlBundle::addModals);

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private Modal renderDemographicsCsvModal() {
    String modalId = "download-demographics-csv-modal";
    String downloadActionText = "Download Demographic Data (CSV)";
    DivTag downloadDemographicCsvModalContent =
        div()
            .withClasses("px-8")
            .with(
                form()
                    .withMethod("GET")
                    .withAction(
                        routes.AdminApplicationController.downloadDemographics(
                                Optional.empty(), Optional.empty())
                            .url())
                    .with(
                        p("This will download demographic data for all applications for all"
                                + " programs. Use the filters below to select a date range for the"
                                + " exported data. If you select a large date range or leave it"
                                + " blank, the data could be slow to export.")
                            .withClass("text-sm"),
                        fieldset()
                            .withClasses("mt-4", "pt-1", "pb-2", "border")
                            .with(
                                legend("Applications submitted").withClass("ml-3"),
                                // The field names below should be kept in sync with
                                // AdminApplicationController.downloadDemographics.
                                FieldWithLabel.date()
                                    .setFieldName("fromDate")
                                    .setLabelText("From:")
                                    .getDateTag()
                                    .withClasses("ml-3", "inline-flex"),
                                FieldWithLabel.date()
                                    .setFieldName("untilDate")
                                    .setLabelText("Until:")
                                    .getDateTag()
                                    .withClasses("ml-3", "inline-flex")),
                        makeSvgTextButton(downloadActionText, Icons.DOWNLOAD)
                            .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON, "mt-6")
                            .withType("submit")));
    return Modal.builder()
        .setModalId(modalId)
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(downloadDemographicCsvModalContent)
        .setModalTitle(downloadActionText)
        .setTriggerButtonContent(makeSvgTextButton(downloadActionText, Icons.DOWNLOAD))
        .build();
  }

  private ImmutableList<Modal> buildPublishSingleProgramModals(
      ImmutableList<ProgramDefinition> programs,
      ImmutableList<Long> universalQuestionsIds,
      Http.Request request) {

    return programs.stream()
        .map(
            program -> {
              FormTag publishSingleProgramForm =
                  form(makeCsrfTokenInputTag(request))
                      .withId("publish-single-program-form")
                      .withMethod(HttpVerbs.POST)
                      .withAction(routes.AdminProgramController.publishProgram(program.id()).url());

              DivTag missingUniversalQuestionsWarning =
                  div()
                      .with(
                          ViewUtils.makeAlert(
                              "Warning: This program does not use all recommended"
                                  + " universal questions.",
                              ViewUtils.ALERT_WARNING,
                              Optional.empty()))
                      .with(
                          p("We recommend using all universal questions when possible"
                                  + " to create consistent reuse of data and question"
                                  + " formatting.")
                              .withClasses("py-4"));

              DivTag buttons =
                  div(
                          submitButton("Publish program").withClasses(ButtonStyles.SOLID_BLUE),
                          button("Cancel")
                              .withClasses(ButtonStyles.LINK_STYLE, ReferenceClasses.MODAL_CLOSE))
                      .withClasses(
                          "flex", "flex-col", StyleUtils.responsiveMedium("flex-row"), "py-4");

              boolean programIsMissingUniversalQuestions =
                  getMissingUniversalQuestions(program, universalQuestionsIds).size() > 0;

              return Modal.builder()
                  .setModalId(program.adminName() + "-publish-modal")
                  .setLocation(Modal.Location.ADMIN_FACING)
                  .setContent(
                      publishSingleProgramForm
                          .condWith(
                              programIsMissingUniversalQuestions, missingUniversalQuestionsWarning)
                          .with(buttons))
                  .setModalTitle(
                      "Are you sure you want to publish "
                          + program.localizedName().getDefault()
                          + " and all of its draft questions?")
                  .setTriggerButtonContent(
                      makeSvgTextButton("Publish", Icons.PUBLISH)
                          .withId(program.adminName() + "-publish-modal-button")
                          .withClasses(ButtonStyles.CLEAR_WITH_ICON))
                  .setWidth(Modal.Width.THIRD)
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<Long> getMissingUniversalQuestions(
      ProgramDefinition program, ImmutableList<Long> universalQuestionsIds) {
    return universalQuestionsIds.stream()
        .filter(id -> !program.getQuestionIdsInProgram().contains(id))
        .collect(ImmutableList.toImmutableList());
  }

  private Optional<Modal> maybeRenderPublishAllModal(
      ActiveAndDraftPrograms programs, ActiveAndDraftQuestions questions, Http.Request request) {
    // We should only render the publish modal / button if there is at least one draft.
    if (!programs.anyDraft() && !questions.draftVersionHasAnyEdits()) {
      return Optional.empty();
    }

    String link = routes.AdminProgramController.publish().url();

    ImmutableList<QuestionDefinition> sortedDraftQuestions =
        questions.getDraftQuestions().stream()
            .sorted(Comparator.comparing(QuestionDefinition::getName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> sortedDraftPrograms =
        programs.getDraftPrograms().stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());

    DivTag publishAllModalContent =
        div()
            .withClasses("flex-row", "space-y-6")
            .with(
                p("Please be aware that due to the nature of shared questions and versioning,"
                        + " all questions and programs will need to be published together.")
                    .withClass("text-sm"),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_QUESTION)
                    .with(
                        p(String.format("Draft questions (%d):", sortedDraftQuestions.size()))
                            .withClass("font-semibold"))
                    .condWith(sortedDraftQuestions.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftQuestions.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(
                                each(sortedDraftQuestions, this::renderPublishModalQuestionItem))),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_PROGRAM)
                    .with(
                        p(String.format("Draft programs (%d):", sortedDraftPrograms.size()))
                            .withClass("font-semibold"))
                    .condWith(sortedDraftPrograms.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftPrograms.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(each(sortedDraftPrograms, this::renderPublishModalProgramItem))),
                p("Would you like to publish all draft questions and programs now?"),
                div()
                    .withClasses("flex", "flex-row")
                    .with(
                        div().withClass("flex-grow"),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE, ButtonStyles.CLEAR_WITH_ICON),
                        toLinkButtonForPost(
                            submitButton("Confirm").withClasses(ButtonStyles.CLEAR_WITH_ICON),
                            link,
                            request)));
    ButtonTag publishAllButton =
        makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
            .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON, "my-2");
    Modal publishAllModal =
        Modal.builder()
            .setModalId("publish-all-programs-modal")
            .setLocation(Modal.Location.ADMIN_FACING)
            .setContent(publishAllModalContent)
            .setModalTitle("All draft programs will be published")
            .setTriggerButtonContent(publishAllButton)
            .build();
    return Optional.of(publishAllModal);
  }

  private LiTag renderPublishModalProgramItem(ProgramDefinition program) {
    String visibilityText = "";
    switch (program.displayMode()) {
      case HIDDEN_IN_INDEX:
        visibilityText = "Hidden from applicants";
        break;
      case PUBLIC:
        visibilityText = "Publicly visible";
        break;
      default:
        break;
    }
    return li().with(
            span(program.localizedName().getDefault()).withClasses("font-medium"),
            span(" - " + visibilityText + " "),
            new LinkElement()
                .setText("Edit")
                .setHref(controllers.admin.routes.AdminProgramController.edit(program.id()).url())
                .asAnchorText());
  }

  private LiTag renderPublishModalQuestionItem(QuestionDefinition question) {
    return li().with(
            span(question.getQuestionText().getDefault()).withClasses("font-medium"),
            span(" - "),
            new LinkElement()
                .setText("Edit")
                .setHref(
                    controllers.admin.routes.AdminQuestionController.edit(question.getId()).url())
                .asAnchorText());
  }

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2");
    return asRedirectElement(button, link);
  }

  private ProgramCardFactory.ProgramCardData buildProgramCardData(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile,
      ImmutableList<Modal> publishSingleProgramModals) {
    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> draftRow = Optional.empty();
    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> activeRow = Optional.empty();
    if (draftProgram.isPresent()) {
      List<ButtonTag> draftRowActions = Lists.newArrayList();
      List<ButtonTag> draftRowExtraActions = Lists.newArrayList();
      if (settingsManifest.getUniversalQuestions(request)) {
        // Add the trigger button belonging to the modal that matches each draft program
        publishSingleProgramModals.stream()
            .forEach(
                (modal) -> {
                  if (modal.modalId().contains(draftProgram.get().adminName())) {
                    draftRowActions.add(modal.getButton());
                  }
                });
      } else {
        draftRowActions.add(renderPublishProgramLink(draftProgram.get(), request));
      }
      draftRowActions.add(renderEditLink(/* isActive= */ false, draftProgram.get(), request));
      draftRowExtraActions.add(renderManageProgramAdminsLink(draftProgram.get()));
      Optional<ButtonTag> maybeManageTranslationsLink =
          renderManageTranslationsLink(draftProgram.get());
      if (maybeManageTranslationsLink.isPresent()) {
        draftRowExtraActions.add(maybeManageTranslationsLink.get());
      }
      draftRowExtraActions.add(renderEditStatusesLink(draftProgram.get()));
      Optional<ButtonTag> maybeSettingsLink = maybeRenderSettingsLink(draftProgram.get());
      if (maybeSettingsLink.isPresent()) {
        draftRowExtraActions.add(maybeSettingsLink.get());
      }
      draftRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(draftProgram.get())
                  .setRowActions(ImmutableList.copyOf(draftRowActions))
                  .setExtraRowActions(ImmutableList.copyOf(draftRowExtraActions))
                  .build());
    }

    if (activeProgram.isPresent()) {
      List<ButtonTag> activeRowActions = Lists.newArrayList();
      List<ButtonTag> activeRowExtraActions = Lists.newArrayList();

      Optional<ButtonTag> applicationsLink =
          maybeRenderViewApplicationsLink(activeProgram.get(), profile, request);
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (draftProgram.isEmpty()) {
        activeRowExtraActions.add(
            renderEditLink(/* isActive= */ true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      activeRowActions.add(renderViewLink(activeProgram.get(), request));
      activeRowActions.add(renderShareLink(activeProgram.get()));
      activeRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(activeProgram.get())
                  .setRowActions(ImmutableList.copyOf(activeRowActions))
                  .setExtraRowActions(ImmutableList.copyOf(activeRowExtraActions))
                  .build());
    }

    return ProgramCardFactory.ProgramCardData.builder()
        .setActiveProgram(activeRow)
        .setDraftProgram(draftRow)
        .build();
  }

  ButtonTag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.DeepLinkController.programBySlug(program.slug()).url();
    return makeSvgTextButton("Share link", Icons.CONTENT_COPY)
        .withClass(ButtonStyles.CLEAR_WITH_ICON)
        .withData("copyable-program-link", programLink);
  }

  ButtonTag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink =
        controllers.admin.routes.AdminProgramBlocksController.index(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    ButtonTag button = makeSvgTextButton("Edit", Icons.EDIT).withId(editLinkId);
    return isActive
        ? toLinkButtonForPost(
            button.withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN), editLink, request)
        : asRedirectElement(button.withClass(ButtonStyles.CLEAR_WITH_ICON), editLink);
  }

  ButtonTag renderViewLink(ProgramDefinition program, Http.Request request) {
    String viewLink =
        controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(program.id()).url();
    String viewLinkId = "program-view-link-" + program.id();

    ButtonTag button =
        makeSvgTextButton("View", Icons.VIEW)
            .withId(viewLinkId)
            .withClasses(ButtonStyles.CLEAR_WITH_ICON);
    return asRedirectElement(button, viewLink);
  }

  private Optional<ButtonTag> renderManageTranslationsLink(ProgramDefinition program) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String linkDestination =
        routes.AdminProgramTranslationsController.redirectToFirstLocale(program.adminName()).url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE)
            .withId("program-translations-link-" + program.id())
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return Optional.of(asRedirectElement(button, linkDestination));
  }

  private ButtonTag renderEditStatusesLink(ProgramDefinition program) {
    String linkDestination = routes.AdminProgramStatusesController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage application statuses", Icons.FLAKY)
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return asRedirectElement(button, linkDestination);
  }

  private ButtonTag renderPublishProgramLink(ProgramDefinition program, Http.Request request) {
    String linkDestination = routes.AdminProgramController.publishProgram(program.id()).url();
    String confirmationMessage =
        String.format(
            "Are you sure you want to publish %s and all of its draft questions?",
            program.localizedName().getDefault());
    return toLinkButtonForPost(
            makeSvgTextButton("Publish ", Icons.PUBLISH)
                .withId("publish-program-button")
                .withClasses(ButtonStyles.CLEAR_WITH_ICON),
            linkDestination,
            request)
        .attr(
            "onclick",
            String.format(
                "if(confirm('%s')){ return true; } else { var e = arguments[0] ||"
                    + " window.event; e.stopImmediatePropagation(); return false; }",
                confirmationMessage));
  }

  private Optional<ButtonTag> maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram,
      Optional<CiviFormProfile> maybeUserProfile,
      Http.Request request) {
    if (maybeUserProfile.isEmpty()) {
      return Optional.empty();
    }
    CiviFormProfile userProfile = maybeUserProfile.get();
    // TODO(#2582): Determine if this has N+1 query behavior and fix if
    // necessary.
    boolean userIsAuthorized;
    try {
      userProfile.checkProgramAuthorization(activeProgram.adminName(), request).join();
      userIsAuthorized = true;
    } catch (CompletionException e) {
      userIsAuthorized = false;
    }
    if (userIsAuthorized) {
      String editLink =
          routes.AdminApplicationController.index(
                  activeProgram.id(),
                  /* search= */ Optional.empty(),
                  /* page= */ Optional.empty(),
                  /* fromDate= */ Optional.empty(),
                  /* untilDate= */ Optional.empty(),
                  /* applicationStatus= */ Optional.empty(),
                  /* selectedApplicationUri= */ Optional.empty())
              .url();

      String buttonText =
          settingsManifest.getIntakeFormEnabled(request) && activeProgram.isCommonIntakeForm()
              ? "Forms"
              : "Applications";
      ButtonTag button =
          makeSvgTextButton(buttonText, Icons.TEXT_SNIPPET).withClass(ButtonStyles.CLEAR_WITH_ICON);
      return Optional.of(asRedirectElement(button, editLink));
    }
    return Optional.empty();
  }

  private ButtonTag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage Program Admins", Icons.GROUP)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return asRedirectElement(button, adminLink);
  }

  private Optional<ButtonTag> maybeRenderSettingsLink(ProgramDefinition program) {
    if (program.isCommonIntakeForm()) {
      return Optional.empty();
    }
    String linkDestination = routes.AdminProgramController.editProgramSettings(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Settings", Icons.SETTINGS)
            .withId("edit-settings-link-" + program.id())
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return Optional.of(asRedirectElement(button, linkDestination));
  }
}
