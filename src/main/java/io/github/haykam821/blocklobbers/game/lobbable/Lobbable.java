package io.github.haykam821.blocklobbers.game.lobbable;

import io.github.haykam821.blocklobbers.game.player.VelocityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

public class Lobbable {
	private static final double KNOCKBACK_STRENGTH = 1.3;
	private static final double AFFECTED_RANGE = 1.5;

	private final BlockState state;

	public Lobbable(BlockState state) {
		this.state = state;
	}

	public Block getBlock() {
		return this.state.getBlock();
	}

	public int getRawId() {
		return Block.getId(this.state);
	}

	public ItemStack createStack(boolean top) {
		ItemStackBuilder builder = ItemStackBuilder.of(this.state.getBlock());

		if (top) {
			builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}

		return builder.build();
	}

	private Vec3 getKnockbackVelocity(Vec3 lobbableVelocity, Entity affected) {
		Vec3 velocity = lobbableVelocity
			.normalize()
			.scale(KNOCKBACK_STRENGTH);

		if (affected.onGround()) {
			double y = Math.max(0, velocity.y() * -2);
			velocity = new Vec3(velocity.x(), y, velocity.y());
		}

		return velocity;
	}

	/**
	 * @return whether the lobbable entity should be discarded after the collision
	 */
	public boolean onCollide(ServerLevel world, LobbableEntity entity, Vec3 pos) {
		AABB affectedBox = entity.getBoundingBox().inflate(AFFECTED_RANGE);
		for (Entity affected : world.getEntities(entity, affectedBox)) {
			Vec3 knockbackVelocity = this.getKnockbackVelocity(entity.getDeltaMovement(), affected);
			VelocityHelper.setVelocity(affected, knockbackVelocity);
		}

		world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, BlockPos.containing(pos), this.getRawId());
		return true;
	}
}
