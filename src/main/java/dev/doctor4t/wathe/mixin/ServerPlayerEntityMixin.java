package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.ItemStack;
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

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    // 断开连接时将光标上的物品放回背包，防止物品被丢出
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void wathe$clearCursorStackOnDisconnect(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
        TrainWorldComponent trainWorldComponent = TrainWorldComponent.KEY.get(self.getWorld());
        if (trainWorldComponent != null && trainWorldComponent.hasHud() && GameFunctions.isPlayerAliveAndSurvival(self)) {
            ItemStack cursorStack = self.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty()) {
                self.getInventory().insertStack(cursorStack);
                self.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            }
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
}