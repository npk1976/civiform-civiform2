package views.questiontypes;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import java.time.LocalDate;
import java.util.Optional;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.DateQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

public class DateQuestionRenderer extends ApplicantQuestionRenderer {

  public DateQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.DATE_QUESTION;
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    DateQuestion dateQuestion = question.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date().setFieldName(dateQuestion.getDatePath().toString());
    if (dateQuestion.getDateValue().isPresent()) {
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      dateField.setValue(value);
    }
    Tag dateQuestionFormContent = dateField.getContainer();

    return renderInternal(params.messages(), dateQuestionFormContent, false);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(question.createDateQuestion().getDatePath());
  }
}
