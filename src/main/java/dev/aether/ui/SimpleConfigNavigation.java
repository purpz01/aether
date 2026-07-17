package dev.aether.ui;

import dev.aether.ui.settings.ModulesTab;

final class SimpleConfigNavigation {
    enum Level {
        SECTIONS,
        MODULES,
        SETTINGS
    }

    private Level level = Level.SECTIONS;
    private MainGUIRegistry.ModuleSection selectedSection;
    private ModulesTab.SubTab selectedSubTab;

    Level level() {
        return level;
    }

    MainGUIRegistry.ModuleSection selectedSection() {
        return selectedSection;
    }

    ModulesTab.SubTab selectedSubTab() {
        return selectedSubTab;
    }

    void selectSection(MainGUIRegistry.ModuleSection section) {
        selectedSection = section;
        selectedSubTab = null;
        level = Level.MODULES;
    }

    void selectSubTab(ModulesTab.SubTab subTab) {
        selectedSubTab = subTab;
        level = Level.SETTINGS;
    }

    void back() {
        if (level == Level.SETTINGS) {
            selectedSubTab = null;
            level = Level.MODULES;
        } else if (level == Level.MODULES) {
            selectedSection = null;
            level = Level.SECTIONS;
        }
    }
}
