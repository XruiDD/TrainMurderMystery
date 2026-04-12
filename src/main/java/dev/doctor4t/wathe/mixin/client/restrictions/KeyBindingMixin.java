package dev.doctor4t.wathe.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.api.event.ShouldAllowSuppressedKey;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
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
        if (WatheClient.shouldDisableChat()) {
            result = this.equals(MinecraftClient.getInstance().options.chatKey) ||
                    this.equals(MinecraftClient.getInstance().options.commandKey);
        }
        if(result) return result;
        //游戏进行中屏蔽部分键位，防止出现bug
        if (!result && WatheClient.isPlayerPlayingAndAlive() && WatheClient.trainComponent != null && WatheClient.trainComponent.hasHud()) {
            result = this.equals(MinecraftClient.getInstance().options.jumpKey) ||
                    this.equals(MinecraftClient.getInstance().options.swapHandsKey) ||
                    this.equals(MinecraftClient.getInstance().options.togglePerspectiveKey) ||
                    this.equals(MinecraftClient.getInstance().options.dropKey) ||
                    this.equals(MinecraftClient.getInstance().options.advancementsKey) ||
                    this.equals(MinecraftClient.getInstance().options.spectatorOutlinesKey);
        }
        // 允许附属模组豁免被屏蔽的按键（如灵界使者灵魂状态需要跳跃键）
        if (result && ShouldAllowSuppressedKey.EVENT.invoker().shouldAllow((KeyBinding) (Object) this)) {
            return false;
        }
        return result;
    }

    @ModifyReturnValue(method = "wasPressed", at = @At("RETURN"))
    private boolean wathe$restrainWasPressedKeys(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }

    @ModifyReturnValue(method = "isPressed", at = @At("RETURN"))
    private boolean wathe$restrainIsPressedKeys(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }

    @ModifyReturnValue(method = "matchesKey", at = @At("RETURN"))
    private boolean wathe$restrainMatchesKey(boolean original) {
        if (this.shouldSuppressKey()) return false;
        else return original;
    }
}
