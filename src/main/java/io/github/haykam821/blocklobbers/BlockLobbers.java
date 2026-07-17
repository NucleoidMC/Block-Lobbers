package io.github.haykam821.blocklobbers;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.blocklobbers.game.BlockLobbersConfig;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableBehavior;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableEntity;
import io.github.haykam821.blocklobbers.game.phase.BlockLobbersWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class BlockLobbers implements ModInitializer {
	private static final String MOD_ID = "blocklobbers";

	private static final Identifier BLOCK_LOBBERS_ID = BlockLobbers.identifier("block_lobbers");
	public static final GameType<BlockLobbersConfig> BLOCK_LOBBERS_TYPE = GameTypes.register(BLOCK_LOBBERS_ID, BlockLobbersConfig.CODEC, BlockLobbersWaitingPhase::open);

	private static final Identifier LOBBABLE_ID = BlockLobbers.identifier("lobbable");
	private static final ResourceKey<EntityType<?>> LOBBABLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, LOBBABLE_ID);

	public static final EntityType<LobbableEntity> LOBBABLE_ENTITY_TYPE = EntityType.Builder.<LobbableEntity>of(LobbableEntity::new, MobCategory.MISC)
		.sized(0.98f, 0.98f)
		.noSave()
		.noSummon()
		.clientTrackingRange(10)
		.updateInterval(20)
		.build(LOBBABLE_KEY);

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, LOBBABLE_KEY, LOBBABLE_ENTITY_TYPE);
		PolymerEntityUtils.registerType(LOBBABLE_ENTITY_TYPE);

		LobbableBehavior.registerDefaults();
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
