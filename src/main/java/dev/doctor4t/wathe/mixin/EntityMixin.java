package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.api.event.MaxAir;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    private World world;

    @WrapMethod(method = "collidesWith")
    protected boolean wathe$solid(Entity other, Operation<Boolean> original) {
        if (GameWorldComponent.KEY.get(this.world).isRunning()) {
            Entity self = (Entity) (Object) this;
            if (self instanceof PlayerEntity && other instanceof PlayerEntity) return true;
        }
        return original.call(other);
    }

    // Override max air to 60 seconds (1200 ticks) for playing players
    @ModifyReturnValue(method = "getMaxAir", at = @At("RETURN"))
    private int wathe$modifyMaxAir(int original) {
        if ((Object) this instanceof PlayerEntity self && GameFunctions.isPlayerPlayingAndAlive(self)) {
            return MaxAir.EVENT.invoker().modifyMaxAir(self, 1200);
        }
        return original;
    }
}