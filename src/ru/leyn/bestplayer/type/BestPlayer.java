package ru.leyn.bestplayer.type;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.leyn.api.hologram.HologramManager;
import ru.leyn.api.hologram.LeynHologram;
import ru.leyn.api.protocol.entity.impl.FakePlayer;
import ru.leyn.api.vault.VaultManager;
import ru.leyn.bestplayer.BestPlayerPlugin;
import ru.leyn.bestplayer.menu.BestPlayerMenu;

import java.sql.Timestamp;
import java.util.List;

@Getter
public class BestPlayer {

    @Setter
    private String playerName;

    @Setter
    private Timestamp expireTimestamp;

    @Setter
    private Location location;

    @Setter
    private int buyCost;


    @Setter
    private FakePlayer fakePlayer;

    @Setter
    private LeynHologram LeynHologram;


    public BestPlayer(String skinName, String playerName, Timestamp expireTimestamp, Location location, int buyCost) {
        this.playerName = playerName;
        this.expireTimestamp = expireTimestamp;
        this.location = location;
        this.buyCost = buyCost;

        this.fakePlayer = new FakePlayer(skinName, location);
        this.LeynHologram = new HologramManager.LeynHologramImpl(location.clone().add(0, .5, 0));

        this.fakePlayer.setClickAction(player -> {
            String inventoryTitle = BestPlayerPlugin.getInstance().getConfig().getString("gui.title");
            int inventoryRows = BestPlayerPlugin.getInstance().getConfig().getInt("gui.rows");

            new BestPlayerMenu(inventoryTitle, inventoryRows).openInventory(player);
        });

        this.LeynHologram.setClickAction(fakePlayer.getClickAction());
    }

    /**
     * Показать голограмму игроку
     *
     * @param player - игрок
     */
    public void showHologramToPlayer(Player player) {
        if (!player.getWorld().equals(this.location.getWorld())) {
            return;
        }

        VaultManager vaultManager = BestPlayerPlugin.getInstance().getVaultManager();
        List<String> hologramLines = BestPlayerPlugin.getInstance().getConfig().getStringList("hologram");

        if (isExpired()) {
            hologramLines = BestPlayerPlugin.getInstance().getConfig().getStringList("default-hologram");
        }

        String bestPlayerDisplayName = vaultManager.getVaultPlayer(playerName).getPrefix() + playerName;

        if (playerName.equals("default")) {
            bestPlayerDisplayName = "§c§o(Неизвестно)";
        }

        int buyCost = BestPlayerPlugin.getInstance().getConfig().getInt("start-sum");

        if (LeynHologram.getLines().isEmpty()) {
            for (String line : hologramLines) {
                line = ChatColor.translateAlternateColorCodes('&', line
                        .replace("%buy-cost%", String.valueOf(buyCost))
                        .replace("%best-player%", bestPlayerDisplayName));

                LeynHologram.addLine(line);
            }
        } else {
            int lineCounter = 0;
            for (String line : hologramLines) {
                line = ChatColor.translateAlternateColorCodes('&', line
                        .replace("%buy-cost%", String.valueOf(buyCost))
                        .replace("%best-player%", bestPlayerDisplayName));

                LeynHologram.modifyLine(lineCounter, line);

                lineCounter++;
            }
        }

        LeynHologram.setClickAction(fakePlayer.getClickAction());
        LeynHologram.addReceiver(player);
    }

    /**
     * Обновить голограмму для игрока
     *
     * @param player - игрок
     */
    public void updateHologramToPlayer(Player player) {
        LeynHologram.removeReceiver(player);

        showHologramToPlayer(player);
    }


    /**
     * Если время лучшего игрока истекло
     */
    public boolean isExpired() {
        return expireTimestamp == null || System.currentTimeMillis() >= expireTimestamp.getTime();
    }

}
