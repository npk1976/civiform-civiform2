package views.components.buttons;

import views.style.BaseStyles;
import views.style.StyleUtils;

/**
 * A collection of styles for buttons. These are used by {@link ButtonStyle} to represent a limited
 * set of styles for CiviForm buttons
 */
// TODO(#4657): Remove redundant styles and simplify class.
// TODO(#4657): Make this class only used by ButtonStyle.java, and change visibility of class to
// package-private.
public final class ButtonStyles {
  /**
   * Base styles for buttons in the applicant UI. This is missing a specified text size, so that
   * should be added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles(
          "block", "py-2", "text-center", "rounded-full", "border", "border-transparent");

  /** Base styles for buttons with a solid background color. */
  private static final String BUTTON_BASE_SOLID =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white",
          "rounded-full",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.disabled("bg-gray-200", "text-gray-400"));

  /** Base styles for semibold buttons with a solid background. */
  private static final String BUTTON_BASE_SOLID_SEMIBOLD =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID, "font-semibold", "px-8");

  /** Base styles for buttons with a transparent background and an outline. */
  private static final String BUTTON_BASE_OUTLINE =
      StyleUtils.joinStyles(
          // Remove "border-transparent" so it doesn't conflict with "border-seattle-blue".
          StyleUtils.removeStyles(BUTTON_BASE, "border-transparent"),
          "bg-transparent",
          BaseStyles.TEXT_SEATTLE_BLUE,
          BaseStyles.BORDER_SEATTLE_BLUE,
          StyleUtils.hover("bg-blue-100"));

  private static final String BUTTON_BASE_OUTLINE_SEMIBOLD =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE, "font-semibold", "px-8");

  public static final String BUTTON_PROGRAM_APPLY =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-sm", "mx-auto");
  public static final String BUTTON_TI_DASHBOARD =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-xl");

  public static final String BUTTON_SUBMIT_APPLICATION =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-base", "mx-auto");
  public static final String BUTTON_ENUMERATOR_ADD_ENTITY =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID, "text-base", "normal-case", "font-normal", "px-4");
  public static final String BUTTON_ENUMERATOR_REMOVE_ENTITY =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE,
          "text-base",
          "normal-case",
          "font-normal",
          "justify-self-end",
          "self-center");
  public static final String SOLID_BLUE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-base");
  public static final String SOLID_WHITE =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_SEMIBOLD, "text-base");

  public static final String BUTTON_PROGRAMS_PAGE_WHITE =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE_SEMIBOLD, BaseStyles.BG_CIVIFORM_WHITE, "text-blue-900");
}
