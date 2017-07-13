package com.xaosia.bungeepex.platform.bungee.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.xaosia.bungeepex.platform.bungee.utils.BungeeSender;
import com.xaosia.bungeepex.platform.bungee.utils.NetworkType;
import lombok.Getter;
import com.xaosia.bungeepex.platform.bungee.BungeePlugin;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.bungee.BungeeConfig;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.utils.Lang;
import com.xaosia.bungeepex.PermissionsManager;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.platform.EventListener;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeEventListener implements Listener, EventListener
{

    @Getter
    private final Map<String, String> playerWorlds = new HashMap<>();

    private boolean enabled = false;

    private final BungeeConfig config;

    public BungeeEventListener(BungeeConfig config)
    {
        this.config = config;
    }

    @Override
    public void enable()
    {
        if (enabled)
        {
            return;
        }
        enabled = true;
        ProxyServer.getInstance().getPluginManager().registerListener(BungeePlugin.getInstance(), this);
    }

    @Override
    public void disable()
    {
        if (!enabled)
        {
            return;
        }
        enabled = false;
        ProxyServer.getInstance().getPluginManager().unregisterListener(this);
    }

    @EventHandler(priority = Byte.MIN_VALUE + 1)
    public void onLogin(LoginEvent e)
    {
        //don't load if cancelled
        if (e.isCancelled())
        {
            return;
        }

        String playername = e.getConnection().getName();
        UUID uuid = null;
        if (config.isUseUUIDs())
        {
            uuid = e.getConnection().getUniqueId();
            BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.LOGIN_UUID, playername, uuid));

            //update uuid player db
            pm().getBackEnd().update(uuid, playername);
        }
        else
        {
            BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.LOGIN, playername));
        }

        //remove user from cache if present
        PermissionUser oldu = config.isUseUUIDs() ? pm().getUser(uuid, false) : pm().getUser(playername, false);
        if (oldu != null)
        {
            pm().removeUserFromCache(oldu);
        }

        //load user from db
        PermissionUser u = config.isUseUUIDs() ? pm().getUser(uuid) : pm().getUser(playername);
        if (u == null)
        {
            //create user and add default groups
            if (config.isUseUUIDs())
            {
                BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.ADDING_DEFAULT_GROUPS_UUID, playername, uuid));
            }
            else
            {
                BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.ADDING_DEFAULT_GROUPS, playername));
            }

            u = pm().createTempUser(playername, uuid);
            pm().getBackEnd().saveUser(u, true);
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void onDisconnect(PlayerDisconnectEvent e)
    {
        String playername = e.getPlayer().getName();
        UUID uuid = e.getPlayer().getUniqueId();

        PermissionUser u = config.isUseUUIDs() ? pm().getUser(uuid) : pm().getUser(playername);
        pm().removeUserFromCache(u);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void onPermissionCheck(PermissionCheckEvent e)
    {
        CommandSender s = e.getSender();
        e.setHasPermission(BungeePEX.getInstance().getPermissionsChecker().hasPermOrConsoleOnServerInWorld(new BungeeSender(s), e.getPermission()));
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void onTabcomplete(TabCompleteEvent e)
    {
        if (!config.isTabComplete())
        {
            return;
        }
        if (e.getSuggestions().isEmpty())
        {
            for (ProxiedPlayer pp : ProxyServer.getInstance().getPlayers())
            {
                if (Statics.toLower(pp.getName()).startsWith(Statics.toLower(e.getCursor())))
                {
                    e.getSuggestions().add(pp.getName());
                }
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void onServerConnected(final ServerConnectedEvent e)
    {
        //plugin messages will arrive later because plugin channels are not registered at this very moment
        playerWorlds.put(e.getPlayer().getName(), null);

        //send delayed uuid message to bukkit
        if (config.isUseUUIDs())
        {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    BungeePlugin.getInstance().getNotifier().sendUUIDAndPlayer(e.getPlayer().getName(), e.getPlayer().getUniqueId());
                }
            };
            ProxyServer.getInstance().getScheduler().schedule(BungeePlugin.getInstance(), r, 1, TimeUnit.SECONDS);
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void onMessage(PluginMessageEvent e)
    {
        if (!e.getTag().equalsIgnoreCase(BungeePEX.CHANNEL))
        {
            return;
        }

        if (!(e.getReceiver() instanceof ProxiedPlayer))
        {
            //lock out silly hackers
            BungeePEX.getLogger().severe(Lang.translate(Lang.MessageType.INTRUSION_DETECTED, e.getSender()));
            e.setCancelled(true);
            return;
        }

        net.md_5.bungee.api.connection.Server scon = (net.md_5.bungee.api.connection.Server) e.getSender();

        //check network type // ignore if standalone or not registered server
        if (config.getNetworkType() == NetworkType.Standalone)
        {
            BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_STANDALONE, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_STANDALONE, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log("sender = " + scon.getInfo().getName());
            BungeePEX.getInstance().getDebug().log("msg = " + new String(e.getData()));
            return;
        }
        if (config.getNetworkType() == NetworkType.ServerDependend && !config.getNetworkServers().contains(scon.getInfo().getName()))
        {
            BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_SERVERDEPENDEND, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_SERVERDEPENDEND, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log("sender = " + scon.getInfo().getName());
            BungeePEX.getInstance().getDebug().log("msg = " + new String(e.getData()));
            return;
        }
        if (config.getNetworkType() == NetworkType.ServerDependendBlacklist && config.getNetworkServers().contains(scon.getInfo().getName()))
        {
            BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_SERVERDEPENDENDBLACKLIST, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_SERVERDEPENDENDBLACKLIST, scon.getInfo().getName()));
            BungeePEX.getInstance().getDebug().log("sender = " + scon.getInfo().getName());
            BungeePEX.getInstance().getDebug().log("msg = " + new String(e.getData()));
            return;
        }

        //process message
        String msg = new String(e.getData());
        if (config.isDebug())
        {
            BungeePEX.getLogger().info("msg=" + msg);
        }
        List<String> data = Statics.toList(msg, ";");

        String cmd = data.get(0);
        String userorgroup = data.size() > 1 ? data.get(1) : null;

        if (cmd.equalsIgnoreCase("playerworldupdate"))
        {
            String world = data.get(2);

            playerWorlds.put(userorgroup, world);
        }
        else if (cmd.equalsIgnoreCase("deleteuser"))
        {
            PermissionUser u = pm().getUser(userorgroup);
            pm().removeUserFromCache(u);

            //forward plugin message to network
            BungeePEX.getInstance().getNetworkNotifier().deleteUser(u, scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("deletegroup"))
        {
            PermissionGroup g = pm().getGroup(userorgroup);
            pm().removeGroupFromCache(g);
            for (PermissionGroup gr : pm().getGroups())
            {
                gr.recalcPerms();
            }
            for (PermissionUser u : pm().getUsers())
            {
                u.recalcPerms();
            }

            //forward plugin message to network
            BungeePEX.getInstance().getNetworkNotifier().deleteGroup(g, scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("reloaduser"))
        {
            pm().reloadUser(userorgroup);

            //forward plugin message to network
            PermissionUser u = pm().getUser(userorgroup);
            if (u == null)
            {
                return;
            }
            BungeePEX.getInstance().getNetworkNotifier().reloadUser(u, scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("reloadgroup"))
        {
            pm().reloadGroup(userorgroup);

            //forward plugin message to network
            PermissionGroup g = pm().getGroup(userorgroup);
            if (g == null)
            {
                return;
            }
            BungeePEX.getInstance().getNetworkNotifier().reloadGroup(g, scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("reloadusers"))
        {
            pm().reloadUsers();

            //forward plugin message to network
            BungeePEX.getInstance().getNetworkNotifier().reloadUsers(scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("reloadgroups"))
        {
            pm().reloadGroups();

            //forward plugin message to network
            BungeePEX.getInstance().getNetworkNotifier().reloadGroups(scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("reloadall"))
        {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    BungeePEX.getInstance().reload(false);
                }
            };
            ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.getInstance(), r);

            //forward plugin message to network except to server which issued the reload
            BungeePEX.getInstance().getNetworkNotifier().reloadAll(scon.getInfo().getName());
        }
        else if (cmd.equalsIgnoreCase("configcheck"))
        {
            String servername = data.get(1);
            BackEndType backend = BackEndType.getByName(data.get(2));
            //UUIDPlayerDBType uuidplayerdb = UUIDPlayerDBType.getByName(data.get(3));
            boolean useuuid = Boolean.parseBoolean(data.get(4));
            if (!scon.getInfo().getName().equals(servername))
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_SERVERNAME, scon.getInfo().getName()));
            }
            if (config.getBackEndType() != backend)
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_BACKEND, scon.getInfo().getName()));
            }
            /*if (config.getUUIDPlayerDBType() != uuidplayerdb)
            {
                BungeePerms.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_UUIDPLAYERDB, scon.getInfo().getName()));
            }*/
            if (config.isUseUUIDs() != useuuid)
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEE_USEUUID, scon.getInfo().getName()));
            }
        }

        e.setCancelled(true);
    }

    private PermissionsManager pm()
    {
        return BungeePEX.getInstance().getPermissionsManager();
    }
}
