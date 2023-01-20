package me.vanthanyx.midas.commands;

import java.io.File;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.vanthanyx.midas.Hydrogen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

  public final Hydrogen plugin;
  private FileConfiguration config;
  public Connection conn;

  public RegisterCommand(Hydrogen plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
  }

  public boolean onCommand(
    CommandSender sender,
    Command command,
    String label,
    String[] args
  ) {
    File dataFolder = plugin.pullDataFolder();

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
        "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + dbFile;
      conn = DriverManager.getConnection(url);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Step 3: Create a table in the database to store the player's name and UUID w/ time and date
    try (
      PreparedStatement pstmt = conn.prepareStatement(
        "CREATE TABLE IF NOT EXISTS players (id INT, uuid TEXT PRIMARY KEY, name TEXT, ip TEXT, port INT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)"
      )
    ) {
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    if (!(sender instanceof Player)) {
      sender.sendMessage("Players only.");
      return true;
    }

    Player player = (Player) sender;

    if (command.getName().equalsIgnoreCase("register")) {
      InetSocketAddress playerAddress = player.getAddress();
      String playerIP = playerAddress.getAddress().getHostAddress();
      int playerPort = playerAddress.getPort();
      UUID playerUUID = player.getUniqueId();
      String playerName = player.getName();

      if (args.length == 1 && args[0].equals("--adminReset")) {
        // No arguments were provided
        try {
          PreparedStatement pstmt = conn.prepareStatement(
            "DELETE FROM players"
          );
          pstmt.executeUpdate();
          sender.sendMessage("§e§lDatabase reset by admin.");
          return true;
        } catch (SQLException e) {
          e.printStackTrace();
          return false;
        }
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
        return false;
      }

      int playerID = 0;
      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "SELECT MAX(id) FROM players"
        )
      ) {
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            playerID = rs.getInt(1) + 1;
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
        return false;
      }

      boolean hidePlayerIDs = config.getBoolean("hidePlayerIDs");
      String playerIDValue;

      if (hidePlayerIDs) {
        playerIDValue = "§c§lHIDDEN";
      } else {
        playerIDValue = String.valueOf(playerID);
      }

      if (playerExists) {
        //player already in database
        String getPlayerName = player.getName();
        try (
          PreparedStatement pstmt = conn.prepareStatement(
            "SELECT id FROM players WHERE name = ?"
          )
        ) {
          pstmt.setString(1, getPlayerName);
          try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
              playerID = rs.getInt(1);
            }
          }
        } catch (SQLException e) {
          e.printStackTrace();
          return false;
        }
        player.sendMessage(
          "You are already in the beta program with ID: §e§l" + playerIDValue
        );
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
        return true;
      } else {
        //save player to database
        saveToDB(playerID, playerUUID, playerName, playerIP, playerPort);
        player.sendMessage(
          "You have been registered to the beta program with ID: §e§l" +
          playerIDValue
        );
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }

      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
      return true;
    }

    return false;
  }

  public void saveToDB(
    int playerID,
    UUID playerUUID,
    String playerName,
    String playerIP,
    int playerPort
  ) {
    try (
      PreparedStatement pstmt = conn.prepareStatement(
        "INSERT INTO players (id, uuid, name, ip, port) VALUES (?, ?, ?, ?, ?)"
      )
    ) {
      pstmt.setString(1, String.valueOf(playerID));
      pstmt.setString(2, playerUUID.toString());
      pstmt.setString(3, playerName);
      pstmt.setString(4, playerIP);
      pstmt.setString(5, String.valueOf(playerPort));
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
