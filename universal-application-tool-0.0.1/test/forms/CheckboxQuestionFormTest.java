package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class CheckboxQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    Path path = Path.create("my.question.path");

    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    CheckboxQuestionDefinition expected =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            path,
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableList.of(
                QuestionOption.create(1L, ImmutableMap.of(Locale.US, "cat")),
                QuestionOption.create(2L, ImmutableMap.of(Locale.US, "dog")),
                QuestionOption.create(3L, ImmutableMap.of(Locale.US, "rabbit"))));

    assertThat(builder.build()).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    CheckboxQuestionDefinition originalQd =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            path,
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableList.of(
                QuestionOption.create(1L, ImmutableMap.of(Locale.US, "hello")),
                QuestionOption.create(2L, ImmutableMap.of(Locale.US, "world"))));

    CheckboxQuestionForm form = new CheckboxQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    // The QuestionForm does not set version)),
    //      which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
