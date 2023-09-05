package services.applications;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.text.DocumentException;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import services.export.AbstractExporterTest;
import services.export.PdfExporter;
import services.program.ProgramDefinition;

public class PdfExporterServiceTest extends AbstractExporterTest {

  @Before
  public void createTestData() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void generatePdf() throws IOException, DocumentException {
    PdfExporter exporter = instanceOf(PdfExporter.class);
    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition program = applicationOne.getProgram().getProgramDefinition();

    assertThat(exporter.export(applicationOne))
        .isEqualTo(service.generatePdf(applicationOne.id, program));
  }
}
