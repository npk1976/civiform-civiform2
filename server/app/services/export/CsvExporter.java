package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import models.Application;
import models.TrustedIntermediaryGroup;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.DateConverter;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ReadOnlyApplicantProgramService;
import services.export.enums.SubmitterType;
import services.program.Column;
import services.question.types.QuestionType;

/**
 * CsvExporter takes a list of {@link Column}s and exports the data specified. A column contains a
 * {@link Path} indexing into an applicant's data, and CsvExporter takes the path and reads the
 * answer from {@link ReadOnlyApplicantProgramService} if present.
 *
 * <p>Call close() directly or use the try-with-resources pattern in order for the underlying {@link
 * CSVPrinter} to be closed.
 */
public final class CsvExporter implements AutoCloseable {
  private final String EMPTY_VALUE = "";

  private final ImmutableList<Column> columns;
  private final String secret;
  private final CSVPrinter printer;
  private final DateConverter dateConverter;
  private final ImmutableMap<String, ImmutableList<String>> checkBoxQuestionScalarMap;

  /** Provide a secret if you will need to use OPAQUE_ID type columns. */
  public CsvExporter(
      ImmutableList<Column> columns,
      String secret,
      Writer writer,
      DateConverter dateConverter,
      ImmutableMap<String, ImmutableList<String>> checkBoxQuestionScalarMap)
      throws IOException {
    this.columns = checkNotNull(columns);
    this.secret = checkNotNull(secret);
    this.dateConverter = dateConverter;
    this.checkBoxQuestionScalarMap = checkNotNull(checkBoxQuestionScalarMap);

    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(columns.stream().map(Column::header).toArray(String[]::new))
            .build();
    this.printer = new CSVPrinter(writer, format);
  }

  /** Writes a single {@link Application} record to the CSV. */
  public void exportRecord(
      Application application,
      ReadOnlyApplicantProgramService roApplicantService,
      Optional<Boolean> optionalEligibilityStatus)
      throws IOException {
    Map<Path, String> answerDataMap = new HashMap<>();
    for (AnswerData answerData : roApplicantService.getSummaryData()) {
      if (answerData.questionDefinition().getQuestionType().equals(QuestionType.CHECKBOX)) {
        String questionName = answerData.questionDefinition().getName();
        // If the question isn't present in the scalar map, it means, its demographic export and
        // this question was flagged not to be included in demographic export
        if (!checkBoxQuestionScalarMap.containsKey(questionName)) {
          continue;
        }
        List<String> optionHeaders = checkBoxQuestionScalarMap.get(questionName);
        // the four options for a value are:
        //    in the selected list and in the question definition and question is answered: Selected
        //    not in the selected list and in the question definition and question is answered: Not
        // Selected
        //    not in the selected list and in the question definition and question is not answered:
        // Not Answered
        //    not in the selected list and not in the question definition: Not An Option At Program
        // Version(or similar. This is both “retired” and “not yet an option at the time of this
        // application”)
        String defaultText =
            answerData.isAnswered()
                ? "Not An Option At Program Version"
                : "Not Answered - optional";
        List<String> selectedList =
            answerData
                .applicantQuestion()
                .createMultiSelectQuestion()
                .getSelectedOptionAdminNames()
                .map(selectedOptions -> selectedOptions.stream().collect(Collectors.toList()))
                .orElse(Collections.singletonList(""));
        List<String> allOptionsShownInQuestion = new ArrayList<>();
        answerData.applicantQuestion().createMultiSelectQuestion().getOptions().stream()
            .forEach(option -> allOptionsShownInQuestion.add(option.adminName()));

        optionHeaders.forEach(
            option ->
                answerDataMap.put(
                    answerData.contextualizedPath().join(String.valueOf(option)),
                    selectedList.contains(option)
                        ? "Selected"
                        : allOptionsShownInQuestion.contains(option) && answerData.isAnswered()
                            ? "Not Selected"
                            : defaultText));

      } else {
        for (Path p : answerData.scalarAnswersInDefaultLocale().keySet()) {
          answerDataMap.put(p, answerData.scalarAnswersInDefaultLocale().get(p));
        }
      }
    }
    ImmutableMap<Path, String> answerMap =
        ImmutableMap.<Path, String>builder().putAll(answerDataMap).build();
    for (Column column : columns) {
      switch (column.columnType()) {
        case APPLICANT_ANSWER:
          printer.print(getValueFromAnswerMap(column, answerMap));
          break;
        case APPLICANT_ID:
          printer.print(application.getApplicant().id);
          break;
        case APPLICATION_ID:
          printer.print(application.id);
          break;
        case LANGUAGE:
          printer.print(application.getApplicantData().preferredLocale().toLanguageTag());
          break;
        case CREATE_TIME:
          printer.print(dateConverter.renderDateTimeDataOnly(application.getCreateTime()));
          break;
        case SUBMIT_TIME:
          if (application.getSubmitTime() == null) {
            printer.print(EMPTY_VALUE);
          } else {
            printer.print(dateConverter.renderDateTimeDataOnly(application.getSubmitTime()));
          }
          break;
        case TI_EMAIL_OPAQUE:
          if (secret.isBlank()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(
              application
                  .getSubmitterEmail()
                  .map(email -> opaqueIdentifier(secret, email))
                  .orElse(EMPTY_VALUE));
          break;
        case TI_EMAIL:
          printer.print(application.getSubmitterEmail().orElse(EMPTY_VALUE));
          break;
        case SUBMITTER_TYPE:
          // The field on the application is called `submitter_email`, but it's only ever used to
          // store the TI's email, never the applicant's.
          // TODO(#5325): Rename the `submitter_email` database field to `ti_email` and move the
          // submitter_type logic upstream.
          printer.print(
              application.getSubmitterEmail().isPresent()
                  ? SubmitterType.TRUSTED_INTERMEDIARY.toString()
                  : SubmitterType.APPLICANT.toString());
          break;
        case PROGRAM:
          printer.print(application.getProgram().getProgramDefinition().adminName());
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
          if (secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(opaqueIdentifier(secret, application.getApplicant().id));
          break;
        case APPLICANT_OPAQUE:
          if (secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque applicant data requested.");
          }
          // We still hash the empty value.
          printer.print(opaqueIdentifier(secret, getValueFromAnswerMap(column, answerMap)));
          break;
        case ELIGIBILITY_STATUS:
          if (optionalEligibilityStatus.isPresent()) {
            String eligibilityText =
                optionalEligibilityStatus.get() ? "Meets eligibility" : "Doesn't meet eligibility";
            printer.print(eligibilityText);
          } else {
            printer.print(EMPTY_VALUE);
          }
          break;
        case STATUS_TEXT:
          printer.print(application.getLatestStatus().orElse(EMPTY_VALUE));
          break;
      }
    }

    printer.println();
  }

  /**
   * Returns the answer retrieved by {@link ReadOnlyApplicantProgramService}. The value is derived
   * from the raw value in applicant data, such as translating enum number to human readable text in
   * default locale or mapping file key to download url.
   */
  private String getValueFromAnswerMap(Column column, ImmutableMap<Path, String> answerMap) {
    Path path = column.jsonPath().orElseThrow();
    if (!answerMap.containsKey(path)) {
      return EMPTY_VALUE;
    }
    return answerMap.get(path);
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

  @Override
  public void close() throws IOException {
    printer.close();
  }
}
