package com.cyloci.buttplugmc.listeners;

import com.cyloci.buttplugmc.ButtplugClientManager;
import com.cyloci.utils.Sleep;

import org.blackspherefollower.buttplug.client.ButtplugClientWSEndpoint;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class VibrateOnBlockBreak implements Listener {

  private final String BLOCK_TO_VIBRATE_OPTIONS = "block-vibrate-options";
  private final JavaPlugin plugin;
  private final ButtplugClientManager clientManager;

  public VibrateOnBlockBreak(JavaPlugin plugin, ButtplugClientManager clientManager) {
    this.plugin = plugin;
    this.clientManager = clientManager;
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".GRASS_BLOCK.level", 10);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".GRASS_BLOCK.duration", 100);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".DIRT.level", 10);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".DIRT.duration", 100);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".STONE.level", 20);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".STONE.duration", 150);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".COBBLESTONE.level", 20);
    this.plugin.getConfig().addDefault(BLOCK_TO_VIBRATE_OPTIONS + ".COBBLESTONE.duration", 150);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) throws Exception {
    Player player = event.getPlayer();
    ButtplugClientWSEndpoint client = this.clientManager.getClient(player);
    if (client == null) {
      return;
    }
    String blockName = event.getBlock().getType().name();
    int level = this.plugin.getConfig().getInt(BLOCK_TO_VIBRATE_OPTIONS + "." + blockName + ".level");
    int duration = this.plugin.getConfig().getInt(BLOCK_TO_VIBRATE_OPTIONS + "." + blockName + ".duration");
    client.getDevices().forEach(device -> {
      try {
        if( device.getScalarVibrateCount() > 0) {
          device.sendScalarVibrateCmd(level / 100.0);
          Sleep.sleep(duration);
          device.sendScalarVibrateCmd(0.0);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
