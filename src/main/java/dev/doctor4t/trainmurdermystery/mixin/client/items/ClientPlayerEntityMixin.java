package dev.doctor4t.trainmurdermystery.mixin.client.items;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.trainmurdermystery.TrainMurderMystery;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    public boolean tmm$disableItemSlowdown(boolean original) {
            return false;
    }

}
