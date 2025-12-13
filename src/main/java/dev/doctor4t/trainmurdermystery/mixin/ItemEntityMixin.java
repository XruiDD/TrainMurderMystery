package dev.doctor4t.trainmurdermystery.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.index.tag.TMMItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract @Nullable Entity getOwner();

    @Shadow
    private @Nullable UUID throwerUuid;

    @Shadow
    public abstract ItemStack getStack();

    @WrapMethod(method = "onPlayerCollision")
    public void tmm$preventGunPickup(PlayerEntity player, Operation<Void> original) {
        if (player.isCreative()) {
            original.call(player);
            return;
        }

        // 检查玩家是否被禁止拾取枪支（射杀无辜惩罚）
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (game.isPreventedFromGunPickup(player) && this.getStack().isIn(TMMItemTags.GUNS)) {
            return; // 阻止拾取枪支
        }

        // 原有逻辑：无辜玩家只能拾取自己掉落的枪或在没有枪的情况下拾取他人的枪
        if (!this.getStack().isIn(TMMItemTags.GUNS) || (game.isInnocent(player) && !player.equals(this.getOwner()) && !player.getInventory().contains(itemStack -> itemStack.isIn(TMMItemTags.GUNS)))) {
            original.call(player);
        }
    }
}
