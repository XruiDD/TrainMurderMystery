package dev.doctor4t.wathe.mixin.client.restrictions;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityInvisibilityMixin {

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void wathe$hideInvisibleFromDeadSpectators(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isInvisible()
                && player.isSpectator()
                && GameFunctions.isPlayerPlayingAndAlive(player)) {
            cir.setReturnValue(true);
        }
    }
}
