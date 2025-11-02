package dev.lrxh.blockChanger.snapshot;

import dev.lrxh.blockChanger.BlockChanger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

@SuppressWarnings("unused")

public class CuboidSnapshot {

  private final Map<Chunk, ChunkSectionSnapshot> snapshots;

  private CuboidSnapshot(final Map<Chunk, ChunkSectionSnapshot> snapshots, boolean add) {
    this.snapshots = Collections.unmodifiableMap(snapshots);
    if (add) snapshots.forEach((chunk, snapshot) -> SnapshotService.addSnapshot(snapshot, chunk.getWorld()));
  }

  public static CompletableFuture<CuboidSnapshot> create(final Location pos1, final Location pos2) {
    final World world = pos1.getWorld();

    final int minChunkX = Math.min(pos1.getChunk().getX(), pos2.getChunk().getX());
    final int maxChunkX = Math.max(pos1.getChunk().getX(), pos2.getChunk().getX());
    final int minChunkZ = Math.min(pos1.getChunk().getZ(), pos2.getChunk().getZ());
    final int maxChunkZ = Math.max(pos1.getChunk().getZ(), pos2.getChunk().getZ());

    final int totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);

    return loadChunksAndSnapshots(totalChunks, (x, z) -> world.getChunkAtAsync(x, z)
        .thenApplyAsync(chunk -> Map.entry(chunk, BlockChanger.createChunkBlockSnapshot(chunk)), BlockChanger.EXECUTOR),
      minChunkX, maxChunkX, minChunkZ, maxChunkZ
    );
  }

  private static CompletableFuture<CuboidSnapshot> loadChunksAndSnapshots(
    final int totalChunks,
    final BiFunction<Integer, Integer, CompletableFuture<Map.Entry<Chunk, ChunkSectionSnapshot>>> loader,
    final int minChunkX, final int maxChunkX, final int minChunkZ, final int maxChunkZ) {

    final List<CompletableFuture<Map.Entry<Chunk, ChunkSectionSnapshot>>> futures = new ArrayList<>(totalChunks);

    for (int x = minChunkX; x <= maxChunkX; x++) {
      for (int z = minChunkZ; z <= maxChunkZ; z++) {
        futures.add(loader.apply(x, z));
      }
    }

    return combineFutures(futures);
  }

  private static CompletableFuture<CuboidSnapshot> combineFutures(
    final List<CompletableFuture<Map.Entry<Chunk, ChunkSectionSnapshot>>> futures) {

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(v -> {
        final Map<Chunk, ChunkSectionSnapshot> result = new HashMap<>(futures.size(), 0.9f);
        for (final CompletableFuture<Map.Entry<Chunk, ChunkSectionSnapshot>> future : futures) {
          final Map.Entry<Chunk, ChunkSectionSnapshot> entry = future.join();
          result.put(entry.getKey(), entry.getValue());
        }
        return new CuboidSnapshot(result, true);
      });
  }

  public Map<Chunk, ChunkSectionSnapshot> getSnapshots() {
    return snapshots;
  }

  public CompletableFuture<Void> restoreAsync(final boolean clearEntities) {
    return BlockChanger.restoreCuboidSnapshotAsync(this, clearEntities);
  }

  public void restore(final boolean clearEntities) {
    BlockChanger.restoreCuboidSnapshot(this, clearEntities);
  }

  @Override
  public CuboidSnapshot clone() {
    return new CuboidSnapshot(new HashMap<>(snapshots), false);
  }

  public CompletableFuture<CuboidSnapshot> offset(final int xOffset, final int zOffset) {
    return offset(xOffset, zOffset, new HashMap<>());
  }

  public CompletableFuture<CuboidSnapshot> offset(final int xOffset, final int zOffset, final Map<ChunkPosition, Chunk> preloadedChunks) {
    if (snapshots.isEmpty()) {
      return CompletableFuture.completedFuture(new CuboidSnapshot(Collections.emptyMap(), false));
    }

    if (xOffset % 16 != 0 || zOffset % 16 != 0) {
      throw new IllegalArgumentException("Offsets must be multiples of 16.");
    }

    final int chunkOffsetX = xOffset / 16;
    final int chunkOffsetZ = zOffset / 16;

    final List<CompletableFuture<Map.Entry<Chunk, ChunkSectionSnapshot>>> futures = new ArrayList<>(snapshots.size());
    final Executor executor = BlockChanger.EXECUTOR;

    for (final Map.Entry<Chunk, ChunkSectionSnapshot> entry : snapshots.entrySet()) {
      final Chunk originalChunk = entry.getKey();
      final ChunkSectionSnapshot snapshot = entry.getValue();

      final int newX = originalChunk.getX() + chunkOffsetX;
      final int newZ = originalChunk.getZ() + chunkOffsetZ;
      final ChunkPosition newPos = new ChunkPosition(newX, newZ);

      final CompletableFuture<Chunk> chunkFuture = Optional.ofNullable(preloadedChunks.get(newPos))
        .map(CompletableFuture::completedFuture)
        .orElseGet(() -> originalChunk.getWorld().getChunkAtAsync(newX, newZ));

      futures.add(chunkFuture.thenApplyAsync(chunk -> Map.entry(chunk, snapshot), executor));
    }

    return combineFutures(futures);
  }
}
