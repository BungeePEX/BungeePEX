package com.xaosia.bungeepex.platform.bukkit.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;
import com.xaosia.bungeepex.platform.bukkit.BukkitPrivilege;
import com.xaosia.bungeepex.platform.bukkit.utils.Injector;
import lombok.Getter;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.bukkit.BukkitConfig;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.utils.Lang;
import com.xaosia.bungeepex.PermissionsManager;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.platform.EventListener;
import com.xaosia.bungeepex.platform.Sender;
import com.xaosia.bungeepex.platform.bukkit.events.BungeePermsUserChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BukkitEventListener implements Listener, EventListener, PluginMessageListener
{

    @Getter
    private final Map<String, String> playerWorlds = new HashMap<>();

    private boolean enabled = false;

    private final BukkitConfig config;

    public BukkitEventListener(BukkitConfig config)
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
        Bukkit.getPluginManager().registerEvents(this, BukkitPlugin.getInstance());

        //inject into console // seems to be best place here
        BukkitPrivilege permissible = new BukkitPrivilege(Bukkit.getConsoleSender(), null, Injector.getPermissible(Bukkit.getConsoleSender()));
        permissible.inject();

        //uninject from players
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (!(Injector.getPermissible(p) instanceof BukkitPrivilege))
            {
                PermissionUser u = config.isUseUUIDs() ? pm().getUser(p.getUniqueId()) : pm().getUser(p.getName());
                BukkitPrivilege perm = new BukkitPrivilege(p, u, Injector.getPermissible(p));
                perm.inject();
            }
            p.recalculatePermissions();
        }
    }

    @Override
    public void disable()
    {
        if (!enabled)
        {
            return;
        }
        enabled = false;
        Statics.unregisterListener(this);

        //uninject from console // seems to be best place here
        Injector.uninject(Bukkit.getConsoleSender());

        //uninject from players
        for (Player p : Bukkit.getOnlinePlayers())
        {
            Injector.uninject(p);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent e)
    {
        String playername = e.getPlayer().getName();
        UUID uuid = null;

        if (config.isUseUUIDs())
        {
            uuid = e.getPlayer().getUniqueId();
            BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.LOGIN_UUID, playername, uuid));

            //update uuid player db
            pm().getBackEnd().update(uuid, playername);
        }
        else
        {
            BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.LOGIN, e.getPlayer().getName()));
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

        BukkitPlugin.getInstance().getNotifier().sendWorldUpdate(e.getPlayer());

        //inject permissible
        BukkitPrivilege permissible = new BukkitPrivilege(e.getPlayer(), u, Injector.getPermissible(e.getPlayer()));
        permissible.inject();

        updateAttachment(e.getPlayer(), u);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e)
    {
        BukkitPlugin.getInstance().getNotifier().sendWorldUpdate(e.getPlayer());

        PermissionUser u = config.isUseUUIDs() ? pm().getUser(e.getPlayer().getUniqueId()) : pm().getUser(e.getPlayer().getName());
        updateAttachment(e.getPlayer(), u);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e)
    {
        //uninject permissible
        Injector.uninject(e.getPlayer());

        PermissionUser u = config.isUseUUIDs() ? pm().getUser(e.getPlayer().getUniqueId()) : pm().getUser(e.getPlayer().getName());
        pm().removeUserFromCache(u);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangedWorld(PlayerChangedWorldEvent e)
    {
        BukkitPlugin.getInstance().getNotifier().sendWorldUpdate(e.getPlayer());

        PermissionUser u = config.isUseUUIDs() ? pm().getUser(e.getPlayer().getUniqueId()) : pm().getUser(e.getPlayer().getName());
        updateAttachment(e.getPlayer(), u);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUserUpdate(BungeePermsUserChangedEvent e)
    {
        Player p = config.isUseUUIDs() ? Bukkit.getPlayer(e.getUser().getUUID()) : Bukkit.getPlayer(e.getUser().getName());
        if (p == null)
        {
            return;
        }
        updateAttachment(p, e.getUser());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPluginChannelRegister(PlayerRegisterChannelEvent e)
    {
        if (!e.getChannel().equals(BungeePEX.CHANNEL))
        {
            return;
        }

        BukkitPlugin.getInstance().getNotifier().sendWorldUpdate(e.getPlayer());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes)
    {
        if (config.isStandalone())
        {
            BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_STANDALONE));
            BungeePEX.getInstance().getDebug().log(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_STANDALONE));
            BungeePEX.getInstance().getDebug().log("sender = BungeeCord");
            BungeePEX.getInstance().getDebug().log("msg = " + new String(bytes));
            return;
        }

        String msg = new String(bytes);
        if (config.isDebug())
        {
            BungeePEX.getLogger().info("msg=" + msg);
        }
        List<String> data = Statics.toList(msg, ";");

        String cmd = data.get(0);
        String userorgroup = data.size() > 1 ? data.get(1) : null;

        if (cmd.equalsIgnoreCase("deleteuser"))
        {
            PermissionUser u = pm().getUser(userorgroup);
            pm().removeUserFromCache(u);
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
        }
        else if (cmd.equalsIgnoreCase("reloaduser"))
        {
            pm().reloadUser(userorgroup);
        }
        else if (cmd.equalsIgnoreCase("reloadgroup"))
        {
            pm().reloadGroup(userorgroup);
        }
        else if (cmd.equalsIgnoreCase("reloadusers"))
        {
            pm().reloadUsers();
        }
        else if (cmd.equalsIgnoreCase("reloadgroups"))
        {
            pm().reloadGroups();
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
            Bukkit.getScheduler().runTaskLater(BukkitPlugin.getInstance(), r, 1);
        }
        else if (cmd.equalsIgnoreCase("configcheck"))
        {
            String servername = data.get(1);
            BackEndType backend = BackEndType.getByName(data.get(2));
            //UUIDPlayerDBType uuidplayerdb = UUIDPlayerDBType.getByName(data.get(3));
            boolean useuuid = Boolean.parseBoolean(data.get(4));
            if (!config.getServername().equals(servername))
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_SERVERNAME));
            }
            if (config.getBackEndType() != backend)
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_BACKEND));
            }
            /*if (config.getUUIDPlayerDBType() != uuidplayerdb)
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_UUIDPLAYERDB));
            }*/
            if (config.isUseUUIDs() != useuuid)
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUKKIT_USEUUID));
            }
        }
        else if (cmd.equalsIgnoreCase("uuidcheck"))
        {
            if (!config.isUseUUIDs())
            {
                return;
            }
            String uuid = data.get(2);
            Sender p = BukkitPlugin.getInstance().getPlayer(userorgroup);
            if (p != null && !p.getUUID().equals(UUID.fromString(uuid)))
            {
                BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_BUNGEECORD_BUKKIT_CONFIG));
            }
        }
    }

    private PermissionsManager pm()
    {
        return BungeePEX.getInstance().getPermissionsManager();
    }

    private void updateAttachment(Player p, PermissionUser u)
    {
        Permissible base = Injector.getPermissible(p);
        if (!(base instanceof BukkitPrivilege))
        {
            return;
        }

        BukkitPrivilege perm = (BukkitPrivilege) base;
        perm.updateAttachment(u, ((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername(), p.getWorld() == null ? null : p.getWorld().getName());
    }
}
