package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PowerToolsCommand implements TabExecutor {

    private final PowerTools plugin;

    public PowerToolsCommand(PowerTools plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String text) {
        s.sendMessage(PowerTools.color(PowerTools.PREFIX + text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("powertools.admin")) {
            msg(sender, "&cKeine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            msg(sender, "&e/pt give <spieler> <blitz_drill|waldzorn_axt|chronos_spitzhacke|abgrund_eimer|midas_axt> [menge]");
            msg(sender, "&e/pt reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.sell().reload();
            msg(sender, "&aConfig neu geladen.");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                msg(sender, "&cNutzung: /pt give <spieler> <typ> [menge]");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg(sender, "&cSpieler nicht online.");
                return true;
            }
            ItemManager.Type type = ItemManager.Type.parse(args[2]);
            if (type == null) {
                msg(sender, "&cUnbekannter Typ. Möglich: blitz_drill, waldzorn_axt, chronos_spitzhacke, abgrund_eimer, midas_axt");
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Math.min(36, Integer.parseInt(args[3])));
                } catch (NumberFormatException ignored) { }
            }
            for (int i = 0; i < amount; i++) {
                ItemStack it = plugin.items().create(type);
                for (ItemStack left : target.getInventory().addItem(it).values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), left);
                }
            }
            msg(sender, "&a" + amount + "x " + type.displayName + " &aan " + target.getName() + " gegeben.");
            return true;
        }

        msg(sender, "&cUnbekannter Unterbefehl.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("powertools.admin")) return out;
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload"), out);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            StringUtil.copyPartialMatches(args[1], names, out);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> types = new ArrayList<>();
            for (ItemManager.Type t : ItemManager.Type.values()) types.add(t.model);
            StringUtil.copyPartialMatches(args[2], types, out);
        }
        return out;
    }
}
