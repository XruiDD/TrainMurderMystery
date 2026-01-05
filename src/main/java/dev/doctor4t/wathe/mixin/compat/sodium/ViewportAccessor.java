package dev.doctor4t.wathe.mixin.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Viewport Accessor 用于获取 Sodium 的 Frustum 对象
 */
@Mixin(Viewport.class)
public interface ViewportAccessor {

    @Accessor(value = "frustum", remap = false)
    Frustum wathe$getFrustum();
}
