package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Decides whether a death should pierce the psycho-mode armour ("疯魔盾") instead of
 * being absorbed by it inside {@link dev.doctor4t.wathe.game.GameFunctions#killPlayer}.
 * <p>
 * If any listener returns {@code true} for the given death reason, the armour does not
 * absorb the death: psycho mode is ended (as with a forced kill) and the player dies.
 * Defaults to {@code false} (the armour absorbs the death as usual).
 */
public interface ShouldPiercePsychoArmour {

    Event<ShouldPiercePsychoArmour> EVENT = createArrayBacked(ShouldPiercePsychoArmour.class, listeners -> (victim, deathReason) -> {
        for (ShouldPiercePsychoArmour listener : listeners) {
            if (listener.pierces(victim, deathReason)) {
                return true;
            }
        }
        return false;
    });

    boolean pierces(ServerPlayerEntity victim, Identifier deathReason);
}
