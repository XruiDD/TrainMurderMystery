package dev.doctor4t.trainmurdermystery.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    public boolean tmm$hasInstinctOutline(boolean original, @Local(argsOnly = true) Entity entity) {
        if (TMMClient.shouldInstinctHighlight(entity)) {
            return true;
        }
        return original;
    }

    @WrapWithCondition(method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;resetEquipProgress(Lnet/minecraft/util/Hand;)V"
            ))
    private boolean stainedLenses$cancelSpyglassUpdateAnimation(HeldItemRenderer instance, Hand hand) {
        return !MinecraftClient.getInstance().player.getStackInHand(hand).isOf(TMMItems.REVOLVER);
    }
}
