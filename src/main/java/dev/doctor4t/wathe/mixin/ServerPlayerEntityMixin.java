package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.InteractionSwingTracker;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    // 阻止"右键交互（开门等）"重置球棒攻击冷却，但保留左键挥击的冷却。
    // 原版 ServerPlayerEntity.swingHand() 每次挥手都会调用 resetLastAttackedTicks()，
    // 而左键攻击/空挥与右键交互在服务端都通过单参 swingHand 触发，无法直接区分。
    // 这里借助 InteractionSwingTracker：右键交互成功时服务端会先调用 swingHand(hand, true)
    // 打上"本 tick 交互挥手"时间戳，仅在该时间戳命中当前 tick 时跳过重置（即右键交互）；
    // 左键攻击/空挥不会打戳，照常重置攻击冷却。
    @WrapOperation(method = "swingHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;resetLastAttackedTicks()V"))
    private void wathe$preventSwingCooldownReset(ServerPlayerEntity instance, Operation<Void> original) {
        boolean isInteractionSwing = ((InteractionSwingTracker) instance).wathe$lastInteractionSwingTick() == instance.getWorld().getTime();
        if (instance.getMainHandStack().isOf(WatheItems.BAT) && isInteractionSwing) {
            return;
        }
        original.call(instance);
    }

    @Inject(method = "canBeSpectated", at = @At("HEAD"), cancellable = true)
    private void wathe$hideInvisibleFromDeadSpectators(ServerPlayerEntity spectator, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (self.isInvisible()
                && spectator.isSpectator()
                && GameFunctions.isPlayerPlayingAndAlive(spectator)) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "trySleep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"))
    public void wathe$disableSleepMessage(ServerPlayerEntity instance, Text message, boolean overlay, Operation<Void> original) {
    }

    @WrapOperation(method = "trySleep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSpawnPoint(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/math/BlockPos;FZZ)V"))
    public void wathe$disableSetSpawnpoint(ServerPlayerEntity instance, RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, Operation<Void> original) {
    }

    @ModifyExpressionValue(method = "trySleep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isDay()Z"))
    public boolean wathe$allowSleepingAtAnyTime(boolean original) {
        return false;
    }

    // Layer 2: 安全网 — 万一原版死亡被触发，将其路由到模组死亡系统
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void wathe$interceptVanillaDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (GameFunctions.isPlayerPlayingAndAlive(self)) {
            GameFunctions.killPlayer(self, true, null, GameConstants.DeathReasons.VANILLA_DEATH, true);
            self.setHealth(self.getMaxHealth());
            ci.cancel();
        }
    }
}