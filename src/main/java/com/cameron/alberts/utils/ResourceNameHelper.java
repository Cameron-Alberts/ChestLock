package com.cameron.alberts.utils;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceNameHelper {
    // Unlocalized names such as those for items have the pattern item.item_name, we only
    // want the item_name from the String
    private static final Pattern UNLOCLAIZED_NAME_MATCHER = Pattern.compile(".*\\.(?<name>.*)");
    private static final String GROUP_NAME = "name";

    @Nullable
    public static String getUnlocalizedName(@Nullable final String name) {
        // Guard against null
        if (name == null) {
            return null;
        }

        Matcher matcher = UNLOCLAIZED_NAME_MATCHER.matcher(name);

        if (matcher.matches()) {
            return matcher.group(GROUP_NAME);
        }

        return null;
    }
}
