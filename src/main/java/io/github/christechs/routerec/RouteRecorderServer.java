package io.github.christechs.routerec;

import io.github.christechs.routerec.commands.*;
import io.github.christechs.routerec.items.*;
import io.github.christechs.routerec.manager.RecorderManager;
import io.github.christechs.routerec.manager.RoomManager;
import io.github.christechs.routerec.manager.RouteManager;
import io.github.christechs.routerec.manager.SecretManager;
import io.github.christechs.routerec.render.RenderUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.util.List;

public class RouteRecorderServer {

    private static final List<CustomItem> CUSTOM_ITEMS = List.of(
            new AOTV(), new Stonk(), new Superboom(), new Pearl(), new WorldModifier(), new Plank(),
            new StepBackward(), new StepForward()
    );
    public static RegistryKey<DimensionType> FULLBRIGHT_DIMENSION_KEY;

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        DimensionType fullbright = DimensionType.builder().ambientLight(2.0f).fixedTime(true).build();
        FULLBRIGHT_DIMENSION_KEY = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), fullbright);

        RouteManager.init("routes/pearlroutes.json");
        SecretManager.init("routes/secretlocations.json");
        RoomManager.init();

        InstanceContainer lobby = MinecraftServer.getInstanceManager().createInstanceContainer(FULLBRIGHT_DIMENSION_KEY);
        lobby.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.DIRT));

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(lobby);
            event.getPlayer().setRespawnPoint(new Pos(0, 42, 0));
        });

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, event -> {
            if (event.getIdentifier().equals("debug:hello")) {
                RenderUtils.markHasMod(event.getPlayer());
                event.getPlayer().sendMessage(Component.text("Native Debug Shapes Enabled!", NamedTextColor.GREEN));
            }
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            RenderUtils.clearAll(event.getPlayer());
            Superboom.clearMemory();
            WorldModifier.clearMemory();
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.setAllowFlying(true);
            player.getInventory().clear();
            CUSTOM_ITEMS.forEach(item -> player.getInventory().addItemStack(item.getItem()));
        });

        EventNode<PlayerEvent> itemNode = EventNode.type("custom-items", EventFilter.PLAYER);
        CUSTOM_ITEMS.forEach(item -> item.registerEvents(itemNode));
        globalEventHandler.addChild(itemNode);

        EventNode<PlayerEvent> secretsNode = EventNode.type("secrets", EventFilter.PLAYER);
        SecretManager.registerEvents(secretsNode);
        globalEventHandler.addChild(secretsNode);

        MinecraftServer.getCommandManager().register(new SpeedCommand());
        MinecraftServer.getCommandManager().register(new LoadCommand());
        MinecraftServer.getCommandManager().register(new GamemodeCommand());
        MinecraftServer.getCommandManager().register(new RecordingCommand());
        MinecraftServer.getCommandManager().register(new LoadRecordingRoutes());
        MinecraftServer.getCommandManager().register(new FixerCommand());
        MinecraftServer.getCommandManager().register(new RoutesCommand());

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (RecorderManager.isRecording && !RecorderManager.isPaused) {
                for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                    RecorderManager.recordLocation(p.getPosition().asVec());
                }
            }
        }).repeat(net.minestom.server.timer.TaskSchedule.tick(10)).schedule();

        server.start("0.0.0.0", 25565);
    }
}