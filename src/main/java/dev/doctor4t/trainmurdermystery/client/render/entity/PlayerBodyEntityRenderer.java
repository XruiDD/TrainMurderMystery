package dev.doctor4t.trainmurdermystery.client.render.entity;

import dev.doctor4t.trainmurdermystery.client.TrainMurderMysteryClient;
import dev.doctor4t.trainmurdermystery.client.model.TrainMurderMysteryEntityModelLayers;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class PlayerBodyEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends LivingEntityRenderer<PlayerBodyEntity, PlayerEntityModel<PlayerBodyEntity>> {
    public PlayerBodyEntityRenderer(EntityRendererFactory.Context ctx, boolean slim) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(slim ? TrainMurderMysteryEntityModelLayers.PLAYER_BODY_SLIM : TrainMurderMysteryEntityModelLayers.PLAYER_BODY), slim), 0F);
    }

    public void render(PlayerBodyEntity playerBodyEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        this.setModelPose();
        super.render(playerBodyEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    private void setModelPose() {
        PlayerEntityModel<PlayerBodyEntity> playerEntityModel = this.getModel();
        playerEntityModel.setVisible(false);
        playerEntityModel.head.visible = true;
        playerEntityModel.hat.visible = true;
        playerEntityModel.setVisible(true);
        playerEntityModel.hat.visible = true;
        playerEntityModel.jacket.visible = true;
        playerEntityModel.leftPants.visible = true;
        playerEntityModel.rightPants.visible = true;
        playerEntityModel.leftSleeve.visible = true;
        playerEntityModel.rightSleeve.visible = true;
    }

    @Override
    public Identifier getTexture(PlayerBodyEntity playerBodyEntity) {
        PlayerListEntry playerListEntry = TrainMurderMysteryClient.PLAYER_ENTRIES_CACHE.get(playerBodyEntity.getPlayerUuid());
        if (playerListEntry != null) {
            return playerListEntry.getSkinTextures().texture();
        } else {
            return Identifier.ofVanilla("missing");
        }
    }



    @Override
    protected void renderLabelIfPresent(PlayerBodyEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {

    }

    @Override
    protected void setupTransforms(PlayerBodyEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 - bodyYaw));
        matrices.translate(0F, 0.1f, 0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.getLyingAngle(entity)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        matrices.translate(0F, -.95f, 0F);
    }

    @Override
    protected void scale(PlayerBodyEntity entity, MatrixStack matrices, float amount) {
        float g = 0.9375F;
        matrices.scale(g, g, g);
    }

    @Override
    protected float getHandSwingProgress(PlayerBodyEntity entity, float tickDelta) {
        return 0f;
    }

    @Override
    protected float getAnimationProgress(PlayerBodyEntity entity, float tickDelta) {
        return 0f;
    }
}
