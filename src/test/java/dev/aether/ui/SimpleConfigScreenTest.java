package dev.aether.ui;

import dev.aether.ui.settings.SliderSetting;
import dev.aether.ui.settings.ToggleSetting;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimpleConfigScreenTest {
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

    @Test
    void vanillaScreensDoNotRequestBackgroundSeparately() throws IOException {
        for (String screen : List.of("SimpleConfigScreen.java", "SimpleStringListScreen.java")) {
            String source = Files.readString(Path.of(
                    "src/main/java/dev/aether/ui", screen));
            assertFalse(source.contains("super.extractBackground("), screen);
        }
    }
}
