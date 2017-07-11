package com.xaosia.bungeepex.processors;

import com.xaosia.bungeepex.platform.Sender;

public interface PermissionsPostProcessor
{

    public Boolean process(String perm, Boolean result, Sender s);
}
