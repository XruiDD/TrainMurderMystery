package dev.doctor4t.wathe.mixin.client.scenery;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.CameraShakeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Unique
    private static final PerlinNoiseSampler sampler = new PerlinNoiseSampler(Random.create());

    @Unique
    private static float randomizeOffset(int offset) {
        float intensity = 0.2f;

        float min = -intensity * 2;
        float max = intensity * 2;
        float sampled = (float) sampler.sample((MinecraftClient.getInstance().world.getTime() % 24000L + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)) / intensity, offset, 0) * 1.5f;
        return min >= max ? min : sampled * max;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void wathe$doScreenshake(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // 空值检查
        if (WatheClient.mapEnhancementsWorldComponent == null) return;
        CameraShakeConfig shakeConfig = WatheClient.mapEnhancementsWorldComponent.getCameraShakeConfig();

        if (WatheClient.isTrainMoving() && shakeConfig.enabled()) {
            Camera camera = (Camera) (Object) this;

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            int age = player.age;
            // moodComponent 可能为 null，使用默认值 1.0f
            float mood = WatheClient.moodComponent != null ? WatheClient.moodComponent.getMood() : 1.0f;
            float v = (1 + (1 - mood)) * 2.5f;
            float amplitude = shakeConfig.amplitudeIndoor();
            float strength = shakeConfig.strengthIndoor();

            float yawOffset = 0;
            float pitchOffset = 0;

            if (Wathe.isSkyVisibleAdjacent(player)) {
                amplitude = shakeConfig.amplitudeOutdoor();
                strength = shakeConfig.strengthOutdoor();

                if (Wathe.isExposedToWind(player)) {
                    yawOffset = 1.5f * randomizeOffset(10);
                    pitchOffset = 1.5f * randomizeOffset(-10);
                }
            }

            amplitude *= v;

            camera.setRotation(camera.getYaw() + yawOffset, camera.getPitch() + pitchOffset);
            camera.setPos(camera.getPos().add(0, Math.sin((age + tickDelta) * strength) / 2f * amplitude, Math.cos((age + tickDelta) * strength) * amplitude));
        }
    }
}
