package dev.doctor4t.wathe.mixin.client.items;

import dev.doctor4t.wathe.client.skin.ItemSkinQuadGenerator;
import dev.doctor4t.wathe.client.skin.ItemSkinTextureManager;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.component.CosmeticComponent;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class SkinItemRendererMixin {
    @Shadow
    @Final
    private ItemColors colors;

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"), cancellable = true)
    private void wathe$renderCustomSkin(ItemStack stack, ModelTransformationMode mode,
                                        boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                        int light, int overlay, BakedModel model, CallbackInfo ci) {
        CosmeticComponent skin = stack.get(WatheDataComponentTypes.SKIN);
        if (skin == null || "default".equals(skin.cosmeticId())) return;

        // Pre-load variant textures (e.g. thrown grenade) so they're cached before use
        for (String url : skin.getResourceTextureUrls()) {
            ItemSkinTextureManager.getInstance().ensureLoaded(url);
        }

        Identifier texId = ItemSkinTextureManager.getInstance().getTextureId(skin.textureUrl());
        if (texId == null) {
            ItemSkinTextureManager.getInstance().ensureLoaded(skin.textureUrl());
            return; // fall through to default rendering
        }

        ItemSkinQuadGenerator.SkinQuadData skinQuads = ItemSkinTextureManager.getInstance().getQuads(skin.textureUrl());
        if (skinQuads == null) return; // quads not ready yet, fall through to default

        ci.cancel();

        // Use the original item's model for display transforms (rotation, translation, scale)
        // The `model` parameter already contains the correct BakedModel resolved by vanilla ItemRenderer
        matrices.push();
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);

        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(texId));
        MatrixStack.Entry entry = matrices.peek();

        // Render pre-generated quads (UVs already in 0-1 for the skin texture)
        for (Direction dir : Direction.values()) {
            renderQuads(skinQuads.getQuads(dir), entry, consumer, stack, light, overlay);
        }
        renderQuads(skinQuads.getQuads(null), entry, consumer, stack, light, overlay);

        matrices.pop();
    }

    private void renderQuads(List<BakedQuad> quads, MatrixStack.Entry entry,
                             VertexConsumer consumer, ItemStack stack, int light, int overlay) {
        for (BakedQuad quad : quads) {
            int c = quad.hasColor() ? colors.getColor(stack, quad.getColorIndex()) : Colors.WHITE;
            float alpha = ColorHelper.Argb.getAlpha(c) / 255f;
            float red = ColorHelper.Argb.getRed(c) / 255f;
            float green = ColorHelper.Argb.getGreen(c) / 255f;
            float blue = ColorHelper.Argb.getBlue(c) / 255f;
            consumer.quad(entry, quad, red, green, blue, alpha, light, overlay);
        }
    }
}
