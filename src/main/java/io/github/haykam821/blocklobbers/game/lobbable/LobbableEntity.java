package io.github.haykam821.blocklobbers.game.lobbable;

import java.util.Collections;

import eu.pb4.polymer.api.entity.PolymerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class LobbableEntity extends ThrownEntity implements PolymerEntity {
	public static final double KNOCKBACK_STRENGTH = 3;

	private final Lobbable lobbable;

	public LobbableEntity(EntityType<? extends LobbableEntity> entityType, World world, Lobbable lobbable) {
		super(entityType, world);
		this.lobbable = lobbable;
	}

	public LobbableEntity(EntityType<? extends LobbableEntity> entityType, World world) {
		this(entityType, world, new Lobbable(Blocks.AIR));
	}

	private void breakLobbable(BlockPos pos) {
		world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, this.lobbable.getRawId());
		this.discard();
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		this.breakLobbable(hit.getBlockPos());
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		Entity entity = hit.getEntity();

		Vec3d velocity = entity.getPos()
			.subtract(this.getPos())
			.normalize()
			.add(0, 1, 0)
			.multiply(KNOCKBACK_STRENGTH);

		if (entity instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) entity;
			player.networkHandler.sendPacket(new ExplosionS2CPacket(0, 0, 0, 0, Collections.emptyList(), velocity));
		} else {
			entity.setVelocity(velocity);
		}

		this.breakLobbable(new BlockPos(entity.getEyePos()));
	}

	@Override
	public EntityType<?> getPolymerEntityType() {
		return EntityType.FALLING_BLOCK;
	}
	
	@Override
	public Packet<?> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this, this.lobbable.getRawId());
	}

	@Override
	protected void initDataTracker() {
		return;
	}

	@Override
	protected float getGravity() {
		return 0.05f;
	}
}