package io.github.haykam821.blocklobbers.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class BlockLobbersConfig {
	private static final IntProvider DEFAULT_TICKS_UNTIL_CLOSE = ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5);

	public static final MapCodec<BlockLobbersConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(BlockLobbersConfig::getMap),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(BlockLobbersConfig::getPlayerConfig),
			Codec.BOOL.optionalFieldOf("consume_lobbables", true).forGetter(BlockLobbersConfig::shouldConsumeLobbables),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", DEFAULT_TICKS_UNTIL_CLOSE).forGetter(BlockLobbersConfig::getTicksUntilClose)
		).apply(instance, BlockLobbersConfig::new);
	});

	private final Identifier map;
	private final WaitingLobbyConfig playerConfig;
	private final boolean consumeLobbables;
	private final IntProvider ticksUntilClose;

	public BlockLobbersConfig(Identifier map, WaitingLobbyConfig playerConfig, boolean consumeLobbables, IntProvider ticksUntilClose) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.consumeLobbables = consumeLobbables;
		this.ticksUntilClose = ticksUntilClose;
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

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}
}