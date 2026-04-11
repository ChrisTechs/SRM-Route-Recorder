package io.github.christechs.routerec.debug;

import net.minestom.server.network.NetworkBuffer;

import java.util.List;

public class DebugShapes {
    public static final String CHANNEL = "debug:shapes";

    public static byte[] createPayload(List<Operation> operations) {
        return NetworkBuffer.makeArray(writer -> {
            writer.write(NetworkBuffer.VAR_INT, operations.size());
            for (Operation op : operations) {
                op.write(writer);
            }
        });
    }

    public sealed interface Operation permits Set, Remove, ClearNamespace, Clear {
        void write(NetworkBuffer writer);
    }

    public record Set(String namespaceId, Shape shape) implements Operation {
        @Override
        public void write(NetworkBuffer writer) {
            writer.write(NetworkBuffer.VAR_INT, 0);
            writer.write(NetworkBuffer.STRING, namespaceId);
            writer.write(NetworkBuffer.VAR_INT, shape.typeOrdinal());
            shape.write(writer);
        }
    }

    public record Remove(String namespaceId) implements Operation {
        @Override
        public void write(NetworkBuffer writer) {
            writer.write(NetworkBuffer.VAR_INT, 1);
            writer.write(NetworkBuffer.STRING, namespaceId);
        }
    }

    public record ClearNamespace(String namespace) implements Operation {
        @Override
        public void write(NetworkBuffer writer) {
            writer.write(NetworkBuffer.VAR_INT, 2);
            writer.write(NetworkBuffer.STRING, namespace);
        }
    }

    public record Clear() implements Operation {
        @Override
        public void write(NetworkBuffer writer) {
            writer.write(NetworkBuffer.VAR_INT, 3);
        }
    }
}