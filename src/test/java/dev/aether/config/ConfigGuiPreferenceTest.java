package dev.aether.config;

import com.google.gson.JsonParser;
import dev.aether.bootstrap.AetherUiActions;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigGuiPreferenceTest {
    @TempDir
    static Path tempDir;

    @BeforeAll
    static void configureFabricPaths() throws Exception {
        setLoaderPath("gameDir", tempDir);
        setLoaderPath("configDir", tempDir.resolve("config"));
    }

    @Test
    void modeActionPersistsTheSelectedPreference() throws Exception {
        assertFalse(AetherConfig.SIMPLE_CONFIG_GUI.getDefault());
        assertTrue(AetherConfig.SIMPLE_CONFIG_GUI.isPersistent());

        Path configFile = tempDir.resolve("aether_config.json");
        Config.setConfigPath(configFile);

        AetherUiActions.setSimpleConfigGui(true);

        var json = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
        assertTrue(json.get("simpleConfigGui").getAsBoolean());
    }

    private static void setLoaderPath(String fieldName, Path value) throws Exception {
        Field field = FabricLoaderImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(FabricLoaderImpl.INSTANCE, value);
    }
}
