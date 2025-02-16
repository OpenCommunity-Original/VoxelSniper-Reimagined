package com.github.kevindagame.voxelsniper;

import com.github.kevindagame.VoxelBrushManager;
import com.github.kevindagame.VoxelSniper;
import com.github.kevindagame.util.Messages;
import com.github.kevindagame.util.VersionChecker;
import com.github.kevindagame.util.schematic.SchematicReader;
import com.github.kevindagame.voxelsniper.biome.VoxelBiome;
import com.github.kevindagame.voxelsniper.entity.entitytype.VoxelEntityType;
import com.github.kevindagame.voxelsniper.entity.player.IPlayer;
import com.github.kevindagame.voxelsniper.entity.player.SpigotPlayer;
import com.github.kevindagame.voxelsniper.fileHandler.IFileHandler;
import com.github.kevindagame.voxelsniper.fileHandler.SpigotFileHandler;
import com.github.kevindagame.voxelsniper.fileHandler.VoxelSniperConfiguration;
import com.github.kevindagame.voxelsniper.integration.bstats.BrushUsageCounter;
import com.github.kevindagame.voxelsniper.integration.bstats.BrushUsersCounter;
import com.github.kevindagame.voxelsniper.integration.plotsquared.PlotSquaredIntegration;
import com.github.kevindagame.voxelsniper.integration.worldguard.WorldGuardIntegration;
import com.github.kevindagame.voxelsniper.material.SpigotMaterial;
import com.github.kevindagame.voxelsniper.material.VoxelMaterial;
import com.github.kevindagame.voxelsniper.permissions.SpigotPermissionManager;
import com.github.kevindagame.voxelsniper.treeType.VoxelTreeType;
import com.github.kevindagame.voxelsniper.world.IWorld;
import com.github.kevindagame.voxelsniper.world.SpigotWorld;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * SPIGOT extension point.
 */
public class SpigotVoxelSniper extends JavaPlugin implements IVoxelsniper, Listener {

    private static SpigotVoxelSniper instance;
    private static BukkitAudiences adventure;

    private final SpigotVoxelSniperListener voxelSniperListener = new SpigotVoxelSniperListener(this);
    private final Map<String, IWorld> worlds = new HashMap<>();
    private final Map<UUID, SpigotPlayer> players = new HashMap<>();
    private VoxelSniperConfiguration voxelSniperConfiguration;
    private IFileHandler fileHandler;

    /**
     * @return {@link SpigotVoxelSniper}
     */
    public static SpigotVoxelSniper getInstance() {
        return SpigotVoxelSniper.instance;
    }

    public static BukkitAudiences getAdventure() {
        return SpigotVoxelSniper.adventure;
    }

    @Override
    public void onEnable() {
        VoxelSniper.voxelsniper = this;
        SpigotVoxelSniper.instance = this;

        this.fileHandler = new SpigotFileHandler(this);

        SpigotPermissionManager.register();

        // Initialize messages
        Messages.load(this);
        SpigotVoxelSniper.adventure = BukkitAudiences.create(this);

        // Initialize brush manager
        VoxelBrushManager brushManager = VoxelBrushManager.initialize();
        getLogger().log(Level.INFO, "Registered {0} Sniper Brushes with {1} handles.", new Object[]{brushManager.registeredSniperBrushes(), brushManager.registeredSniperBrushHandles()});

        // Initialize configuration
        saveDefaultConfig();
        voxelSniperConfiguration = new VoxelSniperConfiguration(this);

        //register events and listeners
        Bukkit.getPluginManager().registerEvents(this.voxelSniperListener, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Registered Sniper Listener.");

        new BrushUsageCounter().registerListeners();
        new BrushUsersCounter().registerListeners();

        // Initialize commands
        SchematicReader.initialize();
        SpigotCommandManager.initialize();

        // Initialize metrics
        Metrics metrics = new Metrics(this, 16602);

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard") && voxelSniperConfiguration.isWorldGuardIntegrationEnabled()) {
            new WorldGuardIntegration();
            getLogger().info("WorldGuard integration enabled.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlotSquared") && voxelSniperConfiguration.isPlotSquaredIntegrationEnabled()) {
            new PlotSquaredIntegration();
            getLogger().info("PlotSquared integration enabled.");
        }

        metrics.addCustomChart(new SimplePie("worldguard_integration", () -> WorldGuardIntegration.Companion.getEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("plotsquared_integration", () -> PlotSquaredIntegration.Companion.getEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SingleLineChart("total_brush_uses_in_last_30_minutes", BrushUsageCounter::getTotalBrushUses));
//        metrics.addCustomChart(new Metrics.MultiLineChart("uses_per_brush", BrushUsageCounter::getUsagePerBrush));
        metrics.addCustomChart(new SingleLineChart("total_snipers", BrushUsersCounter.Companion::getTotalBrushUses));

        // Do update check
        if (voxelSniperConfiguration.isUpdateCheckerEnabled()) {
            // Do update check asynchronously to prevent lag
            new Thread(() -> {
                Bukkit.getLogger().info("Checking for updates...");
                new VersionChecker().getLatestVersionInfo(this.getDescription().getVersion());
            }).start();
        }
    }

    @Override
    public void onDisable() {
        if (SpigotVoxelSniper.adventure != null) {
            SpigotVoxelSniper.adventure.close();
            SpigotVoxelSniper.adventure = null;
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
    }

    @NotNull
    public SpigotPlayer getPlayer(Player player) {
        if (this.players.get(player.getUniqueId()) != null) return this.players.get(player.getUniqueId());
        SpigotPlayer res = new SpigotPlayer(player);
        this.players.put(player.getUniqueId(), res);
        return res;
    }

    @Override
    @Nullable
    public IPlayer getPlayer(UUID uuid) {
        if (this.players.get(uuid) != null) return this.players.get(uuid);
        Player bukkitPlayer = Bukkit.getPlayer(uuid);
        if (bukkitPlayer == null) return null;
        SpigotPlayer res = new SpigotPlayer(bukkitPlayer);
        this.players.put(uuid, res);
        return res;
    }

    @Override
    @Nullable
    public IPlayer getPlayer(String name) {
        Player bukkitPlayer = Bukkit.getPlayer(name);
        if (bukkitPlayer == null) return null;
        SpigotPlayer res = new SpigotPlayer(bukkitPlayer);
        this.players.put(bukkitPlayer.getUniqueId(), res);
        return res;
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        return getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public VoxelMaterial getMaterial(String namespace, String key) {
        Material mat = namespace.equals("minecraft") ?
                Material.getMaterial(key.toUpperCase()) :
                Material.getMaterial(namespace.toUpperCase() + "_" + key.toUpperCase()); // In case of arclight, which uses the syntax "<NAMESPACE>_<KEY>"
        if (mat != null) return new SpigotMaterial(mat);
        return null;
    }

    @Override
    public List<VoxelMaterial> getMaterials() {
        return Arrays.stream(Material.values()).map(SpigotMaterial::new).collect(Collectors.toList());
    }

    @Nullable
    @Override
    public VoxelBiome getBiome(String namespace, String key) {
        return Registry.BIOME.get(new NamespacedKey(namespace, key)) != null ? new VoxelBiome(namespace, key) : null;
    }

    @Override
    public List<VoxelBiome> getBiomes() {
        var biomes = new ArrayList<VoxelBiome>();
        Registry.BIOME.forEach(biome -> biomes.add(new VoxelBiome(biome.getKey().getNamespace(), biome.getKey().getKey())));
        return biomes;
    }

    @Nullable
    @Override
    public VoxelEntityType getEntityType(String namespace, String key) {
        return Registry.ENTITY_TYPE.get(new NamespacedKey(namespace, key)) != null ? new VoxelEntityType(namespace, key) : null;
    }

    @Override
    public List<VoxelEntityType> getEntityTypes() {
        var entityTypes = new ArrayList<VoxelEntityType>();
        Registry.ENTITY_TYPE.forEach(entityType -> entityTypes.add(new VoxelEntityType(entityType.getKey().getNamespace(), entityType.getKey().getKey())));
        return entityTypes;
    }

    @Nullable
    @Override
    public VoxelTreeType getTreeType(String namespace, String key) {
        try {
            var treeType = namespace.equals("minecraft") ?
                    TreeType.valueOf(key.toUpperCase()) :
                    TreeType.valueOf(namespace.toUpperCase() + "_" + key.toUpperCase()); // In case of arclight, which uses the syntax "<NAMESPACE>_<KEY>"
            //it'll be either non null or throw, so we know that the namespace + key are valid
            return new VoxelTreeType(namespace, key);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    @Override
    public VoxelTreeType getDefaultTreeType() {
        return new VoxelTreeType("minecraft", "TREE");
    }

    @Override
    public List<VoxelTreeType> getTreeTypes() {
        return Arrays.stream(TreeType.values()).map(treeType -> new VoxelTreeType("minecraft", treeType.name())).collect(Collectors.toList());
    }

    @Override
    public Environment getEnvironment() {
        return Environment.SPIGOT;
    }

    /**
     * Returns object for accessing global VoxelSniper options.
     *
     * @return {@link VoxelSniperConfiguration} object for accessing global VoxelSniper options.
     */
    @Override
    public VoxelSniperConfiguration getVoxelSniperConfiguration() {
        return voxelSniperConfiguration;
    }

    @Override
    public IFileHandler getFileHandler() {
        return fileHandler;
    }

    @NotNull
    public IWorld getWorld(@NotNull World world) {
        if (this.worlds.get(world.getName()) != null) return this.worlds.get(world.getName());
        SpigotWorld res = new SpigotWorld(world);
        this.worlds.put(world.getName(), res);
        return res;
    }
}
