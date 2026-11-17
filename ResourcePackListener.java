package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Schickt optional das PowerTools-Resourcepack (Texturen) an Spieler, die joinen. */
public class ResourcePackListener implements Listener {

    private final PowerTools plugin;

    public ResourcePackListener(PowerTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) return;
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) return;

        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            byte[] hash = parseSha1(plugin.getConfig().getString("resource-pack.sha1", ""));
            if (hash != null) {
                p.setResourcePack(url.trim(), hash);
            } else {
                p.setResourcePack(url.trim());
            }
        }, 20L);
    }

    private static byte[] parseSha1(String hex) {
        if (hex == null) return null;
        hex = hex.trim();
        if (hex.length() != 40) return null;
        byte[] out = new byte[20];
        try {
            for (int i = 0; i < 20; i++) {
                out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return out;
    }
}
