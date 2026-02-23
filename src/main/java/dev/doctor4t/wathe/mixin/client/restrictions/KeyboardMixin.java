package dev.doctor4t.wathe.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @WrapMethod(method = "processF3")
    private boolean wathe$disableF3Keybinds(int key, Operation<Boolean> original) {
        if (WatheClient.isPlayerPlayingAndAlive() && !WatheClient.isPlayerCreative() && WatheClient.trainComponent != null && WatheClient.trainComponent.hasHud()) {
            return key == 293 ? original.call(key) : false;
        } else {
            // F3+K (75) 切换方块黑名单 debug
            if (key == GLFW.GLFW_KEY_K) {
                WatheClient.blockBlacklistDebugEnabled = !WatheClient.blockBlacklistDebugEnabled;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    String status = WatheClient.blockBlacklistDebugEnabled ? "§a已开启" : "§c已关闭";
                    client.player.sendMessage(Text.literal("§e[Wathe] §f方块黑名单调试: " + status), true);
                }
                return true;
            }
            return original.call(key);
        }
    }
}
