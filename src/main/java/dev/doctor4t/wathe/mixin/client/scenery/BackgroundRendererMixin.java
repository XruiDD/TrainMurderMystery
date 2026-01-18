package dev.doctor4t.wathe.mixin.client.scenery;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.FogConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.*;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;sampleColor(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/CubicSampler$RgbFetcher;)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d wathe$overrideFogColor(Vec3d pos, CubicSampler.RgbFetcher rgbFetcher, Operation<Vec3d> original, @Local(argsOnly = true) ClientWorld world) {
        var player = MinecraftClient.getInstance().player;
        if (WatheClient.isTrainMoving() && world.getTimeOfDay() == 18000 && player != null && !player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            // 空值检查，使用默认配置
            FogConfig fogConfig = WatheClient.mapEnhancementsWorldComponent != null
                ? WatheClient.mapEnhancementsWorldComponent.getFogConfig()
                : FogConfig.DEFAULT;
            Color color = new Color(fogConfig.nightColor(), true);
            return new Vec3d(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        }

        return original.call(pos, rgbFetcher);
    }
}
