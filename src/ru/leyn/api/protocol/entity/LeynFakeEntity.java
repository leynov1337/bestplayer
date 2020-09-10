package ru.leyn.api.protocol.entity;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.leyn.api.protocol.entity.animation.FakeEntityAnimation;
import ru.leyn.api.protocol.entity.equipment.FakeEntityEquipment;
import ru.leyn.api.protocol.packet.entity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public abstract class LeynFakeEntity {

    protected static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);
    protected static final WrappedDataWatcher.Serializer INT_SERIALIZER = WrappedDataWatcher.Registry.get(Integer.class);
    protected static final WrappedDataWatcher.Serializer STRING_SERIALIZER = WrappedDataWatcher.Registry.get(String.class);
    protected static final WrappedDataWatcher.Serializer BOOLEAN_SERIALIZER = WrappedDataWatcher.Registry.get(Boolean.class);


    private static final FieldAccessor ENTITY_ID = Accessors.getFieldAccessor(
            MinecraftReflection.getEntityClass(), "entityCount", true);

    private static final List<LeynFakeEntity> ENTITIES = new ArrayList<>();

    protected LeynFakeEntity(int id, EntityType entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    public static List<LeynFakeEntity> getEntities() {
        return Collections.unmodifiableList(ENTITIES);
    }

    public static LeynFakeEntity getEntityById(int id) {
        for (LeynFakeEntity fakeEntity : ENTITIES) {
            if (fakeEntity.getId() != id) {
                continue;
            }

            return fakeEntity;
        }

        return null;
    }

    private final int id;

    private final EntityType entityType;

    private final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
    private final FakeEntityEquipment entityEquipment = new FakeEntityEquipment(this);

    private final List<Player> receivers = new ArrayList<>();


    private Location location;

    @Setter
    private Consumer<Player> clickAction;

    private boolean burning, sneaking, sprinting, invisible, elytraFlying, customNameVisible;

    private String customName;

    private ChatColor glowingColor;

    public LeynFakeEntity(EntityType entityType, Location location) {
        Object objectId = ENTITY_ID.get(null);

        this.id = objectId instanceof AtomicInteger ? ((AtomicInteger) objectId).get() : ((int) objectId);
        this.entityType = entityType;
        this.location = location;

        ENTITY_ID.set(null, objectId instanceof AtomicInteger ? new AtomicInteger(id + 1) : id + 1);
        ENTITIES.add(this);
    }

    public void spawn() {
        Bukkit.getOnlinePlayers().forEach(this::spawnToPlayer);
    }

    public void remove() {
        Bukkit.getOnlinePlayers().forEach(this::removeToPlayer);
    }

    public void update() {
        Bukkit.getOnlinePlayers().forEach(this::updateToPlayer);
    }

    public void updateToPlayer(Player player) { }

    public void spawnToPlayer(@NonNull Player player) {
        if (!player.getWorld().equals(this.location.getWorld())) {
            return;
        }

        receivers.add(player);

        sendSpawnPacket(player);
        onReceiverAdd(player);

        getEntityEquipment().updateEquipmentPacket(player);
    }

    public void removeToPlayer(@NonNull Player player) {
        receivers.remove(player);

        sendDestroyPacket(player);
        onReceiverRemove(player);
    }

    public boolean hasSpawnedToPlayer(@NonNull Player player) {
        return receivers.contains(player);
    }

    public void teleport(Location location) {
        this.location = location;

        sendTeleportPacket();
    }

    public void setSneaking(boolean sneaking) {
        if (sneaking == this.sneaking) {
            return;
        }

        this.sneaking = sneaking;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    public void setCustomNameVisible(boolean customNameVisible) {
        if (customNameVisible == this.customNameVisible) {
            return;
        }

        this.customNameVisible = customNameVisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, BOOLEAN_SERIALIZER), customNameVisible);
        sendDataWatcherPacket();
    }

    public void setCustomName(@NonNull String customName) {
        this.customName = customName;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, STRING_SERIALIZER), customName);
        sendDataWatcherPacket();
    }

    public void setSprinting(boolean sprinting) {
        if (sprinting == this.sprinting) {
            return;
        }

        this.sprinting = sprinting;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    public void setBurning(boolean burning) {
        if (burning == this.burning) {
            return;
        }

        this.burning = burning;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    public void setInvisible(boolean invisible) {
        if (invisible == this.invisible) {
            return;
        }

        this.invisible = invisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    public void setElytraFlying(boolean elytraFlying) {
        if (elytraFlying == this.elytraFlying) {
            return;
        }

        this.elytraFlying = elytraFlying;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    /**
     * Для обычных энтити цвет свечения будет по умолчанию
     * серый, для FakePlayer (NPC-игрока) будет в зависимости
     * от цвета тимы
     */
    public void setGlowingColor(ChatColor glowingColor) {
        if (glowingColor == this.glowingColor) {
            return;
        }

        this.glowingColor = glowingColor;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), generateBitMask());
        sendDataWatcherPacket();
    }

    public void look(Player player, Location location) {
        if (!player.getWorld().equals(this.location.getWorld())) {
            return;
        }

        Vector vector = location.clone().subtract(this.location).toVector().normalize();

        this.location.setDirection(vector);
        this.location.setYaw(this.location.getYaw());
        this.location.setPitch(this.location.getPitch());

        sendEntityLookPacket(player);
        sendHeadRotationPacket(player);
    }

    public void look(Player player) {
        look(player, player.getLocation());
    }

    public void look(float yaw, float pitch) {
        location.setYaw(yaw);
        location.setPitch(pitch);

        for (Player receiver : receivers) {
            sendEntityLookPacket(receiver);
            sendHeadRotationPacket(receiver);
        }
    }

    public final void playAnimation(FakeEntityAnimation entityAnimation) {
        receivers.forEach(receiver -> playAnimation(receiver, entityAnimation));
    }

    public final void playAnimation(Player player, FakeEntityAnimation entityAnimation) {
        WrapperPlayServerAnimation animationPacket = new WrapperPlayServerAnimation();

        animationPacket.setEntityID(id);
        animationPacket.setAnimation(entityAnimation.ordinal());

        animationPacket.sendPacket(player);
    }

    protected final void sendDataWatcherPacket() {
        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata();

        metadataPacket.setEntityID(id);
        metadataPacket.setMetadata(dataWatcher.getWatchableObjects());

        receivers.forEach(metadataPacket::sendPacket);
    }

    protected final void sendTeleportPacket() {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport();

        packet.setEntityID(id);

        packet.setX(location.getX());
        packet.setY(location.getY());
        packet.setZ(location.getZ());

        packet.setYaw(location.getYaw());
        packet.setPitch(location.getPitch());

        receivers.forEach(packet::sendPacket);
    }

    protected final void sendEntityLookPacket(Player player) {
        WrapperPlayServerEntityLook entityLook = new WrapperPlayServerEntityLook();

        entityLook.setEntityID(id);
        entityLook.setYaw(location.getYaw());
        entityLook.setPitch(location.getPitch());

        entityLook.sendPacket(player);
    }

    protected final void sendHeadRotationPacket(Player player) {
        WrapperPlayServerEntityHeadRotation headRotation = new WrapperPlayServerEntityHeadRotation();

        headRotation.setEntityID(id);
        headRotation.setHeadYaw((byte) ((int) (location.getYaw() * 256.0F / 360.0F)));

        headRotation.sendPacket(player);
    }

    protected final void sendDestroyPacket(Player player) {
        WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy();

        packet.setEntityIds(new int[]{id});

        packet.sendPacket(player);
    }

    /**
     * Отправка пакета спавна энтити, для некоторых
     * может отличаться, поэтому можно переопределять
     */
    protected void sendSpawnPacket(Player player) {
        WrapperPlayServerSpawnEntityLiving packet = new WrapperPlayServerSpawnEntityLiving();

        packet.setEntityID(id);
        packet.setType(entityType);
        packet.setMetadata(dataWatcher);

        packet.setX(location.getX());
        packet.setY(location.getY());
        packet.setZ(location.getZ());

        packet.setYaw(location.getYaw());
        packet.setPitch(location.getPitch());

        packet.sendPacket(player);
    }

    protected void onReceiverAdd(Player player) { }

    protected void onReceiverRemove(Player player) { }

    private byte generateBitMask() {
        return (byte) ((burning ? 0x01 : 0) + (sneaking ? 0x02 : 0) + (sprinting ? 0x08 : 0) + (invisible ? 0x20 : 0) + (glowingColor != null ? 0x40 : 0) + (elytraFlying ? 0x80 : 0));
    }

}
