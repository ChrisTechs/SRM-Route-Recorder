package io.github.christechs.routerec.debug;

import net.minestom.server.network.NetworkBuffer;

public class BoxShape implements Shape {
    public double minX, minY, minZ;
    public double maxX, maxY, maxZ;
    public int faceColor;
    public int faceRenderLayer;
    public int edgeColor;
    public int edgeRenderLayer;
    public float edgeWidth;

    public BoxShape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int faceColor, int faceRenderLayer, int edgeColor, int edgeRenderLayer, float edgeWidth) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.faceColor = faceColor;
        this.faceRenderLayer = faceRenderLayer;
        this.edgeColor = edgeColor;
        this.edgeRenderLayer = edgeRenderLayer;
        this.edgeWidth = edgeWidth;
    }

    @Override
    public int typeOrdinal() {
        return 3;
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(NetworkBuffer.DOUBLE, minX);
        writer.write(NetworkBuffer.DOUBLE, minY);
        writer.write(NetworkBuffer.DOUBLE, minZ);

        writer.write(NetworkBuffer.DOUBLE, maxX);
        writer.write(NetworkBuffer.DOUBLE, maxY);
        writer.write(NetworkBuffer.DOUBLE, maxZ);

        writer.write(NetworkBuffer.INT, faceColor);
        writer.write(NetworkBuffer.VAR_INT, faceRenderLayer);

        writer.write(NetworkBuffer.INT, edgeColor);
        writer.write(NetworkBuffer.VAR_INT, edgeRenderLayer);

        writer.write(NetworkBuffer.FLOAT, edgeWidth);
    }
}