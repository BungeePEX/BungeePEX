package com.xaosia.bungeepex.platform.bukkit.commands;

import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.commands.CommandHandler;
import com.xaosia.bungeepex.PermissionsChecker;
import com.xaosia.bungeepex.platform.PlatformPlugin;
import com.xaosia.bungeepex.platform.Sender;
import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;

public class BukkitCommandHandler extends CommandHandler
{

    public BukkitCommandHandler(PlatformPlugin plugin, PermissionsChecker checker, PEXConfig config)
    {
        super(plugin, checker, config);
    }

    @Override
    public boolean onCommand(Sender sender, String cmd, String label, String[] args)
    {
        boolean b = super.onCommand(sender, cmd, label, args);
        if(b)
        {
            return b;
        }
        return BukkitPlugin.getInstance().getBridge().onCommand(sender, cmd, label, args);
    }
}
