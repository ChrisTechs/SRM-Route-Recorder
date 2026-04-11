package io.github.christechs.routerec.debug;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;

import java.util.List;

public record LineShape(int lineType, List<Vec> points, int color, int renderLayerOrdinal,
                        float lineWidth) implements Shape {

    @Override
    public int typeOrdinal() {
        return 0;
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(NetworkBuffer.VAR_INT, lineType);

        writer.write(NetworkBuffer.VAR_INT, points.size());
        for (Vec p : points) {
            writer.write(NetworkBuffer.DOUBLE, p.x());
            writer.write(NetworkBuffer.DOUBLE, p.y());
            writer.write(NetworkBuffer.DOUBLE, p.z());
        }

        writer.write(NetworkBuffer.INT, color);
        writer.write(NetworkBuffer.VAR_INT, renderLayerOrdinal);
        writer.write(NetworkBuffer.FLOAT, lineWidth);
    }
}