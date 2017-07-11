package com.xaosia.bungeepex.backends.migrate;

import java.util.List;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;

public interface Migrator
{
    public void migrate(final List<PermissionGroup> groups, final List<PermissionUser> users, final int permsversion);
}
