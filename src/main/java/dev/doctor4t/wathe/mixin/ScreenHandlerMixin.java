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
            if (gameComponent != null && gameComponent.isRunning()) {
                // insertStack 受快捷栏限制，如果快捷栏已满则物品仍留在 stack 中
                // 此时强制放入任意可用槽位，避免物品丢失
                if (!serverPlayer.getInventory().insertStack(stack)) {
                    // 仅尝试快捷栏槽位 (0-8)，避免物品进入玩家无法访问的背包区域 (9-35)
                    for (int i = 0; i < 9; i++) {
                        if (serverPlayer.getInventory().main.get(i).isEmpty()) {
                            serverPlayer.getInventory().main.set(i, stack.copy());
                            stack.setCount(0);
                            break;
                        }
                    }
                    // 快捷栏已满，物品丢落到地面
                    if (!stack.isEmpty()) {
                        serverPlayer.dropItem(stack, false);
                        stack.setCount(0);
                    }
                }
                return null;
            }
        }
        return original.call(player, stack, retainOwnership);
    }
}
