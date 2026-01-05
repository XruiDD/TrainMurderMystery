package dev.doctor4t.wathe.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.compat.sodium.SceneryOcclusionVisitor;
import dev.doctor4t.wathe.compat.sodium.SceneryRenderSection;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin
{

    @Unique
    private Long2ReferenceMap<SceneryRenderSection> scenerySectionsByOriginPosition = new Long2ReferenceOpenHashMap<>();

    @Unique
    private Long2ReferenceMap<RenderSection> trainSectionsByPosition = new Long2ReferenceOpenHashMap<>();

    @Unique
    private OcclusionCuller trainOcclusionCuller;


    @Unique
    private static final float CHUNK_SECTION_RADIUS = 8.0f;

    @Unique
    private static final float CHUNK_SECTION_SIZE = CHUNK_SECTION_RADIUS + 1.0f + 0.125f;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructTail(ClientWorld level, int renderDistance, CommandList commandList, CallbackInfo ci) {
        trainOcclusionCuller = new OcclusionCuller(Long2ReferenceMaps.unmodifiable(this.trainSectionsByPosition), level);
    }

    @Inject(method = "onSectionAdded", at = @At("TAIL"))
    private void onSectionAdded(int x, int y, int z, CallbackInfo ci, @Local long key, @Local RenderSection renderSection) {
        if(renderSection == null){
            return;
        }
        if (renderSection.getOriginY() >= 64){
            this.trainSectionsByPosition.put(key, renderSection);
            return;
        }
        MapEnhancementsConfiguration.SceneryConfig sceneryConfig = WatheClient.mapEnhancementsWorldComponent != null
                ? WatheClient.mapEnhancementsWorldComponent.getSceneryConfig()
                : MapEnhancementsConfiguration.SceneryConfig.DEFAULT;
        var minX = sceneryConfig.minX();
        var maxX = sceneryConfig.maxX();
        var minZ = sceneryConfig.minZ();
        var maxZ = sceneryConfig.maxZ();
        if (renderSection.getOriginX() + 16 <= minX || renderSection.getOriginX() >= maxX || renderSection.getOriginZ() + 16 <= minZ || renderSection.getOriginZ() >= maxZ)
        {
            return;
        }
        SceneryRenderSection sceneryRenderSection = new SceneryRenderSection(renderSection);
        scenerySectionsByOriginPosition.put(key, sceneryRenderSection);
        SceneryRenderSection.cache.put(key, sceneryRenderSection);
    }

    @Inject(method = "onSectionRemoved", at = @At("TAIL"))
    private void onSectionRemoved(int x, int y, int z, CallbackInfo ci, @Local long sectionPos, @Local RenderSection section) {
        trainSectionsByPosition.remove(sectionPos);
        scenerySectionsByOriginPosition.remove(sectionPos);
        SceneryRenderSection.cache.remove(sectionPos);
    }

    @Unique
    private static Frustum getFrustum(Viewport viewport) {
        ViewportAccessor accessor = (ViewportAccessor)(Object)viewport;
        return accessor.wathe$getFrustum();
    }

    @Unique
    private static boolean isBoxVisible(Viewport viewport, float originX, int intOriginY, int intOriginZ){
        float floatOriginX = originX - (float)viewport.getTransform().x;
        float floatOriginY = (intOriginY - viewport.getTransform().intY) - viewport.getTransform().fracY;
        float floatOriginZ = (intOriginZ - viewport.getTransform().intZ) - viewport.getTransform().fracZ;
        return getFrustum(viewport).testAab(floatOriginX - RenderSectionManagerMixin.CHUNK_SECTION_SIZE, floatOriginY - RenderSectionManagerMixin.CHUNK_SECTION_SIZE, floatOriginZ - RenderSectionManagerMixin.CHUNK_SECTION_SIZE, floatOriginX + RenderSectionManagerMixin.CHUNK_SECTION_SIZE, floatOriginY + RenderSectionManagerMixin.CHUNK_SECTION_SIZE, floatOriginZ + RenderSectionManagerMixin.CHUNK_SECTION_SIZE);
    }

    @Unique
    private static boolean isWithinFrustum(Viewport viewport, SceneryRenderSection section) {
        return isBoxVisible(viewport,
                section.getVirtualCenterX(),
                section.getVirtualCenterY(),
                section.getVirtualCenterZ()
        );
    }

    @Unique
    private static float nearestToZeroFloat(float min, float max) {
        float clamped = 0;
        if (min > 0) { clamped = min; }
        if (max < 0) { clamped = max; }
        return clamped;
    }

    @Unique
    private static boolean isWithinRenderDistance(
            CameraTransform camera,
            SceneryRenderSection section,
            float maxDistance
    ) {
        var ox = (float) (section.getVirtualOriginX() - camera.x);
        var oy = section.getVirtualOriginY() - camera.intY;
        var oz = section.getVirtualOriginZ() - camera.intZ;
        float dx = nearestToZeroFloat(ox, ox + 16);
        float dy = nearestToZeroFloat(oy, oy + 16) - camera.fracY;
        float dz = nearestToZeroFloat(oz, oz + 16) - camera.fracZ;
        return (((dx * dx) + (dz * dz)) < (maxDistance * maxDistance)) && (Math.abs(dy) < maxDistance);
    }

    @Unique
    private static boolean isSectionVisible(SceneryRenderSection section, Viewport viewport, float maxDistance) {
        return isWithinRenderDistance(viewport.getTransform(), section, maxDistance) && isWithinFrustum(viewport, section);
    }

    @Unique
    private int lastTileWidth;
    @Unique
    private int lastTileLength;
    @Unique
    private int lastHeight;
    @Unique
    private float lastCamaraX;
    @Unique
    private float lastMile;

    @WrapOperation(
            method = "createTerrainRenderList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;findVisible(Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller$Visitor;Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;FZI)V"
            )
    )
    private void findVisible(
            OcclusionCuller instance,
            OcclusionCuller.Visitor visitor,
            Viewport viewport,
            float searchDistance,
            boolean useOcclusionCulling,
            int frame,
            Operation<Void> original
    ) {
        if (WatheClient.isTrainMoving()){
            visitor = new SceneryOcclusionVisitor(visitor);

            trainOcclusionCuller.findVisible(visitor, viewport, searchDistance, useOcclusionCulling, frame);

            var cameraTransform = viewport.getTransform();

            MapEnhancementsConfiguration.SceneryConfig sceneryConfig = WatheClient.mapEnhancementsWorldComponent != null
                    ? WatheClient.mapEnhancementsWorldComponent.getSceneryConfig()
                    : MapEnhancementsConfiguration.SceneryConfig.DEFAULT;
            float trainSpeed = WatheClient.getTrainSpeed();
            int tileWidth  = 15 * 16;
            int tileLength = 32 * 16;
            int height     = sceneryConfig.heightOffset();
            int tileSize   = tileLength * 3;
            float time = WatheClient.trainComponent.getTime()
                    + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
            float cameraX = cameraTransform.intX + cameraTransform.fracX;


            boolean isTileWidthUpdated = tileWidth == lastTileWidth;
            boolean isTileLengthUpdated = tileLength == lastTileLength;
            boolean isHeightUpdated = height == lastHeight;
            boolean isCamaraXUpdated = cameraX == lastCamaraX;

            lastTileWidth = tileWidth;
            lastTileLength = tileLength;
            lastHeight = height;
            lastCamaraX = cameraX;


            int cameraSectionZ = viewport.getChunkCoord().getSectionZ();

            float s = (time / 73.8f) * trainSpeed;
            boolean isMileUpdated = s == lastMile;
            lastMile = s;
            float s1 = s - tileLength;
            float s2 = s + tileLength;
            float offset = tileSize / 2f;

            for (var entry : scenerySectionsByOriginPosition.long2ReferenceEntrySet())
            {
                var sceneryRenderSection = entry.getValue();
                var renderSection = sceneryRenderSection.getRenderSection();
                if (sceneryRenderSection.init(cameraTransform)){
                    sceneryRenderSection.updateY(height);
                    int sectionZ = renderSection.getChunkZ();
                    int zSectionFromCamera = sectionZ - cameraSectionZ;
                    sceneryRenderSection.updateZ(zSectionFromCamera,tileWidth);
                    if (zSectionFromCamera <= -8){
                        sceneryRenderSection.updateX(offset, tileSize, s1, cameraX);
                    } else if (zSectionFromCamera >= 8)
                    {
                        sceneryRenderSection.updateX(offset, tileSize, s2, cameraX);
                    } else{
                        sceneryRenderSection.updateX(offset, tileSize, s, cameraX);
                    }
                } else {
                    if (isHeightUpdated){
                        sceneryRenderSection.updateY(height);
                    }
                    int sectionZ = renderSection.getChunkZ();
                    int zSectionFromCamera = sectionZ - cameraSectionZ;
                    boolean isZSectionUpdated = zSectionFromCamera != sceneryRenderSection.getLastZSectionFromCamera();
                    if (isZSectionUpdated || isTileWidthUpdated){
                        sceneryRenderSection.updateZ(zSectionFromCamera,tileWidth);
                    }
                    if (isZSectionUpdated || isMileUpdated || isTileLengthUpdated || isCamaraXUpdated)
                    {
                        if (zSectionFromCamera <= -8){
                            sceneryRenderSection.updateX(offset, tileSize, s1, cameraX);
                        } else if (zSectionFromCamera >= 8)
                        {
                            sceneryRenderSection.updateX(offset, tileSize, s2, cameraX);
                        } else {
                            sceneryRenderSection.updateX(offset, tileSize, s, cameraX);
                        }
                    }
                }
                if(isSectionVisible(sceneryRenderSection,viewport,searchDistance)){
                    renderSection.setLastVisibleFrame(frame);
                    visitor.visit(renderSection);
                }
            }
        } else {
            original.call(instance, visitor, viewport, searchDistance, useOcclusionCulling, frame);
        }
    }
}
