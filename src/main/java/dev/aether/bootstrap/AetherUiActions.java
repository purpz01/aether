package dev.aether.bootstrap;

import dev.aether.Aether;
import dev.aether.config.AetherConfig;
import dev.aether.ui.MainGUI;
import dev.aether.ui.MainGUIRegistry;
import dev.aether.ui.SimpleConfigScreen;
import dev.aether.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

public final class AetherUiActions {
    private AetherUiActions() {
    }

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
        return configScreenClass(simple) == SimpleConfigScreen.class
                ? new SimpleConfigScreen()
                : new MainGUI();
    }

    static Class<? extends Screen> configScreenClass(boolean simple) {
        return simple ? SimpleConfigScreen.class : MainGUI.class;
    }

    private static void openScreen(Supplier<Screen> factory) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

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
}
