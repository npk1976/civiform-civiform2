package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.form;
import static views.BaseHtmlView.makeCsrfTokenInputTag;
import static views.BaseHtmlView.renderHeader;
import static views.BaseHtmlView.submitButton;

import com.google.inject.Inject;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView {
  private final AdminLayout layout;
  private final FormFactory formFactory;

  @Inject
  public ProgramImageView(AdminLayoutFactory layoutFactory, FormFactory formFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.formFactory = checkNotNull(formFactory);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   *
   * <p>TODO(#5676): Implement the forms to add an image and alt text.
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    String title =
        String.format(
            "Manage program image for %s", programDefinition.localizedName().getDefault());
    H1Tag headerDiv = renderHeader(title, "my-10", "mx-10");
    FormTag imageDescriptionForm = createImageDescriptionForm(request, programDefinition);
    HtmlBundle htmlBundle = layout.getBundle(request).setTitle(title).addMainContent(headerDiv, imageDescriptionForm);

    // TODO: Helper?
    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private FormTag createImageDescriptionForm(Http.Request request, ProgramDefinition programDefinition) {
    String existingDescription = programDefinition.localizedSummaryImageDescription().map(LocalizedStrings::getDefault).orElse("");
    System.out.println("existing description = " + existingDescription);
    ProgramImageDescriptionForm form = new ProgramImageDescriptionForm(existingDescription);
    Form<ProgramImageDescriptionForm> programImageForm =
      formFactory.form(ProgramImageDescriptionForm.class).fill(form);

    return
      form()
        .withMethod("POST")
        .withAction(routes.AdminProgramImageController.updateDescription(programDefinition.id()).url())
        .with(
          makeCsrfTokenInputTag(request),
          FieldWithLabel.input()
            .setFieldName(ProgramImageDescriptionForm.SUMMARY_IMAGE_DESCRIPTION)
            .setLabelText("Image description")
            .setValue(programImageForm.value().get().getSummaryImageDescription())
            .getInputTag())
        .with(
          submitButton("Save").withClass(ButtonStyles.SOLID_BLUE));
  }
}
