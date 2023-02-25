package com.cyloci.buttplugmc.commands;

import org.blackspherefollower.buttplug.client.ButtplugClientWSEndpoint;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.cyloci.buttplugmc.ButtplugClientManager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class VibrateCommand {

  private final ButtplugClientManager clientManager;

  public VibrateCommand(ButtplugClientManager clientManager) {
    this.clientManager = clientManager;
  }

  public void handleCommand(Player player, double level) throws Exception {
    if (level < 0 || level > 100) {
      player.sendMessage(ChatColor.RED + "'level' must be between 0 and 100");
      return;
    }
    ButtplugClientWSEndpoint client = this.clientManager.getClient(player);
    client.getDevices().forEach(device -> {
      try {
        if( device.getScalarVibrateCount() > 0) {
          device.sendScalarVibrateCmd(level / 100.0);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    return;
  }
}
