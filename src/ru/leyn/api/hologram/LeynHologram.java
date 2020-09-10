package ru.leyn.api.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public interface LeynHologram {

    List<String> getLines();

    Location getLocation();

    Consumer<Player> getClickAction();

    int getLineCount();

    String getLine(int index);

    void addLine(String line);

    void modifyLine(int index, String line);

    void spawn();

    void clear();

    void addReceiver(Player player);

    void remove();

    void removeReceiver(Player player);

    void setLocation(Location location);

    void setClickAction(Consumer<Player> clickAction);

    void refreshHologram();

}
