package io.github.christechs.routerec.items;

import io.github.christechs.routerec.RayUtil;
import io.github.christechs.routerec.manager.RecorderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BonzoStaff implements CustomItem {
    private static final Tag<Boolean> BONZO_TAG = Tag.Boolean("bonzo_staff");
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.BLAZE_ROD)
                .set(BONZO_TAG, true)
                .customName(Component.text("Bonzo's Staff", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {
        EventNode<PlayerEvent> itemNode = EventNode.type("bonzo_staff", EventFilter.PLAYER);

        itemNode.addListener(PlayerUseItemEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getItemInMainHand().hasTag(BONZO_TAG)) {
                long now = System.currentTimeMillis();
                if (now - cooldowns.getOrDefault(player.getUuid(), 0L) < 200) return;
                cooldowns.put(player.getUuid(), now);

                shootBalloon(player);
            }
        });

        node.addChild(itemNode);
    }

    private void shootBalloon(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return;

        Vec start = player.getPosition().add(0, player.getEyeHeight(), 0).asVec();
        Vec direction = player.getPosition().direction();

        ItemEntity balloon = new ItemEntity(ItemStack.of(Material.RED_DYE));
        balloon.setNoGravity(true);
        balloon.setPickupDelay(Duration.ofSeconds(9999));
        balloon.setInstance(instance, new Pos(start));

        MinecraftServer.getSchedulerManager().submitTask(new Supplier<TaskSchedule>() {
            Vec currentPos = start;
            int ticksAlive = 0;
            double speedPerTick = 0.75;

            @Override
            public TaskSchedule get() {
                if (balloon.isRemoved()) return TaskSchedule.stop();
                ticksAlive++;

                Vec nextPos = currentPos.add(direction.mul(speedPerTick));

                AtomicReference<Vec> surfacePointRef = new AtomicReference<>(null);
                AtomicReference<Vec> blockHitRef = new AtomicReference<>(null);

                RayUtil.voxelTraversal(currentPos, nextPos, (x, y, z, nx, ny, nz) -> {
                    Block block = instance.getBlock(x, y, z);
                    if (block.isSolid()) {
                        surfacePointRef.set(new Vec(x + 0.5 + (nx * 0.5), y + 0.5 + (ny * 0.5), z + 0.5 + (nz * 0.5)));
                        blockHitRef.set(new Vec(x, y, z));
                        return true;
                    }
                    return false;
                });

                if (surfacePointRef.get() != null || ticksAlive > 40) {
                    balloon.remove();

                    if (blockHitRef.get() != null) {
                        RecorderManager.recordBonzo(blockHitRef.get());
                    }

                    Vec hitPoint = surfacePointRef.get() != null ? surfacePointRef.get() : nextPos;
                    Vec playerVec = player.getPosition().asVec();

                    if (playerVec.distance(hitPoint) <= 5.5) {
                        Vec pushDir = playerVec.sub(hitPoint).withY(0);

                        if (pushDir.lengthSquared() < 0.01) {
                            pushDir = direction.mul(-1).withY(0);
                        }
                        pushDir = pushDir.normalize();

                        Vec knockback = pushDir.mul(28.5).withY(14.5);
                        player.setVelocity(knockback);
                    }
                    return TaskSchedule.stop();
                } else {
                    currentPos = nextPos;
                    balloon.teleport(new Pos(currentPos));
                    return TaskSchedule.tick(1);
                }
            }
        });
    }
}