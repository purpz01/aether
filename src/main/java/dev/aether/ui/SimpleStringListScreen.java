package dev.aether.ui;

import dev.aether.ui.settings.DropdownListSetting;
import dev.aether.ui.settings.ListSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SimpleStringListScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final String placeholder;
    private final Values values;
    private final List<String> allowedOptions;
    private final Consumer<List<String>> setter;
    private int selectedIndex = -1;
    private int optionIndex;
    private int scrollOffset;
    private int maxScroll;
    private EditBox input;

    private SimpleStringListScreen(Screen parent, String title, String placeholder,
                                   List<String> initial, List<String> allowedOptions,
                                   Consumer<List<String>> setter) {
        super(Component.literal(title));
        this.parent = parent;
        this.placeholder = placeholder;
        this.values = new Values(initial);
        this.allowedOptions = List.copyOf(allowedOptions);
        this.setter = setter;
    }

    public static SimpleStringListScreen forList(Screen parent, ListSetting setting) {
        return new SimpleStringListScreen(parent, setting.getName(), setting.getPlaceholder(),
                setting.getValues(), List.of(), setting::setValues);
    }

    public static SimpleStringListScreen forDropdownList(
            Screen parent, DropdownListSetting setting) {
        return new SimpleStringListScreen(parent, setting.getName(), "Select option",
                setting.getValues(), setting.getAllOptions(), newValues -> {
                    List<String> current = setting.getValues();
                    for (int index = current.size() - 1; index >= 0; index--) {
                        setting.removeValue(index);
                    }
                    newValues.forEach(setting::addValue);
                });
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, Math.max(180, width - 20));
        int x = (width - contentWidth) / 2;
        int inputY = 34;

        if (allowedOptions.isEmpty()) {
            input = new EditBox(font, x, inputY, contentWidth - 74, 20, Component.literal(placeholder));
            input.setMaxLength(2048);
            input.setHint(Component.literal(placeholder));
            addRenderableWidget(input);
        } else {
            input = null;
            addRenderableWidget(Button.builder(Component.literal(selectedOption()), button -> {
                        optionIndex = (optionIndex + 1) % allowedOptions.size();
                        button.setMessage(Component.literal(selectedOption()));
                    })
                    .bounds(x, inputY, contentWidth - 74, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.literal("Add"), button -> addCurrent())
                .bounds(x + contentWidth - 70, inputY, 70, 20)
                .build());

        int listTop = 62;
        int visibleRows = rowsPerPage();
        maxScroll = Math.max(0, values.copy().size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int end = Math.min(values.copy().size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            int rowIndex = index;
            String prefix = index == selectedIndex ? "> " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + values.copy().get(index)), button -> {
                        selectedIndex = rowIndex;
                        if (input != null) {
                            input.setValue(values.copy().get(rowIndex));
                        }
                        rebuildWidgets();
                    })
                    .bounds(x, listTop + (index - scrollOffset) * ROW_HEIGHT, contentWidth, 20)
                    .build());
        }

        int actionY = height - 50;
        int actionWidth = Math.max(42, (contentWidth - 12) / 4);
        addRenderableWidget(Button.builder(Component.literal("Replace"), button -> replaceCurrent())
                .bounds(x, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), button -> removeCurrent())
                .bounds(x + actionWidth + 4, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Up"), button -> moveCurrent(-1))
                .bounds(x + (actionWidth + 4) * 2, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Down"), button -> moveCurrent(1))
                .bounds(x + (actionWidth + 4) * 3, actionY,
                        contentWidth - (actionWidth + 4) * 3, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(x, height - 26, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> commitAndClose())
                .bounds(x + contentWidth - 70, height - 26, 70, 20).build());
    }

    private int rowsPerPage() {
        return Math.max(1, (height - 62 - 58) / ROW_HEIGHT);
    }

    private String selectedOption() {
        if (allowedOptions.isEmpty()) {
            return "";
        }
        optionIndex = Math.max(0, Math.min(optionIndex, allowedOptions.size() - 1));
        return allowedOptions.get(optionIndex);
    }

    private String currentInput() {
        return input == null ? selectedOption() : input.getValue();
    }

    private void addCurrent() {
        values.add(currentInput());
        selectedIndex = values.copy().size() - 1;
        rebuildWidgets();
    }

    private void replaceCurrent() {
        values.replace(selectedIndex, currentInput());
        rebuildWidgets();
    }

    private void removeCurrent() {
        values.remove(selectedIndex);
        selectedIndex = Math.min(selectedIndex, values.copy().size() - 1);
        rebuildWidgets();
    }

    private void moveCurrent(int direction) {
        int target = selectedIndex + direction;
        values.move(selectedIndex, direction);
        if (target >= 0 && target < values.copy().size()) {
            selectedIndex = target;
        }
        rebuildWidgets();
    }

    private void commitAndClose() {
        setter.accept(values.copy());
        minecraft.setScreen(parent);
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
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static final class Values {
        private final ArrayList<String> values;

        Values(List<String> initial) {
            values = new ArrayList<>(initial);
        }

        void add(String value) {
            String trimmed = value == null ? "" : value.trim();
            if (!trimmed.isEmpty() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }

        void replace(int index, String value) {
            String trimmed = value == null ? "" : value.trim();
            if (index >= 0 && index < values.size() && !trimmed.isEmpty()
                    && (!values.contains(trimmed) || values.get(index).equals(trimmed))) {
                values.set(index, trimmed);
            }
        }

        void remove(int index) {
            if (index >= 0 && index < values.size()) {
                values.remove(index);
            }
        }

        void move(int index, int direction) {
            List<String> moved = SimpleConfigModel.move(new ArrayList<>(values), index, direction);
            values.clear();
            values.addAll(moved);
        }

        List<String> copy() {
            return List.copyOf(values);
        }
    }
}
