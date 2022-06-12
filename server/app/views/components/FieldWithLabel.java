package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.label;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.Tag;
import j2html.tags.attributes.IName;
import j2html.tags.attributes.IDisabled;
import j2html.tags.attributes.IPlaceholder;
import j2html.tags.attributes.IFormaction;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.TextareaTag;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Utility class for rendering an input field with an optional label. */
public class FieldWithLabel {

  private static final ImmutableSet<String> STRING_TYPES =
      ImmutableSet.of("text", "checkbox", "radio", "date", "email");

  private String fieldName = "";
  private String fieldType = "text";
  protected String fieldValue = "";

  /** For use with fields of type `number`. */
  protected OptionalLong fieldValueNumber = OptionalLong.empty();

  protected OptionalLong minValue = OptionalLong.empty();
  protected OptionalLong maxValue = OptionalLong.empty();
  private String tagType = "";

  private String formId = "";
  private String id = "";
  private String labelText = "";
  protected String placeholderText = "";
  private String screenReaderText = "";
  private Messages messages;
  private ImmutableSet<ValidationError> fieldErrors = ImmutableSet.of();
  private boolean showFieldErrors = true;
  private boolean checked = false;
  private boolean disabled = false;
  protected ImmutableList.Builder<String> referenceClassesBuilder = ImmutableList.builder();
  private ImmutableSet.Builder<String> attributesSetBuilder = ImmutableSet.builder();

  // Make all constructors protected
  protected FieldWithLabel() {}

  public static FieldWithLabel checkbox() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("checkbox");
  }

  public static FieldWithLabel currency() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("text").setIsCurrency();
  }

  public static FieldWithLabel radio() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("radio");
  }

  public static FieldWithLabel input() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("text");
  }

  public static FieldWithLabel number() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("number");
  }

  public static FieldWithLabel date() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("date");
  }

  public static FieldWithLabel textArea() {
    return new FieldWithLabel().setTagTypeTextarea().setFieldType("text");
  }

  public static FieldWithLabel email() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("email");
  }

  /** Add a reference class from {@link views.style.ReferenceClasses} to this element. */
  public FieldWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  public FieldWithLabel setChecked(boolean checked) {
    this.checked = checked;
    return this;
  }

  public FieldWithLabel setFieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  public FieldWithLabel setFieldType(String fieldType) {
    this.fieldType = fieldType;
    return this;
  }

  protected String getFieldType() {
    return this.fieldType;
  }

  public FieldWithLabel setFormId(String formId) {
    this.formId = formId;
    return this;
  }

  public FieldWithLabel setId(String inputId) {
    this.id = inputId;
    return this;
  }

  FieldWithLabel setIsCurrency() {
    // There is no HTML currency input so we identify these with a custom attribute.
    this.setAttribute("currency");
    return this;
  }

  public FieldWithLabel setLabelText(String labelText) {
    this.labelText = labelText;
    return this;
  }

  public FieldWithLabel setPlaceholderText(String placeholder) {
    this.placeholderText = placeholder;
    return this;
  }

  /** Sets a valueless attribute. */
  public FieldWithLabel setAttribute(String attribute) {
    this.attributesSetBuilder.add(attribute);
    return this;
  }

  public FieldWithLabel setMin(OptionalLong value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong min value is only available on fields of type 'number'");
    }
    this.minValue = value;
    return this;
  }

  public FieldWithLabel setMax(OptionalLong value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong max value is only available on fields of type 'number'");
    }

    this.maxValue = value;
    return this;
  }

  public FieldWithLabel setValue(String value) {
    if (!STRING_TYPES.contains(getFieldType())) {
      throw new RuntimeException(
          String.format(
              "setting a String value is not available on fields of type `%s`", this.fieldType));
    }

    this.fieldValue = value;
    return this;
  }

  public FieldWithLabel setValue(Optional<String> value) {
    if (!STRING_TYPES.contains(getFieldType())) {
      throw new RuntimeException(
          "setting a String value is not available on fields of type 'number'");
    }
    value.ifPresent(s -> this.fieldValue = s);
    return this;
  }

  public FieldWithLabel setValue(OptionalInt value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalInt value is only available on fields of type `number`");
    }

    this.fieldValueNumber =
        value.isPresent() ? OptionalLong.of(value.getAsInt()) : OptionalLong.empty();
    return this;
  }

  public FieldWithLabel setValue(OptionalLong value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong value is only available on fields of type `number`");
    }

    this.fieldValueNumber = value;
    return this;
  }

  public FieldWithLabel setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public FieldWithLabel setScreenReaderText(String screenReaderText) {
    this.screenReaderText = screenReaderText;
    return this;
  }

  public FieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    this.messages = messages;
    this.fieldErrors =
        errors.stream()
            .map(
                (ValidationErrorMessage vem) ->
                    new ValidationError(vem.key().getKeyName(), vem.key().getKeyName(), vem.args()))
            .collect(ImmutableSet.toImmutableSet());

    return this;
  }

  public FieldWithLabel setFieldErrors(Messages messages, ValidationError error) {
    this.messages = messages;
    this.fieldErrors = ImmutableSet.of(error);

    return this;
  }

  public FieldWithLabel showFieldErrors(boolean showFieldErrors) {
    this.showFieldErrors = showFieldErrors;
    return this;
  }

  public DivTag getContainer() {
    genRandIdIfEmpty();
    if (isTagTypeTextarea()) {
      TextareaTag textareaFieldTag = TagCreator.textarea();
      applyAttributesFromSet(textareaFieldTag);
      textareaFieldTag.withText(this.fieldValue);
      textareaFieldTag.withCondPlaceholder(!Strings.isNullOrEmpty(this.placeholderText), this.placeholderText);
      return applyAttrsAndGenLabel(textareaFieldTag);
    } else {
      InputTag inputFieldTag = TagCreator.input();
      inputFieldTag.withType(getFieldType());
      applyAttributesFromSet(inputFieldTag);
      if (this.fieldType.equals("number")) {
        numberTagApplyAttrs(inputFieldTag);
      } else {
        inputFieldTag.withValue(this.fieldValue);
      }
      inputFieldTag.withCondForm(!Strings.isNullOrEmpty(this.formId), this.formId);
      inputFieldTag.withCondPlaceholder(!Strings.isNullOrEmpty(this.placeholderText), this.placeholderText);
      return applyAttrsAndGenLabel(inputFieldTag);
    }
  }

  protected void applyAttributesFromSet(Tag fieldTag) {
    this.attributesSetBuilder.build().forEach(attr -> fieldTag.attr(attr, null));
  }

  private DivTag buildFieldErrorsTag(String id) {
    String[] referenceClasses =
        referenceClassesBuilder.build().stream().map(ref -> ref + "-error").toArray(String[]::new);
    return div(each(fieldErrors, error -> div(error.format(messages))))
        .withId(id)
        .withClasses(
            StyleUtils.joinStyles(referenceClasses),
            StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_XS, Styles.P_1),
            fieldErrors.isEmpty() || !showFieldErrors ? Styles.HIDDEN : "");
  }

  protected void genRandIdIfEmpty() {
    // In order for the labels to be associated with the fields (mandatory for screen readers)
    // we need an id.  Generate a reasonable one if none is provided.
    if (this.id.isEmpty()) {
      this.id = RandomStringUtils.randomAlphabetic(8);
    }
  }

  private LabelTag genLabelTag() {

    return label()
        .withFor(this.id)
        // If the text is screen-reader text, then we want the label to be screen-reader
        // only.
        .withClass(labelText.isEmpty() ? Styles.SR_ONLY : BaseStyles.INPUT_LABEL)
        .withText(labelText.isEmpty() ? screenReaderText : labelText);
  }

  private DivTag buildBaseContainer(Tag fieldTag, Tag labelTag, String fieldErrorsId) {
    return div(
            labelTag,
            div(fieldTag, buildFieldErrorsTag(fieldErrorsId))
                .withClasses(Styles.FLEX, Styles.FLEX_COL))
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM);
  }

  private boolean hasFieldErrors() {
    return !fieldErrors.isEmpty() && showFieldErrors;
  }

  protected void numberTagApplyAttrs(InputTag fieldTag) {
    // Setting inputmode to decimal gives iOS users a more accessible keyboard
    fieldTag.attr("inputmode", "decimal");

    // Setting step to any disables the built-in HTML validation so we can use our
    // custom validation message to enforce integers.
    fieldTag.withStep("any");

    // Set min and max values for client-side validation
    if (this.minValue.isPresent()) {
      fieldTag.attr("min", minValue.getAsLong());
    }
    if (this.maxValue.isPresent()) {
      fieldTag.attr("max", maxValue.getAsLong());
    }

    // For number types, only set the value if it's present since there is no empty string
    // equivalent for numbers.
    if (this.fieldValueNumber.isPresent()) {
      fieldTag.withValue(String.valueOf(this.fieldValueNumber.getAsLong()));
    }
  }

  private <T extends Tag<T> & IName<T> & IDisabled<T>> void generalApplyAttrsClassesToTag(
      T fieldTag, boolean hasFieldErrors) {
    fieldTag
        .withClasses(
            StyleUtils.joinStyles(
                BaseStyles.INPUT, hasFieldErrors ? BaseStyles.FORM_FIELD_ERROR_BORDER_COLOR : ""))
        .withId(this.id)
        .withName(this.fieldName)
        .withCondDisabled(this.disabled);
  }

  protected <T extends Tag<T> & IName<T> & IDisabled<T>> DivTag applyAttrsAndGenLabel(T fieldTag) {
    String fieldErrorsId = String.format("%s-errors", this.id);
    boolean hasFieldErrors = hasFieldErrors();
    if (hasFieldErrors) {
      fieldTag.attr("aria-invalid", "true");
      fieldTag.attr("aria-describedBy", fieldErrorsId);
    }

    generalApplyAttrsClassesToTag(fieldTag, hasFieldErrors);

    if (getFieldType().equals("checkbox") || getFieldType().equals("radio")) {
      return getCheckboxContainer(fieldTag);
    }

    LabelTag labelTag = genLabelTag();

    return buildBaseContainer(fieldTag, labelTag, fieldErrorsId);
  }

  /**
   * Swaps the order of the label and field, adds different styles, and possibly adds "checked"
   * attribute.
   */
  private DivTag getCheckboxContainer(Tag fieldTag) {
    if (this.checked) {
      fieldTag.attr("checked");
    }

    return div()
        .with(
            label()
                .withClasses(
                    StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
                    BaseStyles.CHECKBOX_LABEL,
                    BaseStyles.FORM_FIELD_MARGIN_BOTTOM,
                    labelText.isEmpty() ? Styles.W_MIN : "")
                .withCondFor(!this.id.isEmpty(), this.id)
                .with(fieldTag.withClasses(BaseStyles.CHECKBOX))
                .withText(this.labelText));
  }

  private FieldWithLabel setTagTypeInput() {
    tagType = "input";
    return this;
  }

  private FieldWithLabel setTagTypeTextarea() {
    tagType = "textarea";
    return this;
  }

  protected boolean isTagTypeInput() {
    return tagType.equals("input");
  }

  protected boolean isTagTypeTextarea() {
    return tagType.equals("textarea");
  }
}
