package com.xaosia.bungeepex.platform.bukkit.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xaosia.bungeepex.PermissionUser;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@AllArgsConstructor
public class BungeePermsUserChangedEvent extends Event
{

    public static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Getter
    private final PermissionUser user;

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

}
