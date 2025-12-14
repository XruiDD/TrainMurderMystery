package dev.doctor4t.trainmurdermystery.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.event.AllowPlayerChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Shadow
    public abstract boolean equals(KeyBinding other);

    @Unique
    private boolean shouldSuppressKey() {
        boolean result = false;
        //在大厅聊天，在游戏内不可以，旁观和创造始终可以
        if (TMMClient.shouldDisableChat()) {
            result = this.equals(MinecraftClient.getInstance().options.chatKey) ||
                    this.equals(MinecraftClient.getInstance().options.commandKey);
        }
        if(result) return result;
        //游戏开始之后不能跳跃，旁观者跳过
        if (TMMClient.gameComponent != null && TMMClient.gameComponent.isRunning() && TMMClient.isPlayerAliveAndInSurvival()){
            result = this.equals(MinecraftClient.getInstance().options.jumpKey);
        }
        //其他键位始终不允许，防止出现bug
        if (!result && TMMClient.isPlayerAliveAndInSurvival()) {
            result = this.equals(MinecraftClient.getInstance().options.swapHandsKey) ||
                    this.equals(MinecraftClient.getInstance().options.togglePerspectiveKey) ||
                    this.equals(MinecraftClient.getInstance().options.dropKey) ||
                    this.equals(MinecraftClient.getInstance().options.advancementsKey);
        }
        return result;
    }

    @ModifyReturnValue(method = "wasPressed", at = @At("RETURN"))
    private boolean tmm$restrainWasPressedKeys(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }

    @ModifyReturnValue(method = "isPressed", at = @At("RETURN"))
    private boolean tmm$restrainIsPressedKeys(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }

    @ModifyReturnValue(method = "matchesKey", at = @At("RETURN"))
    private boolean tmm$restrainMatchesKey(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }
}
