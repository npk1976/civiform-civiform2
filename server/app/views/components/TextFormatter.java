package views.components;

import static j2html.TagCreator.rawHtml;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import j2html.tags.DomContent;
import java.util.List;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.CiviFormMarkdown;
import views.ViewUtils;

/** The TextFormatter class formats text using Markdown and some custom logic. */
public final class TextFormatter {

  private static final Logger logger = LoggerFactory.getLogger(TextFormatter.class);
  private static final CiviFormMarkdown CIVIFORM_MARKDOWN = new CiviFormMarkdown();
  public static final String DEFAULT_ARIA_LABEL = "opens in a new tab";

  /** Adds an aria label to links before passing provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatTextWithAriaLabel(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator, String ariaLabel) {
    CIVIFORM_MARKDOWN.setAriaLabel(ariaLabel);
    return formatText(text, preserveEmptyLines, addRequiredIndicator);
  }

  /** Passes provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatText(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator) {
    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();
    builder.add(rawHtml(formatTextToSanitizedHTML(text, preserveEmptyLines, addRequiredIndicator)));
    return builder.build();
  }

  public static String formatTextToSanitizedHTMLWithAriaLabel(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator, String ariaLabel) {
    CIVIFORM_MARKDOWN.setAriaLabel(ariaLabel);
    return formatTextToSanitizedHTML(text, preserveEmptyLines, addRequiredIndicator);
  }

  /** Passes provided text through Markdown formatter, generating an HTML String */
  public static String formatTextToSanitizedHTML(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator) {
    if (preserveEmptyLines) {
      text = preserveEmptyLines(text);
    }

    String markdownText = CIVIFORM_MARKDOWN.render(text);
    markdownText = addIconToLinks(markdownText);
    markdownText = addTextSize(markdownText);
    if (addRequiredIndicator) {
      markdownText = addRequiredIndicator(markdownText);
    }

    return sanitizeHtml(markdownText);
  }

  /** Used for testing */
  public static void resetAriaLabelToDefault() {
    CIVIFORM_MARKDOWN.setAriaLabel(DEFAULT_ARIA_LABEL);
  }

  private static String preserveEmptyLines(String text) {
    String[] lines = Iterables.toArray(Splitter.on("\n").split(text), String.class);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isBlank()) {
        // Add an empty space character so the blank line is preserved
        lines[i] = "&nbsp;\n";
      }
    }
    return String.join("\n", lines);
  }

  private static String addIconToLinks(String markdownText) {
    String closingATag = "</a>";
    String svgIconString =
        Icons.svg(Icons.OPEN_IN_NEW)
            .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top")
            .toString();
    return markdownText.replaceAll(closingATag, svgIconString + closingATag);
  }

  private static String addTextSize(String markdownText) {
    // h1 and h2 tags are set to "text-2xl" and "text-xl" respectively in styles.css
    String replacedH3Tags = markdownText.replaceAll("<h3>", "<h3 class=\"text-lg\">");
    return replacedH3Tags.replaceAll("<h4>", "<h4 class=\"text-base\">");
  }

  private static String addRequiredIndicator(String markdownText) {
    int indexOfClosingTag = markdownText.lastIndexOf("</");
    String insertTextAfterRequiredIndicator = markdownText.substring(indexOfClosingTag);
    // For required questions that end in a list, we want the required indicator to show up at
    // the end of the paragraph that precedes the list
    if (insertTextAfterRequiredIndicator.contains("ul")) {
      int indexOfOpeningUlTag = markdownText.lastIndexOf("<ul");
      String substringWithoutList = markdownText.substring(0, indexOfOpeningUlTag);
      indexOfClosingTag = substringWithoutList.lastIndexOf("</");
      insertTextAfterRequiredIndicator = markdownText.substring(indexOfClosingTag);
    }
    String markdownWithRequiredIndicator =
        ViewUtils.requiredQuestionIndicator().toString().concat(insertTextAfterRequiredIndicator);
    return markdownText.substring(0, indexOfClosingTag) + markdownWithRequiredIndicator;
  }

  private static String sanitizeHtml(String markdownText) {
    PolicyFactory customPolicy =
        new HtmlPolicyBuilder()
            .allowElements(
                "p", "div", "h2", "h3", "h4", "h5", "h6", "a", "ul", "ol", "li", "hr", "span",
                "svg", "br", "em", "strong", "code", "path", "pre")
            // Per accessibility best practices, we want to disallow adding h1 headers to
            // ensure the page does not have more than one h1 header
            // https://www.a11yproject.com/posts/how-to-accessible-heading-structure/
            // This logic changes h1 headers to h2 headers which are still larger than the default
            // text
            .allowElements(
                (String elementName, List<String> attrs) -> {
                  return "h2";
                },
                "h1")
            .allowWithoutAttributes()
            .allowAttributes(
                "class",
                "target",
                "xmlns",
                "fill",
                "stroke",
                "stroke-width",
                "aria-label",
                "aria-hidden",
                "viewBox", // <--- This is for SVGs and it **IS** case-sensitive
                "d")
            .globally()
            .toFactory();

    PolicyFactory policy = customPolicy.and(Sanitizers.LINKS);
    return policy.sanitize(markdownText, buildHtmlChangeListener(), /* context= */ null);
  }

  private static HtmlChangeListener<Object> buildHtmlChangeListener() {
    return new HtmlChangeListener<Object>() {
      @Override
      public void discardedTag(Object ctx, String elementName) {
        logger.warn(String.format("HTML element: \"%s\" was caught and discarded.", elementName));
      }

      @Override
      public void discardedAttributes(Object ctx, String tagName, String... attributeNames) {
        for (String attribute : attributeNames) {
          logger.warn(String.format("HTML attribute: \"%s\" was caught and discarded.", attribute));
        }
      }
    };
  }
}
