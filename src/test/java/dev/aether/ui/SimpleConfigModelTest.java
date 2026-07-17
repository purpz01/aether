package dev.aether.ui;

import dev.aether.ui.settings.ColorSetting;
import dev.aether.ui.settings.SettingType;
import dev.aether.ui.settings.ToggleSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SimpleConfigModelTest {
    @Test
    void keepsOnlyOperationalSectionsInRegistryOrder() {
        var sections = List.of(
                new MainGUIRegistry.ModuleSection("visuals", "Visuals", List.of()),
                new MainGUIRegistry.ModuleSection("farming", "Farming", List.of()),
                new MainGUIRegistry.ModuleSection("other", "Other", List.of()),
                new MainGUIRegistry.ModuleSection("failsafes", "Failsafes", List.of()));

        assertEquals(List.of("farming", "other", "failsafes"),
                SimpleConfigModel.operationalSections(sections).stream()
                        .map(MainGUIRegistry.ModuleSection::id)
                        .toList());
    }

    @Test
    void supportsOperationalTypesAndRejectsCustomizationTypes() {
        assertTrue(SimpleConfigModel.supports(SettingType.TOGGLE));
        assertTrue(SimpleConfigModel.supports(SettingType.RANGE_SLIDER));
        assertTrue(SimpleConfigModel.supports(SettingType.POSITION));
        assertFalse(SimpleConfigModel.supports(SettingType.COLOR));
        assertFalse(SimpleConfigModel.supports(SettingType.MULTI_DROPDOWN));
        assertFalse(SimpleConfigModel.supports(SettingType.KEYBIND));
    }

    @Test
    void removesCustomizationSettingsFromOperationalRows() {
        var toggle = new ToggleSetting("Macro toggle", () -> false, ignored -> {});
        var color = new ColorSetting("Flash color", () -> 0, ignored -> {});

        assertEquals(List.of(toggle), SimpleConfigModel.operationalSettings(List.of(toggle, color)));
    }

    @Test
    void convertsSliderValuesAndClampsOutOfRangeInput() {
        assertEquals(0.5, SimpleConfigModel.normalized(0, -10, 10), 0.0001);
        assertEquals(10, SimpleConfigModel.denormalized(2, -10, 10), 0.0001);
        assertEquals(-10, SimpleConfigModel.denormalized(-1, -10, 10), 0.0001);
    }

    @Test
    void movesListValuesWithoutLosingEntries() {
        var values = new ArrayList<>(List.of("a", "b", "c"));
        assertEquals(List.of("b", "a", "c"), SimpleConfigModel.move(values, 1, -1));
        assertEquals(List.of("b", "a", "c"), SimpleConfigModel.move(values, 0, -1));
    }
}
