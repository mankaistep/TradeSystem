package de.codingair.tradesystem.spigot.trade.layout.types.impl.basic;

import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.tradesystem.spigot.trade.Trade;
import de.codingair.tradesystem.spigot.trade.layout.types.MultiTradeIcon;
import de.codingair.tradesystem.spigot.trade.layout.types.TradeIcon;
import de.codingair.tradesystem.spigot.utils.Lang;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ShowStatusIcon extends MultiTradeIcon {
    public ShowStatusIcon(ItemStack[] items) {
        super(new ShowNotReadyIcon(items[0]), new ShowReadyIcon(items[1]));
    }

    @Override
    public @NotNull TradeIcon currentTradeIcon(@NotNull Trade trade, @NotNull Player player, @Nullable Player other, @NotNull String othersName) {
        int id = trade.getOtherId(player);
        if (trade.getReady()[id]) return getIcon(ShowReadyIcon.class);
        else return getIcon(ShowNotReadyIcon.class);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void serialize(@NotNull DataOutputStream out) throws IOException {

    }

    @Override
    public void deserialize(@NotNull DataInputStream in) throws IOException {

    }

    public static class ShowReadyIcon extends DecorationIcon {
        public ShowReadyIcon(@NotNull ItemStack itemStack) {
            super(itemStack);
        }

        @Override
        public @NotNull ItemBuilder prepareItemStack(@NotNull ItemBuilder layout, @NotNull Trade trade, @NotNull Player player, @Nullable Player other, @NotNull String othersName) {
            return layout.setName("§7" + Lang.get("Status", player) + ": §a" + Lang.get("Ready", player));
        }
    }

    public static class ShowNotReadyIcon extends DecorationIcon {
        public ShowNotReadyIcon(@NotNull ItemStack itemStack) {
            super(itemStack);
        }

        @Override
        public @NotNull ItemBuilder prepareItemStack(@NotNull ItemBuilder layout, @NotNull Trade trade, @NotNull Player player, @Nullable Player other, @NotNull String othersName) {
            return layout.setName("§7" + Lang.get("Status", player) + ": §c" + Lang.get("Not_Ready", player));
        }
    }
}
