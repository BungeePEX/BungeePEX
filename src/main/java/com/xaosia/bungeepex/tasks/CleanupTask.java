package com.xaosia.bungeepex.tasks;


import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.PermissionsManager;

public class CleanupTask implements Runnable{

    @Override
    public void run()
    {
        BungeePEX bpex = BungeePEX.getInstance();
        PermissionsManager pm = bpex.getPermissionsManager();

        long threshold = bpex.getConfig().getCleanupThreshold() * 1000;

        pm.getUserlock().writeLock().lock();
        try
        {
            for(PermissionUser u : pm.getUsers())
            {
                if((bpex.getConfig().isUseUUIDs() ? bpex.getPlugin().getPlayer(u.getUUID()) : bpex.getPlugin().getPlayer(u.getName())) != null)
                {
                    continue;
                }
                if(u.getLastAccess() + threshold < System.currentTimeMillis())
                {
                    pm.removeUserFromCache(u);
                }
            }
        }
        finally
        {
            pm.getUserlock().writeLock().unlock();
        }
    }

}
