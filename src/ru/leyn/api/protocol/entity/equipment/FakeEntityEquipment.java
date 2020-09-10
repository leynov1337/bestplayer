package ru.leyn.api.protocol.entity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leyn.api.protocol.entity.LeynFakeEntity;
import ru.leyn.api.protocol.packet.entity.WrapperPlayServerEntityEquipment;

import java.util.EnumMap;
import java.util.Map;

@RequiredArgsConstructor
public class FakeEntityEquipment {

    private final LeynFakeEntity fakeEntity;

    private final EnumMap<EnumWrappers.ItemSlot, ItemStack> equipmentMap = new EnumMap<>(EnumWrappers.ItemSlot.class);

    public ItemStack getEquipment(EnumWrappers.ItemSlot itemSlot) {
        return equipmentMap.get(itemSlot);
    }

    public void setEquipment(EnumWrappers.ItemSlot itemSlot, ItemStack itemStack) {
        equipmentMap.put(itemSlot, itemStack);

        fakeEntity.getReceivers().forEach(receiver -> sendEquipmentPacket(itemSlot, itemStack, receiver));
    }

    public void sendEquipmentPacket(EnumWrappers.ItemSlot itemSlot, ItemStack itemStack, Player player) {
        WrapperPlayServerEntityEquipment entityEquipment = new WrapperPlayServerEntityEquipment();

        entityEquipment.setEntityID(fakeEntity.getId());
        entityEquipment.setSlot(itemSlot);
        entityEquipment.setItem(itemStack);

        entityEquipment.sendPacket(player);
    }

    public void updateEquipmentPacket(Player player) {
        for (Map.Entry<EnumWrappers.ItemSlot, ItemStack> equipmentEntry : equipmentMap.entrySet()) {
            sendEquipmentPacket(equipmentEntry.getKey(), equipmentEntry.getValue(), player);
        }
    }

}
