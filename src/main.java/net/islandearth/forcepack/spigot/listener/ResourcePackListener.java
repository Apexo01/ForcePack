package net.islandearth.forcepack.spigot.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import net.islandearth.forcepack.spigot.ForcePack;

public class ResourcePackListener implements Listener {
	
	private List<UUID> waiting = new ArrayList<>();
	private ForcePack plugin;
	
	public ResourcePackListener(ForcePack plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void ResourcePackStatus(PlayerResourcePackStatusEvent prpse) {
		Player player = prpse.getPlayer();
		if (!player.hasPermission("ForcePack.bypass")) {
			switch (prpse.getStatus()) {
				case DECLINED: {
					plugin.getLogger().info(player.getName() + " declined the resource pack.");
					String cmd = getConfig().getString("Server.Actions.On_Deny.Command").replaceAll("[player]", player.getName());
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
					if (getConfig().getBoolean("Server.kick")) {
						player.kickPlayer(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Declined_Message")));
					} else {
						player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Declined_Message")));
					}
					waiting.remove(player.getUniqueId());
					break;
				}
				
				case FAILED_DOWNLOAD: {
					plugin.getLogger().info(player.getName() + " failed to download the resource pack.");
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Failed_Download_Message")));
					String cmd = getConfig().getString("Server.Actions.On_Fail.Command").replaceAll("[player]", player.getName());
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
					waiting.remove(player.getUniqueId());
					break;
				}
				
				default: {
					plugin.getLogger().info(player.getName() + " accepted the resource pack.");
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Accepted_Message")));
					String cmd = getConfig().getString("Server.Actions.On_Accept.Command").replace("[player]", player.getName());
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
					waiting.remove(player.getUniqueId());
					break;
				}
			}
		}
	}	
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent pje) {
		Player player = pje.getPlayer();
		if (!player.hasPermission("ForcePack.bypass")) {
			waiting.add(player.getUniqueId());
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
				if (waiting.contains(player.getUniqueId())) {
					plugin.getLogger().info(player.getName() + " timed out.");
					player.kickPlayer(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Declined_Message")));
					waiting.remove(player.getUniqueId());
				}
			}, getConfig().getInt("Server.Timeout_ticks"));
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent pqe) {
		Player player = pqe.getPlayer();
		if (!player.hasPermission("ForcePack.bypass")) {
			if (waiting.contains(player.getUniqueId())) {
				waiting.remove(player.getUniqueId());
			}
		}
	}
	
	private FileConfiguration getConfig() {
		return plugin.getConfig();
	}
}
