package com.xaosia.bungeepex.platform.bukkit.bridge.bridges.vault;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;
import com.xaosia.bungeepex.platform.bukkit.bridge.Bridge;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public class VaultBridge implements Bridge
{

    @Override
    public void enable()
    {
        Bukkit.getPluginManager().registerEvents(this, BukkitPlugin.getInstance());
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (plugin != null)
        {
            inject(plugin);
        }
    }

    @Override
    public void disable()
    {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (plugin != null)
        {
            uninject(plugin);
        }

        PluginEnableEvent.getHandlerList().unregister(this);
        PluginDisableEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e)
    {
        if (!e.getPlugin().getName().equalsIgnoreCase("vault"))
        {
            return;
        }
        inject(e.getPlugin());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e)
    {
        if (!e.getPlugin().getName().equalsIgnoreCase("vault"))
        {
            return;
        }
        uninject(e.getPlugin());
    }

    public void inject(Plugin plugin)
    {
        BungeePEX.getLogger().info("Injection of BungeePEX into Vault");
        try
        {
            Vault v = (Vault) plugin;

            if (!v.isEnabled())
            {
                return;
            }

            //inject BungeePerms permissions
            Method m = v.getClass().getDeclaredMethod("hookPermission", String.class, Class.class, ServicePriority.class, String[].class);
            m.setAccessible(true);
            m.invoke(v, "BungeePEX", Permission_BungeePerms.class, ServicePriority.Normal, new String[]
             {
                 "com.xaosia.bungeepex.platform.bukkit.BukkitPlugin"
            });

            Field f = v.getClass().getDeclaredField("perms");
            f.setAccessible(true);
            f.set(v, Bukkit.getServicesManager().getRegistration(Permission.class).getProvider());

            //inject BungeePerms chat
            m = v.getClass().getDeclaredMethod("hookChat", String.class, Class.class, ServicePriority.class, String[].class);
            m.setAccessible(true);
            m.invoke(v, "BungeePEX", Chat_BungeePerms.class, ServicePriority.Normal, new String[]
             {
                 "com.xaosia.bungeepex.platform.bukkit.BukkitPlugin"
            });
        }
        catch (Exception ex)
        {
            BungeePEX.getInstance().getDebug().log(ex);
        }
    }

    public void uninject(Plugin plugin)
    {
        BungeePEX.getLogger().info("Uninjection of BungeePEX into Vault");

        try
        {
            Vault v = (Vault) plugin;

            if (!v.isEnabled())
            {
                return;
            }

            //uninject BungeePerms permissions
            Method m = v.getClass().getDeclaredMethod("loadChat");
            m.setAccessible(true);
            m.invoke(v);

            //inject BungeePerms chat
            m = v.getClass().getDeclaredMethod("loadPermission");
            m.setAccessible(true);
            m.invoke(v);
        }
        catch (Exception ex)
        {
            BungeePEX.getInstance().getDebug().log(ex);
        }
    }
}
