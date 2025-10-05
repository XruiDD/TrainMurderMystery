package dev.doctor4t.trainmurdermystery.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public abstract class TrueDarknessLightmapTextureManagerMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;lerp(Lorg/joml/Vector3fc;F)Lorg/joml/Vector3f;", ordinal = 0))
    private Vector3f tmm$fuckYourBlueAssHueMojang(Vector3f instance, Vector3fc other, float t, Operation<Vector3f> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

//        if (client.player != null && world != null) {
//            float f = MinecraftClient.getInstance().world.getSkyBrightness(1.0f);
//            float absLight = Math.abs(MathHelper.lerp(0, -.5f, .5f));
//            float l = (float) MathHelper.lerp(1 - Math.pow(0.01, absLight), 1f, f);
//            return new Vector3f(f, f, l).lerp(other, t);
//        }

        return original.call(instance, other, t);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;lerp(Lorg/joml/Vector3fc;F)Lorg/joml/Vector3f;", ordinal = 6))
    private Vector3f tmm$trueDarknessAndSunLight(Vector3f instance, Vector3fc other, float t, Operation<Vector3f> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

        if (client.player != null && world != null) {
//            BlockPos blockPos = client.player.getBlockPos();
//            float lightLevelScale = (float) world.getLightLevel(LightType.SKY, blockPos) / (float) world.getMaxLightLevel();

            return original.call(instance, new Vector3f(.75f, .75f, .75f), TMMClient.instinctLightLevel);
        }

        return original.call(instance, other, t);
    }

    @ModifyVariable(method = "update", at = @At(value = "STORE"), ordinal = 2)
    private float tmm$keepSkylight(float value) {
        return value;
    }
}
