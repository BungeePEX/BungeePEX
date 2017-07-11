package com.xaosia.bungeepex.backends.migrate;

import java.util.List;
import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.utils.Debug;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.backends.BackEnd;
import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.backends.MySQLBackEnd;

public class Migrate2MySQL implements Migrator
{

    private final PEXConfig config;
    private final Debug debug;

    public Migrate2MySQL(PEXConfig config, Debug debug)
    {
        this.config = config;
        this.debug = debug;
    }

    @Override
    public void migrate(final List<PermissionGroup> groups, final List<PermissionUser> users, final int permsversion)
    {
        BackEnd be = new MySQLBackEnd(config);
        be.clearDatabase();
        for (PermissionGroup group : groups)
        {
            be.saveGroup(group, false);
        }
        for (PermissionUser user : users)
        {
            be.saveUser(user, false);
        }
        be.saveVersion(permsversion, true);

        config.setBackendType(BackEndType.MySQL);

        BungeePEX.getInstance().getPermissionsManager().setBackEnd(be);
    }
}
