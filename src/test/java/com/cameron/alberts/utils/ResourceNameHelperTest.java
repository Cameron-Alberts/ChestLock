package com.cameron.alberts.utils;

import org.junit.Assert;
import org.junit.Test;

public class ResourceNameHelperTest {
    @Test
    public void getUnlocalizedNameWithNullString() {
        Assert.assertNull(ResourceNameHelper.getUnlocalizedName(null));
    }

    @Test
    public void getUnlocalizedNameWithNoMatch() {
        final String noMatchName = "no match";
        Assert.assertNull(ResourceNameHelper.getUnlocalizedName(noMatchName));
    }

    @Test
    public void getUnlocalizedNameMatch() {
        final String matchingName = "item.my_item_name";
        final String expectedName = "my_item_name";

        Assert.assertEquals(expectedName, ResourceNameHelper.getUnlocalizedName(matchingName));
    }
}
