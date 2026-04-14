package io.github.christechs.routerec.items;

import io.github.christechs.routerec.RayUtil;
import io.github.christechs.routerec.manager.RecorderManager;
import io.github.christechs.routerec.render.DrawMode;
import io.github.christechs.routerec.render.RenderUtils;
import io.github.christechs.routerec.render.RouteColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class AOTV implements CustomItem {
    private static final Tag<Boolean> AOTV_TAG = Tag.Boolean("aotv");

    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Pos> aotvTargetMap = new ConcurrentHashMap<>();

    private static boolean isEtherwarpPassable(Block block) {
        if (block.isAir() || block.isLiquid()) return true;
        String name = block.name().toLowerCase();

        if (name.contains("torch") || name.contains("fire") || name.contains("vine") ||
                name.contains("button") || name.contains("lever") || name.contains("ladder") ||
                name.contains("sign") || name.contains("wire") || name.contains("comparator") ||
                name.contains("repeater") || name.contains("rail") || name.contains("skull") ||
                name.contains("head") || name.contains("pot") || name.contains("web") ||
                name.contains("portal") || name.contains("pressure_plate") || name.contains("tripwire")) {
            return true;
        }

        if (name.equals("minecraft:snow") || name.contains("flower") || name.contains("grass") ||
                name.contains("bush") || name.contains("sapling") || name.contains("crop") ||
                name.contains("stem") || name.contains("seagrass") || name.contains("sugarcane") ||
                name.endsWith("mushroom") || name.contains("wart") || name.contains("plant") ||
                name.contains("lantern") || name.contains("leaf")) {
            return true;
        }

        return name.contains("wall") || name.contains("fence");
    }

    private static boolean isSolidForStandardAotv(Block block) {
        if (!block.isSolid()) return false;
        String name = block.name().toLowerCase();
        if (name.contains("torch") || name.contains("fire")) return false;
        return true;
    }

    private static void handleRightClick(Player player) {
        if (!player.getItemInMainHand().hasTag(AOTV_TAG)) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUuid(), 0L) < 150)
            return;

        cooldowns.put(player.getUuid(), now);

        etherwarpPlayer(player);
    }

    private static void etherwarpPlayer(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return;

        if (player.isSneaking()) {
            Pos target = getEtherwarpTarget(player);
            if (target != null) {
                RecorderManager.recordEtherwarp(target);
                aotvTargetMap.remove(player.getUuid());
                player.teleport(target);
                RenderUtils.clearShape(player, "aotv_target");
            }
        } else {
            Pos startPos = player.getPosition().add(0, 1, 0);
            Vec direction = startPos.direction();

            Vec startFeet = startPos.asVec();
            Vec endFeet = startFeet.add(direction.mul(8.0));

            AtomicReference<Double> hitDist = new AtomicReference<>(8.0);

            RayUtil.voxelTraversal(startFeet, endFeet, (x, y, z, nx, ny, nz) -> {
                Block feet = instance.getBlock(x, y, z);
                Block head = instance.getBlock(x, y + 1, z);

                boolean feetSolid = isSolidForStandardAotv(feet);
                boolean headSolid = isSolidForStandardAotv(head);

                if (feetSolid || headSolid) {
                    if (nx == 0 && ny == 0 && nz == 0) {
                        hitDist.set(0.0);
                        return true;
                    }

                    double tFeet = 1.0;
                    if (feetSolid) {
                        if (nx != 0) tFeet = RayUtil.intersectPlane(startFeet.x(), endFeet.x(), nx < 0 ? x : x + 1.0);
                        else if (ny != 0)
                            tFeet = RayUtil.intersectPlane(startFeet.y(), endFeet.y(), ny < 0 ? y : y + 1.0);
                        else if (nz != 0)
                            tFeet = RayUtil.intersectPlane(startFeet.z(), endFeet.z(), nz < 0 ? z : z + 1.0);
                    }

                    double tHead = 1.0;
                    if (headSolid) {
                        int hy = y + 1;
                        if (nx != 0) tHead = RayUtil.intersectPlane(startFeet.x(), endFeet.x(), nx < 0 ? x : x + 1.0);
                        else if (ny != 0)
                            tHead = RayUtil.intersectPlane(startFeet.y(), endFeet.y(), ny < 0 ? hy : hy + 1.0);
                        else if (nz != 0)
                            tHead = RayUtil.intersectPlane(startFeet.z(), endFeet.z(), nz < 0 ? z : z + 1.0);
                    }

                    hitDist.set(Math.max(0.0, Math.min(tFeet, tHead)) * 8.0);
                    return true;
                }
                return false;
            });

            double finalDist = Math.max(0.0, hitDist.get() - 0.2);
            Pos target = startPos.add(direction.mul(finalDist));

            player.teleport(target);
        }
    }

    private static Pos getEtherwarpTarget(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return null;

        Vec start = player.getPosition().add(0, player.getEyeHeight(), 0).asVec();
        Vec end = start.add(player.getPosition().direction().mul(61));

        AtomicReference<Pos> target = new AtomicReference<>(null);

        RayUtil.voxelTraversal(start, end, (x, y, z, nx, ny, nz) -> {
            Block block = instance.getBlock(x, y, z);
            String name = block.name().toLowerCase();

            boolean isFence = name.contains("fence");
            boolean isHead = name.contains("head") || name.contains("skull");

            if (isEtherwarpPassable(block) && !(isFence || isHead)) return false;

            if (isFence || isHead) {
                double minX = x + 0.25, maxX = x + 0.75;
                double minZ = z + 0.25, maxZ = z + 0.75;
                double minY = y, maxY = isFence ? y + 1.0 : y + 0.5;

                Vec rayDir = end.sub(start);
                if (!RayUtil.rayIntersectsAABB(start, rayDir, minX, minY, minZ, maxX, maxY, maxZ)) {
                    return false;
                }
            }

            Pos standPos = new Pos(x, y + 1, z);
            Pos headSpace = standPos.add(0, 1, 0);

            if (isEtherwarpPassable(instance.getBlock(standPos)) && isEtherwarpPassable(instance.getBlock(headSpace))) {
                target.set(new Pos(x + 0.5, y + 1.0, z + 0.5, player.getPosition().yaw(), player.getPosition().pitch()));
            }
            return true;
        });

        return target.get();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND_SHOVEL)
                .set(AOTV_TAG, true)
                .customName(Component.text("Aspect of the Void", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> parentNode) {
        EventNode<PlayerEvent> node = EventNode.type("aotv", EventFilter.PLAYER);

        node.addListener(PlayerUseItemEvent.class, event -> handleRightClick(event.getPlayer()));
        node.addListener(PlayerUseItemOnBlockEvent.class, event -> handleRightClick(event.getPlayer()));

        node.addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            boolean hodlingAOTV = player.getItemInMainHand().hasTag(AOTV_TAG);
            boolean isSneaking = player.isSneaking();

            if (hodlingAOTV && isSneaking) {
                Pos target = getEtherwarpTarget(player);
                Pos lastTarget = aotvTargetMap.get(player.getUuid());

                if (target != null && !target.sameBlock(lastTarget != null ? lastTarget : Pos.ZERO)) {
                    aotvTargetMap.put(player.getUuid(), target);
                    double bx = target.blockX();
                    double by = target.blockY() - 1;
                    double bz = target.blockZ();

                    RenderUtils.clearShape(player, "aotv_target");

                    Vec min = new Vec(bx - 0.005, by - 0.005, bz - 0.005);
                    Vec max = new Vec(bx + 1.005, by + 1.005, bz + 1.005);
                    RenderUtils.drawBox(player, "aotv_target", min, max, RouteColor.AOTV_TARGET, DrawMode.SOLID_WITH_BORDER);

                } else if (target == null && lastTarget != null) {
                    aotvTargetMap.remove(player.getUuid());
                    RenderUtils.clearShape(player, "aotv_target");
                }
            } else if (aotvTargetMap.containsKey(player.getUuid())) {
                aotvTargetMap.remove(player.getUuid());
                RenderUtils.clearShape(player, "aotv_target");
            }
        });

        parentNode.addChild(node);
    }
}