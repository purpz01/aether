# Simple Config GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in, vanilla Minecraft configuration GUI for operational Aether settings that can be enabled with `/aether simple` and does not touch NanoVG.

**Architecture:** Keep `MainGUIRegistry` as the single configuration model. A small `SimpleConfigModel` filters operational sections and supplies testable range/list helpers; `SimpleConfigScreen` handles vanilla navigation and controls; `SimpleStringListScreen` handles list editing. `AetherUiActions` owns all mode persistence and screen routing.

**Tech Stack:** Java 25, Minecraft 26.1.2 mapped GUI APIs, Fabric Loader/API, Brigadier client commands, Gradle, JUnit Jupiter 5.11.4.

## Global Constraints

- Simple mode is opt-in and defaults to `false`; do not auto-detect Android/Pojav.
- `/aether simple` and `/aether custom` persist and immediately open their selected GUI.
- Existing `/aether` and Insert key honor the persisted selection.
- Only registry sections `farming`, `other`, and `failsafes` are shown.
- Never extend `NVGScreen` or call/import `NanoVGManager`, `NVGRenderer`, or other NanoVG helpers from either simple-screen class.
- Exclude color, multi-dropdown, keybind, visual, theme, profile, HUD-layout, and cosmetic customization controls.
- Support operational `PositionSetting` coordinates because they are used by Rewarp, Pest Manager, Auto Pest Exchange, and Composter.
- Reuse existing setting getters/setters so persistence and runtime side effects remain unchanged.

---

### Task 1: Testable operational model

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/dev/aether/ui/SimpleConfigModel.java`
- Create: `src/test/java/dev/aether/ui/SimpleConfigModelTest.java`

**Interfaces:**
- Produces: `SimpleConfigModel.operationalSections(List<MainGUIRegistry.ModuleSection>)`
- Produces: `SimpleConfigModel.supports(SettingType)`
- Produces: `SimpleConfigModel.normalized(float, float, float)` and `denormalized(double, float, float)`
- Produces: `SimpleConfigModel.move(List<String>, int, int)`

- [ ] **Step 1: Add JUnit support and the failing model tests**

Add to `dependencies`:

~~~groovy
testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"
testRuntimeOnly "org.junit.platform:junit-platform-launcher"
~~~

Add at top level:

~~~groovy
test {
    useJUnitPlatform()
}
~~~

Create `SimpleConfigModelTest.java`:

~~~java
package dev.aether.ui;

import dev.aether.ui.settings.SettingType;
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
~~~

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigModelTest`

Expected: compilation fails because `SimpleConfigModel` does not exist.

- [ ] **Step 3: Implement the minimal model**

Create `SimpleConfigModel.java`:

~~~java
package dev.aether.ui;

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

    private SimpleConfigModel() {}

    static List<MainGUIRegistry.ModuleSection> operationalSections(
            List<MainGUIRegistry.ModuleSection> sections) {
        return sections.stream()
                .filter(section -> OPERATIONAL_SECTIONS.contains(section.id()))
                .toList();
    }

    static boolean supports(SettingType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    static double normalized(float value, float min, float max) {
        if (max <= min) return 0;
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
~~~

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigModelTest`

Expected: four tests pass.

- [ ] **Step 5: Commit**

~~~powershell
git add build.gradle src/main/java/dev/aether/ui/SimpleConfigModel.java src/test/java/dev/aether/ui/SimpleConfigModelTest.java
git commit -m "test: add simple config model"
~~~

---

### Task 2: Persisted mode, commands, and centralized routing

**Files:**
- Modify: `src/main/java/dev/aether/config/AetherConfig.java`
- Modify: `src/main/java/dev/aether/bootstrap/AetherUiActions.java`
- Modify: `src/main/java/dev/aether/bootstrap/AetherCommandRegistrar.java`
- Modify: `src/main/java/dev/aether/bootstrap/AetherKeybindHandler.java`
- Modify: `src/main/java/dev/aether/bootstrap/AetherBootstrapHooks.java`
- Modify: `src/main/java/dev/aether/ui/BootstrapSettingsRegistryProvider.java`
- Create initially: `src/main/java/dev/aether/ui/SimpleConfigScreen.java`
- Create: `src/test/java/dev/aether/bootstrap/AetherUiActionsTest.java`
- Create: `src/test/java/dev/aether/config/ConfigGuiPreferenceTest.java`

**Interfaces:**
- Produces: `AetherConfig.SIMPLE_CONFIG_GUI`
- Produces: `AetherUiActions.toggleConfigGui()`
- Produces: `AetherUiActions.openConfigGui()`
- Produces: `AetherUiActions.setSimpleConfigGui(boolean)`
- Produces: package-visible `AetherUiActions.createConfigScreen(boolean)` for real routing and testing

- [ ] **Step 1: Write the failing routing/default tests**

Create `AetherUiActionsTest.java`:

~~~java
package dev.aether.bootstrap;

import dev.aether.config.AetherConfig;
import dev.aether.ui.MainGUI;
import dev.aether.ui.SimpleConfigScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AetherUiActionsTest {
    @Test
    void simpleModeDefaultsOffAndIsPersistent() {
        assertFalse(AetherConfig.SIMPLE_CONFIG_GUI.getDefault());
        assertTrue(AetherConfig.SIMPLE_CONFIG_GUI.isPersistent());
    }

    @Test
    void createsTheScreenSelectedByThePreference() {
        assertInstanceOf(SimpleConfigScreen.class, AetherUiActions.createConfigScreen(true));
        assertInstanceOf(MainGUI.class, AetherUiActions.createConfigScreen(false));
    }
}
~~~

Create `ConfigGuiPreferenceTest.java` to exercise the same action used by the
commands against a temporary config path:

~~~java
package dev.aether.config;

import com.google.gson.JsonParser;
import dev.aether.bootstrap.AetherUiActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigGuiPreferenceTest {
    @Test
    void modeActionPersistsTheSelectedPreference(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("aether_config.json");
        Config.setConfigPath(configFile);

        AetherUiActions.setSimpleConfigGui(true);

        var json = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
        assertTrue(json.get("simpleConfigGui").getAsBoolean());
    }
}
~~~

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests dev.aether.bootstrap.AetherUiActionsTest --tests dev.aether.config.ConfigGuiPreferenceTest`

Expected: compilation fails because `SIMPLE_CONFIG_GUI`, `SimpleConfigScreen`, and `createConfigScreen` do not exist.

- [ ] **Step 3: Add the preference and minimal vanilla screen shell**

Beside `CUSTOM_UI_ENABLED` in `AetherConfig.java` add:

~~~java
public static final BooleanEntry SIMPLE_CONFIG_GUI = Config.bool("simpleConfigGui", false);
~~~

Create a compileable shell which does not import any Aether renderer:

~~~java
package dev.aether.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SimpleConfigScreen extends Screen {
    public SimpleConfigScreen() {
        super(Component.literal("Aether Simple Config"));
    }
}
~~~

- [ ] **Step 4: Centralize GUI selection and persistence**

Replace `AetherUiActions`' public routing methods with:

~~~java
public static void toggleConfigGui() {
    Minecraft client = Minecraft.getInstance();
    if (client.screen instanceof MainGUI || client.screen instanceof SimpleConfigScreen) {
        client.setScreen(null);
        return;
    }
    openConfigGui();
}

public static void openConfigGui() {
    openScreen(() -> createConfigScreen(AetherConfig.SIMPLE_CONFIG_GUI.get()));
}

public static void setSimpleConfigGui(boolean simple) {
    AetherConfig.SIMPLE_CONFIG_GUI.set(simple);
    AetherConfig.save();
    openScreen(() -> createConfigScreen(simple));
}

static Screen createConfigScreen(boolean simple) {
    return simple ? new SimpleConfigScreen() : new MainGUI();
}

private static void openScreen(java.util.function.Supplier<Screen> factory) {
    Minecraft client = Minecraft.getInstance();
    if (client == null) return;
    try {
        MainGUIRegistry.refresh();
        client.execute(() -> {
            try {
                client.setScreen(factory.get());
            } catch (RuntimeException | LinkageError e) {
                Aether.LOGGER.error("Failed to open Aether GUI from queued client task", e);
                ClientUtils.sendMessage("\u00A7cFailed to open the Aether GUI. Check the client log.", false);
            }
        });
    } catch (RuntimeException | LinkageError e) {
        Aether.LOGGER.error("Failed to open Aether GUI", e);
        ClientUtils.sendMessage("\u00A7cFailed to open the Aether GUI. Check the client log.", false);
    }
}
~~~

Import `AetherConfig`, `SimpleConfigScreen`, `Screen`, and `Supplier` as needed. Remove `openMainGui()` unless another caller remains.

- [ ] **Step 5: Wire commands, keybind, bootstrap toggle, and screen recognition**

Change the root command and keybind to call `toggleConfigGui()`. Add these root children before `farming`:

~~~java
.then(ClientCommands.literal("simple")
        .executes(ctx -> {
            AetherUiActions.setSimpleConfigGui(true);
            return 1;
        }))
.then(ClientCommands.literal("custom")
        .executes(ctx -> {
            AetherUiActions.setSimpleConfigGui(false);
            return 1;
        }))
~~~

Add this setting after `Custom UI` in `BootstrapSettingsRegistryProvider`:

~~~java
group.add(new ToggleSetting("Simple Config GUI",
        () -> AetherConfig.SIMPLE_CONFIG_GUI.get(),
        value -> {
            AetherConfig.SIMPLE_CONFIG_GUI.set(value);
            AetherConfig.save();
        }));
~~~

Update `AetherBootstrapHooks.isBootstrapConfigScreen`:

~~~java
return screen instanceof MainGUI || screen instanceof SimpleConfigScreen;
~~~

- [ ] **Step 6: Run tests and verify GREEN**

Run: `.\gradlew.bat test --tests dev.aether.bootstrap.AetherUiActionsTest --tests dev.aether.config.ConfigGuiPreferenceTest`

Expected: three tests pass.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL` and no stale `toggleMainGui`/`openMainGui` references.

- [ ] **Step 7: Commit**

~~~powershell
git add src/main/java/dev/aether/config/AetherConfig.java src/main/java/dev/aether/bootstrap/AetherUiActions.java src/main/java/dev/aether/bootstrap/AetherCommandRegistrar.java src/main/java/dev/aether/bootstrap/AetherKeybindHandler.java src/main/java/dev/aether/bootstrap/AetherBootstrapHooks.java src/main/java/dev/aether/ui/BootstrapSettingsRegistryProvider.java src/main/java/dev/aether/ui/SimpleConfigScreen.java src/test/java/dev/aether/bootstrap/AetherUiActionsTest.java src/test/java/dev/aether/config/ConfigGuiPreferenceTest.java
git commit -m "feat: add simple config mode routing"
~~~

---

### Task 3: Vanilla section/module navigation and scrolling

**Files:**
- Modify: `src/main/java/dev/aether/ui/SimpleConfigScreen.java`
- Create: `src/test/java/dev/aether/ui/SimpleConfigScreenTest.java`

**Interfaces:**
- Produces: package-visible `SimpleConfigScreen.Level`
- Produces: `selectSection(MainGUIRegistry.ModuleSection)`, `selectSubTab(ModulesTab.SubTab)`, and `back()`
- Consumes: `SimpleConfigModel.operationalSections(...)`

- [ ] **Step 1: Write failing navigation tests**

~~~java
package dev.aether.ui;

import dev.aether.ui.settings.ModulesTab;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SimpleConfigScreenTest {
    @Test
    void navigatesSectionModuleAndBackWithoutRendering() {
        var screen = new SimpleConfigScreen();
        var subTab = new ModulesTab.SubTab("Farming Macro", "", List.of());
        var section = new MainGUIRegistry.ModuleSection("farming", "Farming", List.of(subTab));

        screen.selectSection(section);
        assertEquals(SimpleConfigScreen.Level.MODULES, screen.level());
        screen.selectSubTab(subTab);
        assertEquals(SimpleConfigScreen.Level.SETTINGS, screen.level());
        screen.back();
        assertEquals(SimpleConfigScreen.Level.MODULES, screen.level());
        screen.back();
        assertEquals(SimpleConfigScreen.Level.SECTIONS, screen.level());
    }
}
~~~

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigScreenTest`

Expected: compilation fails because navigation methods and `Level` do not exist.

- [ ] **Step 3: Add navigation state and lifecycle**

Add:

~~~java
enum Level { SECTIONS, MODULES, SETTINGS }

private Level level = Level.SECTIONS;
private MainGUIRegistry.ModuleSection selectedSection;
private ModulesTab.SubTab selectedSubTab;
private int scrollOffset;

Level level() {
    return level;
}

void selectSection(MainGUIRegistry.ModuleSection section) {
    selectedSection = section;
    selectedSubTab = null;
    level = Level.MODULES;
    scrollOffset = 0;
}

void selectSubTab(ModulesTab.SubTab subTab) {
    selectedSubTab = subTab;
    level = Level.SETTINGS;
    scrollOffset = 0;
}

void back() {
    if (level == Level.SETTINGS) {
        selectedSubTab = null;
        level = Level.MODULES;
    } else if (level == Level.MODULES) {
        selectedSection = null;
        level = Level.SECTIONS;
    }
    scrollOffset = 0;
}
~~~

In `init()` stop a running macro exactly as `MainGUI` does, refresh the registry, and build the current page:

~~~java
if (MacroStateManager.isMacroRunning()) {
    MacroStateManager.stopMacro(Minecraft.getInstance(), "SimpleConfigScreen opened", false);
}
MainGUIRegistry.refresh();
switch (level) {
    case SECTIONS -> buildSections();
    case MODULES -> buildModules();
    case SETTINGS -> buildSettings();
}
buildFooter();
~~~

`buildSections()` and `buildModules()` add centered vanilla `Button` rows. Each callback calls the selection method and `rebuildWidgets()`. `buildFooter()` adds Back when not on sections, Done to close, and Use Custom GUI:

~~~java
addRenderableWidget(Button.builder(Component.literal("Use Custom GUI"), button ->
        AetherUiActions.setSimpleConfigGui(false))
        .bounds(width - 130, height - 26, 120, 20)
        .build());
~~~

- [ ] **Step 4: Add bounded scrolling and vanilla rendering**

Use fixed 24-pixel rows and rebuild only visible widgets. Override:

~~~java
@Override
public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    int next = Math.max(0, scrollOffset - (int) Math.signum(scrollY));
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
    graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
}

@Override
public boolean isPauseScreen() {
    return false;
}
~~~

Clamp `scrollOffset` after calculating `rowsPerPage = Math.max(1, (height - 72) / 24)` so blank pages cannot be reached.

- [ ] **Step 5: Run the navigation test and compile**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigScreenTest`

Expected: one test passes.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

~~~powershell
git add src/main/java/dev/aether/ui/SimpleConfigScreen.java src/test/java/dev/aether/ui/SimpleConfigScreenTest.java
git commit -m "feat: add vanilla config navigation"
~~~

---

### Task 4: Operational setting controls

**Files:**
- Modify: `src/main/java/dev/aether/ui/SimpleConfigScreen.java`
- Modify: `src/test/java/dev/aether/ui/SimpleConfigScreenTest.java`

**Interfaces:**
- Consumes all supported `Setting` subclasses from `dev.aether.ui.settings`
- Produces nested `SimpleSliderButton` and pending-editor commit handling

- [ ] **Step 1: Add failing control-model assertions**

Add a test that creates real setting objects backed by local arrays, invokes the package-visible screen helpers, and verifies the existing setters receive changes:

~~~java
@Test
void helperMethodsUseExistingSettingSetters() {
    boolean[] toggle = {false};
    float[] slider = {5};
    var toggleSetting = new ToggleSetting("Toggle", () -> toggle[0], value -> toggle[0] = value);
    var sliderSetting = new SliderSetting("Slider", 0, 10, () -> slider[0], value -> slider[0] = value);

    SimpleConfigScreen.toggle(toggleSetting);
    SimpleConfigScreen.applySlider(sliderSetting, 0.8);

    assertTrue(toggle[0]);
    assertEquals(8, slider[0], 0.001);
}
~~~

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigScreenTest`

Expected: compilation fails because `toggle` and `applySlider` do not exist.

- [ ] **Step 3: Implement tested setters and common widgets**

Add:

~~~java
static void toggle(ToggleSetting setting) {
    setting.toggle();
}

static void applySlider(SliderSetting setting, double normalized) {
    setting.setValue(SimpleConfigModel.denormalized(
            normalized, setting.getMin(), setting.getMax()));
}
~~~

In `buildSettings()`:

- Add a module enable button when `selectedSubTab.hasToggle()`.
- Add a group enable button for every non-`alwaysOn` group.
- Show child settings only when the containing module and group are enabled.
- Re-evaluate `setting.isVisible()` every rebuild.
- Render unsupported types as an inactive button whose message is `Component.literal(setting.getName() + ": Not supported in Simple GUI")`.

Dispatch supported settings with an exhaustive switch:

~~~java
switch (setting.getType()) {
    case TOGGLE -> addToggle((ToggleSetting) setting, x, y);
    case SLIDER -> addSlider((SliderSetting) setting, x, y);
    case RANGE_SLIDER -> addRangeSliders((RangeSliderSetting) setting, x, y);
    case TEXT -> addText((TextSetting) setting, x, y);
    case DROPDOWN -> addDropdown((DropdownSetting) setting, x, y);
    case LIST -> addStringList((ListSetting) setting, x, y);
    case DROPDOWN_LIST -> addDropdownList((DropdownListSetting) setting, x, y);
    case ACTION -> addAction((ActionSetting) setting, x, y);
    case INFO -> addInfo((InfoSetting) setting, x, y);
    case POSITION -> addPosition((PositionSetting) setting, x, y);
    default -> addUnsupported(setting, x, y);
}
~~~

Use `Button` for toggles, dropdown cycling, and actions; `EditBox` for text and coordinates. Store pending field commits as `List<Runnable> pendingCommits`. `commitEditors()` runs before Back, Done, scrolling, list-editor navigation, or rebuilding after another setting changes.

Track each field and commit action in `Map<EditBox, Runnable> editorCommits`. After `super.mouseClicked(...)`, run and remove the actions for fields that changed from focused to unfocused; this implements the specified focus-loss commit without saving on every keystroke.

Add a nested slider:

~~~java
private static final class SimpleSliderButton extends AbstractSliderButton {
    private final String label;
    private final float min;
    private final float max;
    private final int decimals;
    private final String suffix;
    private final java.util.function.Consumer<Float> setter;

    private SimpleSliderButton(int x, int y, int width, String label,
                               float min, float max, float current,
                               int decimals, String suffix,
                               java.util.function.Consumer<Float> setter) {
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
                + String.format(java.util.Locale.ROOT, "%." + decimals + "f", current)
                + suffix));
    }

    @Override
    protected void applyValue() {
        setter.accept(SimpleConfigModel.denormalized(value, min, max));
    }
}
~~~

For `RangeSliderSetting` create lower and upper `SimpleSliderButton` rows; their consumers call `setLowerValue`/`setUpperValue`, whose existing implementation preserves order. For `PositionSetting` create X/Y/Z fields plus Capture and Highlight buttons; coordinate commits parse with `Double.parseDouble` and leave the stored value unchanged on parse failure.

- [ ] **Step 4: Run tests and compile**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleConfigScreenTest --tests dev.aether.ui.SimpleConfigModelTest`

Expected: all focused tests pass.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

~~~powershell
git add src/main/java/dev/aether/ui/SimpleConfigScreen.java src/test/java/dev/aether/ui/SimpleConfigScreenTest.java
git commit -m "feat: render operational config controls"
~~~

---

### Task 5: Vanilla string and dropdown-list editor

**Files:**
- Create: `src/main/java/dev/aether/ui/SimpleStringListScreen.java`
- Modify: `src/main/java/dev/aether/ui/SimpleConfigScreen.java`
- Create: `src/test/java/dev/aether/ui/SimpleStringListScreenTest.java`

**Interfaces:**
- Produces: `SimpleStringListScreen.forList(Screen, ListSetting)`
- Produces: `SimpleStringListScreen.forDropdownList(Screen, DropdownListSetting)`
- Consumes: `SimpleConfigModel.move(...)`

- [ ] **Step 1: Write failing list-edit tests**

~~~java
package dev.aether.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SimpleStringListScreenTest {
    @Test
    void addReplaceRemoveAndMoveOperateOnLocalValues() {
        var model = new SimpleStringListScreen.Values(List.of("a", "b"));
        model.add("c");
        model.replace(1, "B");
        model.move(2, -1);
        model.remove(0);
        assertEquals(List.of("c", "B"), model.copy());
    }

    @Test
    void blankAndDuplicateAddsAreIgnored() {
        var model = new SimpleStringListScreen.Values(List.of("a"));
        model.add(" ");
        model.add("a");
        assertEquals(List.of("a"), model.copy());
    }
}
~~~

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleStringListScreenTest`

Expected: compilation fails because `SimpleStringListScreen` does not exist.

- [ ] **Step 3: Implement the local list model and screen factories**

Implement `Values` exactly as the mutable state used by the screen:

~~~java
static final class Values {
    private final java.util.ArrayList<String> values;

    Values(List<String> initial) {
        values = new java.util.ArrayList<>(initial);
    }

    void add(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.isEmpty() && !values.contains(trimmed)) values.add(trimmed);
    }

    void replace(int index, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (index >= 0 && index < values.size() && !trimmed.isEmpty()
                && (!values.contains(trimmed) || values.get(index).equals(trimmed))) {
            values.set(index, trimmed);
        }
    }

    void remove(int index) {
        if (index >= 0 && index < values.size()) values.remove(index);
    }

    void move(int index, int direction) {
        List<String> moved = SimpleConfigModel.move(new java.util.ArrayList<>(values), index, direction);
        values.clear();
        values.addAll(moved);
    }

    List<String> copy() {
        return List.copyOf(values);
    }
}
~~~

The screen uses vanilla `Button` and `EditBox` only, shows one selectable value per 24-pixel row, and provides Add/Replace/Delete/Up/Down/Done/Back. `Done` passes `values.copy()` to the existing setter and returns to the parent; Back discards local edits.

Factory behavior:

~~~java
public static SimpleStringListScreen forList(Screen parent, ListSetting setting) {
    return new SimpleStringListScreen(parent, setting.getName(), setting.getPlaceholder(),
            setting.getValues(), List.of(), setting::setValues);
}

public static SimpleStringListScreen forDropdownList(
        Screen parent, DropdownListSetting setting) {
    return new SimpleStringListScreen(parent, setting.getName(), "Select option",
            setting.getValues(), setting.getAllOptions(), values -> {
                List<String> current = setting.getValues();
                for (int i = current.size() - 1; i >= 0; i--) setting.removeValue(i);
                values.forEach(setting::addValue);
            });
}
~~~

When `allowedOptions` is non-empty, the Add control cycles through those options instead of accepting arbitrary text.

- [ ] **Step 4: Connect both list setting types**

`SimpleConfigScreen.addStringList` and `addDropdownList` commit pending text, then open the matching factory screen. When the editor returns, call `rebuildWidgets()` so list counts and visibility refresh.

- [ ] **Step 5: Run focused tests and compile**

Run: `.\gradlew.bat test --tests dev.aether.ui.SimpleStringListScreenTest`

Expected: two tests pass.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

~~~powershell
git add src/main/java/dev/aether/ui/SimpleStringListScreen.java src/main/java/dev/aether/ui/SimpleConfigScreen.java src/test/java/dev/aether/ui/SimpleStringListScreenTest.java
git commit -m "feat: add vanilla list config editor"
~~~

---

### Task 6: Full verification and Android handoff

**Files:**
- Modify only if verification exposes a defect: files from Tasks 1–5

**Interfaces:**
- Validates all prior tasks; produces no new abstraction.

- [ ] **Step 1: Prove no NanoVG dependency entered the simple path**

Run:

~~~powershell
rg -n "NVGScreen|NanoVG|NVGRenderer|AetherRenderQueue" src/main/java/dev/aether/ui/SimpleConfigScreen.java src/main/java/dev/aether/ui/SimpleStringListScreen.java
~~~

Expected: no matches.

- [ ] **Step 2: Prove old routing references are gone**

Run:

~~~powershell
rg -n "toggleMainGui|openMainGui" src/main/java
~~~

Expected: no matches.

- [ ] **Step 3: Run the complete automated suite and build**

Run: `.\gradlew.bat clean test build`

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 4: Desktop smoke test**

Launch the Fabric client and verify:

1. `/aether simple` opens `SimpleConfigScreen` immediately.
2. `/aether` and Insert reopen the simple screen after closing it.
3. Farming, Other, and Failsafes appear; Visuals/Colors/Profiles/Settings do not.
4. Toggle, slider, range, text, dropdown, list, dropdown-list, action, info, and operational position controls work and persist after restart.
5. Opening the screen stops an active macro.
6. `/aether custom` opens `MainGUI` and persists custom mode.

- [ ] **Step 5: Android/Pojav smoke-test handoff**

Provide the built jar and this exact checklist:

1. Start the game without opening the custom GUI.
2. Run `/aether simple`.
3. Change Farm Type, a failsafe toggle, a numeric delay, a rewarp coordinate, and one list value.
4. Close/reopen with `/aether` and Insert.
5. Restart and confirm values plus simple-mode preference persisted.
6. Run `/aether custom` only when intentionally testing the original GUI.

- [ ] **Step 6: Commit verification-only fixes, if any**

If verification required code changes, rerun Step 3, then:

~~~powershell
git add src/main/java/dev/aether/ui/SimpleConfigModel.java src/main/java/dev/aether/ui/SimpleConfigScreen.java src/main/java/dev/aether/ui/SimpleStringListScreen.java src/main/java/dev/aether/bootstrap/AetherUiActions.java src/main/java/dev/aether/bootstrap/AetherCommandRegistrar.java src/main/java/dev/aether/bootstrap/AetherKeybindHandler.java src/main/java/dev/aether/bootstrap/AetherBootstrapHooks.java src/main/java/dev/aether/config/AetherConfig.java src/main/java/dev/aether/ui/BootstrapSettingsRegistryProvider.java src/test/java/dev/aether/ui/SimpleConfigModelTest.java src/test/java/dev/aether/ui/SimpleConfigScreenTest.java src/test/java/dev/aether/ui/SimpleStringListScreenTest.java src/test/java/dev/aether/bootstrap/AetherUiActionsTest.java src/test/java/dev/aether/config/ConfigGuiPreferenceTest.java build.gradle
git commit -m "fix: finish simple config verification"
~~~

If no changes were required, do not create an empty commit.
