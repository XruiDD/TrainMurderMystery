package dev.doctor4t.wathe.compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.Unique;

public class SceneryRenderSection{

    private float offsetX;
    private int offsetY;
    private int offsetZ;

    private Integer lastZSectionFromCamera = null;

    private final RenderSection renderSection;

    private boolean initialized = false;

    // Virtual coordinate cache
    private float virtualCenterX;
    private int virtualCenterY;
    private int virtualCenterZ;
    private float virtualOriginX;
    private int virtualOriginY;
    private int virtualOriginZ;

    public static final Long2ReferenceMap<SceneryRenderSection> cache = new Long2ReferenceOpenHashMap<>();


    public SceneryRenderSection(RenderSection section) {
        this.renderSection = section;
    }

    public float getOffsetX() {
        return offsetX;
    }
    public int getOffsetY() {
        return offsetY;
    }
    public int getOffsetZ() {
        return offsetZ;
    }

    public int getLastZSectionFromCamera() {
        return lastZSectionFromCamera;
    }

    public boolean init(CameraTransform transform) {
        if(!initialized)
        {
            initialized = true;
            return true;
        }
        return false;
    }

    public void updateX(float offset, int tileSize, float s, float cameraX) {
        offsetX = calculateXOffset(renderSection.getCenterX(), offset, tileSize, s, cameraX);
        virtualCenterX = renderSection.getCenterX() + offsetX;
        virtualOriginX = renderSection.getOriginX() + offsetX;
    }

    public void updateY(int offset) {
        this.offsetY = offset;
        virtualCenterY = renderSection.getCenterY() + offsetY;
        virtualOriginY = renderSection.getOriginY() + offsetY;
    }

    public void updateZ(int zSectionFromCamera, int tileWidth) {
        this.lastZSectionFromCamera = zSectionFromCamera;
        if (zSectionFromCamera <= -8) {
            offsetZ = tileWidth;
        }
        else if (zSectionFromCamera >= 8) {
            offsetZ = -tileWidth;
        }
        virtualCenterZ = renderSection.getCenterZ() + offsetZ;
        virtualOriginZ = renderSection.getOriginZ() + offsetZ;
    }

    private float calculateXOffset(
            int blockPosX,
            float offset,
            int tileSize,
            float s,
            float cameraX
    ) {
        float v1 = blockPosX - cameraX;
        float finalX = ((v1 + s) % tileSize - offset);
        return finalX - v1;
    }

    public RenderSection getRenderSection() {
        return renderSection;
    }

    public float getVirtualCenterX() {
        return virtualCenterX;
    }

    public int getVirtualCenterY() {
        return virtualCenterY;
    }

    public int getVirtualCenterZ() {
        return virtualCenterZ;
    }

    public float getVirtualOriginX() {
        return virtualOriginX;
    }

    public int getVirtualOriginY() {
        return virtualOriginY;
    }

    public int getVirtualOriginZ() {
        return virtualOriginZ;
    }
}
