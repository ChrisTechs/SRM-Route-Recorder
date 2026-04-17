package io.github.christechs.routerec.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.christechs.routerec.Rotations;
import io.github.christechs.routerec.render.DrawMode;
import io.github.christechs.routerec.render.RenderUtils;
import io.github.christechs.routerec.render.RouteColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class RouteManager {
    public static RenderMode renderMode = RenderMode.ALL;
    public static int renderStepIndex = 0;
    public static String currentRoomName = null;
    public static int flipState = 0;
    public static int anchorMode = 0;
    public static double offsetX = 0;
    public static double offsetY = 0;
    public static double offsetZ = 0;
    public static Rotations baseRotation = Rotations.NONE;
    public static Rotations appliedRotation = Rotations.NONE;
    public static Vec clayPos = Vec.ZERO;
    public static Vec activeAnchor = Vec.ZERO;
    public static Vec customAnchor = null;
    private static JsonObject routeDataCache;

    public static void init(String jsonFilePath) {
        try (Reader reader = new FileReader(jsonFilePath)) {
            routeDataCache = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            routeDataCache = new JsonObject();
        }
    }

    public static String getMatchedKey(String roomName) {
        String cleanRoomName = roomName.replaceAll("_\\d+$", "");
        for (String key : routeDataCache.keySet()) {
            String cleanKey = key.replace("-0", "");
            if (cleanKey.equalsIgnoreCase(roomName) || cleanKey.equalsIgnoreCase(cleanRoomName)) {
                return key;
            }
        }
        return null;
    }

    public static void visualize(String roomName, Instance instance) {
        if (routeDataCache == null || routeDataCache.isEmpty()) return;

        if (currentRoomName != null && !currentRoomName.equals(roomName)) {
            offsetX = 0;
            offsetY = 0;
            offsetZ = 0;
            flipState = 0;
            anchorMode = 0;

            renderStepIndex = 0;
            RecorderManager.currentStep = 0;
            RecorderManager.updateSidebar();
        }
        currentRoomName = roomName;

        List<Tile> tiles = getTiles(instance);
        int topLayer = getTopLayerOfRoom(instance, new Tile(7, 7));

        baseRotation = Rotations.NONE;
        clayPos = Vec.ZERO;

        for (Tile tile : tiles) {
            for (Rotations rot : List.of(Rotations.NORTH, Rotations.SOUTH, Rotations.WEST, Rotations.EAST)) {
                Vec checkPos = new Vec(tile.x() + rot.x, topLayer, tile.z() + rot.z);

                if (isBlueTerracotta(instance, checkPos)) {
                    boolean neighborsValid = true;
                    if (tiles.size() > 1) {
                        for (Direction facing : Direction.HORIZONTAL) {
                            Vec neighbour = checkPos.add(facing.normalX(), facing.normalY(), facing.normalZ());
                            Block neighborBlock = instance.getBlock(neighbour);
                            if (!isBlueTerracottaOrAir(instance, neighbour)) {
                                neighborsValid = false;
                                break;
                            }
                        }
                    }
                    if (neighborsValid) {
                        clayPos = new Vec(checkPos.x(), 0, checkPos.z());
                        baseRotation = rot;
                        break;
                    }
                }
            }
            if (baseRotation != Rotations.NONE) break;
        }

        appliedRotation = applyFlip(baseRotation, flipState);
        Vec[] corners = getRoomCorners(tiles);

        activeAnchor = switch (anchorMode) {
            case -1 -> customAnchor != null ? customAnchor : clayPos;
            case 1 -> corners[0];
            case 2 -> corners[1];
            case 3 -> corners[2];
            case 4 -> corners[3];
            default -> clayPos;
        };

        JsonArray routes = getRoutesIgnoreCase(roomName);
        boolean hasRoutes = routes != null && !routes.isEmpty();

        if (!hasRoutes) {
            instance.sendMessage(Component.text("No Routes Found for " + roomName));
        }

        for (Player player : instance.getPlayers()) {
            RenderUtils.clearAll(player);

            if (hasRoutes && renderMode != RenderMode.NONE) {
                for (int rIndex = 0; rIndex < routes.size(); rIndex++) {
                    if (renderMode == RenderMode.STEP && rIndex != renderStepIndex) continue;

                    JsonObject route = routes.get(rIndex).getAsJsonObject();

                    drawBoxShapesForPlayer(player, route.getAsJsonArray("etherwarps"), RouteColor.ETHERWARP, appliedRotation, activeAnchor);
                    drawBoxShapesForPlayer(player, route.getAsJsonArray("mines"), RouteColor.MINE, appliedRotation, activeAnchor);
                    drawBoxShapesForPlayer(player, route.getAsJsonArray("tnts"), RouteColor.TNT, appliedRotation, activeAnchor);
                    drawBoxShapesForPlayer(player, route.getAsJsonArray("interacts"), RouteColor.INTERACT, appliedRotation, activeAnchor);

                    if (route.has("locations")) {
                        List<Vec> points = parseLineLocations(route.getAsJsonArray("locations"), appliedRotation, activeAnchor);

                        if (!points.isEmpty() && (rIndex == 0 || renderMode == RenderMode.STEP)) {
                            Pos startTextPos = Pos.fromPoint(points.get(0)).add(0, 0.5, 0);
                            RenderUtils.drawText(player, "pearlroutes:text_" + UUID.randomUUID(), startTextPos, Component.text("Start", NamedTextColor.GREEN));
                        }
                        if (points.size() > 1) {
                            RenderUtils.drawLine(player, "pearlroutes:line_" + UUID.randomUUID(), points, RouteColor.PATH_LINE, 3.0f, false);
                        }
                    }

                    if (route.has("secret") && route.getAsJsonObject("secret").has("location")) {
                        JsonObject secret = route.getAsJsonObject("secret");
                        JsonArray coords = secret.getAsJsonArray("location");
                        String type = secret.has("type") ? secret.get("type").getAsString() : "interact";

                        Vec basePos = relativeToActual(coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), coords.get(2).getAsDouble(), appliedRotation, activeAnchor);
                        basePos = basePos.add(offsetX, offsetY, offsetZ);

                        if (type.equalsIgnoreCase("exitroute")) {
                            RenderUtils.drawBox(player, "pearlroutes:secret_" + UUID.randomUUID(), basePos, basePos.add(1, 1, 1), RouteColor.EXIT, DrawMode.SOLID_WITH_BORDER);
                            RenderUtils.drawText(player, "pearlroutes:text_" + UUID.randomUUID(), Pos.fromPoint(basePos).add(0.5, 1.2, 0.5), Component.text("Exit", NamedTextColor.WHITE));
                        } else if (type.equalsIgnoreCase("bat")) {
                            RenderUtils.drawBox(player, "pearlroutes:secret_" + UUID.randomUUID(), basePos, basePos.add(1, 1, 1), RouteColor.BAT, DrawMode.SOLID_WITH_BORDER);
                        } else if (type.equalsIgnoreCase("item")) {
                            RenderUtils.drawBox(player, "pearlroutes:secret_" + UUID.randomUUID(), basePos, basePos.add(1, 1, 1), RouteColor.ITEM, DrawMode.SOLID_WITH_BORDER);
                            RenderUtils.drawText(player, "pearlroutes:text_" + UUID.randomUUID(), Pos.fromPoint(basePos).add(0.5, 1.2, 0.5), Component.text("Item", RouteColor.ITEM.textColor));
                        } else {
                            RenderUtils.drawBox(player, "pearlroutes:secret_" + UUID.randomUUID(), basePos, basePos.add(1, 1, 1), RouteColor.SECRET, DrawMode.SOLID_WITH_BORDER);
                        }
                    }

                    if (route.has("enderpearls")) {
                        JsonArray pearls = route.getAsJsonArray("enderpearls");
                        JsonArray angles = route.has("enderpearlangles") ? route.getAsJsonArray("enderpearlangles") : null;

                        float yawOffset = switch (appliedRotation) {
                            case SOUTH -> 0f;
                            case WEST -> 90f;
                            case NORTH -> 180f;
                            case EAST -> 270f;
                            default -> 0f;
                        };

                        for (int i = 0; i < pearls.size(); i++) {
                            JsonArray p = pearls.get(i).getAsJsonArray();
                            Vec worldPos = relativeToActual(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble(), appliedRotation, activeAnchor);
                            worldPos = worldPos.add(offsetX, offsetY, offsetZ);

                            Vec minBox = worldPos.sub(0.25, 0, 0.25);
                            Vec maxBox = worldPos.add(0.25, 0.5, 0.25);

                            RenderUtils.drawBox(player, "pearlroutes:pearl_" + UUID.randomUUID(), minBox, maxBox, RouteColor.ENDERPEARL, DrawMode.SOLID_WITH_BORDER);
                            RenderUtils.drawText(player, "pearlroutes:text_" + UUID.randomUUID(), Pos.fromPoint(worldPos).add(0, 0.8, 0), Component.text("Pearl " + (i + 1), RouteColor.ENDERPEARL.textColor));

                            if (angles != null && i < angles.size()) {
                                JsonArray ang = angles.get(i).getAsJsonArray();
                                double pitch = ang.get(0).getAsDouble();
                                double relativeYaw = ang.get(1).getAsDouble();

                                double yaw = relativeYaw + yawOffset + 90.0;
                                double yawRadians = Math.toRadians(yaw);
                                double pitchRadians = Math.toRadians(pitch);

                                double length = 10.0;
                                double xDir = -Math.sin(yawRadians) * Math.cos(pitchRadians);
                                double yDir = -Math.sin(pitchRadians);
                                double zDir = Math.cos(yawRadians) * Math.cos(pitchRadians);

                                double sideLength = Math.sqrt(xDir * xDir + yDir * yDir + zDir * zDir);
                                xDir /= sideLength;
                                yDir /= sideLength;
                                zDir /= sideLength;

                                Vec startLine = worldPos.add(0, 1.62, 0);
                                Vec endLine = startLine.add(xDir * length, yDir * length, zDir * length);

                                RenderUtils.drawLine(player, "pearlroutes:pearlline_" + UUID.randomUUID(), List.of(startLine, endLine), RouteColor.ENDERPEARL, 5.0f, false);
                            }
                        }
                    }
                }
            }

            SecretManager.visualizeSecrets(roomName, instance, player, appliedRotation, activeAnchor);
        }
    }

    public static JsonObject getRouteDataCache() {
        return routeDataCache;
    }

    public static Vec actualToRelative(Vec actual) {
        double ax = actual.x() - offsetX;
        double ay = actual.y() - offsetY;
        double az = actual.z() - offsetZ;

        double cx = activeAnchor.x();
        double cz = activeAnchor.z();

        return switch (appliedRotation) {
            case SOUTH, NONE -> new Vec(ax - cx, ay, az - cz);
            case WEST -> new Vec(az - cz, ay, cx - ax);
            case NORTH -> new Vec(cx - ax, ay, cz - az);
            case EAST -> new Vec(cz - az, ay, ax - cx);
        };
    }

    private static void drawBoxShapesForPlayer(Player player, JsonArray arr, RouteColor color, Rotations activeRot, Vec activeCorner) {
        if (arr == null) return;
        for (JsonElement el : arr) {
            drawSingleBoxShape(player, el.getAsJsonArray(), color, activeRot, activeCorner);
        }
    }

    private static void drawSingleBoxShape(Player player, JsonArray coords, RouteColor color, Rotations activeRot, Vec activeCorner) {
        if (coords == null || coords.size() < 3) return;

        Vec basePos = relativeToActual(coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), coords.get(2).getAsDouble(), activeRot, activeCorner);
        basePos = basePos.add(offsetX, offsetY, offsetZ);

        Vec maxPos = basePos.add(1.0, 1.0, 1.0);
        RenderUtils.drawBox(player, "pearlroutes:box_" + UUID.randomUUID(), basePos, maxPos, color, DrawMode.SOLID_WITH_BORDER);
    }

    public static Vec relativeToActual(double rx, double ry, double rz, Rotations rotation, Vec corner) {
        if (corner == null) corner = Vec.ZERO;
        if (rotation == null) rotation = Rotations.SOUTH;

        double cx = corner.x();
        double cz = corner.z();

        return switch (rotation) {
            case WEST -> new Vec(cx - rz, ry, cz + rx);
            case NORTH -> new Vec(cx - rx, ry, cz - rz);
            case EAST -> new Vec(cx + rz, ry, cz - rx);
            default -> new Vec(cx + rx, ry, cz + rz);
        };
    }

    private static List<Vec> parseLineLocations(JsonArray locs, Rotations appliedRotation, Vec activeAnchor) {
        List<Vec> points = new ArrayList<>();
        for (int i = 0; i < locs.size(); i++) {
            JsonArray p = locs.get(i).getAsJsonArray();
            Vec basePos = relativeToActual(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble(), appliedRotation, activeAnchor);
            basePos = basePos.add(offsetX, offsetY, offsetZ);
            points.add(basePos.add(0.5, 0.5, 0.5));
        }
        return points;
    }

    private static boolean isBlueTerracotta(Instance instance, Vec pos) {
        return instance.getBlock(pos).compare(Block.BLUE_TERRACOTTA);
    }

    private static boolean isBlueTerracottaOrAir(Instance instance, Vec pos) {
        Block b = instance.getBlock(pos);
        return b == Block.AIR || b == Block.BLUE_TERRACOTTA;
    }

    private static int getTopLayerOfRoom(Instance instance, Tile tile) {
        for (int y = 160; y >= 12; y--) {
            Block b = instance.getBlock(tile.x(), y, tile.z());
            if (!b.isAir()) return b.compare(Block.GOLD_BLOCK) ? y - 1 : y;
        }
        return 0;
    }

    private static List<Tile> getTiles(Instance instance) {
        List<Tile> tiles = new ArrayList<>();
        Set<String> processedTiles = new HashSet<>();

        for (Chunk chunk : instance.getChunks()) {
            for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                    for (int y = 12; y < 160; y++) {
                        if (!chunk.getBlock(x, y, z).isAir()) {
                            int blockX = chunk.getChunkX() * 16 + x;
                            int blockZ = chunk.getChunkZ() * 16 + z;

                            int tileX = (int) Math.floor((blockX + 8) / 32.0) * 32 + 7;
                            int tileZ = (int) Math.floor((blockZ + 8) / 32.0) * 32 + 7;

                            String key = tileX + "," + tileZ;

                            if (processedTiles.add(key)) {
                                tiles.add(new Tile(tileX, tileZ));
                            }

                            break;
                        }
                    }
                }
            }
        }
        return tiles;
    }

    private static Vec[] getRoomCorners(List<Tile> tiles) {
        if (tiles.isEmpty()) return new Vec[]{Vec.ZERO, Vec.ZERO, Vec.ZERO, Vec.ZERO};
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Tile comp : tiles) {
            if (comp.x() < minX) minX = comp.x();
            if (comp.x() > maxX) maxX = comp.x();
            if (comp.z() < minZ) minZ = comp.z();
            if (comp.z() > maxZ) maxZ = comp.z();
        }

        return new Vec[]{
                new Vec(minX - 15, 0, minZ - 15),
                new Vec(maxX + 15, 0, minZ - 15),
                new Vec(maxX + 15, 0, maxZ + 15),
                new Vec(minX - 15, 0, maxZ + 15)
        };
    }

    public static Rotations applyFlip(Rotations base, int flips) {
        if (base == Rotations.NONE || base == null) return Rotations.NONE;
        Rotations[] cycle = {Rotations.NORTH, Rotations.EAST, Rotations.SOUTH, Rotations.WEST};
        int idx = Arrays.asList(cycle).indexOf(base);
        return cycle[(idx + flips) % 4];
    }

    private static JsonArray getRoutesIgnoreCase(String roomName) {
        String cleanRoomName = roomName.replaceAll("_\\d+$", "");
        for (String key : routeDataCache.keySet()) {
            String cleanKey = key.replace("-0", "");
            if (cleanKey.equalsIgnoreCase(roomName) || cleanKey.equalsIgnoreCase(cleanRoomName)) {
                return routeDataCache.getAsJsonArray(key);
            }
        }
        return null;
    }
}