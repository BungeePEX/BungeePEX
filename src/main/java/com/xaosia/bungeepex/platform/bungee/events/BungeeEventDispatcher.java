package com.xaosia.bungeepex.platform.bungee.events;

import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.EventDispatcher;
import net.md_5.bungee.api.ProxyServer;

public class BungeeEventDispatcher implements EventDispatcher
{

    @Override
    public void dispatchReloadedEvent()
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePermsReloadedEvent());
    }

    @Override
    public void dispatchGroupChangeEvent(PermissionGroup g)
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePermsGroupChangedEvent(g));
    }

    @Override
    public void dispatchUserChangeEvent(PermissionUser u)
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePermsUserChangedEvent(u));
    }

}
