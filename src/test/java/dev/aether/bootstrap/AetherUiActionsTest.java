package dev.aether.bootstrap;

import dev.aether.ui.MainGUI;
import dev.aether.ui.SimpleConfigScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

final class AetherUiActionsTest {
    @Test
    void selectsTheScreenClassForThePreference() {
        assertSame(SimpleConfigScreen.class, AetherUiActions.configScreenClass(true));
        assertSame(MainGUI.class, AetherUiActions.configScreenClass(false));
    }
}
