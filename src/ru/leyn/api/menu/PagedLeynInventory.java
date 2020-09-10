package ru.leyn.api.menu;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leyn.api.utility.ItemUtil;
import ru.leyn.api.Clickable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class PagedLeynInventory extends LeynInventory {

    private int page, pagesCount;

    private Player viewerPlayer;

    private final String inventoryTitle;

    private LinkedHashMap<ItemStack, Clickable<Player>> buttonMap;
    private List<Integer> slotsList;

    /**
     * Инициализация инвентаря
     *
     * @param inventoryTitle - Заголовок инвентаря
     * @param inventoryRows  - Количество строк в инвентаре
     */
    public PagedLeynInventory(String inventoryTitle, int inventoryRows) {
        this(0, inventoryTitle, inventoryRows);
    }

    /**
     * Инициализация инвентаря
     *
     * @param page           - Страница инвентаря
     * @param inventoryTitle - Заголовок инвентаря
     * @param inventoryRows  - Количество строк в инвентаре
     */
    private PagedLeynInventory(int page, String inventoryTitle, int inventoryRows) {
        super(inventoryTitle.concat(" | " + (page + 1)), inventoryRows);

        this.page = page;
        this.inventoryTitle = inventoryTitle;
    }

    /**
     * Инициализация предметов в инвентарь
     */
    public abstract LinkedHashMap<ItemStack, Clickable<Player>> initializeItems();

    /**
     * Инициализация слотов, на которых будут стоять предметы
     */
    public abstract List<Integer> initializeSlots();


    /**
     * На страницу назад
     */
    private void backward(Player player) {
        if (page - 1 < 0) {
            throw new RuntimeException( String.format("Page cannot be < 0 (%s - 1 < 0)", page) );
        }

        this.page--;

        setTitle( inventoryTitle.concat(" | " + (page + 1)) );

        openInventory(player);
    }

    /**
     * На страницу вперед
     */
    private void forward(Player player) {
        if (page >= pagesCount) {
            throw new RuntimeException( String.format("Page cannot be >= max pages count (%s >= %s)", page, pagesCount) );
        }

        this.page++;

        setTitle( inventoryTitle.concat(" | " + (page + 1)) );

        openInventory(player);
    }

    /**
     * Построение страничного инвентаря
     */
    private void buildPage(Player player) {
        this.viewerPlayer = player;

        this.buttonMap = initializeItems();
        this.slotsList = initializeSlots();

        this.pagesCount = buttonMap.size() / slotsList.size();

        if ( !(page >= pagesCount) ) {
            setItem(getInventory().getSize() - 3, ItemUtil.getItemStack(Material.ARROW,
                    "§eВперед"), player1 -> forward(player));
        }

        if ( !(page - 1 < 0) ) {
            setItem(getInventory().getSize() - 5, ItemUtil.getItemStack(Material.ARROW,
                    "§eНазад"), player1 -> backward(player));
        }


        for (int i = 0; i < slotsList.size(); i++) {
            int index = page * slotsList.size() + i;

            if (buttonMap.size() <= index) {
                return;
            }

            int slot = slotsList.get(i);

            Map.Entry<ItemStack, Clickable<Player>> itemEntry = new ArrayList<>(buttonMap.entrySet()).get(index);

            ItemStack itemStack = itemEntry.getKey();

            setItem(slot, itemStack, itemEntry.getValue());
        }
    }


    /**
     * Открытие инвентаря игроку
     */
    @Override
    public void openInventory(Player player) {
        clear();

        buildPage(player);

        super.openInventory(player);
    }

    /**
     * Обновление инвентаря игроку
     */
    @Override
    public void updateInventory(Player player) {
        super.updateInventory(player, () -> {
            clear();

            buildPage(player);
            generateInventory(player);
        });
    }
}
