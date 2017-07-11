package com.xaosia.bungeepex.processors;

import java.util.List;

import com.xaosia.bungeepex.Privilege;
import com.xaosia.bungeepex.platform.Sender;

public interface PermissionsPreProcessor
{

    public List<String> process(List<String> perms, Sender s);

    public List<Privilege> processWithOrigin(List<Privilege> perms, Sender s);
}
