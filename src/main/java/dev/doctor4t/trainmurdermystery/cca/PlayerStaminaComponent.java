package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class PlayerStaminaComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PlayerStaminaComponent> KEY =
            ComponentRegistry.getOrCreate(TMM.id("stamina"), PlayerStaminaComponent.class);

    private static final float EXHAUSTION_RECOVERY_THRESHOLD = 0.4f; // 恢复到40%才能解除疲惫

    private final PlayerEntity player;
    private float sprintingTicks = 0f;
    private int maxSprintTime = -1;
    private boolean exhausted = false; // 疲惫状态
    private float lastSyncedValue = -1f;
    private int lastSyncedMaxTime = -1;
    private boolean lastSyncedExhausted = false;

    public PlayerStaminaComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public float getSprintingTicks() {
        return this.sprintingTicks;
    }

    public void setSprintingTicks(float ticks) {
        this.sprintingTicks = ticks;
    }

    public int getMaxSprintTime() {
        return this.maxSprintTime;
    }

    public void setMaxSprintTime(int maxTime) {
        this.maxSprintTime = maxTime;
    }

    public boolean isExhausted() {
        return this.exhausted;
    }

    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }

    public float getExhaustionRecoveryThreshold() {
        return EXHAUSTION_RECOVERY_THRESHOLD;
    }

    @Override
    public void serverTick() {
        // 只负责同步数据到客户端，疾跑限制逻辑在 PlayerEntityMixin 中处理
        boolean needsSync = false;

        if (Math.abs(this.sprintingTicks - this.lastSyncedValue) >= 1f) {
            needsSync = true;
        }

        if (this.maxSprintTime != this.lastSyncedMaxTime) {
            needsSync = true;
        }

        if (this.exhausted != this.lastSyncedExhausted) {
            needsSync = true;
        }

        if (needsSync) {
            this.sync();
            this.lastSyncedValue = this.sprintingTicks;
            this.lastSyncedMaxTime = this.maxSprintTime;
            this.lastSyncedExhausted = this.exhausted;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putFloat("sprintingTicks", this.sprintingTicks);
        tag.putInt("maxSprintTime", this.maxSprintTime);
        tag.putBoolean("exhausted", this.exhausted);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.sprintingTicks = tag.contains("sprintingTicks", NbtElement.FLOAT_TYPE) ? tag.getFloat("sprintingTicks") : 0f;
        this.maxSprintTime = tag.contains("maxSprintTime", NbtElement.INT_TYPE) ? tag.getInt("maxSprintTime") : -1;
        this.exhausted = tag.contains("exhausted") && tag.getBoolean("exhausted");
    }
}
