package io.github.haykam821.blocklobbers.game.map;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import io.github.haykam821.blocklobbers.game.player.PlayerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

public class BlockLobbersMap {
	public static final String SPAWN_KEY = "spawn";
	public static final String WAITING_SPAWN_KEY = "waiting_spawn";

	public static final String FACING_KEY = "Facing";
	public static final String PRIORITY_KEY = "Priority";

	private final MapTemplate template;

	private final List<TemplateRegion> spawns;
	private final TemplateRegion waitingSpawn;

	public BlockLobbersMap(MapTemplate template) {
		this.template = template;

		this.spawns = template
			.getMetadata()
			.getRegions(SPAWN_KEY)
			.sorted((a, b) -> {
				return BlockLobbersMap.getPriority(a) - BlockLobbersMap.getPriority(b);
			})
			.collect(Collectors.toUnmodifiableList());
		Preconditions.checkState(!this.spawns.isEmpty(), "No spawn is present");

		this.waitingSpawn = template.getMetadata().getFirstRegion(WAITING_SPAWN_KEY);
		Preconditions.checkNotNull(this.waitingSpawn, "Waiting spawn is not present");
	}

	public BlockBounds getBounds() {
		return this.template.getBounds();
	}

	public void spawn(ServerWorld world, Set<PlayerEntry> players) {
		int index = 0;
		for (PlayerEntry entry : players) {
			TemplateRegion region = this.spawns.get(index % this.spawns.size());
			this.spawn(world, entry.getPlayer(), region);

			index += 1;
		}
	}

	public Vec3d getWaitingSpawnPos() {
		return this.waitingSpawn.getBounds().centerBottom();
	}

	public void spawnAtWaiting(ServerWorld world, ServerPlayerEntity player) {
		this.spawn(world, player, this.waitingSpawn);
	}

	private void spawn(ServerWorld world, ServerPlayerEntity player, TemplateRegion region) {
		Vec3d pos = region.getBounds().centerBottom();
		float yaw = region.getData().getFloat(FACING_KEY, 0);

		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), yaw, 0, true);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private static int getPriority(TemplateRegion region) {
		if (region == null || region.getData() == null) {
			return 0;
		}
		return region.getData().getInt(PRIORITY_KEY, 0);
    }
}