package dev.aether.ui;

import dev.aether.bootstrap.AetherUiActions;
import dev.aether.macro.MacroStateManager;
import dev.aether.ui.settings.ModulesTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SimpleConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_CONTENT_WIDTH = 360;

    private final SimpleConfigNavigation navigation = new SimpleConfigNavigation();
    private int scrollOffset;
    private int maxScroll;
    private int itemCount;
    private boolean macroStopped;

    public SimpleConfigScreen() {
        super(Component.literal("Aether Simple Config"));
    }

    @Override
    protected void init() {
        if (!macroStopped) {
            macroStopped = true;
            if (MacroStateManager.isMacroRunning()) {
                MacroStateManager.stopMacro(Minecraft.getInstance(), "SimpleConfigScreen opened", false);
            }
        }

        MainGUIRegistry.refresh();
        switch (navigation.level()) {
            case SECTIONS -> buildSections();
            case MODULES -> buildModules();
            case SETTINGS -> buildSettings();
        }
        buildFooter();
    }

    private void buildSections() {
        addPagedButtons(
                SimpleConfigModel.operationalSections(MainGUIRegistry.MODULE_SECTIONS),
                MainGUIRegistry.ModuleSection::displayName,
                section -> {
                    navigation.selectSection(section);
                    openCurrentPage();
                });
    }

    private void buildModules() {
        MainGUIRegistry.ModuleSection section = navigation.selectedSection();
        if (section == null) {
            navigation.back();
            openCurrentPage();
            return;
        }
        addPagedButtons(section.subtabs(), ModulesTab.SubTab::name, subTab -> {
            navigation.selectSubTab(subTab);
            openCurrentPage();
        });
    }

    private void buildSettings() {
        ModulesTab.SubTab subTab = navigation.selectedSubTab();
        if (subTab == null) {
            navigation.back();
            openCurrentPage();
            return;
        }
        addPagedButtons(subTab.groups(), group -> group.getName(), null);
    }

    private <T> void addPagedButtons(List<T> items, Function<T, String> label, Consumer<T> action) {
        itemCount = items.size();
        int visibleRows = rowsPerPage();
        maxScroll = Math.max(0, items.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(120, width - 40));
        int x = (width - contentWidth) / 2;
        int end = Math.min(items.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            T item = items.get(index);
            int y = HEADER_HEIGHT + (index - scrollOffset) * ROW_HEIGHT;
            Button row = Button.builder(Component.literal(label.apply(item)), button -> {
                        if (action != null) {
                            action.accept(item);
                        }
                    })
                    .bounds(x, y, contentWidth, 20)
                    .build();
            row.active = action != null;
            addRenderableWidget(row);
        }
    }

    private int rowsPerPage() {
        return Math.max(1, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
    }

    private void buildFooter() {
        int y = height - 26;
        if (navigation.level() != SimpleConfigNavigation.Level.SECTIONS) {
            addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
                navigation.back();
                openCurrentPage();
            }).bounds(10, y, 70, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds((width - 70) / 2, y, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Use Custom GUI"), button ->
                        AetherUiActions.setSimpleConfigGui(false))
                .bounds(Math.max(90, width - 130), y, 120, 20)
                .build());
    }

    private void openCurrentPage() {
        scrollOffset = 0;
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Math.max(0, Math.min(maxScroll,
                scrollOffset - (int) Math.signum(scrollY)));
        if (next != scrollOffset) {
            scrollOffset = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, Component.literal(pageTitle()), width / 2, 12, 0xFFFFFFFF);
        if (maxScroll > 0) {
            graphics.centeredText(font,
                    Component.literal((scrollOffset + 1) + "-"
                            + Math.min(itemCount, scrollOffset + rowsPerPage()) + " of " + itemCount),
                    width / 2, 26, 0xFFAAAAAA);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private String pageTitle() {
        return switch (navigation.level()) {
            case SECTIONS -> "Aether Simple Config";
            case MODULES -> navigation.selectedSection() == null
                    ? "Modules"
                    : navigation.selectedSection().displayName();
            case SETTINGS -> navigation.selectedSubTab() == null
                    ? "Settings"
                    : navigation.selectedSubTab().name();
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
