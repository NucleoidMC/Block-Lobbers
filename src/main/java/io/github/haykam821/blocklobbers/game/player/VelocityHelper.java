package io.github.haykam821.blocklobbers.game.player;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VelocityHelper {
	private static final double LEAP_MULTIPLIER = 0.9;

	private static final double LEAP_MIN_Y = 0.15;
	private static final double STEALTHY_LEAP_MIN_Y = 0;

	public static void setVelocity(Entity entity, Vec3 velocity) {
		if (entity instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) entity;
			VelocityHelper.sendVelocityPacket(player, velocity);
		} else {
			entity.setDeltaMovement(velocity);
		}
	}

	public static void sendVelocityPacket(ServerPlayer player, Vec3 velocity) {
		player.connection.send(new ClientboundExplodePacket(Vec3.ZERO, 0, 0, Optional.of(velocity), ParticleTypes.EXPLOSION, Holder.direct(SoundEvents.EMPTY), WeightedList.of()));
	}

	public static void sendLeapVelocityPacket(ServerPlayer player) {
		VelocityHelper.sendVelocityPacket(player, VelocityHelper.getLeapVelocity(player));
	}

	public static Vec3 getLeapVelocity(ServerPlayer player) {
		Vec3 facing = Vec3
			.directionFromRotation(player.getXRot(), player.getYRot())
			.scale(LEAP_MULTIPLIER);

		double y = Math.max(VelocityHelper.getLeapMinY(player), facing.y());
		return new Vec3(facing.x(), y, facing.z());
	}

	private static double getLeapMinY(ServerPlayer player) {
		if (player.isShiftKeyDown()) {
			return STEALTHY_LEAP_MIN_Y;
		} else {
			return LEAP_MIN_Y;
		}
	}
}
