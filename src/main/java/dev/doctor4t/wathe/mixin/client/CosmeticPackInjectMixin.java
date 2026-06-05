package dev.doctor4t.wathe.mixin.client;

import dev.doctor4t.wathe.client.pack.CosmeticPackProvider;
import dev.doctor4t.wathe.client.pack.CosmeticPackStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

@Mixin(MinecraftClient.class)
public class CosmeticPackInjectMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourcePackManager;<init>([Lnet/minecraft/resource/ResourcePackProvider;)V"),
            index = 0)
    private ResourcePackProvider[] wathe$addCosmeticProvider(ResourcePackProvider[] providers) {
        ResourcePackProvider[] out = Arrays.copyOf(providers, providers.length + 1);
        out[providers.length] = new CosmeticPackProvider(CosmeticPackStorage.resolvePackDir());
        return out;
    }
}
