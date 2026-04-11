package io.github.christechs.routerec.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;

public class SpeedCommand extends Command {

    public SpeedCommand() {
        super("speed");

        var speedArg = ArgumentType.Float("speed");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /speed <amount>", NamedTextColor.RED));
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            float speed = context.get(speedArg) / 10.0f;

            if (player.isFlying()) {
                player.setFlyingSpeed(speed);
                player.sendMessage(Component.text("Flight speed set to " + context.get(speedArg), NamedTextColor.GREEN));
            } else {
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
                player.sendMessage(Component.text("Walk speed set to " + context.get(speedArg), NamedTextColor.GREEN));
            }
        }, speedArg);
    }
}