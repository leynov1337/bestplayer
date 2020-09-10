package ru.leyn.api.hologram;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.leyn.api.Applicable;
import ru.leyn.api.protocol.entity.impl.FakeArmorStand;
import ru.leyn.api.type.AbstractCacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class HologramManager extends AbstractCacheManager<LeynHologram> {

    /**
     * Кеширование голограммы в мапу по ее имени.
     */
    public void cacheHologram(String hologramName, LeynHologram hologram) {
        cacheData(hologramName.toLowerCase(), hologram);
    }

    /**
     * Получение голограммы из кеша по ее имени.
     */
    public LeynHologram getCachedHologram(String hologramName) {
        return getCache(hologramName.toLowerCase());
    }

    /**
     * Создание голограммы без использования абстракции.
     *
     * Все действия можно проводить через специальный для этого
     * Applicable, что указан в аргументах.
     */
    public void createHologram(String hologramName, Location location, Applicable<LeynHologram> hologramApplicable) {
        LeynHologram hologram = createHologram(location);

        cacheHologram(hologramName, hologram);

        hologramApplicable.apply(hologram);
    }

    /**
     * Создание голограммы и ее получение.
     */
    public LeynHologram createHologram(Location location) {
        return new LeynHologramImpl(location);
    }


    @Getter
    public static class LeynHologramImpl implements LeynHologram {

        private Location location;

        public LeynHologramImpl(Location location) {
            this.location = location;
        }


        private final List<FakeArmorStand> entities = new ArrayList<>();
        private final List<String> lines = new ArrayList<>();

        private Consumer<Player> clickAction;

        private final double distance = 0.25D;

        @Override
        public int getLineCount() {
            return getLines().size();
        }

        @Override
        public String getLine(int index) {
            return lines.get(index);
        }

        @Override
        public void addLine(String line) {
            if (location == null || location.getWorld() == null) {
                return;
            }

            FakeArmorStand stand = new FakeArmorStand(location.clone().add(0, -(distance * lines.size()), 0));

            stand.setInvisible(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(line);

            stand.setClickAction(clickAction);

            Bukkit.getOnlinePlayers().forEach(stand::spawnToPlayer);

            entities.add(stand);
            lines.add(line);
        }

        @Override
        public void modifyLine(int index, String line) {
            lines.set(index, line);

            refreshHologram();
        }

        @Override
        public void spawn() {
            Bukkit.getOnlinePlayers().forEach(this::addReceiver);
        }

        @Override
        public void clear() {
            for (FakeArmorStand fakeArmorStand : entities) {
                fakeArmorStand.remove();
            }

            entities.clear();
            lines.clear();
        }

        @Override
        public void addReceiver(Player player) {
            entities.forEach(fakeArmorStand -> fakeArmorStand.spawnToPlayer(player));
        }

        @Override
        public void remove() {
            Bukkit.getOnlinePlayers().forEach(this::removeReceiver);

            entities.clear();
        }

        @Override
        public void removeReceiver(Player player) {
            entities.forEach(fakeArmorStand -> fakeArmorStand.removeToPlayer(player));
        }

        @Override
        public void setLocation(Location location) {
            this.location = location;

            int count = 0;
            for (FakeArmorStand stand : entities) {
                stand.teleport(location.clone().add(0, -(distance * count), 0));

                count++;
            }
        }

        @Override
        public void setClickAction(Consumer<Player> clickAction) {
            this.clickAction = clickAction;

            entities.forEach(fakeArmorStand -> fakeArmorStand.setClickAction(clickAction));
        }

        @Override
        public void refreshHologram() {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                FakeArmorStand stand = entities.get(i);

                stand.setCustomName(line);
            }

            setLocation(location);
        }
    }

}
