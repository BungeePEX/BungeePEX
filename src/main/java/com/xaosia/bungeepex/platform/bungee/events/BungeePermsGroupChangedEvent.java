package com.xaosia.bungeepex.platform.bungee.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xaosia.bungeepex.PermissionGroup;
import net.md_5.bungee.api.plugin.Event;

@AllArgsConstructor
public class BungeePermsGroupChangedEvent extends Event
{

    @Getter
    private final PermissionGroup group;
}
