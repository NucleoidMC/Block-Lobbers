package io.github.haykam821.blocklobbers.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class BlockLobbersConfig {
	public static final MapCodec<BlockLobbersConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(BlockLobbersConfig::getMap),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(BlockLobbersConfig::getPlayerConfig),
			Codec.BOOL.optionalFieldOf("consume_lobbables", true).forGetter(BlockLobbersConfig::shouldConsumeLobbables)
		).apply(instance, BlockLobbersConfig::new);
	});

	private final Identifier map;
	private final WaitingLobbyConfig playerConfig;
	private final boolean consumeLobbables;

	public BlockLobbersConfig(Identifier map, WaitingLobbyConfig playerConfig, boolean consumeLobbables) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.consumeLobbables = consumeLobbables;
	}

	public Identifier getMap() {
		return this.map;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public boolean shouldConsumeLobbables() {
		return this.consumeLobbables;
	}
}