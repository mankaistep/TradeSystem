package de.codingair.tradesystem.spigot.trade.gui;

import de.codingair.tradesystem.spigot.TradeSystem;
import de.codingair.tradesystem.spigot.trade.Trade;
import de.codingair.tradesystem.spigot.trade.layout.TradeLayout;
import de.codingair.tradesystem.spigot.trade.layout.types.impl.basic.TradeSlot;
import de.codingair.tradesystem.spigot.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TradeGUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();
            Trade trade = TradeSystem.man().getTrade(player);

            if (trade != null) {
                if (e.getNewItems().isEmpty()) return;

                //check if it's blocked
                if (TradeSystem.getInstance().getTradeManager().isBlocked(e.getNewItems().values().iterator().next())) {
                    e.setCancelled(true);
                    player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Placed_Blocked_Item", player));
                } else if (!e.isCancelled() && !TradeSystem.getInstance().getTradeManager().isDropItems() && !trade.fitsTrade(player, e.getNewItems().values().toArray(new ItemStack[0]))) {
                    player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Partner_No_Space", player));
                    TradeSystem.getInstance().getTradeManager().playBlockSound(player);
                    e.setCancelled(true);
                } else {
                    e.setCancelled(false);
                    for (Integer rawSlot : e.getRawSlots()) {
                        if (rawSlot < 54) {
                            if (!trade.getSlots().contains(rawSlot)) {
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }

                    trade.updateLater(1);
                }
            }
        }
    }

    //use higher priority than the GUI listener
    @EventHandler (priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();
            Trade trade = TradeSystem.man().getTrade(player);

            if (trade != null) {
                TradeLayout layout = trade.getLayout()[trade.getId(player)];
                e.setCancelled(true);

                if (e.getClickedInventory() == null && e.getCursor() != null) {
                    e.setCancelled(false);
                    onDrop(player, trade, e);
                } else if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                    //do not allow
                    onCursorCollect(trade, layout, e);
                } else if (e.getClickedInventory() == e.getView().getBottomInventory()) {
                    onClickBottomInventory(player, trade, e);
                } else {
                    //top inventory
                    if (trade.getSlots().contains(e.getSlot())) {
                        //own slots
                        e.setCancelled(false);
                        onTopInventoryClick(player, trade, e);
                    }
                }
            }
        }
    }

    private void onDrop(Player player, Trade trade, InventoryClickEvent e) {
        if (!TradeSystem.getInstance().getTradeManager().isDropItems()) {
            //check for cursor
            updateWaitForPickup(trade, e, player);
        }
    }

    private void onClickBottomInventory(Player player, Trade trade, InventoryClickEvent e) {
        if (!TradeSystem.getInstance().getTradeManager().isDropItems()) {
            //check for cursor
            trade.getWaitForPickup()[trade.getId(player)] = true;
            Bukkit.getScheduler().runTaskLater(TradeSystem.getInstance(), () -> {
                trade.getCursor()[trade.getId(player)] = e.getCursor() != null && e.getCursor().getType() != Material.AIR;
                trade.getWaitForPickup()[trade.getId(player)] = false;
                trade.cancelOverflow(trade.getOtherId(player));
            }, 1);
        }

        ItemStack item = e.getCurrentItem();
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            //check if it's blocked

            if (TradeSystem.getInstance().getTradeManager().isBlocked(item)) {
                player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Placed_Blocked_Item", player));
                TradeSystem.getInstance().getTradeManager().playBlockSound(player);
            } else if (!TradeSystem.getInstance().getTradeManager().isDropItems() && !trade.fitsTrade(player, item)) {
                player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Partner_No_Space", player));
                TradeSystem.getInstance().getTradeManager().playBlockSound(player);
            } else {
                if (item != null && item.getType() != Material.AIR) {
                    //move to own slots
                    List<Integer> slots = trade.getSlots();
                    Inventory top = e.getView().getTopInventory();

                    Integer empty = null;
                    for (Integer slot : slots) {
                        ItemStack i = top.getItem(slot);
                        if (i == null || i.getType() == Material.AIR) {
                            if (empty == null) empty = slot;
                            continue;
                        }

                        int allowed = i.getMaxStackSize() - i.getAmount();
                        if (allowed > 0 && item.isSimilar(i)) {
                            if (allowed >= item.getAmount()) {
                                i.setAmount(i.getAmount() + item.getAmount());
                                item.setAmount(0);
                                e.getView().getBottomInventory().setItem(e.getSlot(), null);
                                break;
                            } else {
                                i.setAmount(i.getMaxStackSize());
                                item.setAmount(item.getAmount() - allowed);
                            }
                        }
                    }

                    if (empty != null && item.getAmount() > 0) {
                        top.setItem(empty, item);
                        e.getView().getBottomInventory().setItem(e.getSlot(), null);
                    }

                    trade.updateLater(1);
                }
            }
        } else e.setCancelled(false);
    }

    private void onCursorCollect(Trade trade, TradeLayout layout, InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        assert cursor != null;

        Inventory inv = e.getView().getTopInventory();
        int startSize = cursor.getAmount();

        for (Integer slot : layout.getPattern().getSlotsOf(TradeSlot.class)) {
            if (collectSlotToCursor(inv, cursor, slot)) break;
        }

        Inventory bottom = e.getView().getBottomInventory();

        for (int slot = 0; slot < bottom.getSize(); slot++) {
            if (collectSlotToCursor(bottom, cursor, slot)) break;
        }

        if (cursor.getAmount() > startSize) {
            //update!
            trade.updateLater(1);
        }
    }

    /**
     * Collects the current item in the clicked slot to the cursor.
     *
     * @param inv    The current inventory.
     * @param cursor The current cursor.
     * @param slot   The applying slot.
     * @return true if the cursor has reached its maximum stack size.
     */
    private boolean collectSlotToCursor(Inventory inv, ItemStack cursor, Integer slot) {
        ItemStack other = inv.getItem(slot);

        if (other != null && cursor.isSimilar(other)) {
            int amount = cursor.getAmount();

            if (amount < cursor.getMaxStackSize()) {
                applyAmount(inv, cursor, other, slot, amount);
            } else return true;
        }
        return false;
    }

    /**
     * Moves the given amount from 'other' to 'cursor'.
     *
     * @param bottom The bottom inventory.
     * @param cursor The current cursor.
     * @param other  The other ItemStack which should be collected to the cursor.
     * @param slot   The clicked slot.
     * @param amount The amount to collect.
     */
    private void applyAmount(Inventory bottom, ItemStack cursor, ItemStack other, int slot, int amount) {
        int a = cursor.getMaxStackSize() - amount;

        if (other.getAmount() > a) {
            other.setAmount(other.getAmount() - a);
            cursor.setAmount(cursor.getMaxStackSize());
        } else {
            cursor.setAmount(cursor.getAmount() + other.getAmount());
            bottom.setItem(slot, new ItemStack(Material.AIR));
        }
    }

    private void onTopInventoryClick(Player player, Trade trade, InventoryClickEvent e) {
        //cancel faster --> fix dupe glitch
        if (e.getView().getTopInventory().equals(e.getClickedInventory()) && trade.getSlots().contains(e.getSlot()) && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
            trade.updateReady(trade.getId(player), false);
        }

        if (e.getClick().name().equals("SWAP_OFFHAND")) {
            if (e.getView().getTopInventory().equals(e.getClickedInventory())) {
                e.setCancelled(true);
                return;
            }
        }

        //check if it's blocked
        ItemStack blockedItem = null;
        switch (e.getAction().name()) {
            case "SWAP_WITH_CURSOR":
            case "PLACE_ALL":
            case "PLACE_ONE": {
                //check cursor
                blockedItem = e.getCursor();
                break;
            }

            case "MOVE_TO_OTHER_INVENTORY": {
                //check current
                blockedItem = e.getCurrentItem();
                break;
            }

            case "HOTBAR_SWAP":
            case "HOTBAR_MOVE_AND_READD": {
                //check hotbar
                blockedItem = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                break;
            }
        }

        if (blockedItem != null && TradeSystem.getInstance().getTradeManager().isBlocked(blockedItem)) {
            e.setCancelled(true);
            player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Placed_Blocked_Item", player));
            TradeSystem.getInstance().getTradeManager().playBlockSound(player);
            return;
        }

        boolean fits = true;
        if (!TradeSystem.getInstance().getTradeManager().isDropItems()) {
            //check for cursor
            updateWaitForPickup(trade, e, player);

            if (!e.isCancelled()) {
                //check if fits
                switch (e.getAction().name()) {
                    case "PLACE_ONE": {
                        ItemStack item = e.getCurrentItem();
                        List<Integer> remove = new ArrayList<>();

                        if (item != null && item.getType() != Material.AIR) {
                            item = item.clone();
                            item.setAmount(item.getAmount() + 1);
                            remove.add(e.getSlot());
                        } else {
                            assert e.getCursor() != null;
                            item = e.getCursor().clone();
                            item.setAmount(1);
                        }

                        if (!trade.fitsTrade(player, remove, item)) fits = false;
                        break;
                    }

                    case "PLACE_SOME": {
                        assert e.getCurrentItem() != null;
                        ItemStack item = e.getCurrentItem().clone();
                        item.setAmount(item.getMaxStackSize());

                        List<Integer> remove = new ArrayList<>();
                        remove.add(e.getSlot());

                        if (!trade.fitsTrade(player, remove, item)) fits = false;
                        break;
                    }

                    case "PLACE_ALL":
                        assert e.getCursor() != null;
                        if (!trade.fitsTrade(player, e.getCursor().clone())) fits = false;
                        break;

                    case "HOTBAR_SWAP": {
                        ItemStack item = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                        if (item != null && !trade.fitsTrade(player, item.clone())) fits = false;
                        break;
                    }

                    case "HOTBAR_MOVE_AND_READD": {
                        ItemStack item = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                        List<Integer> remove = new ArrayList<>();
                        remove.add(e.getSlot());

                        if (item != null && !trade.fitsTrade(player, remove, item.clone())) fits = false;
                        else {
                            e.setCancelled(true);

                            ItemStack current = e.getView().getTopInventory().getItem(e.getSlot());
                            assert current != null;
                            ItemStack top = current.clone();

                            current = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                            assert current != null;
                            ItemStack bottom = current.clone();

                            e.getView().getTopInventory().setItem(e.getSlot(), bottom);
                            e.getView().getBottomInventory().setItem(e.getHotbarButton(), top);
                        }
                        break;
                    }

                    case "SWAP_WITH_CURSOR": {
                        List<Integer> remove = new ArrayList<>();
                        remove.add(e.getSlot());

                        assert e.getCursor() != null;
                        if (!trade.fitsTrade(player, remove, e.getCursor().clone())) fits = false;
                        break;
                    }

                    case "DROP_ALL_CURSOR":
                    case "DROP_ALL_SLOT":
                    case "DROP_ONE_CURSOR":
                    case "DROP_ONE_SLOT":
                        assert e.getCurrentItem() != null;
                        if (!trade.fitsTrade(player, e.getCurrentItem().clone())) fits = false;
                        break;

                    default:
                        fits = true;
                        break;
                }
            }
        }

        if (!fits) {
            player.sendMessage(Lang.getPrefix() + Lang.get("Trade_Partner_No_Space", player));
            TradeSystem.getInstance().getTradeManager().playBlockSound(player);
            e.setCancelled(true);
        } else trade.updateLater(1);
    }

    private void updateWaitForPickup(Trade trade, InventoryClickEvent e, Player player) {
        trade.getWaitForPickup()[trade.getId(player)] = true;
        Bukkit.getScheduler().runTaskLater(TradeSystem.getInstance(), () -> {
            trade.getCursor()[trade.getId(player)] = e.getCursor() != null && e.getCursor().getType() != Material.AIR;
            trade.getWaitForPickup()[trade.getId(player)] = false;
        }, 1);
    }
}
