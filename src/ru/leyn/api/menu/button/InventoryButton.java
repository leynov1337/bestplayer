package ru.leyn.api.menu.button;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leyn.api.Clickable;

public interface InventoryButton {

    Clickable<Player> getCommand();
    ItemStack getItem();
}
