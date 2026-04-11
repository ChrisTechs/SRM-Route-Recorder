package io.github.christechs.routerec.commands;

import io.github.christechs.routerec.manager.RenderMode;
import io.github.christechs.routerec.manager.RouteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class RoutesCommand extends Command {

    public RoutesCommand() {
        super("routes");

        var actionArg = ArgumentType.Word("action").from("renderSteps", "disableRendering", "renderAll");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String action = context.get(actionArg).toLowerCase();

            switch (action) {
                case "rendersteps" -> {
                    RouteManager.renderMode = RenderMode.STEP;
                    RouteManager.renderStepIndex = 0;
                    player.sendMessage(Component.text("Step Rendering ENABLED.", NamedTextColor.GREEN));
                }
                case "disablerendering" -> {
                    RouteManager.renderMode = RenderMode.NONE;
                    player.sendMessage(Component.text("Rendering DISABLED.", NamedTextColor.RED));
                }
                case "renderall" -> {
                    RouteManager.renderMode = RenderMode.ALL;
                    player.sendMessage(Component.text("Rendering ALL routes.", NamedTextColor.GREEN));
                }
            }

            if (RouteManager.currentRoomName != null) {
                RouteManager.visualize(RouteManager.currentRoomName, player.getInstance());
            }

        }, actionArg);
    }
}