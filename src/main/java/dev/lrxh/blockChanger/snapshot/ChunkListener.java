package dev.lrxh.blockChanger.snapshot;

import dev.lrxh.blockChanger.BlockChanger;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public class ChunkListener implements Listener {
  private final JavaPlugin plugin;

  public ChunkListener(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    final ChunkPos chunkPos = new ChunkPos(event.getChunk().getX(), event.getChunk().getZ());
    final QueuedChunkSnapshot queuedChunkSnapshot = SnapshotService.getSnapshot(
      event.getWorld(), chunkPos);

    if (queuedChunkSnapshot == null) return;
    if (!event.getChunk().getWorld().getName().equals(queuedChunkSnapshot.worldName())) return;

    Bukkit.getScheduler().runTask(plugin, () -> {
      SnapshotService.removeSnapshot(event.getWorld(), chunkPos);
      BlockChanger.restoreChunkBlockSnapshot(event.getChunk(), queuedChunkSnapshot.snapshot(), true).thenRun(() -> BlockChanger.updateLighting(Set.of(event.getChunk())));
    });
  }
}
