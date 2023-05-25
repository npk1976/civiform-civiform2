package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;

public final class SettingsManifest extends AbstractSettingsManifest {

  private final ImmutableMap<String, SettingsSection> settingsSections;

  public SettingsManifest(ImmutableMap<String, SettingsSection> settingsSections) {
    this.settingsSections = checkNotNull(settingsSections);
  }

  @Override
  public ImmutableMap<String, SettingsSection> getSections() {
    return settingsSections;
  }
}
