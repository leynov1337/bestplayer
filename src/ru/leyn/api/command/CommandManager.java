package ru.leyn.api.command;

import org.bukkit.plugin.Plugin;
import ru.leyn.api.type.AbstractCacheManager;

import java.util.Arrays;

public final class CommandManager extends AbstractCacheManager<LeynCommand> {

    /**
     * Регистрация команды, добавление в org.bukkit.command.CommandMap
     */
    public void registerCommand(Plugin plugin, LeynCommand command, String... aliases) {
        CommandRegister.reg(plugin, (sender, command1, s, args) -> {
            try {
                command.execute(sender, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }, Arrays.asList(aliases));

        cacheData(aliases[0], command);
    }

    /**
     * Получение самой команды по ее имени
     */
    public LeynCommand getCommandByName(String commandName) {
        return getCache(commandName);
    }

}
