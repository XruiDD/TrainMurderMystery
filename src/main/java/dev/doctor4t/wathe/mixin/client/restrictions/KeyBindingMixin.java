package dev.doctor4t.wathe.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
        // 跳跃限制由服务端 LivingEntityMixin.wathe$restrictJump 处理，客户端不再屏蔽跳跃键
        //其他键位始终不允许，防止出现bug
        if (!result && WatheClient.isPlayerPlayingAndAlive() && WatheClient.trainComponent != null && WatheClient.trainComponent.hasHud()) {
            result = this.equals(MinecraftClient.getInstance().options.swapHandsKey) ||
                    this.equals(MinecraftClient.getInstance().options.togglePerspectiveKey) ||
                    this.equals(MinecraftClient.getInstance().options.dropKey) ||
                    this.equals(MinecraftClient.getInstance().options.advancementsKey) ||
                    this.equals(MinecraftClient.getInstance().options.spectatorOutlinesKey);
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
