package me.vanthanyx.midas;

import java.io.File;
import java.io.IOException;
import me.vanthanyx.midas.commands.RegisterCommand;
import me.vanthanyx.midas.handlers.JoinHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Hydrogen extends JavaPlugin implements Listener {

  private FileConfiguration config;

  @Override
  public void onEnable() {
    File dataFolder = getDataFolder();
    File databaseFile = new File(getDataFolder(), "registry.db");
    File configFile = new File(getDataFolder(), "config.yml");
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
    if (!databaseFile.exists()) {
      logWarning("Database does not yet exist.");
      logWarning("Run /register as a player.");
    }

    if (!configFile.exists()) {
      try {
        if (configFile.createNewFile()) {}
      } catch (IOException e) {
        logSevere("Config file failed creation.");
        e.printStackTrace();
      }
    }

    saveDefaultConfig();
    config = getConfig();

    if (!config.contains("displayRegistryStatus")) {
      config.set("displayRegistryStatus", false);
      saveConfig();
    }
    if (!config.contains("hidePlayerIDs")) {
      config.set("hidePlayerIDs", true);
      saveConfig();
    }

    Bukkit.getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(new JoinHandler(this), this);
    getCommand("register").setExecutor(new RegisterCommand(this));
  }

  @Override
  public void onDisable() {
    saveConfig();
  }

  public File pullDataFolder() {
    return getDataFolder();
  }

  public void saveSetting(String key, Object value) {
    config.set(key, value);
    saveConfig();
  }

  public void logWarning(String message) {
    getLogger().warning(message);
  }

  public void logInfo(String message) {
    getLogger().info(message);
  }

  public void logSevere(String message) {
    getLogger().severe(message);
  }
}
