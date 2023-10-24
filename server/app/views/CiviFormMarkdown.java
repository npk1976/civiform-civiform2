package views;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Renders markdown to HTML with styles consistent with CiviForm's UI. */
public final class CiviFormMarkdown {

  /** Renders markdown to HTML with styles consistent with CiviForm's UI. */
  public String render(String markdown) {
    Node markdownRootNode = PARSER.parse(markdown);
    return RENDERER.render(markdownRootNode);
  }

  private static final List<Extension> extensions = Arrays.asList(AutolinkExtension.create());

  private static final Parser PARSER = Parser.builder().extensions(extensions).build();

  private static final HtmlRenderer RENDERER =
      HtmlRenderer.builder()
          .extensions(extensions)
          .attributeProviderFactory(context -> new CiviFormAttributeProvider())
          .softbreak("<br/>")
          .build();

  /** Customizes HTML element attributes for the CiviForm UI. */
  private static class CiviFormAttributeProvider implements AttributeProvider {

    @Override
    public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
      if (node instanceof Link) {
        attributes.put(
            "class",
            StyleUtils.joinStyles(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT, "underline"));
        attributes.put("target", "_blank");
      } else if (node instanceof ListBlock) {
        attributes.put("class", "list-disc mx-8");
      }
    }
  }
}
