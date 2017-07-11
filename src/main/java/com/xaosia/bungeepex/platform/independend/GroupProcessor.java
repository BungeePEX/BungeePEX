package com.xaosia.bungeepex.platform.independend;

import java.util.List;
import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.Privilege;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionsManager;
import com.xaosia.bungeepex.processors.PermissionsPreProcessor;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.Sender;

public class GroupProcessor implements PermissionsPreProcessor
{

    @Override
    public List<String> process(List<String> perms, Sender s)
    {
        if (s == null)
        {
            return perms;
        }
        PEXConfig config = BungeePEX.getInstance().getConfig();
        if (config.isGroupPermission())
        {
            PermissionsManager pm = BungeePEX.getInstance().getPermissionsManager();
            PermissionUser u = config.isUseUUIDs() ? pm.getUser(s.getUUID()) : pm.getUser(s.getName());
            if (u == null)
            {
                return perms;
            }

            for (PermissionGroup g : u.getGroups())
            {
                perms.add(0, "group." + Statics.toLower(g.getName()));
            }
        }

        return perms;
    }

    @Override
    public List<Privilege> processWithOrigin(List<Privilege> perms, Sender s)
    {
        if (s == null)
        {
            return perms;
        }
        PEXConfig config = BungeePEX.getInstance().getConfig();
        if (config.isGroupPermission())
        {
            PermissionsManager pm = BungeePEX.getInstance().getPermissionsManager();
            PermissionUser u = config.isUseUUIDs() ? pm.getUser(s.getUUID()) : pm.getUser(s.getName());
            if (u == null)
            {
                return perms;
            }

            for (PermissionGroup g : u.getGroups())
            {
                perms.add(0, new Privilege("group." + Statics.toLower(g.getName()), "GroupProcessor", true, null, null));
            }
        }

        return perms;
    }

}
