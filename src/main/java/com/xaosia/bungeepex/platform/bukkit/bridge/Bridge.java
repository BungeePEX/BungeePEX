package com.xaosia.bungeepex.platform.bukkit.bridge;

import org.bukkit.event.Listener;

public interface Bridge extends Listener
{

    public void enable();

    public void disable();
}
