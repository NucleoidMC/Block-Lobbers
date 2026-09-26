package io.github.haykam821.blocklobbers.game.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import io.github.haykam821.blocklobbers.BlockLobbers;
import io.github.haykam821.blocklobbers.game.lobbable.Lobbable;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableBehavior;
import io.github.haykam821.blocklobbers.game.lobbable.LobbableEntity;
import io.github.haykam821.blocklobbers.game.phase.BlockLobbersActivePhase;
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
	private final ServerPlayer player;
	private final List<Lobbable> lobbables = new ArrayList<>(MAX_LOBBABLES);

	public PlayerEntry(BlockLobbersActivePhase phase, ServerPlayer player) {
		this.phase = phase;
		this.player = player;
	}

	public void start() {
		this.player.setGameMode(GameType.SURVIVAL);
		this.player.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
		this.updateHotbar();

		this.player.getAbilities().mayfly = true;
		this.player.onUpdateAbilities();
	}

	public boolean tick() {
		// Eliminate players that are out of bounds
		if (!this.phase.getMap().getBounds().contains(this.player.blockPosition())) {
			this.phase.eliminate(this, false);
			return true;
		}

		return false;
	}

	// Listeners
	@Override
	public EventResult onBreak(ServerPlayer player, ServerLevel world, BlockPos pos) {
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
	public InteractionResult onUse(ServerPlayer player, InteractionHand hand) {
		if (!this.lobbables.isEmpty()) {
			Lobbable lobbable = this.lobbables.get(0);

			if (this.phase.getConfig().shouldConsumeLobbables()) {
				this.removeTopLobbable();
				this.updateHotbar();
			}

			LobbableEntity entity = new LobbableEntity(BlockLobbers.LOBBABLE_ENTITY_TYPE, player.level(), lobbable);
			entity.setPosRaw(player.getX(), player.getEyeY() - 0.1, player.getZ());
			entity.setOwner(player);

			entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 0.8f, 0);
			entity.syncVelocity = true;
			
			player.level().addFreshEntity(entity);
		}

		return InteractionResult.FAIL;
	}

	@Override
	public EventResult onPacket(ServerPlayer player, Packet<?> packet) {
		if (packet instanceof ServerboundPlayerAbilitiesPacket) {
			ServerboundPlayerAbilitiesPacket abilitiesPacket = (ServerboundPlayerAbilitiesPacket) packet;
			Abilities abilities = player.getAbilities();

			if (abilitiesPacket.isFlying() && abilities.mayfly) {
				abilities.mayfly = false;
				abilities.flying = false;

				player.onUpdateAbilities();
				VelocityHelper.sendLeapVelocityPacket(player);

				this.playSoundFromPlayer(SoundEvents.HORSE_SADDLE.value(), SoundSource.PLAYERS, 0.3f, 1.1f);
				this.updateExperienceBar(player);
			}
		} else if (packet instanceof ServerboundMovePlayerPacket && player.onGround()) {
			player.getAbilities().mayfly = true;

			player.onUpdateAbilities();
			this.updateExperienceBar(player);
		}

		return EventResult.PASS;
	}

	// Utilities
	public void updateHotbar() {
		Container inventory = this.player.getInventory();

		for (int slot = HOTBAR_START_SLOT; slot <= HOTBAR_END_SLOT; slot += 1) {
			if (slot == HOTBAR_START_SLOT) {
				inventory.setItem(HOTBAR_START_SLOT, PICKAXE.build());
			} else if (lobbables.size() > HOTBAR_END_SLOT - slot) {
				Lobbable lobbable = lobbables.get(HOTBAR_END_SLOT - slot);
				inventory.setItem(slot, lobbable.createStack(slot == HOTBAR_END_SLOT));
			} else {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	private void playSoundFromPlayer(SoundEvent event, SoundSource category, float volume, float pitch) {
		Level world = this.player.level();
		world.playSound(null, this.player, event, category, volume, pitch);
	}

	public void updateExperienceBar(ServerPlayer player) {
		player.experienceProgress = player.getAbilities().mayfly ? 1 : 0;
		player.setExperienceLevels(0);
	}

	private void removeTopLobbable() {
		this.lobbables.remove(0);
	}

	public Component getWinText() {
		return Component.translatable("text.blocklobbers.win", this.getName()).withStyle(ChatFormatting.GOLD);
	}

	public Component getEliminatedText(String suffix) {
		return Component.translatable("text.blocklobbers.eliminated" + suffix, this.getName()).withStyle(ChatFormatting.RED);
	}

	// Getters
	public BlockLobbersActivePhase getPhase() {
		return this.phase;
	}

	public ServerPlayer getPlayer() {
		return this.player;
	}

	public Component getName() {
		return this.player.getDisplayName();
	}
}
