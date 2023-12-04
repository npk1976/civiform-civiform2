package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.ScriptTag;
import j2html.tags.specialized.DivTag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import services.DateConverter;
import views.style.StyleUtils;
import java.util.Optional;

public class ViewUtilsTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock public AssetsFinder assetsFinder;

  @Mock public DateConverter dateConverter;

  public ViewUtils viewUtils;

  @Before
  public void setUp() {
    viewUtils = new ViewUtils(assetsFinder, dateConverter);
  }

  @Test
  public void makeLocalJsTag_createsAScriptTagWithTheJs() {
    when(assetsFinder.path("hello.js")).thenReturn("/full/asset/path.js");
    ScriptTag result = viewUtils.makeLocalJsTag("hello");

    assertThat(result.render())
        .isEqualTo("<script src=\"/full/asset/path.js\" type=\"text/javascript\"></script>");
  }

  @Test
  public void makeLocalCssTag_createsALinkTagWithTheCss() {
    when(assetsFinder.path("stylesheets/hello.css")).thenReturn("/full/asset/path.css");
    LinkTag result = viewUtils.makeLocalCssTag("stylesheets/hello");

    assertThat(result.render())
        .isEqualTo("<link href=\"/full/asset/path.css\" rel=\"stylesheet\">");
  }

  @Test
  public void makeAlert_createsAlertComponentWithTheCorrectClasses() {
    DivTag alertComponent = ViewUtils.makeAlert("some text", StyleUtils.joinStyles(ViewUtils.ALERT_INFO, ViewUtils.ALERT_SLIM), Optional.of("title"));
    assertThat(alertComponent.render()).isEqualTo("<div class=\"usa-alert usa-alert--info usa-alert--slim\"><div class=\"usa-alert__body\"><h4 class=\"usa-alert__heading\">title</h4><p class=\"usa-alert__text\">some text</p></div></div>");
  }

  @Test
  public void makeAlert_doesNotIncludeTitleIfNoneIsPresent() {
    DivTag alertComponent = ViewUtils.makeAlert("some text", ViewUtils.ALERT_WARNING, Optional.empty());
    assertThat(alertComponent.render()).isEqualTo("<div class=\"usa-alert usa-alert--warning\"><div class=\"usa-alert__body\"><p class=\"usa-alert__text\">some text</p></div></div>");
  }
}
