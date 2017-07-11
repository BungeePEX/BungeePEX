package com.xaosia.bungeepex.platform.bukkit.bridge.bridges.vault;

import java.util.ArrayList;
import java.util.List;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.bukkit.BukkitConfig;
import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public class Permission_BungeePerms extends Permission
{

    private final String name = "BungeePEX";

    private Plugin plugin = null;
    private BungeePEX perms;

    public Permission_BungeePerms(Plugin plugin)
    {
        super();
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(new PermissionServerListener(), BukkitPlugin.getInstance());

        // Load Plugin in case it was loaded before
        Plugin p = plugin.getServer().getPluginManager().getPlugin("BungeePEX");
        if (p != null)
        {
            this.perms = BungeePEX.getInstance();
            log.info(String.format("[%s][Permission] %s hooked.", plugin.getDescription().getName(), name));
        }
    }

    public class PermissionServerListener implements Listener
    {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginEnable(PluginEnableEvent event)
        {
            if (perms == null)
            {
                Plugin p = event.getPlugin();
                if (p.getDescription().getName().equals("BungeePEX"))
                {
                    perms = BungeePEX.getInstance();
                    log.info(String.format("[%s][Permission] %s hooked.", plugin.getDescription().getName(), name));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent event)
        {
            if (perms != null)
            {
                if (event.getPlugin().getDescription().getName().equals("BungeePEX"))
                {
                    perms = null;
                    log.info(String.format("[%s][Permission] %s un-hooked.", plugin.getDescription().getName(), name));
                }
            }
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isEnabled()
    {
        return perms != null && perms.isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat()
    {
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        return BungeePEX.getInstance().getPermissionsChecker().hasPermOnServerInWorld(player, permission, server, world);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return false;
        }

        if (world == null)
        {
            BungeePEX.getInstance().getPermissionsManager().addUserPerServerPerm(u, server, permission);
        }
        else
        {
            BungeePEX.getInstance().getPermissionsManager().addUserPerServerWorldPerm(u, server, world, permission);
        }

        return true;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return false;
        }

        if (world == null)
        {
            BungeePEX.getInstance().getPermissionsManager().removeUserPerServerPerm(u, server, permission);
        }
        else
        {
            BungeePEX.getInstance().getPermissionsManager().removeUserPerServerWorldPerm(u, server, world, permission);
        }

        return true;
    }

    @Override
    public boolean groupHas(String world, String group, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        return g.hasOnServerInWorld(permission, server, world);
    }

    @Override
    public boolean groupAdd(String world, String group, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        if (world == null)
        {
            BungeePEX.getInstance().getPermissionsManager().addGroupPerServerPerm(g, server, permission);
        }
        else
        {
            BungeePEX.getInstance().getPermissionsManager().addGroupPerServerWorldPerm(g, server, world, permission);
        }

        return true;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission)
    {
        String server = Statics.toLower(((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername());
        world = Statics.toLower(world);
        permission = Statics.toLower(permission);
        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        if (world == null)
        {
            BungeePEX.getInstance().getPermissionsManager().removeGroupPerServerPerm(g, server, permission);
        }
        else
        {
            BungeePEX.getInstance().getPermissionsManager().removeGroupPerServerWorldPerm(g, server, world, permission);
        }

        return true;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group)
    {
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return false;
        }

        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        return u.getGroups().contains(g);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group)
    {
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return false;
        }

        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        if (u.getGroups().contains(g))
        {
            return false;
        }

        BungeePEX.getInstance().getPermissionsManager().addUserGroup(u, g);
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group)
    {
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return false;
        }

        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        if (!u.getGroups().contains(g))
        {
            return false;
        }

        BungeePEX.getInstance().getPermissionsManager().removeUserGroup(u, g);
        return true;
    }

    @Override
    public String[] getPlayerGroups(String world, String player)
    {
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return new String[0];
        }

        return u.getGroupsString().toArray(new String[u.getGroupsString().size()]);
    }

    @Override
    public String getPrimaryGroup(String world, String player)
    {
        PermissionUser u = BungeePEX.getInstance().getPermissionsManager().getUser(player);
        if (u == null)
        {
            return null;
        }
        PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getMainGroup(u);
        return g != null ? g.getName() : null;
    }

    @Override
    public String[] getGroups()
    {
        List<String> groups = new ArrayList<>();
        for (PermissionGroup g : BungeePEX.getInstance().getPermissionsManager().getGroups())
        {
            groups.add(g.getName());
        }
        return groups.toArray(new String[groups.size()]);
    }

    @Override
    public boolean hasGroupSupport()
    {
        return true;
    }
}
