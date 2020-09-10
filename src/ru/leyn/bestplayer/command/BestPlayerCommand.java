package ru.leyn.bestplayer.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.leyn.api.command.LeynCommand;
import ru.leyn.api.utility.LocationUtil;
import ru.leyn.bestplayer.BestPlayerPlugin;

public final class BestPlayerCommand implements LeynCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        if (!sender.hasPermission("bestplayer.setup")) {
            return;
        }

        Player player = ((Player) sender);

        BestPlayerPlugin.getInstance().getConfig().set("npc-location", LocationUtil.locationToString(player.getLocation()));
        BestPlayerPlugin.getInstance().saveConfig();

        BestPlayerPlugin.getInstance().loadBestPlayer();

        player.sendMessage(ChatColor.GREEN + "Локация успешно установлена!");
    }

}
