package services.applicant.question;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a question in the context of a specific applicant. Other type-specific classes (e.g.
 * {@link NameQuestion}) use this class's data to represent a specific question type. These other
 * classes provide access to the applicant's answer for the question. They can also implement
 * server-side validation logic.
 */
public class ApplicantQuestion {

  private final QuestionDefinition questionDefinition;
  private final Path applicationPathContext;
  private final ApplicantData applicantData;

  public ApplicantQuestion(
      QuestionDefinition questionDefinition,
      ApplicantData applicantData,
      Path applicationPathContext) {
    this.questionDefinition = checkNotNull(questionDefinition);
    this.applicantData = checkNotNull(applicantData);
    this.applicationPathContext = checkNotNull(applicationPathContext);
  }

  // TODO(#783): Get rid of this constructor.
  public ApplicantQuestion(QuestionDefinition questionDefinition, ApplicantData applicantData) {
    this(questionDefinition, applicantData, Path.create("applicant"));
  }

  protected ApplicantData getApplicantData() {
    return this.applicantData;
  }

  protected QuestionDefinition getQuestionDefinition() {
    return this.questionDefinition;
  }

  public QuestionType getType() {
    return questionDefinition.getQuestionType();
  }

  public String getQuestionText() {
    return questionDefinition.getQuestionTextOrDefault(applicantData.preferredLocale());
  }

  public String getQuestionHelpText() {
    return questionDefinition.getQuestionHelpTextOrDefault(applicantData.preferredLocale());
  }

  /**
   * Returns the contextualized path for this question. The path is contextualized with respect to
   * the enumerated elements it is about.
   *
   * <p>For example, a generic path about the name of an applicant's household member may look like
   * "applicant.household_member[].name", while a contextualized path would look like
   * "applicant.household_member[3].name".
   */
  public Path getContextualizedPath() {
    return applicationPathContext.join(questionDefinition.getQuestionPathSegment());
  }

  public boolean hasErrors() {
    return errorsPresenter().hasQuestionErrors() || errorsPresenter().hasTypeSpecificErrors();
  }

  public Optional<Long> getUpdatedInProgramMetadata() {
    return applicantData.readLong(getContextualizedPath().join(Scalar.PROGRAM_UPDATED_IN));
  }

  public Optional<Long> getLastUpdatedTimeMetadata() {
    return applicantData.readLong(getContextualizedPath().join(Scalar.UPDATED_AT));
  }

  public AddressQuestion createAddressQuestion() {
    return new AddressQuestion(this);
  }

  public FileUploadQuestion createFileUploadQuestion() {
    return new FileUploadQuestion(this);
  }

  public MultiSelectQuestion createMultiSelectQuestion() {
    return new MultiSelectQuestion(this);
  }

  public NameQuestion createNameQuestion() {
    return new NameQuestion(this);
  }

  public NumberQuestion createNumberQuestion() {
    return new NumberQuestion(this);
  }

  public RepeaterQuestion createRepeaterQuestion() {
    return new RepeaterQuestion(this);
  }

  public SingleSelectQuestion createSingleSelectQuestion() {
    return new SingleSelectQuestion(this);
  }

  public TextQuestion createTextQuestion() {
    return new TextQuestion(this);
  }

  public PresentsErrors errorsPresenter() {
    switch (getType()) {
      case ADDRESS:
        return createAddressQuestion();
      case CHECKBOX:
        return createMultiSelectQuestion();
      case FILEUPLOAD:
        return createFileUploadQuestion();
      case NAME:
        return createNameQuestion();
      case NUMBER:
        return createNumberQuestion();
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return createSingleSelectQuestion();
      case REPEATER:
        return createRepeaterQuestion();
      case TEXT:
        return createTextQuestion();
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantQuestion) {
      ApplicantQuestion that = (ApplicantQuestion) object;
      return this.questionDefinition.equals(that.questionDefinition)
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionDefinition, applicantData);
  }
}
