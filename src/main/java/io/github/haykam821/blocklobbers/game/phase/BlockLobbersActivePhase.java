package io.github.haykam821.blocklobbers.game.phase;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import io.github.haykam821.blocklobbers.game.BlockLobbersConfig;
import io.github.haykam821.blocklobbers.game.map.BlockLobbersMap;
import io.github.haykam821.blocklobbers.game.player.PlayerEntry;
import io.github.haykam821.blocklobbers.game.player.WinManager;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class BlockLobbersActivePhase implements BlockBreakEvent, GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Remove, GamePlayerEvents.Accept, ItemUseEvent, PlayerC2SPacketEvent, PlayerDamageEvent, PlayerDeathEvent {
	private final GameSpace gameSpace;
	private final ServerLevel world;
	private final BlockLobbersMap map;
	private final BlockLobbersConfig config;
	private final Set<PlayerEntry> players;
	private final WinManager winManager;

	private boolean closing;

	public BlockLobbersActivePhase(GameSpace gameSpace, ServerLevel world, BlockLobbersMap map, BlockLobbersConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;

		this.players = gameSpace.getPlayers().participants().stream().map(player -> {
			return new PlayerEntry(this, player);
		}).collect(Collectors.toSet());
		this.winManager = new WinManager(this, this.players.size() == 1);
	}

	public static void setRules(GameActivity activity) {
		activity.allow(GameRuleType.BREAK_BLOCKS);
	}

	public static void open(GameSpace gameSpace, ServerLevel world, BlockLobbersMap map, BlockLobbersConfig config) {
		gameSpace.setActivity(activity -> {
			BlockLobbersActivePhase phase = new BlockLobbersActivePhase(gameSpace, world, map, config);

			BlockLobbersWaitingPhase.setRules(activity);
			BlockLobbersActivePhase.setRules(activity);

			activity.listen(BlockBreakEvent.EVENT, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(GamePlayerEvents.REMOVE, phase);
			activity.listen(ItemUseEvent.EVENT, phase);
			activity.listen(PlayerC2SPacketEvent.EVENT, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public EventResult onBreak(ServerPlayer player, ServerLevel world, BlockPos pos) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			return entry.onBreak(player, world, pos);
		}

		return EventResult.PASS;
	}

	@Override
	public void onEnable() {
		this.map.spawn(this.world, this.players);

		for (PlayerEntry entry : this.players) {
			entry.start();
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.map.spawnAtWaiting(this.world, player);
			this.setSpectator(player);
		}
	}

	@Override
	public void onTick() {
		Iterator<PlayerEntry> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			PlayerEntry entry = playerIterator.next();
			if (entry.tick()) {
				playerIterator.remove();
			}
		}

		// Attempt to determine a winner
		if (this.winManager.checkForWinner()) {
			this.closing = true;
			gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	@Override
	public void onRemovePlayer(ServerPlayer player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			this.eliminate(entry, true);
		}
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	@Override
	public InteractionResult onUse(ServerPlayer player, InteractionHand hand) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			return entry.onUse(player, hand);
		}

		return InteractionResult.PASS;
	}

	@Override
	public EventResult onPacket(ServerPlayer player, Packet<?> packet) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			return entry.onPacket(player, packet);
		}

		return EventResult.PASS;
	}

	@Override
	public EventResult onDamage(ServerPlayer player, DamageSource source, float amount) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			this.eliminate(entry, true);
		}

		return EventResult.DENY;
	}

	// Getters
	public ServerLevel getWorld() {
		return this.world;
	}

	public GameSpace getGameSpace() {
		return this.gameSpace;
	}

	public BlockLobbersMap getMap() {
		return this.map;
	}

	public BlockLobbersConfig getConfig() {
		return this.config;
	}

	public Set<PlayerEntry> getPlayers() {
		return this.players;
	}

	// Utilities
	public PlayerEntry getPlayerEntry(ServerPlayer player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	private boolean eliminate(PlayerEntry entry, String suffix, boolean remove) {
		if (this.closing) return false;

		Component message = entry.getEliminatedText(suffix);
		for (ServerPlayer player : this.gameSpace.getPlayers()) {
			player.sendSystemMessage(message, false);
		}

		if (remove) {
			this.players.remove(entry);
		}
		this.setSpectator(entry.getPlayer());

		return true;
	}

	public boolean eliminate(PlayerEntry entry, boolean remove) {
		return this.eliminate(entry, "", remove);
	}
}