package forms;

import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an enumerator question. */
public class EnumeratorQuestionForm extends QuestionForm {
  private String entityType;
  private OptionalLong maxEntities;

  public EnumeratorQuestionForm() {
    super();
    this.entityType = "";
    this.maxEntities = OptionalLong.empty();
  }

  public EnumeratorQuestionForm(EnumeratorQuestionDefinition qd) {
    super(qd);
    this.entityType = qd.getEntityType().isEmpty() ? "" : qd.getEntityType().getDefault();
    this.maxEntities = qd.getMaxEntities();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  public String getEntityType() {
    return entityType;
  }

  public OptionalLong getMaxEntities() {
    return maxEntities;
  }

  public void setMaxEntities(String maxEntitiesAsString) {
    this.maxEntities =
        maxEntitiesAsString.isEmpty()
            ? OptionalLong.empty()
            : OptionalLong.of(Long.parseLong(maxEntitiesAsString));
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    EnumeratorQuestionDefinition.EnumeratorValidationPredicates validationPredicates =
        EnumeratorQuestionDefinition.EnumeratorValidationPredicates.builder()
            .setMaxEntities(getMaxEntities())
            .build();

    return super.getBuilder()
        .setEntityType(LocalizedStrings.withDefaultValue(this.entityType))
        .setValidationPredicates(validationPredicates);
  }
}
