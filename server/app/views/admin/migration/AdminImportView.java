package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.CiviFormError;
import services.ErrorAnd;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.fileupload.FileUploadViewStrategy;

/**
 * A view allowing admins to import a JSON representation of a program and add that program to their
 * instance.
 */
public class AdminImportView extends BaseHtmlView {
  private final AdminLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public AdminImportView(AdminLayoutFactory layoutFactory, MessagesApi messagesApi) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Renders the import page, showing a file upload area to upload a JSON file. If {@code
   * programData} is present, then the program data will also be displayed.
   *
   * @param programData the program data that was parsed from the uploaded JSON file, including any
   *     errors that may have happened while parsing. If empty, no file has been uploaded yet.
   */
  public Content render(
      Http.Request request, Optional<ErrorAnd<String, CiviFormError>> programData) {
    String title = "Import a program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20")
            .with(h1(title))
            .with(
                p("This page allows you to import a program that exists in another environment"
                      + " (like staging) and easily add the program to this environment.")
                    .withClass("my-2"))
            .with(
                p("First, open the other environment and use the \"Export\" tab to download a JSON"
                      + " file that represents the existing program.")
                    .withClass("my-2"))
            .with(
                p("Then, upload the JSON file here. The program information will be displayed"
                      + " below, and you can verify all the information before adding the program.")
                    .withClass("my-2"));

    contentDiv.with(createUploadProgramJsonForm(request));
    contentDiv.with(renderProgramData(programData));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent createUploadProgramJsonForm(Http.Request request) {
    DivTag fileUploadElement =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            "import-program-id",
            Http.MimeTypes.JSON,
            /* hints= */ ImmutableList.of(),
            /* disabled= */ false,
            // TODO(#7087): Determine a reasonable file size limit.
            /* fileLimitMb= */ 5,
            messagesApi.preferred(request));
    return div()
        .with(h2("Upload program JSON"))
        .with(
            form()
                .withEnctype("multipart/form-data")
                .withMethod("POST")
                .with(makeCsrfTokenInputTag(request), fileUploadElement)
                .with(submitButton("Import program").withClass(ButtonStyles.SOLID_BLUE))
                .withAction(routes.AdminImportController.importProgram().url()))
        .withClass("mb-10");
  }

  private DomContent renderProgramData(Optional<ErrorAnd<String, CiviFormError>> programData) {
    DivTag data = div().with(h2("Uploaded program data"));
    if (programData.isEmpty()) {
      data.with(p("No data has been uploaded yet."));
      return data;
    }
    if (programData.get().isError()) {
      data.with(each(programData.get().getErrors(), error -> p(error.message())));
    }
    // TODO(#7087): Render the program data correctly by showing the blocks and questions in a
    // readable format.
    data.with(p(programData.get().getResult()));
    return data;
  }
}
