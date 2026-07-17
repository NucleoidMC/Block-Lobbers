package io.github.haykam821.blocklobbers.game.lobbable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosiveLobbable extends Lobbable {
	public ExplosiveLobbable(BlockState state) {
		super(state);
	}

	@Override
	public boolean onCollide(ServerLevel world, LobbableEntity entity, Vec3 pos) {
		world.explode(entity, pos.x(), pos.y(), pos.z(), 5, ExplosionInteraction.NONE);
		return true;
	}
}
