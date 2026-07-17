package io.github.haykam821.blocklobbers.game.lobbable;

import java.util.List;
import java.util.function.Consumer;

import eu.pb4.polymer.virtualentity.api.data.EntityData;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.MobAnchorElement;
import it.unimi.dsi.fastutil.ints.IntList;

public class LobbableEntity extends Projectile implements PolymerEntity {
	private static final Vec3 ANCHOR_OFFSET = new Vec3(-0.5d, 0, -0.5d);

	private final Lobbable lobbable;

	private final ElementHolder holder;
	private final EntityAttachment attachment;
	private final MobAnchorElement rideAnchor = new MobAnchorElement();

	public LobbableEntity(EntityType<? extends LobbableEntity> entityType, Level world, Lobbable lobbable) {
		super(entityType, world);
		this.lobbable = lobbable;

		this.holder = new ElementHolder() {
			@Override
			protected void notifyElementsOfPositionUpdate(Vec3 newPos, Vec3 delta) {
				LobbableEntity.this.rideAnchor.notifyMove(this.currentPos, newPos, delta);
			}

			@Override
			public Vec3 getPos() {
				return this.getAttachment().getPos();
			}
		};

		rideAnchor.setOffset(ANCHOR_OFFSET);
		this.holder.addElement(rideAnchor);

		BlockDisplayElement blockDisplay = new BlockDisplayElement(lobbable.getBlock().defaultBlockState());
		blockDisplay.setOffset(ANCHOR_OFFSET);
		blockDisplay.ignorePositionUpdates();
		this.holder.addElement(blockDisplay);

		VirtualEntityUtils.addVirtualPassenger(this, blockDisplay.getEntityId());

		this.attachment = new EntityAttachment(this.holder, this, false);
	}

	public LobbableEntity(EntityType<? extends LobbableEntity> entityType, Level world) {
		this(entityType, world, new Lobbable(Blocks.AIR.defaultBlockState()));
	}

	protected boolean onBlockHit(VoxelShape shape) {
		if (!this.level().isClientSide()) {
			ServerLevel world = (ServerLevel) this.level();
			return this.lobbable.onCollide(world, this, this.position());
		}

		return false;
	}

	protected boolean onEntityHit(Entity entity) {
		if (!this.level().isClientSide()) {
			ServerLevel world = (ServerLevel) this.level();
			return this.lobbable.onCollide(world, this, entity.getEyePosition());
		}

		return false;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && (this.getOwner() != entity || this.tickCount > 5);
	}

	@Override
	public void tick() {
		super.tick();

		// Update position
		Vec3 velocity = this.getDeltaMovement();

		double x = this.getX() + velocity.x();
		double y = this.getY() + velocity.y();
		double z = this.getZ() + velocity.z();

		this.setPos(x, y, z);

		// Update velocity
		velocity = velocity.scale(0.99);

		if (!this.isNoGravity()) {
			velocity = velocity.subtract(0, this.getDefaultGravity(), 0);
		}

		this.setDeltaMovement(velocity);

		this.updateRotation();
		this.applyEffectsFromBlocks();

		Level world = this.level();

		// Handle collisions
		for (VoxelShape shape : world.getBlockCollisions(this, this.getBoundingBox())) {
			if (!shape.isEmpty() && this.onBlockHit(shape)) {
				this.discard();
				return;
			}
		}

		for (Entity entity : world.getEntities(this, this.getBoundingBox(), this::canHitEntity)) {
			if (this.onEntityHit(entity)) {
				this.discard();
				return;
			}
		}

		this.holder.tick();
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);

		this.holder.destroy();
		this.attachment.destroy();
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		return EntityTypes.ARMOR_STAND;
	}

	@Override
	public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
		if (packet instanceof ClientboundSetPassengersPacket passengersSetS2CPacket) {
			IntList passengers = IntList.of(passengersSetS2CPacket.getPassengers());
			packet = VirtualEntityUtils.createClientboundSetPassengersPacket(this.rideAnchor.getEntityId(), passengers);
		}

		consumer.accept(packet);
	}
	
	@Override
	public void modifyRawTrackedData(List<DataValue<?>> data, ServerPlayer player, boolean initial) {
		data.add(SynchedEntityData.DataValue.create(EntityData.FLAGS, (byte) (1 << EntityData.INVISIBLE_FLAG_INDEX)));
		data.add(SynchedEntityData.DataValue.create(ArmorStand.DATA_CLIENT_FLAGS, (byte) ArmorStand.CLIENT_FLAG_MARKER));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		return;
	}

	protected double getDefaultGravity() {
		return 0.05;
	}
}
