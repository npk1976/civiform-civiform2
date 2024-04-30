package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.OptionalLong;
import services.LocalizedStrings;

/**
 * Enumerator questions provide a variable list of user-defined identifiers for some repeated
 * entity. Examples of repeated entities could be household members, vehicles, jobs, etc.
 *
 * <p>An enumerator question definition can be referenced by other question definitions that
 * themselves repeat for each of the enumerator-defined entities. For example, an enumerator for
 * vehicles may ask the user to identify each of their vehicles, with other questions referencing it
 * that ask about each vehicle's make, model, and year.
 */
public class EnumeratorQuestionDefinition extends QuestionDefinition {

  static final String DEFAULT_ENTITY_TYPE = "Item";

  private final LocalizedStrings entityType;

  public EnumeratorQuestionDefinition(
      QuestionDefinitionConfig config, LocalizedStrings entityType) {
    super(config);
    this.entityType = checkNotNull(entityType);
  }

  public EnumeratorValidationPredicates getEnumeratorValidationPredicates() {
    return (EnumeratorValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return EnumeratorValidationPredicates.create();
  }

  public LocalizedStrings getEntityType() {
    return entityType;
  }

  public OptionalLong getMaxEntities() {
    return getEnumeratorValidationPredicates().maxEntities();
  }

  @JsonDeserialize(
      builder = AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class EnumeratorValidationPredicates extends ValidationPredicates {

    public static EnumeratorValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static EnumeratorValidationPredicates create() {
      return builder().build();
    }

    public static EnumeratorValidationPredicates create(long maxEntities) {
      return builder().setMaxEntities(maxEntities).build();
    }

    @JsonProperty("maxEntities")
    public abstract OptionalLong maxEntities();

    public static Builder builder() {
      return new AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("maxEntities")
      public abstract Builder setMaxEntities(OptionalLong maxEntities);

      public abstract Builder setMaxEntities(long maxEntities);

      public abstract EnumeratorValidationPredicates build();
    }
  }
}
