package com.xaosia.bungeepex.backends.migrate;

import java.util.List;
import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.backends.BackEnd;
import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.backends.YAMLBackEnd;

public class Migrate2YAML implements Migrator
{

    private final PEXConfig config;

    public Migrate2YAML(PEXConfig config)
    {
        this.config = config;
    }

    @Override
    public void migrate(final List<PermissionGroup> groups, final List<PermissionUser> users, final int permsversion)
    {
        BackEnd be = new YAMLBackEnd();
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

        config.setBackendType(BackEndType.YAML);

        BungeePEX.getInstance().getPermissionsManager().setBackEnd(be);
    }
}
