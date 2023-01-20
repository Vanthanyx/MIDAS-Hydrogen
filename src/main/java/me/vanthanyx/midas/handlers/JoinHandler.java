package me.vanthanyx.midas.handlers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.vanthanyx.midas.Hydrogen;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinHandler implements Listener {

  public final Hydrogen plugin;
  private FileConfiguration config;
  public Connection conn;

  public JoinHandler(Hydrogen plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();

    File dataFolder = plugin.pullDataFolder();
    File databaseFile = new File(dataFolder, "registry.db");
    if (databaseFile.exists()) {
      // Step 1: Add the SQLite JDBC driver to the classpath
      try {
        Class.forName("org.sqlite.JDBC");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      // Step 2: Connect to the SQLite database
      try {
        String dbFile = "registry.db";
        String url =
          "jdbc:sqlite:" +
          dataFolder.getAbsolutePath() +
          File.separator +
          dbFile;
        conn = DriverManager.getConnection(url);
      } catch (SQLException e) {
        e.printStackTrace();
      }

      boolean playerExists = false;
      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "SELECT uuid FROM players WHERE uuid = ?"
        )
      ) {
        pstmt.setString(1, playerUUID.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            playerExists = true;
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      boolean displayRegistryStatus = config.getBoolean(
        "displayRegistryStatus"
      );
      String registered;

      if (displayRegistryStatus) {
        if (playerExists) {
          registered = "§aRegistered For Beta";
        } else {
          registered = "§cNot Registered For Beta";
        }

        player
          .spigot()
          .sendMessage(
            ChatMessageType.ACTION_BAR,
            new TextComponent(registered)
          );
      }

      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
