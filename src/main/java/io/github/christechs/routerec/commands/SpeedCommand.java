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
            sender.sendMessage(Component.text("Usage: /speed <Hypixel Speed Stat>", NamedTextColor.RED));
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            float hypixelSpeedStat = context.get(speedArg);
            float speedModifier = hypixelSpeedStat / 100.0f;

            if (player.isFlying()) {
                float baseFlySpeed = 0.05f;
                player.setFlyingSpeed(baseFlySpeed * speedModifier);
                player.sendMessage(Component.text("Flight Speed set to " + hypixelSpeedStat, NamedTextColor.GREEN));
            } else {
                float baseWalkSpeed = 0.1f;
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(baseWalkSpeed * speedModifier);
                player.sendMessage(Component.text("Walk Speed set to " + hypixelSpeedStat, NamedTextColor.GREEN));
            }
        }, speedArg);
    }
}