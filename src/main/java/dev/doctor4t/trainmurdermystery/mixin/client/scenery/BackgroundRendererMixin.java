package dev.doctor4t.trainmurdermystery.mixin.client.scenery;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfiguration.FogConfig;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.*;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;sampleColor(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/CubicSampler$RgbFetcher;)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d tmm$overrideFogColor(Vec3d pos, CubicSampler.RgbFetcher rgbFetcher, Operation<Vec3d> original, @Local(argsOnly = true) ClientWorld world) {
        if (TMMClient.isTrainMoving() && world.getTimeOfDay() == 18000) {
            // 空值检查，使用默认配置
            FogConfig fogConfig = TMMClient.areasComponent != null
                ? TMMClient.areasComponent.getFogConfig()
                : FogConfig.DEFAULT;
            Color color = new Color(fogConfig.nightColor(), true);
            return new Vec3d(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        }

        return original.call(pos, rgbFetcher);
    }
}
