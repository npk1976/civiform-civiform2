package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.Path;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class NumberQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    NumberQuestionForm form = new NumberQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMin("2");
    form.setMax("8");
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    NumberQuestionDefinition expected =
        new NumberQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            NumberQuestionDefinition.NumberValidationPredicates.create(2, 8));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    NumberQuestionDefinition originalQd =
        new NumberQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            NumberQuestionDefinition.NumberValidationPredicates.create(2, 8));

    NumberQuestionForm form = new NumberQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    Path path = Path.create("my.question.path.name");

    NumberQuestionForm form = new NumberQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMin("");
    form.setMax("");
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    NumberQuestionDefinition expected =
        new NumberQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            NumberQuestionDefinition.NumberValidationPredicates.create());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
