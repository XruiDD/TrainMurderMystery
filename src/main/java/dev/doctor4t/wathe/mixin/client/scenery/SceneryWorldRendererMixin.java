package dev.doctor4t.wathe.mixin.client.scenery;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.SceneryConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.VisibilityConfig;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class SceneryWorldRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderLayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/ShaderProgram;bind()V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void wathe$renderScenery(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo ci, @Local ObjectListIterator<ChunkBuilder.BuiltChunk> objectListIterator, @Local ShaderProgram shaderProgram) {
        if (WatheClient.isTrainMoving()) {
            // 空值检查
            if (WatheClient.mapEnhancementsWorldComponent == null || WatheClient.trainComponent == null) return;

            GlUniform glUniform = shaderProgram.chunkOffset;

            // 从配置获取参数
            SceneryConfig sceneryConfig = WatheClient.mapEnhancementsWorldComponent.getSceneryConfig();
            VisibilityConfig visibilityConfig = WatheClient.mapEnhancementsWorldComponent.getVisibilityConfig();

            float trainSpeed = WatheClient.getTrainSpeed(); // in kmh
            int chunkSize = 16;
            int tileWidth = sceneryConfig.getTileWidth();
            int height = sceneryConfig.heightOffset();
            int tileLength = sceneryConfig.getTileLength();
            int tileSize = sceneryConfig.getTileSize();

            // 根据时间段获取可见距离
            int visibility = switch (WatheClient.trainComponent.getTimeOfDay()) {
                case DAY -> visibilityConfig.day();
                case NIGHT -> visibilityConfig.night();
                case SUNDOWN -> visibilityConfig.sundown();
            };

            float time = WatheClient.trainComponent.getTime() + client.getRenderTickCounter().getTickDelta(true);

            boolean isTranslucent = renderLayer != RenderLayer.getTranslucent();
            while (isTranslucent ? objectListIterator.hasNext() : objectListIterator.hasPrevious()) {
                boolean tooFar = false;

                // cameraEntity 空值检查
                if (client.cameraEntity == null) continue;
                ChunkPos chunkPos = new ChunkPos(client.cameraEntity.getBlockPos());
                client.chunkCullingEnabled = false;

                ChunkBuilder.BuiltChunk builtChunk2 = isTranslucent ? objectListIterator.next() : objectListIterator.previous();
                if (!builtChunk2.getData().isEmpty(renderLayer)) {
                    VertexBuffer vertexBuffer = builtChunk2.getBuffer(renderLayer);
                    BlockPos blockPos = builtChunk2.getOrigin();

                    if (glUniform != null) {
                        boolean trainSection = ChunkSectionPos.getSectionCoord(blockPos.getY()) >= 4;
                        float v1 = (float) ((double) blockPos.getX() - x);
                        float v2 = (float) ((double) blockPos.getY() - y);
                        float v3 = (float) ((double) blockPos.getZ() - z);

                        int zSection = blockPos.getZ() / chunkSize - chunkPos.z;

                        float finalX = v1;
                        float finalY = v2;
                        float finalZ = v3;

                        if (zSection <= -8) {
                            finalX = ((v1 - tileLength + ((time) / 73.8f * trainSpeed)) % tileSize - tileSize / 2f);
                            finalY = (v2 + height);
                            finalZ = v3 + tileWidth;
                        } else if (zSection >= 8) {
                            finalX = ((v1 + tileLength + ((time) / 73.8f * trainSpeed)) % tileSize - tileSize / 2f);
                            finalY = (v2 + height);
                            finalZ = v3 - tileWidth;
                        } else if (!trainSection) {
                            finalX = ((v1 + ((time) / 73.8f * trainSpeed)) % tileSize - tileSize / 2f);
                            finalY = (v2 + height);
                            finalZ = v3;
                        }

                        if (Math.abs(finalX) < visibility) {
                            glUniform.set(
                                    finalX,
                                    finalY,
                                    finalZ
                            );
                            glUniform.upload();
                        } else {
                            tooFar = true;
                        }
                    }

                    if (!tooFar) {
                        vertexBuffer.bind();
                        vertexBuffer.draw();
                    }
                }
            }

            if (glUniform != null) {
                glUniform.set(0.0F, 0.0F, 0.0F);
            }

            shaderProgram.unbind();
            VertexBuffer.unbind();
            this.client.getProfiler().pop();
            renderLayer.endDrawing();

            ci.cancel();
        }
    }
}
