package services.question;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

/**
 * Repeater questions provide a variable list of user-defined identifiers for some repeated entity.
 * Examples of repeated entities could be household members, vehicles, jobs, etc.
 *
 * <p>A repeater question definition can be referenced by other question definitions that themselves
 * repeat for each of the repeater-defined entities. For example, a repeater for vehicles may ask
 * the user to identify each of their vehicles, with other questions referencing it that ask about
 * each vehicle's make, model, and year.
 */
public class RepeaterQuestionDefinition extends QuestionDefinition {

  public static final String REPEATED_ENTITY_NAME_KEY = "entity_name";

  public RepeaterQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        id,
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        RepeaterValidationPredicates.create());
  }

  public RepeaterQuestionDefinition(
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        RepeaterValidationPredicates.create());
  }

  public RepeaterValidationPredicates getRepeaterValidationPredicates() {
    return (RepeaterValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.REPEATER;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of();
  }

  @AutoValue
  public abstract static class RepeaterValidationPredicates extends ValidationPredicates {
    public static RepeaterValidationPredicates create() {
      return new AutoValue_RepeaterQuestionDefinition_RepeaterValidationPredicates();
    }
  }
}
