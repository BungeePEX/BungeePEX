package com.xaosia.bungeepex;

import java.util.logging.Logger;

import com.xaosia.bungeepex.commands.CommandHandler;
import com.xaosia.bungeepex.utils.Debug;
import com.xaosia.bungeepex.utils.FileExtractor;
import com.xaosia.bungeepex.utils.Lang;
import lombok.Getter;
import com.xaosia.bungeepex.platform.EventDispatcher;
import com.xaosia.bungeepex.platform.EventListener;
import com.xaosia.bungeepex.platform.NetworkNotifier;
import com.xaosia.bungeepex.platform.PlatformPlugin;
import com.xaosia.bungeepex.platform.PluginMessageSender;

@Getter
public class BungeePEX {

    public final static String CHANNEL = "bungeepex";

    @Getter
    private static BungeePEX instance;

    @Getter
    private static Logger logger = Logger.getLogger("bungeepex");

    private final PlatformPlugin plugin;
    private final PEXConfig config;
    private final Debug debug;

    private final PermissionsManager permissionsManager;
    private final CommandHandler commandHandler;
    private final PermissionsChecker permissionsChecker;
    private final PluginMessageSender pluginMessageSender;
    private final NetworkNotifier networkNotifier;
    private final EventListener eventListener;
    private final EventDispatcher eventDispatcher;
    private final PermissionsResolver permissionsResolver;
    private final CleanupTask cleanupTask;
    private int cleanupTaskId = -1;

    private boolean enabled;

    public BungeePEX(PlatformPlugin plugin, PEXConfig config, PluginMessageSender pluginMessageSender,
                     NetworkNotifier networkNotifier, EventListener eventListener, EventDispatcher eventDispatcher)
    {
        //static
        instance = this;
        logger = plugin.getLogger();

        //basic
        this.plugin = plugin;
        this.config = config;
        debug = new Debug(plugin, config.getConfig(), "BPEX");

        //extract packed files
        FileExtractor.extractAll();
        Lang.load(plugin.getPluginFolderPath() + "/lang/" + Statics.localeString(config.getLocale()) + ".yml"); //early load needed

        //adv
        permissionsManager = new PermissionsManager(plugin, config, debug);
        permissionsChecker = new PermissionsChecker();
        commandHandler = new CommandHandler(plugin, permissionsChecker, config);
        this.pluginMessageSender = pluginMessageSender;
        this.networkNotifier = networkNotifier;
        this.eventListener = eventListener;
        this.eventDispatcher = eventDispatcher;
        permissionsResolver = new PermissionsResolver();
        cleanupTask = new CleanupTask();
    }

    public void load()
    {
        Lang.load(plugin.getPluginFolderPath() + "/lang/" + Statics.localeString(config.getLocale()) + ".yml");
        permissionsResolver.setUseRegex(config.isUseRegexPerms());
    }

    public void enable()
    {
        if (enabled)
        {
            return;
        }
        enabled = true;

        logger.info("Activating BungeePEX...");
        permissionsManager.enable();
        eventListener.enable();
        cleanupTaskId = plugin.registerRepeatingTask(cleanupTask, 0, config.getCleanupInterval() * 1000);
    }

    public void disable()
    {
        if (!enabled)
        {
            return;
        }
        enabled = false;

        logger.info("Deactivating BungeePEX...");
        plugin.cancelTask(cleanupTaskId);
        cleanupTaskId = -1;
        eventListener.disable();
        permissionsManager.disable();
    }

    public void reload(boolean notifynetwork)
    {
        disable();
        load();
        permissionsManager.reload();
        if (notifynetwork)
        {
            networkNotifier.reloadAll("");
        }
        enable();
    }

}
