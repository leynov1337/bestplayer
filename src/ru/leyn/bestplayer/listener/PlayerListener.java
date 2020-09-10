package ru.leyn.bestplayer.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.leyn.bestplayer.BestPlayerPlugin;
import ru.leyn.bestplayer.type.BestPlayer;

public final class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BestPlayer bestPlayer = BestPlayerPlugin.getInstance().getBestPlayer();

        //создаем npc
        if (bestPlayer == null || bestPlayer.getLocation() == null) {
            return;
        }

        BestPlayerPlugin.getInstance().showBestPlayer(player);

        //чисто фича
        if ( BestPlayerPlugin.getInstance().isBestPlayer(player) ) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    BestPlayerPlugin.getInstance().getConfig().getString("messages.status_not_expired")));
        }
    }

}
