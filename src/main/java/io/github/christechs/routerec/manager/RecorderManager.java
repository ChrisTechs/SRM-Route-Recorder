package io.github.christechs.routerec.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;

public class RecorderManager {
    public static boolean isRecording = false;
    public static boolean isPaused = false;
    public static boolean pauseOnNext = false;
    public static boolean isLocationRecordingPaused = false;
    public static int currentStep = 0;

    private static Sidebar sidebar;
    private static Vec lastRecordedLoc = Vec.ZERO;

    public static void init(Player player) {
        if (sidebar == null) {
            sidebar = new Sidebar(Component.text("Route Recorder", NamedTextColor.RED));
            sidebar.addViewer(player);
        } else if (!sidebar.getViewers().contains(player)) {
            sidebar.addViewer(player);
        }
        updateSidebar();
    }

    public static void updateSidebar() {
        if (sidebar == null) return;

        Component status = isRecording
                ? (isPaused ? Component.text("PAUSED", NamedTextColor.YELLOW) : Component.text("RECORDING", NamedTextColor.RED))
                : Component.text("OFF", NamedTextColor.GRAY);

        String room = RouteManager.currentRoomName != null ? RouteManager.currentRoomName : "None";

        updateOrAddLine("status", Component.text("Status: ").append(status), 5);
        updateOrAddLine("room", Component.text("Room: ").append(Component.text(room, NamedTextColor.AQUA)), 4);
        updateOrAddLine("step", Component.text("Current Step: ").append(Component.text(currentStep, NamedTextColor.GREEN)), 3);

        Component pauseComp = pauseOnNext ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED);
        updateOrAddLine("pause", Component.text("Auto-Pause: ").append(pauseComp), 2);

        Component locPauseComp = isLocationRecordingPaused ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED);
        updateOrAddLine("locpause", Component.text("Loc Pause: ").append(locPauseComp), 1);
    }

    private static void updateOrAddLine(String id, Component content, int score) {
        if (sidebar.getLine(id) == null) {
            sidebar.createLine(new Sidebar.ScoreboardLine(id, content, score));
        } else {
            sidebar.updateLineContent(id, content);
            sidebar.updateLineScore(id, score);
        }
    }

    public static JsonObject getCurrentStep() {
        if (RouteManager.currentRoomName == null) return null;

        JsonObject root = RouteManager.getRouteDataCache();
        if (!root.has(RouteManager.currentRoomName)) {
            root.add(RouteManager.currentRoomName, new JsonArray());
        }

        JsonArray roomRoutes = root.getAsJsonArray(RouteManager.currentRoomName);

        while (roomRoutes.size() <= currentStep) {
            JsonObject newStep = new JsonObject();
            newStep.add("locations", new JsonArray());
            newStep.add("etherwarps", new JsonArray());
            newStep.add("mines", new JsonArray());
            newStep.add("interacts", new JsonArray());
            newStep.add("tnts", new JsonArray());
            newStep.add("enderpearls", new JsonArray());
            newStep.add("enderpearlangles", new JsonArray());
            roomRoutes.add(newStep);
        }
        return roomRoutes.get(currentStep).getAsJsonObject();
    }

    public static void recordLocation(Vec pos) {
        if (!isRecording || isPaused || isLocationRecordingPaused) return;
        if (pos.distance(lastRecordedLoc) < 2.0) return;

        lastRecordedLoc = pos;
        addCoordinateToArray("locations", pos);
    }

    public static void recordEtherwarp(Pos target) {
        if (!isRecording || isPaused) return;

        Vec blockPos = new Vec(target.blockX(), target.blockY() - 1, target.blockZ());

        addCoordinateToArray("etherwarps", blockPos);
    }

    public static void recordMine(Pos pos) {
        if (!isRecording || isPaused) return;
        addCoordinateToArray("mines", pos.asVec());
    }

    public static void recordTnt(Pos pos) {
        if (!isRecording || isPaused) return;
        addCoordinateToArray("tnts", pos.asVec());
    }

    public static void recordPearl(Pos startPos, float pitch, float yaw) {
        if (!isRecording || isPaused) return;

        JsonObject step = getCurrentStep();
        if (step == null) return;

        Vec footPos = startPos.asVec().sub(0, 1.62, 0);
        Vec rel = RouteManager.actualToRelative(footPos);

        JsonArray pArr = step.getAsJsonArray("enderpearls");
        if (!pArr.isEmpty()) {
            JsonArray last = pArr.get(pArr.size() - 1).getAsJsonArray();
            double dx = Math.abs(last.get(0).getAsDouble() - rel.x());
            double dy = Math.abs(last.get(1).getAsDouble() - rel.y());
            double dz = Math.abs(last.get(2).getAsDouble() - rel.z());
            if (dx < 0.01 && dy < 0.01 && dz < 0.01) return;
        }

        JsonArray newPearl = new JsonArray();
        newPearl.add(rel.x());
        newPearl.add(rel.y());
        newPearl.add(rel.z());
        pArr.add(newPearl);

        float yawOffset = switch (RouteManager.appliedRotation) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };

        float relYaw = (yaw % 360) - yawOffset - 90.0f;

        JsonArray aArr = new JsonArray();
        aArr.add(pitch);
        aArr.add(relYaw);
        step.getAsJsonArray("enderpearlangles").add(aArr);
    }

    public static void setSecret(String type, Vec pos) {
        JsonObject step = getCurrentStep();
        if (step == null) return;

        JsonObject secretObj = new JsonObject();
        secretObj.addProperty("type", type);

        Vec rel = RouteManager.actualToRelative(pos);
        JsonArray loc = new JsonArray();

        loc.add((int) Math.floor(rel.x()));
        loc.add((int) Math.floor(rel.y()));
        loc.add((int) Math.floor(rel.z()));

        secretObj.add("location", loc);
        step.add("secret", secretObj);
    }

    private static void addCoordinateToArray(String arrayName, Vec worldPos) {
        JsonObject step = getCurrentStep();
        if (step == null) return;

        Vec rel = RouteManager.actualToRelative(worldPos);
        JsonArray coord = new JsonArray();

        coord.add((int) Math.floor(rel.x()));
        coord.add((int) Math.floor(rel.y()));
        coord.add((int) Math.floor(rel.z()));

        step.getAsJsonArray(arrayName).add(coord);
    }
}