package io.github.christechs.routerec.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.christechs.routerec.manager.RecorderManager;
import io.github.christechs.routerec.manager.RouteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.io.File;
import java.io.FileWriter;

public class RecordingCommand extends Command {

    public RecordingCommand() {
        super("recording", "routerec", "rr");

        var actionArg = ArgumentType.Word("action").from(
                "start", "pause", "resume", "stop", "pauseOnNextStep",
                "pauseLocation", "nextStep", "prevStep", "deleteStep", "deleteRoute", "set", "save"
        );
        var secondaryArg = ArgumentType.String("value").setDefaultValue("");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            RecorderManager.init(player);

            String action = context.get(actionArg).toLowerCase();
            String value = context.get(secondaryArg);

            switch (action) {
                case "start" -> {
                    RecorderManager.isRecording = true;
                    RecorderManager.isPaused = false;
                    player.sendMessage(Component.text("Recording started.", NamedTextColor.GREEN));
                }
                case "pause" -> {
                    RecorderManager.isPaused = true;
                    player.sendMessage(Component.text("Recording paused.", NamedTextColor.YELLOW));
                }
                case "resume" -> {
                    RecorderManager.isPaused = false;
                    player.sendMessage(Component.text("Recording resumed.", NamedTextColor.GREEN));
                }
                case "stop" -> {
                    RecorderManager.isRecording = false;
                    player.sendMessage(Component.text("Recording stopped.", NamedTextColor.RED));
                }
                case "pauseonnextstep" -> {
                    RecorderManager.pauseOnNext = Boolean.parseBoolean(value);
                    player.sendMessage(Component.text("Auto-pause set to " + RecorderManager.pauseOnNext, NamedTextColor.YELLOW));
                }
                case "pauselocation" -> {
                    RecorderManager.isLocationRecordingPaused = !RecorderManager.isLocationRecordingPaused;
                    player.sendMessage(Component.text("Location tracking paused: " + RecorderManager.isLocationRecordingPaused, NamedTextColor.YELLOW));
                }
                case "nextstep" -> {
                    RecorderManager.currentStep++;
                    if (RecorderManager.pauseOnNext) RecorderManager.isPaused = true;
                    player.sendMessage(Component.text("Moved to Step " + RecorderManager.currentStep, NamedTextColor.AQUA));
                }
                case "prevstep" -> {
                    if (RecorderManager.currentStep > 0) RecorderManager.currentStep--;
                    player.sendMessage(Component.text("Moved to Step " + RecorderManager.currentStep, NamedTextColor.AQUA));
                }
                case "deletestep" -> {
                    if (RouteManager.currentRoomName != null) {
                        JsonArray arr = RouteManager.getRouteDataCache().getAsJsonArray(RouteManager.currentRoomName);
                        if (arr != null && arr.size() > RecorderManager.currentStep) {
                            arr.remove(RecorderManager.currentStep);
                            player.sendMessage(Component.text("Deleted Step " + RecorderManager.currentStep, NamedTextColor.RED));
                        }
                    }
                }
                case "deleteroute" -> {
                    if (RouteManager.currentRoomName != null) {
                        JsonObject cache = RouteManager.getRouteDataCache();
                        if (cache.has(RouteManager.currentRoomName)) {
                            cache.add(RouteManager.currentRoomName, new JsonArray());
                            RecorderManager.currentStep = 0;
                            player.sendMessage(Component.text("Deleted all recorded routes for room: " + RouteManager.currentRoomName, NamedTextColor.RED));
                        }
                    }
                }
                case "set" -> {
                    if (value.isEmpty()) {
                        player.sendMessage(Component.text("Provide a type: tnt, item, bat, interact, exit, etc.", NamedTextColor.RED));
                        return;
                    }
                    Pos target = player.getPosition();
                    RecorderManager.setSecret(value, target.asVec());
                    player.sendMessage(Component.text("Set secret '" + value + "' at your position.", NamedTextColor.GREEN));
                }
                case "save" -> {
                    try {
                        File dir = new File("routes");
                        if (!dir.exists()) dir.mkdirs();

                        String normalFile = "routes/pearlroutes-current-recording.json";
                        saveCompactJson(normalFile, RouteManager.getRouteDataCache());

                        String legacyFile = "routes/pearlroutes-legacy.json";
                        JsonObject legacyCache = createLegacyCache(RouteManager.getRouteDataCache());
                        saveCompactJson(legacyFile, legacyCache);

                        player.sendMessage(Component.text("Successfully saved normal & legacy routes!", NamedTextColor.GREEN));
                    } catch (Exception e) {
                        player.sendMessage(Component.text("Failed to save: " + e.getMessage(), NamedTextColor.RED));
                        e.printStackTrace();
                    }
                }
            }
            RecorderManager.updateSidebar();
        }, actionArg, secondaryArg);
    }

    private void saveCompactJson(String path, JsonObject cache) {
        try (FileWriter writer = new FileWriter(path)) {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;
            for (String key : cache.keySet()) {
                if (!first) sb.append(",\n");
                sb.append("  \"").append(key).append("\":");
                sb.append(gson.toJson(cache.get(key)));
                first = false;
            }
            sb.append("\n}");
            writer.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JsonObject createLegacyCache(JsonObject original) {
        JsonObject legacy = original.deepCopy();

        for (String key : legacy.keySet()) {
            if (!legacy.get(key).isJsonArray()) {
                continue;
            }

            JsonArray routes = legacy.getAsJsonArray(key);

            if (routes != null && !routes.isEmpty()) {
                if (routes.get(routes.size() - 1).isJsonObject()) {
                    JsonObject lastStep = routes.get(routes.size() - 1).getAsJsonObject();
                    if (!lastStep.has("secret")) {
                        JsonObject dummySecret = new JsonObject();
                        dummySecret.addProperty("type", "dummy");

                        JsonArray dummyLocation = new JsonArray();
                        dummyLocation.add(0);
                        dummyLocation.add(0);
                        dummyLocation.add(0);

                        dummySecret.add("location", dummyLocation);
                        lastStep.add("secret", dummySecret);
                    }
                }
            }

            String cleanKey = key.replace("-0", "").replaceAll("_\\d+$", "");

            if (cleanKey.equalsIgnoreCase("Slime-5") || cleanKey.equalsIgnoreCase("Sewer-7")) {
                double maxX = 0;
                double maxZ = 0;
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).isJsonObject()) {
                        double[] maxes = getStepMaxCoords(routes.get(i).getAsJsonObject());
                        maxX = Math.max(maxX, maxes[0]);
                        maxZ = Math.max(maxZ, maxes[1]);
                    }
                }

                int flipWidth = getDungeonDimension(maxX);
                int flipLength = getDungeonDimension(maxZ);

                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).isJsonObject()) {
                        flipStep180(routes.get(i).getAsJsonObject(), flipWidth, flipLength);
                    }
                }
            }
        }
        return legacy;
    }

    private int getDungeonDimension(double maxCoord) {
        int gridUnits = (int) Math.max(1, Math.round((maxCoord + 2) / 32.0));
        return (gridUnits * 32) - 2;
    }

    private double[] getStepMaxCoords(JsonObject step) {
        double maxX = 0, maxZ = 0;
        String[] standardArrays = {"locations", "etherwarps", "mines", "interacts", "tnts"};

        for (String arrName : standardArrays) {
            if (step.has(arrName)) {
                JsonArray arr = step.getAsJsonArray(arrName);
                for (int j = 0; j < arr.size(); j++) {
                    JsonArray coord = arr.get(j).getAsJsonArray();
                    if (coord.size() >= 3) {
                        maxX = Math.max(maxX, coord.get(0).getAsDouble());
                        maxZ = Math.max(maxZ, coord.get(2).getAsDouble());
                    }
                }
            }
        }

        if (step.has("enderpearls")) {
            JsonArray arr = step.getAsJsonArray("enderpearls");
            for (int j = 0; j < arr.size(); j++) {
                JsonArray coord = arr.get(j).getAsJsonArray();
                if (coord.size() >= 3) {
                    maxX = Math.max(maxX, coord.get(0).getAsDouble());
                    maxZ = Math.max(maxZ, coord.get(2).getAsDouble());
                }
            }
        }

        if (step.has("secret")) {
            JsonObject secret = step.getAsJsonObject("secret");
            if (secret.has("location")) {
                JsonArray coord = secret.getAsJsonArray("location");
                if (coord.size() >= 3) {
                    maxX = Math.max(maxX, coord.get(0).getAsDouble());
                    maxZ = Math.max(maxZ, coord.get(2).getAsDouble());
                }
            }
        }
        return new double[]{maxX, maxZ};
    }

    private void flipStep180(JsonObject step, int flipWidth, int flipLength) {
        String[] standardArrays = {"locations", "etherwarps", "mines", "interacts", "tnts"};

        for (String arrName : standardArrays) {
            if (step.has(arrName)) {
                JsonArray arr = step.getAsJsonArray(arrName);
                for (int j = 0; j < arr.size(); j++) {
                    JsonArray coord = arr.get(j).getAsJsonArray();
                    if (coord.size() >= 3) {
                        coord.set(0, new JsonPrimitive(flipWidth - coord.get(0).getAsInt()));
                        coord.set(2, new JsonPrimitive(flipLength - coord.get(2).getAsInt()));
                    }
                }
            }
        }

        if (step.has("enderpearls")) {
            JsonArray arr = step.getAsJsonArray("enderpearls");
            for (int j = 0; j < arr.size(); j++) {
                JsonArray coord = arr.get(j).getAsJsonArray();
                if (coord.size() >= 3) {
                    coord.set(0, new JsonPrimitive((double) flipWidth - coord.get(0).getAsDouble()));
                    coord.set(2, new JsonPrimitive((double) flipLength - coord.get(2).getAsDouble()));
                }
            }
        }

        if (step.has("enderpearlangles")) {
            JsonArray arr = step.getAsJsonArray("enderpearlangles");
            for (int j = 0; j < arr.size(); j++) {
                JsonArray ang = arr.get(j).getAsJsonArray();
                if (ang.size() >= 2) {
                    double yaw = ang.get(1).getAsDouble();
                    yaw += 180.0;
                    if (yaw > 180.0) yaw -= 360.0;
                    ang.set(1, new JsonPrimitive(yaw));
                }
            }
        }

        if (step.has("secret")) {
            JsonObject secret = step.getAsJsonObject("secret");
            if (secret.has("location")) {
                JsonArray coord = secret.getAsJsonArray("location");
                if (coord.size() >= 3) {
                    coord.set(0, new JsonPrimitive(flipWidth - coord.get(0).getAsInt()));
                    coord.set(2, new JsonPrimitive(flipLength - coord.get(2).getAsInt()));
                }
            }
        }
    }
}