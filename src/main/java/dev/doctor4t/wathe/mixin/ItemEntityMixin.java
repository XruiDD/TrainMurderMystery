package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract @Nullable Entity getOwner();

    @Shadow
    public abstract ItemStack getStack();

    /**
     * 阻止特定条件下的枪支拾取
     */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void wathe$preventGunPickup(PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || player.getWorld().isClient) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());

        if (!this.getStack().isIn(WatheItemTags.GUNS)) {
            return;
        }

        // 被禁止拾取枪支（射杀无辜惩罚）
        if (game.isPreventedFromGunPickup(player)) {
            ci.cancel();
            return;
        }

        // 无辜玩家只能在没有枪和刀的情况下拾取非自己掉落的枪
        boolean allowedGunPickup = game.isInnocent(player)
                && !player.equals(this.getOwner())
                && !player.getInventory().contains(itemStack -> itemStack.isIn(WatheItemTags.GUNS))
                && !player.getInventory().contains(itemStack -> itemStack.isOf(WatheItems.KNIFE));
        if (!allowedGunPickup) {
            ci.cancel();
        }
    }

    /**
     * 在 sendPickup 调用处精准记录物品拾取事件
     * sendPickup 仅在 insertStack 成功后调用，此时拾取一定成功
     * 通过 @Local 捕获原始总量 i，实际拾取量 = i - 剩余量
     */
    @Inject(method = "onPlayerCollision",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendPickup(Lnet/minecraft/entity/Entity;I)V"))
    private void wathe$recordItemPickup(PlayerEntity player, CallbackInfo ci, @Local(ordinal = 0) int originalCount) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            int pickedUp = originalCount - this.getStack().getCount();
            GameRecordManager.recordItemPickup(serverPlayer, this.getStack(), pickedUp);
        }
    }
}
