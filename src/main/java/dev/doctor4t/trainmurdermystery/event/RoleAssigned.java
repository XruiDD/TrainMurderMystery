package dev.doctor4t.trainmurdermystery.event;

import dev.doctor4t.trainmurdermystery.api.Role;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public interface RoleAssigned {

    Event<RoleAssigned> EVENT = createArrayBacked(RoleAssigned.class, listeners -> (player, role) -> {
        for (RoleAssigned listener : listeners) {
            listener.assignRole(player, role);
        }
    });

    void assignRole(PlayerEntity player, Role role);
}