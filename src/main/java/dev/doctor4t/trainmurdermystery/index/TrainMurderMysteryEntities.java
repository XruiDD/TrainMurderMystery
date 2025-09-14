package dev.doctor4t.trainmurdermystery.index;

import dev.doctor4t.ratatouille.util.registrar.EntityTypeRegistrar;
import dev.doctor4t.trainmurdermystery.TrainMurderMystery;
import dev.doctor4t.trainmurdermystery.block.entity.SeatEntity;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public interface TrainMurderMysteryEntities {
    EntityTypeRegistrar registrar = new EntityTypeRegistrar(TrainMurderMystery.MOD_ID);

    EntityType<SeatEntity> SEAT = registrar.create("seat", EntityType.Builder.create(SeatEntity::new, SpawnGroup.MISC)
            .dimensions(1f, 1f)
            .maxTrackingRange(128)
            .disableSummon()
    );
    EntityType<PlayerBodyEntity> PLAYER_BODY = registrar.create("player_body", EntityType.Builder.create(PlayerBodyEntity::new, SpawnGroup.MISC)
            .dimensions(1f, 0.25f)
            .maxTrackingRange(128)
            .disableSummon()
    );

    static void initialize() {
        registrar.registerEntries();

        FabricDefaultAttributeRegistry.register(PLAYER_BODY, PlayerBodyEntity.createAttributes());
    }
}
