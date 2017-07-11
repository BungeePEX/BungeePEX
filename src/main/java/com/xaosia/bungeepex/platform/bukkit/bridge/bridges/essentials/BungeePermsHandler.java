package com.xaosia.bungeepex.platform.bukkit.bridge.bridges.essentials;

import com.earth2me.essentials.perm.IPermissionsHandler;
import java.util.ArrayList;
import java.util.List;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.bukkit.BukkitConfig;
import com.xaosia.bungeepex.platform.bukkit.utils.BukkitSender;
import org.bukkit.entity.Player;

class BungeePermsHandler implements IPermissionsHandler
{

    private final BungeePEX perms;

    public BungeePermsHandler()
    {
        perms = BungeePEX.getInstance();
    }

    @Override
    public String getGroup(Player player)
    {
        PermissionUser u = perms.getPermissionsManager().getUser(player.getName());
        if (u == null)
        {
            return "";
        }
        PermissionGroup g = perms.getPermissionsManager().getMainGroup(u);
        return g == null ? "" : g.getName();
    }

    @Override
    public List<String> getGroups(Player player)
    {
        List<String> groups = new ArrayList<>();

        PermissionUser u = perms.getPermissionsManager().getUser(player.getName());
        if (u == null)
        {
            return groups;
        }

        for (PermissionGroup g : u.getGroups())
        {
            groups.add(g.getName());
        }

        return groups;
    }

    @Override
    public boolean canBuild(Player player, String group)
    {
        return true;
    }

    @Override
    public boolean inGroup(Player player, String group)
    {
        PermissionUser u = perms.getPermissionsManager().getUser(player.getName());
        if (u == null)
        {
            return false;
        }

        PermissionGroup g = perms.getPermissionsManager().getGroup(group);
        if (g == null)
        {
            return false;
        }

        return u.getGroups().contains(g);
    }

    @Override
    public boolean hasPermission(Player player, String node)
    {
        return perms.getPermissionsChecker().hasPermOrConsoleOnServerInWorld(new BukkitSender(player), Statics.toLower(node));
    }

    @Override
    public String getPrefix(Player player)
    {
        PermissionUser u = perms.getPermissionsManager().getUser(player.getName());
        if (u == null)
        {
            return "";
        }
        
        return u.buildPrefix(new BukkitSender(player));
    }

    @Override
    public String getSuffix(Player player)
    {
        PermissionUser u = perms.getPermissionsManager().getUser(player.getName());
        if (u == null)
        {
            return "";
        }
        
        return u.buildSuffix(new BukkitSender(player));
    }
}
