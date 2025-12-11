package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public interface ResetPlayer {

    Event<ResetPlayer> EVENT = createArrayBacked(ResetPlayer.class, listeners -> player -> {
        for (ResetPlayer listener : listeners) {
            listener.onReset(player);
        }
    });

    void onReset(ServerPlayerEntity player);
}
