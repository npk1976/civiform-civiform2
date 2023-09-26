package views.components;

import static j2html.TagCreator.rawHtml;

import com.google.common.collect.ImmutableList;
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

  /** Passes provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatText(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator) {

    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();

    if (preserveEmptyLines) {
      text = text.replaceAll("\n", "<br/>");
    }

    String markdownText = CIVIFORM_MARKDOWN.render(text);
    markdownText = addIconToLinks(markdownText);
    if (addRequiredIndicator) {
      markdownText = addRequiredIndicatorInsidePTag(markdownText);
    }

    builder.add(rawHtml(sanitizeHtml(markdownText)));
    return builder.build();
  }

  private static String addIconToLinks(String markdownText) {
    String closingATag = "</a>";
    String svgIconString =
        Icons.svg(Icons.OPEN_IN_NEW)
            .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top")
            .toString();
    return markdownText.replaceAll(closingATag, svgIconString + closingATag);
  }

  private static String addRequiredIndicatorInsidePTag(String markdownText) {
    int indexOfPTag = markdownText.lastIndexOf("</p>");
    String stringWithRequiredIndicator = ViewUtils.requiredQuestionIndicator().toString() + "</p>";
    return markdownText.substring(0, indexOfPTag) + stringWithRequiredIndicator;
  }

  private static String sanitizeHtml(String markdownText) {
    PolicyFactory customPolicy =
        new HtmlPolicyBuilder()
            .allowElements(
                "p", "div", "h2", "h3", "h4", "h5", "h6", "a", "ul", "li", "span", "svg", "br",
                "em", "strong", "code", "path", "pre")
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
            .allowAttributes("class", "target")
            .globally()
            .toFactory();

    PolicyFactory policy = customPolicy.and(Sanitizers.LINKS);
    return policy.sanitize(markdownText, buildHtmlChangeListener(), /*context=*/ null);
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
          logger.warn(
              String.format("HTML attribute: \"%s\" was caught and discarded.", attribute));
        }
      }
    };
  }
}
