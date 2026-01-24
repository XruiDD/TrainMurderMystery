package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PlayerVeteranComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerVeteranComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("veteran"), PlayerVeteranComponent.class);
    public static final int MAX_STAB_USES = 2;

    private final PlayerEntity player;
    private int stabUsesLeft = 0;

    public PlayerVeteranComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.stabUsesLeft = 0;
        this.sync();
    }

    public void initialize() {
        this.stabUsesLeft = MAX_STAB_USES;
        this.sync();
    }

    public int getStabUsesLeft() {
        return this.stabUsesLeft;
    }

    public boolean hasStabUsesLeft() {
        return this.stabUsesLeft > 0;
    }

    public boolean useStab() {
        if (this.stabUsesLeft > 0) {
            this.stabUsesLeft--;
            this.sync();
            return true;
        }
        return false;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("StabUsesLeft", this.stabUsesLeft);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.stabUsesLeft = tag.getInt("StabUsesLeft");
    }
}
