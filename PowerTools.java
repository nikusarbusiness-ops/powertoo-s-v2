package de.powertools;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerTools extends JavaPlugin {

    public static final String PREFIX = "&6[PowerTools] &r";

    private ItemManager items;
    private SellManager sell;
    private OrderManager orders;
    private SellCommand sellCommand;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        items = new ItemManager(this);
        sell = new SellManager(this);
        orders = new OrderManager(this);
        sellCommand = new SellCommand(this);
        ExpiryManager timed = new ExpiryManager(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new DrillListener(this), this);
        pm.registerEvents(new TreeAxeListener(this), this);
        pm.registerEvents(timed, this);
        pm.registerEvents(new VoidBucketListener(this), this);
        pm.registerEvents(new SellAxeListener(this), this);
        pm.registerEvents(sellCommand, this);
        pm.registerEvents(new ResourcePackListener(this), this);

        PowerToolsCommand admin = new PowerToolsCommand(this);
        getCommand("powertools").setExecutor(admin);
        getCommand("powertools").setTabCompleter(admin);

        getCommand("sell").setExecutor(sellCommand);

        OrderCommand orderCommand = new OrderCommand(this);
        getCommand("order").setExecutor(orderCommand);
        getCommand("order").setTabCompleter(orderCommand);

        if (economy() == null) {
            getLogger().warning("Kein Vault/Economy-Plugin gefunden - /sell, /order und die Sell-Axe funktionieren erst, wenn eines vorhanden ist.");
        }
    }

    @Override
    public void onDisable() {
        if (sellCommand != null) sellCommand.returnAllOpenGuis();
        if (orders != null) orders.save();
    }

    /** Economy wird bei Bedarf geholt, damit die Ladereihenfolge egal ist. */
    public Economy economy() {
        if (economy == null && getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        return economy;
    }

    public ItemManager items() { return items; }
    public SellManager sell() { return sell; }
    public OrderManager orders() { return orders; }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
