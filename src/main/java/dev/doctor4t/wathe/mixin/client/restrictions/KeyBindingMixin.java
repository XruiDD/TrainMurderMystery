package dev.doctor4t.wathe.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.cca.MapEnhancementsWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.JumpConfig;
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
        //游戏开始之后根据地图配置决定是否屏蔽跳跃键
        if (WatheClient.gameComponent != null && WatheClient.gameComponent.isRunning() && WatheClient.isPlayerPlayingAndAlive()){
            if (WatheClient.mapEnhancementsWorldComponent != null) {
                JumpConfig jumpConfig = WatheClient.mapEnhancementsWorldComponent.getJumpConfig();
                if (!jumpConfig.allowed()) {
                    // 不允许跳跃
                    result = this.equals(MinecraftClient.getInstance().options.jumpKey);
                } else if (jumpConfig.staminaCost() > 0 && this.equals(MinecraftClient.getInstance().options.jumpKey)) {
                    // 允许跳跃但需要体力，体力不足时屏蔽
                    var player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
                        // 非无限体力时，体力不足则屏蔽跳跃键
                        if (!stamina.isInfiniteStamina()) {
                            result = stamina.getSprintingTicks() < jumpConfig.staminaCost();
                        }
                    }
                }
            } else {
                result = this.equals(MinecraftClient.getInstance().options.jumpKey);
            }
        }
        //其他键位始终不允许，防止出现bug
        if (!result && WatheClient.isPlayerPlayingAndAlive() && WatheClient.trainComponent != null && WatheClient.trainComponent.hasHud()) {
            result = this.equals(MinecraftClient.getInstance().options.swapHandsKey) ||
                    this.equals(MinecraftClient.getInstance().options.togglePerspectiveKey) ||
                    this.equals(MinecraftClient.getInstance().options.dropKey) ||
                    this.equals(MinecraftClient.getInstance().options.advancementsKey);
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
