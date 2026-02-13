package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    // 游戏运行期间，ScreenHandler 关闭时阻止光标物品掉落，改为放回背包
    @WrapOperation(
            method = "onClosed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;"
            )
    )
    private ItemEntity wathe$preventCursorDropOnScreenClose(
            PlayerEntity player,
            ItemStack stack,
            boolean retainOwnership,
            Operation<ItemEntity> original
    ) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverPlayer.getWorld());
            if (gameComponent.isRunning()) {
                serverPlayer.getInventory().insertStack(stack);
                return null;
            }
        }
        return original.call(player, stack, retainOwnership);
    }
}
