package dev.doctor4t.trainmurdermystery.client.model;

import dev.doctor4t.trainmurdermystery.TrainMurderMystery;
import dev.doctor4t.trainmurdermystery.client.render.block_entity.SmallDoorBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public interface TrainMurderMysteryEntityModelLayers {
    EntityModelLayer SMALL_DOOR = layer("small_door");
    EntityModelLayer PLAYER_BODY = layer("player_body");
    EntityModelLayer PLAYER_BODY_SLIM = layer("player_body_slim");

    static void initialize() {
        EntityModelLayerRegistry.registerModelLayer(SMALL_DOOR, SmallDoorBlockEntityRenderer::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(PLAYER_BODY, () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64));
        EntityModelLayerRegistry.registerModelLayer(PLAYER_BODY_SLIM, () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, true), 64, 64));
    }

    private static EntityModelLayer layer(String id, String name) {
        return new EntityModelLayer(TrainMurderMystery.id(id), name);
    }

    private static EntityModelLayer layer(String id) {
        return new EntityModelLayer(TrainMurderMystery.id(id), "main");
    }

}
