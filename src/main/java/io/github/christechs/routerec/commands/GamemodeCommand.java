package io.github.christechs.routerec.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class GamemodeCommand extends Command {

    public GamemodeCommand() {
        super("gamemode");

        var typeArg = ArgumentType.String("type");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /gamemode <creative|survival|spectator>", NamedTextColor.RED));
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            String gamemodeType = context.get(typeArg).toLowerCase();

            if (gamemodeType.startsWith("s")) {
                if (gamemodeType.equals("spectator")) {
                    player.setGameMode(GameMode.SPECTATOR);
                } else {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setAllowFlying(true);
                    player.setFlying(true);
                }
            } else if (gamemodeType.startsWith("c")) {
                player.setGameMode(GameMode.CREATIVE);
            }
        }, typeArg);
    }

}