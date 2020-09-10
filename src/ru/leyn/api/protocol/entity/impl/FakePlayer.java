package ru.leyn.api.protocol.entity.impl;

import com.comphenix.protocol.wrappers.*;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.leyn.api.protocol.entity.LeynFakeEntity;
import ru.leyn.api.protocol.packet.entity.WrapperPlayServerNamedEntitySpawn;
import ru.leyn.api.protocol.packet.entity.WrapperPlayServerPlayerInfo;
import ru.leyn.api.protocol.packet.scoreboard.WrapperPlayServerScoreboardTeam;
import ru.leyn.api.utility.MojangUtil;
import ru.leyn.bestplayer.BestPlayerPlugin;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FakePlayer extends LeynFakeEntity {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final UUID uuid;
    private final MojangUtil.Skin skin;

    private final String name;

    private WrappedGameProfile wrappedGameProfile;


    public FakePlayer(String skin, Location location) {
        super(EntityType.PLAYER, location);

        this.name = "§8NPC [" + RANDOM.nextInt(999999) + "]";
        this.uuid = UUID.randomUUID();
        this.skin = MojangUtil.getSkinTextures(skin);
    }

    @Override
    public void sendSpawnPacket(Player player) {
        String teamName = getTeamName();

        sendTeamPacket(teamName, player, WrapperPlayServerScoreboardTeam.Mode.TEAM_CREATED);
        sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, player);

        WrapperPlayServerNamedEntitySpawn spawned = new WrapperPlayServerNamedEntitySpawn();

        spawned.setEntityID(getId());
        spawned.setPosition(getLocation().toVector());
        spawned.setPlayerUUID(uuid);

        spawned.setPitch(getLocation().getPitch());
        spawned.setYaw(getLocation().getYaw());

        spawned.sendPacket(player);

        sendEntityLookPacket(player);
        sendHeadRotationPacket(player);

        sendTeamPacket(teamName, player, WrapperPlayServerScoreboardTeam.Mode.PLAYERS_ADDED);

        new BukkitRunnable() {

            @Override
            public void run() {
                removeFakePlayer(player);
            }

        }.runTaskLater(BestPlayerPlugin.getInstance(), 20);
    }

    @Override
    public void onReceiverRemove(Player player) {
        sendTeamPacket(getTeamName(), player, WrapperPlayServerScoreboardTeam.Mode.PLAYERS_REMOVED);
        sendTeamPacket(getTeamName(), player, WrapperPlayServerScoreboardTeam.Mode.TEAM_REMOVED);
    }

    private void sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction action, Player player) {
        WrapperPlayServerPlayerInfo playerInfoPacket = new WrapperPlayServerPlayerInfo();

        this.wrappedGameProfile = new WrappedGameProfile(uuid, name);

        if (skin != null && action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
            wrappedGameProfile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
        }

        PlayerInfoData playerInfoData = new PlayerInfoData(wrappedGameProfile, 0,
                EnumWrappers.NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(name));

        playerInfoPacket.setAction(action);
        playerInfoPacket.setData(Collections.singletonList(playerInfoData));

        playerInfoPacket.sendPacket(player);
    }

    private void sendTeamPacket(String teamName, Player player, int mode) {
        WrapperPlayServerScoreboardTeam scoreboardTeam = new WrapperPlayServerScoreboardTeam();

        scoreboardTeam.setName(teamName);
        scoreboardTeam.setMode(mode);

        scoreboardTeam.setCollisionRule("never");
        scoreboardTeam.setNameTagVisibility("never");

        if (mode == WrapperPlayServerScoreboardTeam.Mode.TEAM_CREATED || mode == WrapperPlayServerScoreboardTeam.Mode.TEAM_UPDATED) {
            scoreboardTeam.setDisplayName(teamName);
            scoreboardTeam.setPrefix(getGlowingColor() == null ? "§8" : getGlowingColor().toString());
            scoreboardTeam.setPackOptionData(0);
            scoreboardTeam.setColor(0);
        } else {
            scoreboardTeam.setPlayers(Collections.singletonList(name));
        }

        scoreboardTeam.sendPacket(player);
    }

    @Override
    public void setGlowingColor(ChatColor glowingColor) {
        super.setGlowingColor(glowingColor);

        getReceivers().forEach(receiver ->
                sendTeamPacket(getTeamName(), receiver, WrapperPlayServerScoreboardTeam.Mode.TEAM_UPDATED));
    }

    private void removeFakePlayer(Player player) {
        WrapperPlayServerPlayerInfo playerInfoPacket = new WrapperPlayServerPlayerInfo();

        playerInfoPacket.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoPacket.setData(Collections.singletonList(
                new PlayerInfoData(wrappedGameProfile,
                        0,
                        EnumWrappers.NativeGameMode.ADVENTURE,
                        WrappedChatComponent.fromText(name))
        ));

        playerInfoPacket.sendPacket(player);
    }

    private String getTeamName() {
        String teamName = name + "_TEAM";

        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        return teamName;
    }
}
