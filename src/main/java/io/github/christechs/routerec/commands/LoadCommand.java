package io.github.christechs.routerec.commands;

import io.github.christechs.routerec.manager.RecorderManager;
import io.github.christechs.routerec.manager.RoomManager;
import io.github.christechs.routerec.manager.RouteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.ScoreboardObjectivePacket;
import net.minestom.server.timer.TaskSchedule;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class LoadCommand extends Command {

    public LoadCommand() {
        super("load");
        var worldArg = ArgumentType.String("world");

        worldArg.setSuggestionCallback((_, _, suggestion) -> {
            String input = suggestion.getInput();
            String[] args = input.split(" ", -1);
            String search = args.length > 1 ? args[args.length - 1].toLowerCase() : "";

            RoomManager.getAvailableWorlds().forEach(file -> {
                String name = file.getName().replace(".polar", "");
                if (search.isEmpty() || name.toLowerCase().contains(search)) {
                    suggestion.addEntry(new SuggestionEntry(name));
                }
            });
        });

        setDefaultExecutor((sender, _) ->
                sender.sendMessage(Component.text("Usage: /load <world_name>", NamedTextColor.RED))
        );

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String worldName = context.get(worldArg);

            try {
                int index = Integer.parseInt(worldName);
                List<File> files = RoomManager.getAvailableWorlds();
                if (index > 0 && index <= files.size()) {
                    worldName = files.get(index - 1).getName().replace(".polar", "");
                }
            } catch (NumberFormatException ignored) {
            }

            String displayRoomName = worldName.replace("_", " ");

            player.sendMessage(Component.text("Loading room: " + displayRoomName, NamedTextColor.YELLOW));

            final String fileToLoad = worldName;
            final String routeRoomName = displayRoomName;

            RoomManager.loadRoom(fileToLoad).thenAccept(result -> {
                if (!result.success()) {
                    player.sendMessage(Component.text("Failed to load world!", NamedTextColor.RED));
                    return;
                }

                player.setGameMode(GameMode.SPECTATOR);
                player.setInstance(result.instance(), result.spawnPos()).thenRun(() -> {
                    RoomManager.cleanupOldInstance();

                    player.setAllowFlying(true);
                    player.setFlying(true);

                    RouteManager.offsetX = 0;
                    RouteManager.offsetY = 0;
                    RouteManager.offsetZ = 0;
                    RouteManager.flipState = 0;
                    RouteManager.anchorMode = 0;

                    RouteManager.renderStepIndex = 0;
                    RecorderManager.currentStep = 0;
                    RecorderManager.updateSidebar();

                    RouteManager.visualize(routeRoomName, result.instance());
                    player.sendMessage(Component.text("Room loaded and routes visualized.", NamedTextColor.GREEN));

                    MinecraftServer.getSchedulerManager().buildTask(() -> {

                        player.sendPacket(new ScoreboardObjectivePacket(
                                "SBScoreboard",
                                (byte) 1,
                                null,
                                null,
                                null
                        ));

                        player.sendPacket(new ScoreboardObjectivePacket(
                                "SBScoreboard",
                                (byte) 0,
                                Component.text("SKYBLOCK"),
                                ScoreboardObjectivePacket.Type.INTEGER,
                                null
                        ));

                        player.sendPacket(new PlayerInfoUpdatePacket(
                                EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                                List.of(new PlayerInfoUpdatePacket.Entry(
                                        UUID.randomUUID(),
                                        "!odin_dummy",
                                        List.of(),
                                        false,
                                        0,
                                        GameMode.SPECTATOR,
                                        Component.text("Dungeon: The Catacombs"),
                                        null,
                                        0,
                                        false
                                ))
                        ));

                    }).delay(TaskSchedule.tick(10)).schedule();
                });
            });

        }, worldArg);
    }
}