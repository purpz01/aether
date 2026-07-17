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
