package ru.leyn.bestplayer.menu;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leyn.api.Clickable;
import ru.leyn.api.menu.LeynInventory;
import ru.leyn.api.utility.IntegerUtil;
import ru.leyn.api.utility.ItemUtil;
import ru.leyn.api.vault.VaultManager;
import ru.leyn.bestplayer.BestPlayerPlugin;
import ru.leyn.bestplayer.type.BestPlayer;

import java.util.List;

public class BestPlayerMenu extends LeynInventory {

    public BestPlayerMenu(String title, int rows) {
        super(title, rows);
    }

    @Override
    public void generateInventory(Player player) {
        BestPlayer bestPlayer = BestPlayerPlugin.getInstance().getBestPlayer();
        Economy vaultEconomy = BestPlayerPlugin.getInstance().getVaultManager().getEconomyManager().getVaultEconomy();

        int startSum = BestPlayerPlugin.getInstance().getConfig().getInt("start-sum");

        if (bestPlayer == null) {
            player.sendMessage("§cBestPlayer == null");
            player.closeInventory();

            return;
        }

        if (!bestPlayer.isExpired()) {
            startSum = bestPlayer.getBuyCost();
        }

        if (bestPlayer.getExpireTimestamp() != null) {
            setItem("bestplayer", null);
        } else {
            setItem(getSlot("bestplayer"), ItemUtil.newBuilder(Material.STAINED_GLASS_PANE)
                    .setDurability(14)
                    .setName("§c")
                    .setLore("§cНа данный момент этот статус никто еще не выкупил,",
                            "§cа Вы можете быть первыми!",
                            "")
                    .build());
        }

        setItem("buy1", getBuyItemClickable(vaultEconomy, player, "buy1", startSum));
        setItem("buy2", getBuyItemClickable(vaultEconomy, player, "buy2", startSum));
        setItem("buy3", getBuyItemClickable(vaultEconomy, player, "buy3", startSum));
    }


    /**
     * Установить предмет в инвентарь по названию секции
     *
     * @param sectionName - секция
     * @param playerClickable - действие при клике
     */
    private void setItem(String sectionName, Clickable<Player> playerClickable) {
        int slot = getSlot( sectionName );
        ItemStack itemStack = getConfigItem( sectionName );

        if (itemStack == null) {
            return;
        }

        setItem(slot, itemStack, playerClickable);
    }

    private Clickable<Player> getBuyItemClickable(Economy vaultEconomy, Player player, String sectionName, int startSum) {
        return player1 -> {
            int paySum = startSum + getGivingBuyCost(sectionName);

            if (BestPlayerPlugin.getInstance().isBestPlayer(player)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        BestPlayerPlugin.getInstance().getConfig().getString("messages.already_bestplayer")));
                return;
            }

            //CoinPlayer coinPlayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());

            if (!vaultEconomy.has(player, paySum)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        BestPlayerPlugin.getInstance().getConfig().getString("messages.not_enough_money")));
                return;
            }

            player.closeInventory();
            //coinPlayer.removeCoins(paySum);
            vaultEconomy.withdrawPlayer(player, paySum);

            BestPlayerPlugin.getInstance().buyBestPlayer(player, paySum);
        };
    }

    /**
     * Получить конфигурационный предмет по наименованию сенкции
     *
     * @param sectionName - имя секции
     */
    private ItemStack getConfigItem(String sectionName) {
        // best player
        BestPlayer bestPlayer = BestPlayerPlugin.getInstance().getBestPlayer();
        VaultManager vaultManager = BestPlayerPlugin.getInstance().getVaultManager();

        String bestPlayerDisplayName = vaultManager.getChatManager().getVaultChat().getPlayerPrefix((String)null, bestPlayer.getPlayerName())
                 + bestPlayer.getPlayerName();

        if (bestPlayer.getPlayerName().equals("default")) {
            bestPlayerDisplayName = "§c§o(Неизвестно)";
        }

        //buy cost
        int buyCost = BestPlayerPlugin.getInstance().getConfig().getInt("start-sum");

        if ( !bestPlayer.isExpired() ) {
            buyCost = bestPlayer.getBuyCost();
        }


        // item
        ConfigurationSection itemSection
                = BestPlayerPlugin.getInstance().getConfig().getConfigurationSection("gui.items." + sectionName);

        if (itemSection == null) {
            return null;
        }

        String skullTexture = itemSection.getString("texture");
        String colouredName = ChatColor.translateAlternateColorCodes('&', itemSection.getString("name"));

        List<String> colouredLore = itemSection.getStringList("lore");

        //colorizing lore
        String expireDate = "§cНеизвестно";

        if (bestPlayer.getExpireTimestamp() != null) {
            expireDate = IntegerUtil.getTime((int) ((bestPlayer.getExpireTimestamp().getTime() - System.currentTimeMillis()) / 1000L));
        }

        for (int i = 0 ; i < colouredLore.size() ; i++) {
            String loreLine = ChatColor.translateAlternateColorCodes('&', colouredLore.get(i)
                    .replace("%expire-date%", expireDate)
                    .replace("%buy-cost%", IntegerUtil.spaced(buyCost + getGivingBuyCost(sectionName)))
                    .replace("%best-player%", bestPlayerDisplayName));

            colouredLore.set(i, loreLine);
        }

        return ItemUtil.newBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setName(colouredName)
                .setLore(colouredLore)
                .setPlayerSkull(skullTexture == null ? BestPlayerPlugin.getInstance().getBestPlayer().getPlayerName() : null)
                .setTextureValue(skullTexture).build();
    }


    /**
     * Получить слот предмета по названию секции
     *
     * @param sectionName - имя секции предмета
     */
    private int getSlot(String sectionName) {
        return BestPlayerPlugin.getInstance().getConfig().getInt("gui.items." + sectionName + ".slot");
    }

    /**
     * Получить количество добавляемой суммы к
     * перекупке лучшего игрока
     *
     * @param sectionName - имя секции
     */
    private int getGivingBuyCost(String sectionName) {
        return BestPlayerPlugin.getInstance().getConfig().getInt("gui.items." + sectionName + ".cost-add");
    }

}
