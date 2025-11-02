package dev.lrxh.blockChanger.world;

import dev.lrxh.blockChanger.BlockChanger;
import dev.lrxh.blockChanger.snapshot.ChunkSectionSnapshot;
import dev.lrxh.blockChanger.snapshot.CuboidSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@SuppressWarnings({"unused"})
public class VirtualWorld {
  private final ServerLevel level;

  public VirtualWorld(ServerLevel level) {
    this.level = level;
  }

  public World getWorld() {
    return level.getWorld();
  }

  public void unload() {
    try {
      level.getChunkSource().getDataStorage().close();
      level.moonrise$getChunkTaskScheduler().chunkHolderManager.close(false, false);
      level.levelStorageAccess.close();
    } catch (Exception ignored) {
    }

    MinecraftServer.getServer().removeLevel(level);
    BlockChanger.removeVirtualWorld(this);

    Path worldPath = MinecraftServer.getServer()
      .server.getWorldContainer()
      .toPath()
      .resolve(level.getWorld().getName());

    CompletableFuture.runAsync(() -> {
      if (Files.exists(worldPath)) {
        try (Stream<Path> paths = Files.walk(worldPath)) {
          paths.sorted(Comparator.reverseOrder())
            .forEach(path -> {
              try {
                Files.delete(path);
              } catch (IOException e) {
                BlockChanger.log(e.getMessage());
              }
            });
        } catch (IOException e) {
          BlockChanger.log(e.getMessage());
        }
      }
    });
  }
  public void paste(CuboidSnapshot snapshot) {
    BlockChanger.paste(getWorld(), snapshot);
  }
}
