package de.codingair.tradesystem.spigot.trade.layout.registration;

import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.tradesystem.spigot.trade.editor.Editor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Info class for {@link de.codingair.tradesystem.spigot.trade.layout.types.MultiTradeIcon}.
 */
public class MultiEditorInfo extends EditorInfo {
    private final String[] iconName;

    public MultiEditorInfo(@NotNull String name, @NotNull Type type, @NotNull Function<Editor, ItemBuilder> editorIconSupplier, boolean necessary, String... iconName) {
        super(name, type, editorIconSupplier, necessary);
        this.iconName = iconName;
    }

    public MultiEditorInfo(@NotNull String name, String... iconName) {
        super(name);
        this.iconName = iconName;
    }

    public String[] getIconName() {
        return iconName;
    }
}
