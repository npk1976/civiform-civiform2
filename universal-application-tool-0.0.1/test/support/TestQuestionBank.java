package support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import models.LifecycleStage;
import models.Question;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.*;

/**
 * A cached {@link Question} bank for testing.
 *
 * <p>The {@link Question}s in this question bank should be treated as constants, but they need to
 * be persisted in the database for some tests so they are persisted and cached. When used with
 * tests that do not have a database available (see {@link #maybeSave(QuestionDefinition)}), the
 * question IDs may not be reliable since in production, the IDs are set by the database.
 *
 * <p>The properties of these questions (e.g. question path) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a {@link QuestionEnum} for it,
 * create a private static method to construct the question, and create a public static method to
 * retrieve the cached question. Add new methods in alphabetical order by {@link QuestionType},
 * grouping those methods with the same type together.
 */
public class TestQuestionBank {
  private static final long VERSION = 1L;

  private boolean canSave = false;
  private Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();
  private AtomicLong nextId = new AtomicLong(1L);

  private ImmutableMap<QuestionType, Question> typeToQuestionMap =
      new ImmutableMap.Builder<QuestionType, Question>()
        .put(QuestionType.ADDRESS, applicantAddress())
        .put(QuestionType.CHECKBOX, applicantKitchenTools())
        .put(QuestionType.DROPDOWN, applicantIceCream())
        .put(QuestionType.FILEUPLOAD, applicantFile())
        .put(QuestionType.NAME, applicantName())
        .put(QuestionType.NUMBER, applicantJugglingNumber())
        .put(QuestionType.RADIO_BUTTON, applicantSeason())
        .put(QuestionType.REPEATER, applicantHouseholdMembers())
        .put(QuestionType.TEXT, applicantFavoriteColor())
        .build();

  /**
   * Pass `true` if there is a database that comes up with the test (e.g., the test class extends
   * WithPostgresContainer), otherwise false.
   */
  public TestQuestionBank(boolean canSave) {
    this.canSave = canSave;
  }

  public void reset() {
    questionCache.clear();
    nextId.set(1L);
  }

  public ImmutableMap<QuestionType, Question> getTypeToQuestionMap() {
    return typeToQuestionMap;
  }

  // Address
  public Question applicantAddress() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ADDRESS, this::applicantAddress);
  }

  // Checkbox
  public Question applicantKitchenTools() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_KITCHEN_TOOLS, this::applicantKitchenTools);
  }

  // Dropdown
  public Question applicantIceCream() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ICE_CREAM, this::applicantIceCream);
  }

  // File upload
  public Question applicantFile() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_FILE, this::applicantFile);
  }

  // Name
  public Question applicantName() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_NAME, this::applicantName);
  }

  // Repeated name
  public Question applicantHouseholdMemberName() {
    // Make sure the next call will have the question ready
    applicantHouseholdMembers();
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBER_NAME, this::applicantHouseholdMemberName);
  }

  // Number
  public Question applicantJugglingNumber() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_JUGGLING_NUMBER, this::applicantJugglingNumber);
  }

  // Radio button
  public Question applicantSeason() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_SEASON, this::applicantSeason);
  }

  // Repeater
  public Question applicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBERS, this::applicantHouseholdMembers);
  }

  // Text
  public Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, this::applicantFavoriteColor);
  }

  // Address
  private Question applicantAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            VERSION,
            "applicant address",
            Path.create("applicant.applicant_address"),
            Optional.empty(),
            "address of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your address?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Checkbox
  private Question applicantKitchenTools(QuestionEnum ignore) {
    QuestionDefinition definition =
        new CheckboxQuestionDefinition(
            1L,
            "kitchen tools",
            Path.create("applicant.kitchen_tools"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Which of the following kitchen instruments do you own?"),
            ImmutableMap.of(Locale.US, "help text"),
            ImmutableList.of(
                QuestionOption.create(1L, ImmutableMap.of(Locale.US, "toaster")),
                QuestionOption.create(2L, ImmutableMap.of(Locale.US, "pepper grinder")),
                QuestionOption.create(3L, ImmutableMap.of(Locale.US, "garlic press"))));
    return maybeSave(definition);
  }

  // Dropdown
  private Question applicantIceCream(QuestionEnum ignore) {
    QuestionDefinition definition =
        new DropdownQuestionDefinition(
            1L,
            "applicant ice cream",
            Path.create("applicant.applicant_ice_cream"),
            Optional.empty(),
            "select your favorite ice cream flavor",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Select your favorite ice cream flavor from the following"),
            ImmutableMap.of(Locale.US, "this is sample help text"),
            ImmutableList.of(
                QuestionOption.create(1L, ImmutableMap.of(Locale.US, "chocolate")),
                QuestionOption.create(2L, ImmutableMap.of(Locale.US, "strawberry")),
                QuestionOption.create(3L, ImmutableMap.of(Locale.US, "vanilla")),
                QuestionOption.create(4L, ImmutableMap.of(Locale.US, "coffee"))));
    return maybeSave(definition);
  }

  // File upload
  private Question applicantFile(QuestionEnum ignore) {
    QuestionDefinition definition =
        new FileUploadQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.applicant_name"),
            Optional.empty(),
            "name of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your name?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Name
  private Question applicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.applicant_name"),
            Optional.empty(),
            "name of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your name?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Repeated name
  private Question applicantHouseholdMemberName(QuestionEnum ignore) {
    Question householdMembers = applicantHouseholdMembers();
    QuestionDefinition definition =
        new NameQuestionDefinition(
            VERSION,
            "household members name",
            Path.create("applicant.applicant_household_members[].name"),
            Optional.of(householdMembers.id),
            "The applicant's household member's name",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is the household member's name?"),
            ImmutableMap.of(Locale.US, "help text"));

    return maybeSave(definition);
  }

  // Number
  private Question applicantJugglingNumber(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NumberQuestionDefinition(
            VERSION,
            "number of items applicant can juggle",
            Path.create("applicant.juggling_number"),
            Optional.empty(),
            "number of items applicant can juggle at once",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "How many items can you juggle at one time?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Radio button
  private Question applicantSeason(QuestionEnum ignore) {
    QuestionDefinition definition =
        new RadioButtonQuestionDefinition(
            1L,
            "radio",
            Path.create("applicant.radio"),
            Optional.empty(),
            "favorite season in the year",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is your favorite season?"),
            ImmutableMap.of(Locale.US, "this is sample help text"),
            ImmutableList.of(
                QuestionOption.create(1L, ImmutableMap.of(Locale.US, "winter")),
                QuestionOption.create(2L, ImmutableMap.of(Locale.US, "spring")),
                QuestionOption.create(3L, ImmutableMap.of(Locale.US, "summer")),
                QuestionOption.create(4L, ImmutableMap.of(Locale.US, "fall"))));
    return maybeSave(definition);
  }

  // Repeater
  private Question applicantHouseholdMembers(QuestionEnum ignore) {
    QuestionDefinition definition =
        new RepeaterQuestionDefinition(
            VERSION,
            "applicant household members",
            Path.create("applicant.applicant_household_members[]"),
            Optional.empty(),
            "The applicant's household members",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Who are your household members?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Text
  private Question applicantFavoriteColor(QuestionEnum ignore) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            VERSION,
            "applicant favorite color",
            Path.create("applicant.applicant_favorite_color"),
            Optional.empty(),
            "favorite color of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your favorite color?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  private Question maybeSave(QuestionDefinition questionDefinition) {
    Question question = new Question(questionDefinition);
    if (canSave) {
      question.save();
    } else {
      question.id = nextId.getAndIncrement();
      try {
        question.loadQuestionDefinition();
      } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
        throw new IllegalArgumentException(
            "Questions in the TestQuestionBank better be supported QuestionTypes.");
      }
    }
    return question;
  }

  private enum QuestionEnum {
    APPLICANT_ADDRESS,
    APPLICANT_KITCHEN_TOOLS,
    APPLICANT_ICE_CREAM,
    APPLICANT_SEASON,
    APPLICANT_NAME,
    APPLICANT_JUGGLING_NUMBER,
    APPLICANT_HOUSEHOLD_MEMBERS,
    APPLICANT_HOUSEHOLD_MEMBER_NAME,
    APPLICANT_FAVORITE_COLOR,
    APPLICANT_FILE
  }
}
