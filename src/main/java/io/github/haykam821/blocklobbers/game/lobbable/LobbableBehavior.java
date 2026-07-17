package io.github.haykam821.blocklobbers.game.lobbable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LobbableBehavior {
	private static final Map<Block, Function<BlockState, Lobbable>> BEHAVIORS = new HashMap<>();

	private LobbableBehavior() {
		return;
	}

	private static void register(Block block, Function<BlockState, Lobbable> behavior) {
		BEHAVIORS.put(block, behavior);
	}

	public static void registerDefaults() {
		register(Blocks.TNT, ExplosiveLobbable::new);
	}

	public static Lobbable create(BlockState state) {
		Function<BlockState, Lobbable> behavior = BEHAVIORS.get(state.getBlock());
		if (behavior == null) {
			return new Lobbable(state);
		}

		return behavior.apply(state);
	}
}
