package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public record AnnounceWelcomePayload(String role, int killers, int targets) implements CustomPayload {
    public static final Id<AnnounceWelcomePayload> ID = new Id<>(Wathe.id("announcewelcome"));
    public static final PacketCodec<PacketByteBuf, AnnounceWelcomePayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, AnnounceWelcomePayload::role, PacketCodecs.INTEGER, AnnounceWelcomePayload::killers, PacketCodecs.INTEGER, AnnounceWelcomePayload::targets, AnnounceWelcomePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AnnounceWelcomePayload> {
        @Override
        public void receive(@NotNull AnnounceWelcomePayload payload, ClientPlayNetworking.@NotNull Context context) {
            Identifier roleId = Identifier.tryParse(payload.role());
            RoleAnnouncementTexts.RoleAnnouncementText roleText = RoleAnnouncementTexts.getForRole(roleId);
            RoundTextRenderer.startWelcome(roleText, payload.killers(), payload.targets());
            MinecraftClient client = context.client();

            client.debugChunkInfo = false;
            client.debugChunkOcclusion = false;
            client.wireFrame = false;
            client.getDebugHud().clear();
            client.getEntityRenderDispatcher().setRenderHitboxes(false);
        }
    }
}