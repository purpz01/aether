package dev.aether.ui;

import dev.aether.bootstrap.AetherUiActions;
import dev.aether.macro.MacroStateManager;
import dev.aether.ui.settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SimpleConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_CONTENT_WIDTH = 560;

    private final SimpleConfigNavigation navigation = new SimpleConfigNavigation();
    private final Map<EditBox, Runnable> editorCommits = new IdentityHashMap<>();
    private int scrollOffset;
    private int maxScroll;
    private int itemCount;
    private boolean macroStopped;

    public SimpleConfigScreen() {
        super(Component.literal("Aether Simple Config"));
    }

    static void toggle(ToggleSetting setting) {
        setting.toggle();
    }

    static void applySlider(SliderSetting setting, double normalized) {
        setting.setValue(SimpleConfigModel.denormalized(
                normalized, setting.getMin(), setting.getMax()));
    }

    @Override
    protected void init() {
        editorCommits.clear();
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

        List<SettingsRow> rows = settingsRows(subTab);
        int visibleRows = preparePage(rows.size());
        int contentWidth = contentWidth();
        int x = (width - contentWidth) / 2;
        int end = Math.min(rows.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            int y = HEADER_HEIGHT + (index - scrollOffset) * ROW_HEIGHT;
            addSettingRow(rows.get(index), x, y, contentWidth);
        }
    }

    private List<SettingsRow> settingsRows(ModulesTab.SubTab subTab) {
        List<SettingsRow> rows = new ArrayList<>();
        if (subTab.hasToggle()) {
            rows.add(new SettingsRow(RowKind.MODULE, null, null));
            if (!subTab.isEnabled()) {
                return rows;
            }
        }

        for (SettingGroup group : subTab.groups()) {
            rows.add(new SettingsRow(RowKind.GROUP, group, null));
            if (!group.isAlwaysOn() && !group.isEnabled()) {
                continue;
            }
            for (Setting setting : group.getSettings()) {
                if (!setting.isVisible()) {
                    continue;
                }
                if (setting.getType() == SettingType.RANGE_SLIDER) {
                    rows.add(new SettingsRow(RowKind.RANGE_LOWER, group, setting));
                    rows.add(new SettingsRow(RowKind.RANGE_UPPER, group, setting));
                } else if (setting.getType() == SettingType.POSITION) {
                    rows.add(new SettingsRow(RowKind.POSITION_X, group, setting));
                    rows.add(new SettingsRow(RowKind.POSITION_Y, group, setting));
                    rows.add(new SettingsRow(RowKind.POSITION_Z, group, setting));
                    rows.add(new SettingsRow(RowKind.POSITION_ACTIONS, group, setting));
                } else {
                    rows.add(new SettingsRow(RowKind.SETTING, group, setting));
                }
            }
        }
        return rows;
    }

    private void addSettingRow(SettingsRow row, int x, int y, int rowWidth) {
        ModulesTab.SubTab subTab = navigation.selectedSubTab();
        switch (row.kind()) {
            case MODULE -> addToggleButton(subTab.name(), subTab.isEnabled(), x, y, rowWidth, () -> {
                subTab.toggle();
                refreshSettings();
            });
            case GROUP -> addGroupButton(row.group(), x, y, rowWidth);
            case RANGE_LOWER -> addRangeSlider((RangeSliderSetting) row.setting(), true, x, y, rowWidth);
            case RANGE_UPPER -> addRangeSlider((RangeSliderSetting) row.setting(), false, x, y, rowWidth);
            case POSITION_X -> addPositionField((PositionSetting) row.setting(), 'X', x, y, rowWidth);
            case POSITION_Y -> addPositionField((PositionSetting) row.setting(), 'Y', x, y, rowWidth);
            case POSITION_Z -> addPositionField((PositionSetting) row.setting(), 'Z', x, y, rowWidth);
            case POSITION_ACTIONS -> addPositionActions((PositionSetting) row.setting(), x, y, rowWidth);
            case SETTING -> addSetting(row.setting(), x, y, rowWidth);
        }
    }

    private void addGroupButton(SettingGroup group, int x, int y, int rowWidth) {
        if (group.isAlwaysOn()) {
            addInactiveButton(group.getName(), x, y, rowWidth);
            return;
        }
        addToggleButton(group.getName(), group.isEnabled(), x, y, rowWidth, () -> {
            group.toggle();
            refreshSettings();
        });
    }

    private void addSetting(Setting setting, int x, int y, int rowWidth) {
        if (!SimpleConfigModel.supports(setting.getType())) {
            addInactiveButton(setting.getName() + ": Not supported in Simple GUI", x, y, rowWidth);
            return;
        }

        switch (setting.getType()) {
            case TOGGLE -> addToggle((ToggleSetting) setting, x, y, rowWidth);
            case SLIDER -> addSlider((SliderSetting) setting, x, y, rowWidth);
            case TEXT -> addText((TextSetting) setting, x, y, rowWidth);
            case DROPDOWN -> addDropdown((DropdownSetting) setting, x, y, rowWidth);
            case LIST -> addStringList((ListSetting) setting, x, y, rowWidth);
            case DROPDOWN_LIST -> addDropdownList((DropdownListSetting) setting, x, y, rowWidth);
            case ACTION -> addAction((ActionSetting) setting, x, y, rowWidth);
            case INFO -> addInfo((InfoSetting) setting, x, y, rowWidth);
            default -> addInactiveButton(setting.getName() + ": Not supported in Simple GUI", x, y, rowWidth);
        }
    }

    private void addToggle(ToggleSetting setting, int x, int y, int rowWidth) {
        addToggleButton(setting.getName(), setting.getValue(), x, y, rowWidth, () -> {
            toggle(setting);
            refreshSettings();
        });
    }

    private void addToggleButton(String name, boolean value, int x, int y, int rowWidth, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(name + ": " + (value ? "ON" : "OFF")), button -> action.run())
                .bounds(x, y, rowWidth, 20)
                .build());
    }

    private void addSlider(SliderSetting setting, int x, int y, int rowWidth) {
        addRenderableWidget(new SimpleSliderButton(
                x, y, rowWidth, setting.getName(), setting.getMin(), setting.getMax(),
                setting.getValue(), setting.getDecimals(), setting.getSuffix(), setting::setValue));
    }

    private void addRangeSlider(RangeSliderSetting setting, boolean lower, int x, int y, int rowWidth) {
        String side = lower ? " min" : " max";
        float current = lower ? setting.getLowerValue() : setting.getUpperValue();
        Consumer<Float> setter = lower ? setting::setLowerValue : setting::setUpperValue;
        addRenderableWidget(new SimpleSliderButton(
                x, y, rowWidth, setting.getName() + side, setting.getMin(), setting.getMax(),
                current, setting.getDecimals(), setting.getSuffix(), setter));
    }

    private void addText(TextSetting setting, int x, int y, int rowWidth) {
        int labelWidth = Math.min(180, rowWidth / 3);
        addInactiveButton(setting.getName(), x, y, labelWidth - 4);
        EditBox field = new EditBox(font, x + labelWidth, y, rowWidth - labelWidth, 20,
                Component.literal(setting.getName()));
        field.setMaxLength(setting.isMultiline() ? 8192 : 2048);
        field.setValue(setting.getValue());
        field.setHint(Component.literal(setting.getPlaceholder()));
        addRenderableWidget(field);
        editorCommits.put(field, () -> {
            if (!Objects.equals(field.getValue(), setting.getValue())) {
                setting.setValue(field.getValue());
            }
        });
    }

    private void addDropdown(DropdownSetting setting, int x, int y, int rowWidth) {
        List<String> options = setting.getOptions();
        if (options.isEmpty()) {
            addInactiveButton(setting.getName() + ": No options", x, y, rowWidth);
            return;
        }
        addRenderableWidget(Button.builder(
                        Component.literal(setting.getName() + ": " + setting.getSelectedOption()), button -> {
                            int current = Math.max(0, setting.getSelectedIndex());
                            setting.setSelectedIndex((current + 1) % options.size());
                            refreshSettings();
                        })
                .bounds(x, y, rowWidth, 20)
                .build());
    }

    private void addStringList(ListSetting setting, int x, int y, int rowWidth) {
        addRenderableWidget(Button.builder(
                        Component.literal(setting.getName() + " (" + setting.getValues().size() + ")"), button -> {
                            commitEditors();
                            minecraft.setScreen(SimpleStringListScreen.forList(this, setting));
                        })
                .bounds(x, y, rowWidth, 20)
                .build());
    }

    private void addDropdownList(DropdownListSetting setting, int x, int y, int rowWidth) {
        addRenderableWidget(Button.builder(
                        Component.literal(setting.getName() + " (" + setting.getValues().size() + ")"), button -> {
                            commitEditors();
                            minecraft.setScreen(SimpleStringListScreen.forDropdownList(this, setting));
                        })
                .bounds(x, y, rowWidth, 20)
                .build());
    }

    private void addAction(ActionSetting setting, int x, int y, int rowWidth) {
        addRenderableWidget(Button.builder(Component.literal(setting.getName()), button -> {
                    setting.execute();
                    refreshSettings();
                })
                .bounds(x, y, rowWidth, 20)
                .build());
    }

    private void addInfo(InfoSetting setting, int x, int y, int rowWidth) {
        addInactiveButton(setting.getName() + ": " + setting.getValue(), x, y, rowWidth);
    }

    private void addPositionField(PositionSetting setting, char axis, int x, int y, int rowWidth) {
        int labelWidth = Math.min(180, rowWidth / 3);
        String label = setting.getName() + " " + axis;
        addInactiveButton(label, x, y, labelWidth - 4);
        double value = switch (axis) {
            case 'X' -> setting.getX();
            case 'Y' -> setting.getY();
            default -> setting.getZ();
        };
        EditBox field = new EditBox(font, x + labelWidth, y, rowWidth - labelWidth, 20,
                Component.literal(label));
        field.setMaxLength(64);
        field.setValue(formatCoordinate(value));
        addRenderableWidget(field);
        editorCommits.put(field, () -> {
            try {
                double parsed = Double.parseDouble(field.getValue().trim());
                switch (axis) {
                    case 'X' -> setting.setX(parsed);
                    case 'Y' -> setting.setY(parsed);
                    default -> setting.setZ(parsed);
                }
            } catch (NumberFormatException ignored) {
                field.setValue(formatCoordinate(value));
            }
        });
    }

    private void addPositionActions(PositionSetting setting, int x, int y, int rowWidth) {
        int half = (rowWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal(setting.getName() + ": Capture"), button -> {
                    setting.capture();
                    refreshSettings();
                })
                .bounds(x, y, half, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.literal("Highlight: " + (setting.isHighlighted() ? "ON" : "OFF")), button -> {
                            setting.setHighlighted(!setting.isHighlighted());
                            refreshSettings();
                        })
                .bounds(x + half + 4, y, rowWidth - half - 4, 20)
                .build());
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void addInactiveButton(String message, int x, int y, int buttonWidth) {
        Button button = Button.builder(Component.literal(message), ignored -> {
                })
                .bounds(x, y, buttonWidth, 20)
                .build();
        button.active = false;
        addRenderableWidget(button);
    }

    private <T> void addPagedButtons(List<T> items, Function<T, String> label, Consumer<T> action) {
        int visibleRows = preparePage(items.size());
        int contentWidth = Math.min(360, contentWidth());
        int x = (width - contentWidth) / 2;
        int end = Math.min(items.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            T item = items.get(index);
            int y = HEADER_HEIGHT + (index - scrollOffset) * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal(label.apply(item)), button -> action.accept(item))
                    .bounds(x, y, contentWidth, 20)
                    .build());
        }
    }

    private int preparePage(int size) {
        itemCount = size;
        int visibleRows = rowsPerPage();
        maxScroll = Math.max(0, size - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return visibleRows;
    }

    private int contentWidth() {
        return Math.min(MAX_CONTENT_WIDTH, Math.max(160, width - 40));
    }

    private int rowsPerPage() {
        return Math.max(1, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
    }

    private void buildFooter() {
        int y = height - 26;
        if (navigation.level() != SimpleConfigNavigation.Level.SECTIONS) {
            addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
                commitEditors();
                navigation.back();
                openCurrentPage();
            }).bounds(10, y, 70, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds((width - 70) / 2, y, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Use Custom GUI"), button -> {
                    commitEditors();
                    AetherUiActions.setSimpleConfigGui(false);
                })
                .bounds(Math.max(90, width - 130), y, 120, 20)
                .build());
    }

    private void openCurrentPage() {
        commitEditors();
        scrollOffset = 0;
        rebuildWidgets();
    }

    private void refreshSettings() {
        commitEditors();
        rebuildWidgets();
    }

    private void commitEditors() {
        List.copyOf(editorCommits.values()).forEach(Runnable::run);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        List<EditBox> focused = editorCommits.keySet().stream()
                .filter(EditBox::isFocused)
                .toList();
        boolean handled = super.mouseClicked(click, doubled);
        focused.stream()
                .filter(field -> !field.isFocused())
                .map(editorCommits::get)
                .filter(Objects::nonNull)
                .forEach(Runnable::run);
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Math.max(0, Math.min(maxScroll,
                scrollOffset - (int) Math.signum(scrollY)));
        if (next != scrollOffset) {
            commitEditors();
            scrollOffset = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        boolean handled = super.keyPressed(input);
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            commitEditors();
            rebuildWidgets();
            return true;
        }
        return handled;
    }

    @Override
    public void onClose() {
        commitEditors();
        super.onClose();
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

    private enum RowKind {
        MODULE,
        GROUP,
        SETTING,
        RANGE_LOWER,
        RANGE_UPPER,
        POSITION_X,
        POSITION_Y,
        POSITION_Z,
        POSITION_ACTIONS
    }

    private record SettingsRow(RowKind kind, SettingGroup group, Setting setting) {
    }

    private static final class SimpleSliderButton extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final int decimals;
        private final String suffix;
        private final Consumer<Float> setter;

        private SimpleSliderButton(int x, int y, int width, String label,
                                   float min, float max, float current,
                                   int decimals, String suffix, Consumer<Float> setter) {
            super(x, y, width, 20, Component.empty(),
                    SimpleConfigModel.normalized(current, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.suffix = suffix;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float current = SimpleConfigModel.denormalized(value, min, max);
            setMessage(Component.literal(label + ": "
                    + String.format(Locale.ROOT, "%." + decimals + "f", current) + suffix));
        }

        @Override
        protected void applyValue() {
            setter.accept(SimpleConfigModel.denormalized(value, min, max));
        }
    }
}
