package dev.lrxh.blockChanger.snapshot;

import net.minecraft.world.level.ChunkPos;
import org.bukkit.World;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotService {
  // key: "<world-uuid>:<chunkX>:<chunkZ>"
  private static final ConcurrentHashMap<String, QueuedChunkSnapshot> chunkSnapshots = new ConcurrentHashMap<>();

  private static String makeKey(final UUID worldUUID, final int chunkX, final int chunkZ) {
    return worldUUID.toString() + ":" + chunkX + ":" + chunkZ;
  }

  public static void addSnapshot(final ChunkSectionSnapshot snapshot, final World world) {
    final ChunkPos pos = snapshot.position();
    final String key = makeKey(world.getUID(), pos.x, pos.z);
    chunkSnapshots.put(key, new QueuedChunkSnapshot(world.getName(), snapshot));
  }

  public static QueuedChunkSnapshot getSnapshot(final World world, final ChunkPos position) {
    return chunkSnapshots.get(makeKey(world.getUID(), position.x, position.z));
  }

  public static void removeSnapshot(final World world, final ChunkPos position) {
    chunkSnapshots.remove(makeKey(world.getUID(), position.x, position.z));
  }
}
