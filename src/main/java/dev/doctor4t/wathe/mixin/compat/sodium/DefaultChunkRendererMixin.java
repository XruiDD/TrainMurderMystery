package dev.doctor4t.wathe.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.compat.SodiumShaderInterface;
import dev.doctor4t.wathe.compat.sodium.SceneryRenderSection;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferUsage;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.BitwiseMath;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererMixin {
    @Unique
    private static ByteBuffer wathe_buffer = MemoryUtil.memAlloc(RenderRegion.REGION_SIZE * 16);
    @Unique
    private static GlMutableBuffer glBuffer;

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();

    private static final int MODEL_POS_X      = ModelQuadFacing.POS_X.ordinal();
    private static final int MODEL_POS_Y      = ModelQuadFacing.POS_Y.ordinal();
    private static final int MODEL_POS_Z      = ModelQuadFacing.POS_Z.ordinal();

    private static final int MODEL_NEG_X      = ModelQuadFacing.NEG_X.ordinal();
    private static final int MODEL_NEG_Y      = ModelQuadFacing.NEG_Y.ordinal();
    private static final int MODEL_NEG_Z      = ModelQuadFacing.NEG_Z.ordinal();

    @WrapOperation(
            method = "fillCommandBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;getVisibleFaces(IIIIII)I"  // 替换为实际方法签名
            ),
            remap = false
    )
    private static int getVisibleFaces(int originX, int originY, int originZ, int chunkX, int chunkY, int chunkZ , Operation<Integer> original, @Local(name = "sectionIndex") int sectionIndex) {
        if (wathe_buffer == null) {
            wathe_buffer = MemoryUtil.memAlloc(RenderRegion.REGION_SIZE * 16);
        }
        if (WatheClient.isTrainMoving()) {
            var section = SceneryRenderSection.cache.get(ChunkSectionPos.asLong(chunkX,chunkY,chunkZ));
            if(section != null){
                wathe_buffer.putFloat(sectionIndex * 16, section.getOffsetX());
                wathe_buffer.putFloat(sectionIndex * 16 + 4, section.getOffsetY());
                wathe_buffer.putFloat(sectionIndex * 16 + 8, section.getOffsetZ());
                float boundsMinX = section.getVirtualOriginX(), boundsMaxX = boundsMinX + 16;
                int boundsMinY =  section.getVirtualOriginY(), boundsMaxY = boundsMinY + 16;
                int boundsMinZ =  section.getVirtualOriginZ(), boundsMaxZ = boundsMinZ + 16;

                int planes = (1 << MODEL_UNASSIGNED);

                planes |= (originX > boundsMinX - 3.0f ? 1 : 0) << MODEL_POS_X;
                planes |= BitwiseMath.greaterThan(originY, (boundsMinY - 3)) << MODEL_POS_Y;
                planes |= BitwiseMath.greaterThan(originZ, (boundsMinZ - 3)) << MODEL_POS_Z;

                planes |= (originX > boundsMaxX + 3.0f ? 1 : 0) << MODEL_NEG_X;
                planes |=    BitwiseMath.lessThan(originY, (boundsMaxY + 3)) << MODEL_NEG_Y;
                planes |=    BitwiseMath.lessThan(originZ, (boundsMaxZ + 3)) << MODEL_NEG_Z;

                return  planes;
            }else{
                wathe_buffer.putFloat(sectionIndex * 16, 0);
                wathe_buffer.putFloat(sectionIndex * 16 + 4, 0);
                wathe_buffer.putFloat(sectionIndex * 16 + 8, 0);
            }
        }else
        {
            wathe_buffer.putFloat(sectionIndex * 16, 0);
            wathe_buffer.putFloat(sectionIndex * 16 + 4, 0);
            wathe_buffer.putFloat(sectionIndex * 16 + 8, 0);
        }
        return original.call(originX, originY, originZ, chunkX, chunkY, chunkZ);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V"),
            remap = false)
    private void modifyChunkRenderBefore(ChunkRenderMatrices matrices,
                                         CommandList commandList,
                                         ChunkRenderListIterable renderLists,
                                         TerrainRenderPass renderPass,
                                         CameraTransform camera,
                                         CallbackInfo ci,
                                         @Local(ordinal = 0) ChunkShaderInterface shader,
                                         @Local(ordinal = 0) RenderRegion region) {
        glBuffer = commandList.createMutableBuffer();
        commandList.uploadData(glBuffer, wathe_buffer, GlBufferUsage.STREAM_DRAW);

        ((SodiumShaderInterface) shader).wathe$set(glBuffer);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V",
            shift = At.Shift.AFTER),
            remap = false)
    private void modifyChunkRenderAfter(ChunkRenderMatrices matrices,
                                        CommandList commandList,
                                        ChunkRenderListIterable renderLists,
                                        TerrainRenderPass renderPass,
                                        CameraTransform camera,
                                        CallbackInfo ci) {
        MemoryUtil.memFree(wathe_buffer);
        commandList.deleteBuffer(glBuffer);
        wathe_buffer = null;
    }

}
