package dev.aether.ui;

import dev.aether.ui.settings.Setting;
import dev.aether.ui.settings.SettingType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class SimpleConfigModel {
    private static final Set<String> OPERATIONAL_SECTIONS =
            Set.of("farming", "other", "failsafes");
    private static final EnumSet<SettingType> SUPPORTED_TYPES = EnumSet.of(
            SettingType.TOGGLE,
            SettingType.SLIDER,
            SettingType.RANGE_SLIDER,
            SettingType.TEXT,
            SettingType.INFO,
            SettingType.LIST,
            SettingType.DROPDOWN,
            SettingType.DROPDOWN_LIST,
            SettingType.ACTION,
            SettingType.POSITION);

    private SimpleConfigModel() {
    }

    static List<MainGUIRegistry.ModuleSection> operationalSections(
            List<MainGUIRegistry.ModuleSection> sections) {
        return sections.stream()
                .filter(section -> OPERATIONAL_SECTIONS.contains(section.id()))
                .toList();
    }

    static boolean supports(SettingType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    static List<Setting> operationalSettings(List<Setting> settings) {
        return settings.stream()
                .filter(setting -> supports(setting.getType()))
                .toList();
    }

    static double normalized(float value, float min, float max) {
        if (max <= min) {
            return 0;
        }
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    static float denormalized(double value, float min, float max) {
        double clamped = Math.max(0, Math.min(1, value));
        return (float) (min + clamped * (max - min));
    }

    static List<String> move(List<String> values, int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= values.size() || target < 0 || target >= values.size()) {
            return List.copyOf(values);
        }
        java.util.Collections.swap(values, index, target);
        return List.copyOf(values);
    }
}
