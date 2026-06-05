package dev.doctor4t.wathe.mixin.client.items;

import dev.doctor4t.wathe.client.model.cosmetic.CosmeticModelStore;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.component.CosmeticComponent;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backports 1.21.4's minecraft:item_model behaviour to 1.21.1: when a stack carries the cosmetic
 * SKIN component, render the cosmetic's baked model instead of the item's normal model - for ANY
 * item (vanilla or modded), with no per-item registration and no model wrapping. The cosmetic models
 * are baked + stored by {@link dev.doctor4t.wathe.client.model.cosmetic.CosmeticModelLoadingPlugin}.
 *
 * getModel is the single funnel every item-render path goes through (GUI, ground, first/third person),
 * so returning here swaps the model everywhere consistently. Items with no SKIN component, or whose
 * cosmetic model isn't loaded, fall through untouched to vanilla rendering.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererCosmeticMixin {
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void wathe$cosmeticModel(ItemStack stack, @Nullable World world, @Nullable LivingEntity entity, int seed,
                                     CallbackInfoReturnable<BakedModel> cir) {
        CosmeticComponent skin = stack.get(WatheDataComponentTypes.SKIN);
        if (skin == null || "default".equals(skin.cosmeticId())) return;
        BakedModel model = CosmeticModelStore.get(skin.cosmeticId());
        if (model != null) {
            cir.setReturnValue(model);
        }
    }
}
