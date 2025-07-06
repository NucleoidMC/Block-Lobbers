package io.github.haykam821.blocklobbers.game.player;

import java.util.ArrayList;
import java.util.List;

import io.github.haykam821.blocklobbers.BlockLobbers;
import io.github.haykam821.blocklobbers.game.lobbable.Lobbable;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableBehavior;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableEntity;
import io.github.haykam821.blocklobbers.game.phase.BlockLobbersActivePhase;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;

public class PlayerEntry implements BlockBreakEvent, ItemUseEvent, PlayerC2SPacketEvent {
	private static final int MAX_LOBBABLES = 3;

	private static final int HOTBAR_START_SLOT = 0;
	private static final int HOTBAR_END_SLOT = 8;

	private static final ItemStackBuilder PICKAXE = ItemStackBuilder.of(Items.IRON_PICKAXE)
		.setUnbreakable();

	private final BlockLobbersActivePhase phase;
	private final ServerPlayerEntity player;
	private final List<Lobbable> lobbables = new ArrayList<>(MAX_LOBBABLES);

	public PlayerEntry(BlockLobbersActivePhase phase, ServerPlayerEntity player) {
		this.phase = phase;
		this.player = player;
	}

	public void start() {
		this.player.changeGameMode(GameMode.SURVIVAL);
		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, StatusEffectInstance.INFINITE, 0, true, false, false));
		this.updateHotbar();

		this.player.getAbilities().allowFlying = true;
		this.player.sendAbilitiesUpdate();
	}

	public boolean tick() {
		// Eliminate players that are out of bounds
		if (!this.phase.getMap().getBounds().contains(this.player.getBlockPos())) {
			this.phase.eliminate(this, false);
			return true;
		}

		return false;
	}

	// Listeners
	@Override
	public EventResult onBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return EventResult.PASS;

		if (this.lobbables.size() >= MAX_LOBBABLES) {
			this.removeTopLobbable();
		}

		this.lobbables.add(LobbableBehavior.create(state));
		this.updateHotbar();

		return EventResult.ALLOW;
	}

	@Override
	public ActionResult onUse(ServerPlayerEntity player, Hand hand) {
		if (!this.lobbables.isEmpty()) {
			Lobbable lobbable = this.lobbables.get(0);

			if (this.phase.getConfig().shouldConsumeLobbables()) {
				this.removeTopLobbable();
				this.updateHotbar();
			}

			LobbableEntity entity = new LobbableEntity(BlockLobbers.LOBBABLE_ENTITY_TYPE, player.getWorld(), lobbable);
			entity.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
			entity.setOwner(player);

			entity.setVelocity(player, player.getPitch(), player.getYaw(), 0, 0.8f, 0);
			entity.velocityModified = true;
			
			player.getWorld().spawnEntity(entity);
		}

		return ActionResult.FAIL;
	}

	@Override
	public EventResult onPacket(ServerPlayerEntity player, Packet<?> packet) {
		if (packet instanceof UpdatePlayerAbilitiesC2SPacket) {
			UpdatePlayerAbilitiesC2SPacket abilitiesPacket = (UpdatePlayerAbilitiesC2SPacket) packet;
			PlayerAbilities abilities = player.getAbilities();

			if (abilitiesPacket.isFlying() && abilities.allowFlying) {
				abilities.allowFlying = false;
				abilities.flying = false;

				player.sendAbilitiesUpdate();
				VelocityHelper.sendLeapVelocityPacket(player);

				this.playSoundFromPlayer(SoundEvents.ENTITY_HORSE_SADDLE.value(), SoundCategory.PLAYERS, 0.3f, 1.1f);
				this.updateExperienceBar(player);
			}
		} else if (packet instanceof PlayerMoveC2SPacket && player.isOnGround()) {
			player.getAbilities().allowFlying = true;

			player.sendAbilitiesUpdate();
			this.updateExperienceBar(player);
		}

		return EventResult.PASS;
	}

	// Utilities
	public void updateHotbar() {
		Inventory inventory = this.player.getInventory();

		for (int slot = HOTBAR_START_SLOT; slot <= HOTBAR_END_SLOT; slot += 1) {
			if (slot == HOTBAR_START_SLOT) {
				inventory.setStack(HOTBAR_START_SLOT, PICKAXE.build());
			} else if (lobbables.size() > HOTBAR_END_SLOT - slot) {
				Lobbable lobbable = lobbables.get(HOTBAR_END_SLOT - slot);
				inventory.setStack(slot, lobbable.createStack(slot == HOTBAR_END_SLOT));
			} else {
				inventory.setStack(slot, ItemStack.EMPTY);
			}
		}
	}

	private void playSoundFromPlayer(SoundEvent event, SoundCategory category, float volume, float pitch) {
		World world = this.player.getWorld();
		world.playSoundFromEntity(null, this.player, event, category, volume, pitch);
	}

	public void updateExperienceBar(ServerPlayerEntity player) {
		player.experienceProgress = player.getAbilities().allowFlying ? 1 : 0;
		player.setExperienceLevel(0);
	}

	private void removeTopLobbable() {
		this.lobbables.remove(0);
	}

	public Text getWinText() {
		return Text.translatable("text.blocklobbers.win", this.getName()).formatted(Formatting.GOLD);
	}

	public Text getEliminatedText(String suffix) {
		return Text.translatable("text.blocklobbers.eliminated" + suffix, this.getName()).formatted(Formatting.RED);
	}

	// Getters
	public BlockLobbersActivePhase getPhase() {
		return this.phase;
	}

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public Text getName() {
		return this.player.getDisplayName();
	}
}
