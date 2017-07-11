package com.xaosia.bungeepex.backends.mysql;

public enum EntityType
{

    User(0),
    Group(1),
    Version(2);

    private final int code;

    private EntityType(int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }

    public static EntityType getByCode(int code)
    {
        for (EntityType et : values())
        {
            if (et.getCode() == code)
            {
                return et;
            }
        }
        return null;
    }
}
