package com.cyloci.buttplugmc.listeners;

import com.cyloci.buttplugmc.ButtplugClientManager;
import com.cyloci.utils.Sleep;

import io.github.blackspherefollower.buttplug4j.client.ButtplugClientWSEndpoint;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class VibrateOnDamage implements Listener {

  private final String DAMAGE_VIBRATE_OPTIONS = "damage-vibrate-options";
  private final JavaPlugin plugin;
  private final ButtplugClientManager clientManager;

  public VibrateOnDamage(JavaPlugin plugin, ButtplugClientManager clientManager) {
    this.plugin = plugin;
    this.clientManager = clientManager;
    this.plugin.getConfig().addDefault(DAMAGE_VIBRATE_OPTIONS + ".level", 50);
    this.plugin.getConfig().addDefault(DAMAGE_VIBRATE_OPTIONS + ".duration", 250);
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) throws Exception {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getEntity();
    ButtplugClientWSEndpoint client = this.clientManager.getClient(player);
    if (client == null) {
      return;
    }
    int level = this.plugin.getConfig().getInt(DAMAGE_VIBRATE_OPTIONS + ".level");
    int duration = this.plugin.getConfig().getInt(DAMAGE_VIBRATE_OPTIONS + ".duration");
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
