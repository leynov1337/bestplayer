package ru.leyn.api.menu.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import ru.leyn.api.menu.LeynInventory;
import ru.leyn.api.menu.button.InventoryButton;

@RequiredArgsConstructor
public class InventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        LeynInventory inventory = LeynInventory.getInventoryMap().get(player.getName().toLowerCase());

        int slot = e.getSlot();

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || inventory == null || !inventory.getButtons().containsKey(slot + 1)) {
            return;
        }

        e.setCancelled(true);

        InventoryButton button = inventory.getButtons().get(slot + 1);

        if ( button.getCommand() == null ) {
            return;
        }

        button.getCommand().onClick(player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        LeynInventory inventory = LeynInventory.getInventoryMap().get(player.getName().toLowerCase());

        if (inventory == null) {
            return;
        }

        LeynInventory.getInventoryMap().remove(player.getName().toLowerCase());

        inventory.onClose(player);
    }

}
