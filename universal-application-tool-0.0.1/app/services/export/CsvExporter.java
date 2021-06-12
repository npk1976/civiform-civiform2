package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import models.Application;
import models.Program;
import models.TrustedIntermediaryGroup;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import repository.ProgramRepository;
import services.program.Column;

public class CsvExporter {
  private final String EMPTY_VALUE = "";

  private boolean wroteHeaders;
  private ImmutableList<Column> columns;
  private Optional<String> secret;
  private Optional<ProgramRepository> programRepository;

  public CsvExporter(List<Column> columns) {
    this.wroteHeaders = false;
    this.columns = ImmutableList.copyOf(columns);
    this.secret = Optional.empty();
    this.programRepository = Optional.empty();
  }

  /** Provide a secret if you will need to use OPAQUE_ID type columns. */
  public CsvExporter(ImmutableList<Column> columns, String secret, ProgramRepository programRepository) {
    this(columns);
    this.secret = Optional.of(secret);
    this.programRepository = Optional.of(programRepository);
  }

  private void writeHeadersOnFirstExport(CSVPrinter printer) throws IOException {
    if (!wroteHeaders) {
      for (Column column : columns) {
        printer.print(column.header());
      }
      printer.println();
      wroteHeaders = true;
    }
  }

  protected ImmutableList<Column> getColumns() {
    return columns;
  }

  /**
   * The CSV exporter will write the headers on first call to services.export(). It does not store
   * the writer between calls. Since it is intended for many applications, this function is intended
   * to be called several times.
   */
  public void export(Application application, Writer writer) throws IOException {
    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader());

    this.writeHeadersOnFirstExport(printer);

    for (Column column : getColumns()) {
      switch (column.columnType()) {
        case APPLICANT:
          Optional<String> value =
              application.getApplicantData().readAsString(column.jsonPath().orElseThrow());
          printer.print(value.orElse(EMPTY_VALUE));
          break;
        case ID:
          printer.print(application.id);
          break;
        case LANGUAGE:
          printer.print(application.getApplicantData().preferredLocale().toLanguageTag());
          break;
        case CREATE_TIME:
          printer.print(application.getCreateTime().toString());
          break;
        case SUBMIT_TIME:
          printer.print(application.getSubmitTime().toString());
          break;
        case SUBMITTER_EMAIL_OPAQUE:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(
              application
                  .getSubmitterEmail()
                  .map(email -> opaqueIdentifier(this.secret.get(), email))
                  .orElse(EMPTY_VALUE));
          break;
        case SUBMITTER_EMAIL:
          printer.print(application.getSubmitterEmail().orElse("Applicant"));
          break;
        case PROGRAM:
          Program program = application.getProgram();
          if (programRepository.isEmpty()) {
            throw new RuntimeException("No program repository provided, but program details requested.");
          }
          // This is a strange workaround for a bug in ebean.  For some reason, the program that
          // is returned from the application crashes ebean's server when we attempt to access anything
          // other than the id.  This is hard to debug since ebean doesn't write code, it writes bytecode,
          // directly.  This workaround costs 1 extremely cheap query per application - bad, but
          // probably not problematic until the size of the database gets huge.
          printer.print(programRepository.get().lookupProgram(program.id).toCompletableFuture().join().get().getProgramDefinition().adminName());
          break;
        case TI_ORGANIZATION:
          printer.print(
              application
                  .getApplicant()
                  .getAccount()
                  .getManagedByGroup()
                  .map(TrustedIntermediaryGroup::getName)
                  .orElse(EMPTY_VALUE));
          break;
        case OPAQUE_ID:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(opaqueIdentifier(this.secret.get(), application.getApplicant().id));
          break;
        case APPLICANT_OPAQUE:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque applicant data requested.");
          }
          Optional<String> applicantValue =
              application.getApplicantData().readAsString(column.jsonPath().orElseThrow());
          // We still hash the empty value.
          printer.print(opaqueIdentifier(this.secret.get(), applicantValue.orElse(EMPTY_VALUE)));
      }
    }

    printer.println();
  }
  /** Returns an opaque identifier - the ID hashed with the application secret key. */
  private static String opaqueIdentifier(String secret, Long id) {
    return Hashing.sha256()
        .newHasher()
        .putString(secret, StandardCharsets.UTF_8)
        .putLong(id)
        .hash()
        .toString();
  }

  private static String opaqueIdentifier(String secret, String value) {
    return Hashing.sha256()
        .newHasher()
        .putString(secret, StandardCharsets.UTF_8)
        .putString(value, StandardCharsets.UTF_8)
        .hash()
        .toString();
  }
}
