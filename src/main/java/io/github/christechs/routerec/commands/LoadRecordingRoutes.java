package io.github.christechs.routerec.commands;

import io.github.christechs.routerec.manager.RoomManager;
import io.github.christechs.routerec.manager.RouteManager;
import net.minestom.server.command.builder.Command;

public class LoadRecordingRoutes extends Command {
    public LoadRecordingRoutes() {
        super("loadrec");

        addSyntax((sender, context) -> {
            String file = "routes/pearlroutes-current-recording.json";
            RouteManager.init(file);
            RoomManager.loadRoom(RouteManager.currentRoomName);
            sender.sendMessage("Reloaded routes");
        });
    }
}