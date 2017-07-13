package com.xaosia.bungeepex.platform.bungee.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xaosia.bungeepex.PermissionUser;
import net.md_5.bungee.api.plugin.Event;

@AllArgsConstructor
public class BungeePermsUserChangedEvent extends Event
{
    @Getter
    private final PermissionUser user;
}
