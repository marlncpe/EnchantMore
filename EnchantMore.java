package me.exphc.EnchantMore;

import java.util.Random;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Formatter;
import java.lang.Byte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.Material.*;
import org.bukkit.material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.scheduler.*;
import org.bukkit.enchantments.*;
import org.bukkit.*;

import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftSpider;
import org.bukkit.craftbukkit.entity.CraftCaveSpider;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

import net.minecraft.server.MobEffect;
import net.minecraft.server.FurnaceRecipes;

class EnchantMoreListener implements Listener {

    // Better enchantment names more closely matching in-game display
    // TODO: replace with ItemStackX
    final Enchantment PROTECTION = Enchantment.PROTECTION_ENVIRONMENTAL;
    final Enchantment FIRE_PROTECTION = Enchantment.PROTECTION_FIRE;
    final Enchantment FEATHER_FALLING = Enchantment.PROTECTION_FALL;
    final Enchantment BLAST_PROTECTION = Enchantment.PROTECTION_EXPLOSIONS;
    final Enchantment PROJECTILE_PROTECTION = Enchantment.PROTECTION_PROJECTILE;
    final Enchantment RESPIRATION = Enchantment.OXYGEN;
    final Enchantment AQUA_AFFINITY = Enchantment.WATER_WORKER;
    final Enchantment SHARPNESS = Enchantment.DAMAGE_ALL;
    final Enchantment SMITE = Enchantment.DAMAGE_UNDEAD;
    final Enchantment BANE = Enchantment.DAMAGE_ARTHROPODS;
    final Enchantment KNOCKBACK = Enchantment.KNOCKBACK;
    final Enchantment FIRE_ASPECT = Enchantment.FIRE_ASPECT;
    final Enchantment LOOTING = Enchantment.LOOT_BONUS_MOBS;
    final Enchantment EFFICIENCY = Enchantment.DIG_SPEED;
    final Enchantment SILK_TOUCH = Enchantment.SILK_TOUCH;
    final Enchantment UNBREAKING = Enchantment.DURABILITY;
    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;
    final Enchantment POWER = Enchantment.ARROW_DAMAGE;
    final Enchantment PUNCH = Enchantment.ARROW_KNOCKBACK;
    final Enchantment FLAME = Enchantment.ARROW_FIRE;
    final Enchantment INFINITE = Enchantment.ARROW_INFINITE;

    Random random;
   
    EnchantMore plugin;

    public EnchantMoreListener(EnchantMore pl) {
        plugin = pl;

        random = new Random();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Action action = event.getAction();
        Player player = event.getPlayer();

        if (item == null) {
            return;
        }

        if (block == null) {
            // TODO: we might need to handle non-block (air) events
            return;
        }

        World world = block.getWorld();

        if (item.getType() == Material.FLINT_AND_STEEL && action == Action.RIGHT_CLICK_BLOCK) {
        
            // Flint & Steel + Smite = strike lightning
            if (item.containsEnchantment(SMITE)) {
                world.strikeLightning(block.getLocation());
            }

            // Flint & Steel + Fire Protection = player fire resistance
            if (item.containsEnchantment(FIRE_PROTECTION)) {
                ((CraftPlayer)player).getHandle().addEffect(new MobEffect(
                    12, // fireResistance - http://wiki.vg/Protocol#Effects
                    20*10*item.getEnchantmentLevel(FIRE_PROTECTION), // length
                    1)); // amplifier
            }

            // Flint & Steel + Aqua Affinity = vaporize water
            if (item.containsEnchantment(AQUA_AFFINITY)) {
                // Can't actually click on water, the click "goes through" as if it was air
                // Not like buckets filled or lily pads placements
                /* 
                if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
                    block.setType(Material.AIR);
                    plugin.log.info("water");
                }
                */

                // Find water within ignited cube area
                int r = item.getEnchantmentLevel(AQUA_AFFINITY);

                Location loc = block.getLocation();
                int x0 = loc.getBlockX();
                int y0 = loc.getBlockY();
                int z0 = loc.getBlockZ();
               
                for (int dx = -r; dx <= r; dx += 1) {
                    for (int dy = -r; dy <= r; dy += 1) {
                        for (int dz = -r; dz <= r; dz += 1) {
                            Block b = world.getBlockAt(dx+x0, dy+y0, dz+z0);
                           
                            if (b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER) {
                                b.setType(Material.AIR);
                                world.playEffect(b.getLocation(), Effect.SMOKE, 0); // TODO: direction
                            }
                        }
                    }
                }
            }

            // Flint & Steel + Sharpness = firey explosion
            if (item.containsEnchantment(SHARPNESS)) {
                float power = item.getEnchantmentLevel(SHARPNESS) * 1.0f;

                world.createExplosion(block.getLocation(), power, true);
            }

        } else if (isHoe(item.getType())) {
            // Hoe + Aqua Affinity = hydrate below
            // TODO: maybe should add water on side of, if air? better for farming
            if (item.containsEnchantment(AQUA_AFFINITY)) {
                Block below = block.getRelative(BlockFace.DOWN, item.getEnchantmentLevel(AQUA_AFFINITY));
                
                if (below.getType() == Material.DIRT) {
                    below.setType(Material.STATIONARY_WATER);
                }
            }

            // Hoe + Fortune = chance to drop seeds
            if (item.containsEnchantment(FORTUNE) && action == Action.RIGHT_CLICK_BLOCK) {
                if (block.getType() == Material.DIRT || block.getType() == Material.GRASS) {
                    if (random.nextInt(3) != 0) {   // TODO: configurable, and depend on level
                        Material seedType;

                        // TODO: configurable probabilities
                        switch (random.nextInt(4)) {
                        case 2: seedType = Material.MELON_SEEDS; break;
                        case 3: seedType = Material.PUMPKIN_SEEDS; break;
                        default: seedType = Material.SEEDS; // wheat, 50%
                        }

                        // TODO: configurable and random quantity
                      
                        ItemStack drop = new ItemStack(seedType, 1);

                        world.dropItemNaturally(block.getRelative(BlockFace.UP, 1).getLocation(), drop);
                    }
                }
            }

            // Hoe + Efficiency = hoe larger area
            if (item.containsEnchantment(EFFICIENCY)) { // also can use left-click, for efficiency!
                int r = item.getEnchantmentLevel(EFFICIENCY);

                Location loc = block.getLocation();
                int x0 = loc.getBlockX();
                int y0 = loc.getBlockY();
                int z0 = loc.getBlockZ();
               
                for (int dx = -r; dx <= r; dx += 1) {
                    for (int dz = -r; dz <= r; dz += 1) {
                        Block b = world.getBlockAt(dx+x0, y0, dz+z0);
                       
                        if (b.getType() == Material.DIRT || b.getType() == Material.GRASS) {
                            b.setType(Material.SOIL);
                        }
                    }
                }
               
            }
        }
    }

    private boolean isHoe(Material m) {
        return m == Material.DIAMOND_HOE ||
            m == Material.GOLD_HOE || 
            m == Material.IRON_HOE ||
            m == Material.STONE_HOE ||
            m == Material.WOOD_HOE;
    }

    private boolean isPickaxe(Material m) {
        return m == Material.DIAMOND_PICKAXE ||
            m == Material.GOLD_PICKAXE ||
            m == Material.IRON_PICKAXE ||
            m == Material.STONE_PICKAXE ||
            m == Material.WOOD_PICKAXE;
    }

    private boolean isShovel(Material m) {
        return m == Material.DIAMOND_SPADE ||
            m == Material.GOLD_SPADE ||
            m == Material.IRON_SPADE ||
            m == Material.STONE_SPADE ||
            m == Material.WOOD_SPADE;
    }

    private boolean isAxe(Material m) {
        return m == Material.DIAMOND_AXE ||
            m == Material.GOLD_AXE ||
            m == Material.IRON_AXE ||
            m == Material.STONE_AXE ||
            m == Material.WOOD_AXE;
    }



    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null) {
            return;
        }

        if (item.getType() == Material.FLINT_AND_STEEL) {
            if (entity == null) {
                return;
            }

            // Flint & Steel + Fire Aspect = set mobs on fire
            if (item.containsEnchantment(FIRE_ASPECT)) {
                entity.setFireTicks(getFireTicks(item.getEnchantmentLevel(FIRE_ASPECT)));

                // TODO: fix
                item.setDurability((short)(item.getDurability() - 1));
            }

            // Flint & Steel + Respiration = smoke inhalation (confusion)
            if (item.containsEnchantment(RESPIRATION)) {
                World world = entity.getWorld();

                world.playEffect(entity.getLocation(), Effect.SMOKE, 0);    // TOOD: smoke direction
                world.playEffect(entity.getLocation(), Effect.EXTINGUISH, 0);    // TOOD: smoke direction

                // Confusion effect on players
                if (entity instanceof CraftPlayer) {
                    ((CraftPlayer)entity).getHandle().addEffect(new MobEffect(
                        9,      // confusion  - http://wiki.vg/Protocol#Effects
                        20*10*item.getEnchantmentLevel(RESPIRATION),  // length
                        1));    // amplifier
                }
            }
        } else if (item.getType() == Material.SHEARS) {
            // Shears + Smite = gouge eyes (blindness)
            if (item.containsEnchantment(SMITE)) {
                if (entity instanceof CraftPlayer) {
                    ((CraftPlayer)entity).getHandle().addEffect(new MobEffect(
                        15,     // blindness
                        20*10*item.getEnchantmentLevel(SMITE),  // length
                        1));    // amplifier
                }
                // TODO: use durability
            }

            // Shears + Bane of Arthropods = collect spider eyes
            if (item.containsEnchantment(BANE)) {
                if (entity instanceof CaveSpider || entity instanceof Spider) {
                    Creature bug = (Creature)entity;

                    // If at least 50% health, cut out eyes, then drop health
                    if (bug.getHealth() >= bug.getMaxHealth() / 2) {
                        World world = player.getWorld();

                        world.dropItemNaturally(bug.getEyeLocation(), new ItemStack(Material.SPIDER_EYE, 1));

                        bug.setHealth(bug.getMaxHealth() / 2 - 1);
                    }
                }
                // TODO: use durability
            }
        }
    }



    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null) {
            return;
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity entity = event.getCaught();

            if (entity == null) {
                return;
            }

            // Fishing Rod + Fire Aspect = set mobs on fire
            if (item.containsEnchantment(FIRE_ASPECT)) {
                entity.setFireTicks(getFireTicks(item.getEnchantmentLevel(FIRE_ASPECT)));

                // TODO: fix
                item.setDurability((short)(item.getDurability() - 1));
            }
            
            // Fishing Rod + Smite = strike mobs with lightning
            if (item.containsEnchantment(SMITE)) {
                World world = entity.getWorld();

                world.strikeLightning(entity.getLocation());
            }
        }
    }

    // Get time to burn entity for given enchantment level
    private int getFireTicks(int level) {
         // TODO: configurable ticks per level
        return 20 * 10 * level;
    }

    @EventHandler(priority = EventPriority.NORMAL) 
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = player.getItemInHand();
        World world = player.getWorld();

        if (item == null) {
            return;
        }

        if (isPickaxe(item.getType()) ||
            isShovel(item.getType()) ||
            isAxe(item.getType())) {

            // Pickaxe/shovel/axe + Flame = auto-smelt
            if (item.containsEnchantment(FLAME)) {
                Collection<ItemStack> rawDrops = block.getDrops(item);

                for (ItemStack rawDrop: rawDrops) {
                    ItemStack smeltedDrop = smelt(rawDrop);

                    world.dropItemNaturally(block.getLocation(), smeltedDrop);
                }


                block.setType(Material.AIR);
            }
        } else if (item.getType() == Material.SHEARS) {
            // Shears + Silk Touch = collect cobweb, dead bush
            if (item.containsEnchantment(SILK_TOUCH)) {
                if (block.getType() == Material.DEAD_BUSH ||
                    block.getType() == Material.WEB) {

                    world.dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));

                    block.setType(Material.AIR);
                } 
                // TODO: cut grass, turn into dirt (no drop)
                /*
                else if (block.getType() == Material.GRASS) {
                    block.setType(Material.DIRT);
                    event.setCancelled(true);
                }
                */
            }

            // Shears + Fortune = apples from leaves
            if (item.containsEnchantment(FORTUNE)) {
                if (block.getType() == Material.LEAVES) {
                    Material dropType;

                    // TODO: different probabilities, depending on level too (higher, more golden)
                    switch (random.nextInt(10)) {
                    case 0: dropType = Material.GOLDEN_APPLE; break;
                    default: dropType = Material.APPLE;
                    }

                    world.dropItemNaturally(block.getLocation(), new ItemStack(dropType, 1));
                    
                    block.setType(Material.AIR);
                }
            }
        } else if (isHoe(item.getType())) {
            // Hoe + Silk Touch = collect farmland
            if (item.containsEnchantment(SILK_TOUCH)) {
                // TODO: collect growing plants/food? wheat seeds, cake block, pumpkin stem, melon stem, nether wart block
                // would have to override block place too, to be able to place (and store growth stage in data)
                if (block.getType() == Material.SOIL) {

                    world.dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));
                    
                    block.setType(Material.AIR);
                }
            }
        }
    }

    // Get item as if it was smelted
    private ItemStack smelt(ItemStack raw) {
        net.minecraft.server.ItemStack smeltNMS = FurnaceRecipes.getInstance().a(raw.getTypeId());

        ItemStack smelted = (ItemStack)(new CraftItemStack(smeltNMS));
    
        return smelted;
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        ItemStack tool = player.getItemInHand();
        World world = player.getWorld();

        if (tool == null) {
            return;
        }

        if (!(entity instanceof Sheep)) {
            return;
        }
        // TODO: mooshroom?

        // Shears + Looting = more wool, random colors
        if (tool.getType() == Material.SHEARS && tool.containsEnchantment(LOOTING)) {
            Location loc = entity.getLocation();

            int quantity = random.nextInt(tool.getEnchantmentLevel(LOOTING) * 2);
            for (int i = 0; i < quantity; i += 1) {
                short color = (short)random.nextInt(16);

                world.dropItemNaturally(entity.getLocation(), new ItemStack(Material.WOOL, 1, color));
            }
        }
    }
}

public class EnchantMore extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    EnchantMoreListener listener;

    public void onEnable() {
        listener = new EnchantMoreListener(this);
    }
    
    public void onDisable() {
    }
}
