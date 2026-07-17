package dev.aether.ui;

import dev.aether.ui.settings.ModulesTab;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SimpleConfigNavigationTest {
    @Test
    void navigatesSectionModuleAndBackWithoutRendering() {
        var navigation = new SimpleConfigNavigation();
        var subTab = new ModulesTab.SubTab("Farming Macro", "", List.of());
        var section = new MainGUIRegistry.ModuleSection("farming", "Farming", List.of(subTab));

        navigation.selectSection(section);
        assertEquals(SimpleConfigNavigation.Level.MODULES, navigation.level());
        navigation.selectSubTab(subTab);
        assertEquals(SimpleConfigNavigation.Level.SETTINGS, navigation.level());
        navigation.back();
        assertEquals(SimpleConfigNavigation.Level.MODULES, navigation.level());
        assertNull(navigation.selectedSubTab());
        navigation.back();
        assertEquals(SimpleConfigNavigation.Level.SECTIONS, navigation.level());
        assertNull(navigation.selectedSection());
    }
}
