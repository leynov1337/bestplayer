package ru.leyn.api.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.leyn.api.menu.button.InventoryButton;
import ru.leyn.api.menu.info.InventoryInfo;
import ru.leyn.api.Clickable;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class LeynInventory {

    private Inventory inventory;
    private final InventoryInfo info;
    private final Map<Integer, InventoryButton> buttons;

    @Getter
    private static final Map<String, LeynInventory> inventoryMap = new HashMap<>();

    public LeynInventory(String title, int rows) {
        this.info = new LeynInventoryInfo(title, rows * 9, rows);
        this.buttons = new HashMap<>();
        this.inventory = Bukkit.createInventory(null, info.getSize(), info.getTitle());
    }

    /**
     * Передающийся метод.
     *
     * Вызывается перед открытием инвентаря
     */
    public void onOpen(Player player) { }

    /**
     * Передающийся метод.
     *
     * Вызывается после закрытия инвентаря
     */
    public void onClose(Player player) { }

    /**
     * Установка названия инвенетарю
     */
    protected void setTitle(String title) {
        this.inventory = Bukkit.createInventory(null, inventory.getSize(), title);
    }

    /**
     * Установка количества строк в инвентаре
     */
    protected void setRows(int rows) {
        setSize(rows * 9);
    }

    /**
     * Установка размера инвентаря (rows * 9)
     */
    protected void setSize(int size) {
        this.inventory = Bukkit.createInventory(null, size, inventory.getTitle());
    }

    /**
     * Процесс генерации инвентаря, выставление предметов в сам инвентарь
     */
    public abstract void generateInventory(Player player);

    /**
     * Установка предмета в инвентарь
     */
    public void setItem(int slot, InventoryButton button) {
        buttons.put(slot, button);
    }

    /**
     * Установка предмета в инвентарь
     */
    public void setItem(int slot, ItemStack itemStack, Clickable<Player> clickable) {
        InventoryButton button = new LeynInventoryButton(itemStack, clickable);

        setItem(slot, button);
    }

    /**
     * Установка предмета в инвентарь
     */
    public void setItem(int slot, ItemStack itemStack) {
        InventoryButton button = new LeynInventoryButton(itemStack, player -> {});

        setItem(slot, button);
    }

    /**
     * Очистка предметов в инвентаре
     */
    public void clear() {
        buttons.clear();
        inventory.clear();
    }

    /**
     * Открытие инвентаря игроку
     */
    public void openInventory(Player player) {
        openInventory(player, true);
    }

    /**
     * Приватное открытие инвентаря игроку
     */
    private void openInventory(Player player, boolean isOpen) {
        this.generateInventory(player);

        if (isOpen) {
            player.openInventory(inventory);

            inventoryMap.put(player.getName().toLowerCase(), this);
        }

        setupItems();
    }

    private void setupItems() {
        for (Map.Entry<Integer, InventoryButton> buttonEntry : buttons.entrySet()) {
            inventory.setItem(buttonEntry.getKey() - 1, buttonEntry.getValue().getItem());
        }
    }

    /**
     * Обновлени инвентаря игроку
     */
    public void updateInventory(Player player) {
        clear();

        openInventory(player, false);
    }

    protected void updateInventory(Player player, Runnable command) {
        clear();

        command.run();

        openInventory(player, false);
    }



    @RequiredArgsConstructor
    @Getter
    public static class LeynInventoryButton implements InventoryButton {

        private final ItemStack item;
        private final Clickable<Player> command;
    }

    @RequiredArgsConstructor
    @Getter
    public static class LeynInventoryInfo implements InventoryInfo {

        private final String title;
        private final int size;
        private final int rows;
    }

}
