package com.xaosia.bungeepex.platform.bukkit;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.xaosia.bungeepex.platform.bukkit.events.BukkitEventDispatcher;
import com.xaosia.bungeepex.platform.bukkit.listeners.BukkitEventListener;
import com.xaosia.bungeepex.platform.bukkit.processors.SuperPermsPreProcessor;
import com.xaosia.bungeepex.platform.bukkit.utils.BukkitMessageEncoder;
import com.xaosia.bungeepex.platform.bukkit.utils.BukkitNotifier;
import com.xaosia.bungeepex.platform.bukkit.utils.BukkitPluginMessageSender;
import com.xaosia.bungeepex.platform.bukkit.utils.BukkitSender;
import com.xaosia.bungeepex.utils.Config;
import lombok.Getter;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.utils.Color;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.platform.MessageEncoder;
import com.xaosia.bungeepex.platform.bukkit.bridge.BridgeManager;
import com.xaosia.bungeepex.platform.Sender;
import com.xaosia.bungeepex.platform.PlatformPlugin;
import com.xaosia.bungeepex.platform.PlatformType;
import com.xaosia.bungeepex.platform.PluginMessageSender;
import com.xaosia.bungeepex.platform.independend.GroupProcessor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class BukkitPlugin extends JavaPlugin implements PlatformPlugin {

    private static final double MILLI2TICK = 20F / 1000;

    @Getter
    private static BukkitPlugin instance;

    private BukkitConfig conf;

    //platform dependend parts
    private BukkitEventListener listener;
    private BukkitEventDispatcher dispatcher;
    private BukkitNotifier notifier;
    private PluginMessageSender pmsender;

    private BungeePEX bungeepex;

    private final PlatformType platformType = PlatformType.Bukkit;

    //platform extra things
    @Getter
    private BridgeManager bridge;

    @Override
    public void onLoad()
    {
        //static
        instance = this;

        //load config
        Config config = new Config(this, "/config.yml");
        config.load();
        conf = new BukkitConfig(config);
        conf.load();

        //register commands
        loadCommands();

        listener = new BukkitEventListener(conf);
        dispatcher = new BukkitEventDispatcher();
        notifier = new BukkitNotifier(conf);
        pmsender = new BukkitPluginMessageSender();

        bungeepex = new BungeePEX(this, conf, pmsender, notifier, listener, dispatcher);
        bungeepex.load();

        //extra part
        bridge = new BridgeManager();
        bridge.load();
        bungeepex.getPermissionsResolver().registerProcessor(new GroupProcessor());
        bungeepex.getPermissionsResolver().registerProcessor(new SuperPermsPreProcessor());
    }

    @Override
    public void onEnable()
    {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, BungeePEX.CHANNEL, listener);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, BungeePEX.CHANNEL);
        bungeepex.enable();
        bridge.enable();
    }

    @Override
    public void onDisable()
    {
        bridge.disable();
        bungeepex.disable();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, BungeePEX.CHANNEL, listener);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, BungeePEX.CHANNEL);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        return bungeepex.getCommandHandler().onCommand(new BukkitSender(sender), cmd.getName(), label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
    {
        List<String> l = new ArrayList<>();
        if (!conf.isTabComplete() || args.length == 0)
        {
            return l;
        }

        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (Statics.toLower(p.getName()).startsWith(Statics.toLower(args[args.length - 1])))
            {
                l.add(p.getName());
            }
        }

        return l;
    }

    private void loadCommands()
    {
        Command command = new Command("bungeepex")
        {
            @Override
            public boolean execute(final CommandSender sender, final String alias, final String[] args)
            {
                final Command cmd = this;
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (!BukkitPlugin.this.onCommand(sender, cmd, alias, args))
                        {
                            sender.sendMessage(Color.Error + "[BungeePEX] Command not found");
                        }
                    }
                };
                if (conf.isAsyncCommands())
                {
                    Bukkit.getScheduler().runTaskAsynchronously(instance, r);
                }
                else
                {
                    r.run();
                }
                return true;
            }
        };

        command.setAliases(Arrays.asList("bpex"));
        command.setPermission(null);

        getCommandMap().register("bungeepex", command);

    }

    private CommandMap getCommandMap()
    {
        try
        {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(Bukkit.getPluginManager());
        }
        catch (Exception ex)
        {
        }
        return null;
    }

    //plugin info
    @Override
    public String getPluginName()
    {
        return this.getDescription().getName();
    }

    @Override
    public String getVersion()
    {
        return this.getDescription().getVersion();
    }

    @Override
    public String getAuthor()
    {
        return this.getDescription().getAuthors().get(0);
    }

    @Override
    public String getPluginFolderPath()
    {
        return this.getDataFolder().getAbsolutePath();
    }

    @Override
    public File getPluginFolder()
    {
        return this.getDataFolder();
    }

    @Override
    public Sender getPlayer(String name)
    {
        CommandSender sender = Bukkit.getPlayer(name);

        Sender s = null;

        if (sender != null)
        {
            s = new BukkitSender(sender);
        }

        return s;
    }

    @Override
    public Sender getPlayer(UUID uuid)
    {
        CommandSender sender = Bukkit.getPlayer(uuid);

        Sender s = null;

        if (sender != null)
        {
            s = new BukkitSender(sender);
        }

        return s;
    }

    @Override
    public Sender getConsole()
    {
        return new BukkitSender(Bukkit.getConsoleSender());
    }

    @Override
    public List<Sender> getPlayers()
    {
        List<Sender> senders = new ArrayList<>();

        for (Player pp : Bukkit.getOnlinePlayers())
        {
            senders.add(new BukkitSender(pp));
        }

        return senders;
    }

    @Override
    public boolean isChatApiPresent()
    {
        try
        {
            Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    @Override
    public MessageEncoder newMessageEncoder()
    {
        return new BukkitMessageEncoder("");
    }

    @Override
    public int registerRepeatingTask(Runnable r, long delay, long interval)
    {
        return getServer().getScheduler().runTaskTimer(this, r, (long) (delay * MILLI2TICK), (long) (interval * MILLI2TICK)).getTaskId();
    }

    @Override
    public void cancelTask(int id)
    {
        getServer().getScheduler().cancelTask(id);
    }

    @Override
    public void doAsync(Runnable r) {
        getServer().getScheduler().runTaskAsynchronously(this, r);
    }

}
