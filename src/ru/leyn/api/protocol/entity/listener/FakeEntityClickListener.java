package ru.leyn.api.protocol.entity.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.leyn.api.protocol.entity.LeynFakeEntity;
import ru.leyn.api.utility.PlayerCooldownUtil;

public class FakeEntityClickListener extends PacketAdapter {

    public FakeEntityClickListener(Plugin plugin) {
        super(plugin, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        if (PlayerCooldownUtil.hasCooldown("fake-entity_click", player)) {
            return;
        }

        LeynFakeEntity fakeEntity = LeynFakeEntity.getEntityById(event.getPacket().getIntegers().read(0));

        if (fakeEntity == null || fakeEntity.getClickAction() == null) {
            return;
        }

        fakeEntity.getClickAction().accept(player);

        PlayerCooldownUtil.putCooldown("fake-entity_click", player, 1000);
    }

}
