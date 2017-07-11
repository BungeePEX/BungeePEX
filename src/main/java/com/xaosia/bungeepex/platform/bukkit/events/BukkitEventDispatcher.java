package com.xaosia.bungeepex.platform.bukkit.events;

import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;
import lombok.SneakyThrows;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.EventDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

public class BukkitEventDispatcher implements EventDispatcher
{

    @Override
    public void dispatchReloadedEvent()
    {
        callSyncEvent(BukkitPlugin.getInstance(), new BungeePermsReloadedEvent());
    }

    @Override
    public void dispatchGroupChangeEvent(PermissionGroup g)
    {
        callSyncEvent(BukkitPlugin.getInstance(), new BungeePermsGroupChangedEvent(g));
    }

    @Override
    public void dispatchUserChangeEvent(PermissionUser u)
    {
        callSyncEvent(BukkitPlugin.getInstance(), new BungeePermsUserChangedEvent(u));
    }
    
    @SneakyThrows
    private static void runSync(Plugin p, Runnable r, boolean waitfinished)
    {
        if (Bukkit.isPrimaryThread())
        {
            r.run();
        }
        else
        {
            int id = Bukkit.getScheduler().runTask(p, r).getTaskId();
            if (waitfinished)
            {
                while (Bukkit.getScheduler().isCurrentlyRunning(id) || Bukkit.getScheduler().isQueued(id))
                {
                    Thread.sleep(1);
                }
            }
        }
    }
    
    private static void callSyncEvent(Plugin p, final Event e)
    {
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                Bukkit.getPluginManager().callEvent(e);
            }
        };
        runSync(p, r, true);
    }
}
