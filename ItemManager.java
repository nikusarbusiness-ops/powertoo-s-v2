package de.powertools;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemManager {

    /**
     * Alle PowerTools-Items. "model" ist der Schluessel fuer die Textur im Resourcepack
     * (custom_model_data -> strings[0]) und gleichzeitig der Name im /pt give Befehl.
     */
    public enum Type {
        DRILL(Material.NETHERITE_PICKAXE, "blitz_drill", "&b&lBlitz Drill"),
        TREE_AXE(Material.NETHERITE_AXE, "waldzorn_axt", "&2&lWaldzorn-Axt"),
        TIMED_PICKAXE(Material.DIAMOND_PICKAXE, "chronos_spitzhacke", "&e&lChronos-Spitzhacke"),
        VOID_BUCKET(Material.BUCKET, "abgrund_eimer", "&5&lAbgrund-Eimer"),
        SELL_AXE(Material.GOLDEN_AXE, "midas_axt", "&6&lMidas-Axt");

        public final Material material;
        public final String model;
        public final String displayName;

        Type(Material material, String model, String displayName) {
            this.material = material;
            this.model = model;
            this.displayName = displayName;
        }

        public static Type parse(String s) {
            if (s == null) return null;
            String n = s.toLowerCase(Locale.ROOT).replace('-', '_');
            for (Type t : values()) {
                if (t.name().toLowerCase(Locale.ROOT).equals(n) || t.model.equals(n)) return t;
            }
            return null;
        }
    }

    private final PowerTools plugin;
    private final NamespacedKey keyType;
    private final NamespacedKey keyExpire;

    public ItemManager(PowerTools plugin) {
        this.plugin = plugin;
        this.keyType = new NamespacedKey(plugin, "tool_type");
        this.keyExpire = new NamespacedKey(plugin, "expire_at");
    }

    public long lifetimeMillis() {
        return plugin.getConfig().getInt("items.lifetime-days", 5) * 86_400_000L;
    }

    private List<String> baseLore(Type type) {
        List<String> l = new ArrayList<>();
        switch (type) {
            case DRILL -> {
                l.add("&8&oGeschmiedet im Auge des Sturms.");
                l.add("");
                l.add("&b» &7Ein Schlag, &bneun Blöcke &7- 3x3-Abbau");
                l.add("&b» &7Blitzschnell durch Stein, Erz & Erde");
            }
            case TREE_AXE -> {
                l.add("&8&oDer Wald verneigt sich vor seinem Zorn.");
                l.add("");
                l.add("&2» &7Fällt &2ganze Bäume &7mit einem Schlag");
                l.add("&2» &7Kein Stamm bleibt stehen");
            }
            case TIMED_PICKAXE -> {
                l.add("&8&oJede Sekunde ist kostbar.");
                l.add("");
                l.add("&e» &7Spitzhacke mit &eEffizienz V");
                l.add("&e» &7Die Zeit tickt - nutze sie weise");
            }
            case VOID_BUCKET -> {
                l.add("&8&oEs flüstert aus der Leere.");
                l.add("");
                l.add("&5» &7Schluckt &5unendlich viel Wasser");
                l.add("&5» &7Alles verschwindet im Abgrund");
            }
            case SELL_AXE -> {
                l.add("&8&oAlles, was sie berührt, wird zu Gold.");
                l.add("");
                l.add("&6» &7Rechtsklick auf eine Kiste:");
                l.add("&6» &7verkauft den &6gesamten Inhalt");
            }
        }
        return l;
    }

    public ItemStack create(Type type) {
        ItemStack item = new ItemStack(type.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PowerTools.color("&r" + type.displayName));

        // Eigene Textur: Resourcepack ueberschreibt das Material anhand dieses Strings
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setStrings(List.of(type.model));
        meta.setCustomModelDataComponent(cmd);

        switch (type) {
            case DRILL, TREE_AXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            }
            case TIMED_PICKAXE -> meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            case SELL_AXE -> {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
            default -> { }
        }

        // Jedes Item zerfaellt nach X Tagen (Echtzeit)
        long expire = System.currentTimeMillis() + lifetimeMillis();
        meta.getPersistentDataContainer().set(keyExpire, PersistentDataType.LONG, expire);

        List<String> lore = baseLore(type);
        lore.add("");
        lore.add("&c» Zerfällt am: &f" + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(expire)));
        lore.add("&8PowerTools Relikt");

        meta.setEnchantmentGlintOverride(true);
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(PowerTools.color("&r" + line));
        meta.setLore(colored);
        meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public Type getType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        return Type.parse(s);
    }

    public Long getExpiry(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyExpire, PersistentDataType.LONG);
    }

    public boolean isExpired(ItemStack item) {
        if (getType(item) == null) return false;
        Long exp = getExpiry(item);
        return exp != null && exp <= System.currentTimeMillis();
    }

    /** Zieht Haltbarkeit vom Item in der Haupthand ab und zerstoert es bei Bedarf. */
    public void damageMainHand(Player p, int amount) {
        if (amount <= 0) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemMeta m = hand.getItemMeta();
        if (!(m instanceof Damageable d) || m.isUnbreakable()) return;
        d.setDamage(d.getDamage() + amount);
        if (d.getDamage() >= hand.getType().getMaxDurability()) {
            p.getInventory().setItemInMainHand(null);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
        } else {
            hand.setItemMeta(d);
            p.getInventory().setItemInMainHand(hand);
        }
    }

    public static String formatRemaining(long ms) {
        long s = Math.max(0, ms / 1000);
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m";
        return m + "m " + (s % 60) + "s";
    }
}
