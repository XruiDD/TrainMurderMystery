package dev.doctor4t.wathe.record;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record GameRecordEvent(
    UUID matchId,
    int seq,
    String type,
    long worldTick,
    long realTimeMs,
    Identifier dimensionId,
    NbtCompound data
) {
}
