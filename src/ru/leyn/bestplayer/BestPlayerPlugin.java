package ru.leyn.bestplayer;

import com.comphenix.protocol.ProtocolLibrary;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.leyn.api.command.CommandManager;
import ru.leyn.api.hologram.HologramManager;
import ru.leyn.api.menu.listener.InventoryListener;
import ru.leyn.api.mysql.MysqlConnection;
import ru.leyn.api.mysql.MysqlExecutor;
import ru.leyn.api.protocol.entity.impl.FakePlayer;
import ru.leyn.api.utility.CooldownUtil;
import ru.leyn.api.utility.LocationUtil;
import ru.leyn.api.vault.VaultManager;
import ru.leyn.bestplayer.command.BestPlayerCommand;
import ru.leyn.api.protocol.entity.listener.FakeEntityClickListener;
import ru.leyn.bestplayer.listener.PlayerListener;
import ru.leyn.bestplayer.type.BestPlayer;

import java.sql.Timestamp;

public final class BestPlayerPlugin extends JavaPlugin {

    @Getter
    private static BestPlayerPlugin instance; {
        instance = this;
    }


    @Getter
    private BestPlayer bestPlayer;


    @Getter
    private final HologramManager hologramManager = new HologramManager();

    @Getter
    private final CommandManager commandManager = new CommandManager();


    @Getter
    private MysqlExecutor database;

    @Getter
    private VaultManager vaultManager;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        connectToMysql();

        loadBestPlayer(); //подключились к миске? ну теперь загрузим лучшего игрока :/
        registerProtocolListeners();

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        commandManager.registerCommand(this, new BestPlayerCommand(), "bestplayer", "bplayer");

        this.vaultManager = new VaultManager(); //okay boomer, no comment this please.

        startUpdaters();
    }


    /**
     * Загружаем лучшего игрока из базы данных
     */
    public void loadBestPlayer() {
        String playerLocationString = getConfig().getString("npc-location");

        if (playerLocationString == null || playerLocationString.isEmpty()) {
            this.bestPlayer = null;
            return;
        }

        Location playerLocation = LocationUtil.stringToLocation( playerLocationString );

        this.bestPlayer = database.executeQuery(false, "SELECT * FROM `BestPlayer` WHERE `Id`=?", rs -> {

            if ( !rs.next() ) {
                String playerName = getConfig().getString("default-npc-skin");
                int buyCost = getConfig().getInt("start-sum");

                return new BestPlayer(playerName, getConfig().getString("default-npc-skin"),null, playerLocation, buyCost);
            }

            String playerName = rs.getString("Name");
            Timestamp expireDate = rs.getTimestamp("ExpireDate");

            int buyCost = rs.getInt("BuyCost");

            return new BestPlayer(playerName, playerName, expireDate, playerLocation, buyCost);
        }, 0);
    }

    /**
     * Запустить авто-обновления
     */
    private void startUpdaters() {
        new BukkitRunnable() {

            @Override
            public void run() {
                if (bestPlayer == null) {
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> bestPlayer.getFakePlayer().look(player));
            }

        }.runTaskTimer(this, 0, 2);

        new BukkitRunnable() {

            @Override
            public void run() {
                if (bestPlayer == null) {
                    return;
                }

                FakePlayer fakePlayer = bestPlayer.getFakePlayer();

                String playerPrefix = (vaultManager.getVaultPlayer(bestPlayer.getPlayerName()).getPrefix() + bestPlayer.getPlayerName()).replace("&", "§");
                ChatColor glowingColor = ChatColor.getByChar( playerPrefix.charAt(playerPrefix.indexOf("§") + 1) );

                fakePlayer.setSneaking(!fakePlayer.isSneaking());
                fakePlayer.setGlowingColor(glowingColor);

                bestPlayer.getLeynHologram().setLocation(bestPlayer.getLeynHologram()
                        .getLocation()
                        .clone()
                        .subtract(0, fakePlayer.isSneaking() ? .25 : -.25, 0));
            }

        }.runTaskTimer(this, 20, 20);


        new BukkitRunnable() {

            @Override
            public void run() {
                if (bestPlayer == null) {
                    return;
                }

                removeNPC();
                loadBestPlayer();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    showBestPlayer(player);
                }

            }

        }.runTaskTimer(this, 0, 20 * 60);
    }

    /**
     * Подключение к базе данных MySQl
     */
    private void connectToMysql() {
        int port = getConfig().getInt("mysql.port");

        String host = getConfig().getString("mysql.host");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");
        String database = getConfig().getString("mysql.database");

        this.database = MysqlConnection.newBuilder()
                .setHost(host)
                .setPort(port)
                .setUsername(username)
                .setPassword(password)
                .setDatabase(database)

                .createTable("BestPlayer", "`Id` INT NOT NULL PRIMARY KEY, `Name` VARCHAR(16) NOT NULL, `ExpireDate` TIMESTAMP, `BuyCost` INT NOT NULL")

                .build().getExecutor();
    }

    /**
     * Регистрация листенера для фейковых Entity
     * (иначе говоря, пакетных, созданных на основе ProtocolLib)
     */
    private void registerProtocolListeners() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new FakeEntityClickListener(this));
    }

    /**
     * Если игрок является лучшим игроком на сервере
     *
     * @param player - игрок, которого проверяем
     */
    public boolean isBestPlayer(Player player) {
        if (bestPlayer == null) {
            return false;
        }

        if (bestPlayer.getPlayerName().equals("default")) {
            return false;
        }

        if (bestPlayer.isExpired()) {
            return false;
        }

        return player.getName().toLowerCase().equals(bestPlayer.getPlayerName().toLowerCase());
    }

    /**
     * Перекупить лучшего игрока
     *
     * @param player - лучший игрок
     * @param buyCost - цена, за которую перекупают
     */
    public void buyBestPlayer(Player player, int buyCost) {
        if (CooldownUtil.hasCooldown("buy-bestplayer")) {
            player.sendMessage("§cОшибка, подождите немного, пожалуйста!");

            return;
        }

        //update best player data
        Timestamp expireDate = new Timestamp(System.currentTimeMillis() + ((60L * 60L * 24L * 30L) * 1000L));

        this.bestPlayer.setPlayerName(player.getName());
        this.bestPlayer.setExpireTimestamp(expireDate);
        this.bestPlayer.setBuyCost(buyCost);

        this.database.execute(true, "INSERT INTO `BestPlayer` (`Id`,`Name`,`ExpireDate`,`BuyCost`) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE `Name`=?, `ExpireDate`=?, `BuyCost`=?", 0,
                bestPlayer.getPlayerName(), bestPlayer.getExpireTimestamp(), bestPlayer.getBuyCost(),
                bestPlayer.getPlayerName(), bestPlayer.getExpireTimestamp(), bestPlayer.getBuyCost());

        //change npc skin
        updateNPC();


        //particles
        player.getWorld().playEffect(bestPlayer.getLocation(), Effect.FIREWORKS_SPARK, 30);
        player.getWorld().playSound(bestPlayer.getLocation(), Sound.FIREWORK_BLAST, 1, 1);

        CooldownUtil.putCooldown("buy-bestplayer", 1000 * 15);
    }

    /**
     * Обновить NPC
     */
    private void updateNPC() {
        bestPlayer.getFakePlayer().remove();
        Bukkit.getOnlinePlayers().forEach(player -> bestPlayer.updateHologramToPlayer(player));

        bestPlayer.setFakePlayer(new FakePlayer(bestPlayer.getPlayerName(), bestPlayer.getLocation()));
        bestPlayer.getFakePlayer().setClickAction(bestPlayer.getLeynHologram().getClickAction());
        bestPlayer.getFakePlayer().setGlowingColor(ChatColor.getByChar(vaultManager.getVaultPlayer(bestPlayer.getPlayerName())
                .getPrefix()
                .trim()
                .substring(1)));

        bestPlayer.getFakePlayer().spawn();
    }

    public void removeNPC() {
        bestPlayer.getFakePlayer().remove();
        bestPlayer.getLeynHologram().remove();
    }

    public void showBestPlayer(Player player) {
        BestPlayerPlugin.getInstance().getDatabase().ASYNC_THREAD_POOL.submit(() -> {
            BestPlayer bestPlayerNPC = BestPlayerPlugin.getInstance().getBestPlayer();

            bestPlayerNPC.getFakePlayer().spawnToPlayer(player);
            bestPlayerNPC.showHologramToPlayer(player);
        });
    }

}
