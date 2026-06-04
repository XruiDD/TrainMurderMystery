package dev.doctor4t.wathe.mixin.client.restrictions;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerCosmeticsComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.skin.PlayerSkinTextureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @WrapMethod(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V")
    protected void wathe$disableNameTags(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, Operation<Void> original) {
        if (WatheClient.trainComponent == null || !WatheClient.trainComponent.hasHud())
            original.call(abstractClientPlayerEntity, text, matrixStack, vertexConsumerProvider, i, f);
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void wathe$psychoSkinTexture(AbstractClientPlayerEntity p,
                                         CallbackInfoReturnable<Identifier> cir) {
        if (PlayerPsychoComponent.KEY.get(p).getPsychoTicks() <= 0) return;
        cir.setReturnValue(resolvePsychoTexture(p));
    }

    @ModifyVariable(method = "renderArm", at = @At("STORE"), ordinal = 0)
    private Identifier wathe$psychoArmTexture(Identifier skinTexture) {
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) return skinTexture;
        if (PlayerPsychoComponent.KEY.get(self).getPsychoTicks() <= 0) return skinTexture;
        return resolvePsychoTexture(self);
    }

    /**
     * 共享逻辑：装备的疯魔皮肤匹配当前 player.skin model 时使用，否则回退 mod 内置贴图。
     */
    private static Identifier resolvePsychoTexture(AbstractClientPlayerEntity p) {
        SkinTextures.Model current = p.getSkinTextures().model();
        boolean slim = current == SkinTextures.Model.SLIM;
        String currentModel = slim ? "SLIM" : "WIDE";

        PlayerCosmeticsComponent.PlayerSkinEntry entry =
                PlayerCosmeticsComponent.KEY.get(p).getPlayerSkin("psycho");

        if (entry != null && currentModel.equals(entry.model())) {
            Identifier texId = PlayerSkinTextureManager.getInstance().getTextureId(entry.textureUrl());
            if (texId != null) return texId;
            // 未加载完成 → 触发加载并兜底返回（下一帧切回来）
            PlayerSkinTextureManager.getInstance().ensureLoaded(entry.textureUrl());
        }
        return Wathe.id("textures/entity/psycho" + (slim ? "_thin" : "") + ".png");
    }
}
