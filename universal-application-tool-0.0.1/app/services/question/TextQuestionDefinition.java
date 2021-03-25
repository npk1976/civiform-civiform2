package services.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.Path;

public class TextQuestionDefinition extends QuestionDefinition {

  public TextQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      TextValidationPredicates validationPredicates) {
    super(
        id, version, name, path, description, questionText, questionHelpText, validationPredicates);
  }

  public TextQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      TextValidationPredicates validationPredicates) {
    super(version, name, path, description, questionText, questionHelpText, validationPredicates);
  }

  public TextQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        version,
        name,
        path,
        description,
        questionText,
        questionHelpText,
        TextValidationPredicates.create());
  }

  @JsonDeserialize(
      builder = AutoValue_TextQuestionDefinition_TextValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class TextValidationPredicates extends ValidationPredicates {

    public static TextValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_TextQuestionDefinition_TextValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static TextValidationPredicates create() {
      return builder().build();
    }

    public static TextValidationPredicates create(int minLength, int maxLength) {
      return builder().setMinLength(minLength).setMaxLength(maxLength).build();
    }

    @JsonProperty("minLength")
    public abstract OptionalInt minLength();

    @JsonProperty("maxLength")
    public abstract OptionalInt maxLength();

    public static Builder builder() {
      return new AutoValue_TextQuestionDefinition_TextValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minLength")
      public abstract Builder setMinLength(OptionalInt minLength);

      public abstract Builder setMinLength(int minLength);

      @JsonProperty("maxLength")
      public abstract Builder setMaxLength(OptionalInt maxLength);

      public abstract Builder setMaxLength(int maxLength);

      public abstract TextValidationPredicates build();
    }
  }

  public TextValidationPredicates getTextValidationPredicates() {
    return (TextValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  @Override
  ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(getTextPath(), getTextType());
  }

  public Path getTextPath() {
    return getPath().toBuilder().append("text").build();
  }

  public ScalarType getTextType() {
    return ScalarType.STRING;
  }

  public OptionalInt getMinLength() {
    return getTextValidationPredicates().minLength();
  }

  public OptionalInt getMaxLength() {
    return getTextValidationPredicates().maxLength();
  }
}
