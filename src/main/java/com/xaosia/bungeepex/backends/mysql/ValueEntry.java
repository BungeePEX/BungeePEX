package com.xaosia.bungeepex.backends.mysql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ValueEntry
{

    private String value;
    private String server;
    private String world;
}
