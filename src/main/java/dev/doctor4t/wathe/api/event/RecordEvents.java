package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.world.ServerWorld;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public final class RecordEvents {
    private RecordEvents() {
    }

    /**
     * Called when a match record has finished and is ready for consumption.
     */
    public static final Event<OnRecordEnd> ON_RECORD_END = createArrayBacked(OnRecordEnd.class, listeners -> (world, record) -> {
        for (OnRecordEnd listener : listeners) {
            listener.onRecordEnd(world, record);
        }
    });

    @FunctionalInterface
    public interface OnRecordEnd {
        void onRecordEnd(ServerWorld world, GameRecordManager.MatchRecord record);
    }
}
