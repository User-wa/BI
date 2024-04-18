package com.yupi.springbootinit.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;

import java.io.IOException;

/**
 * @Author goblin
 * @Date 2024/4/4 17:18
 * @注释
 */
public class StringUtils {
    public static boolean isValidStrictly(String json) {
        if(org.apache.commons.lang3.StringUtils.isBlank(json)){
            return false;
        }
        TypeAdapter<JsonElement> strictAdapter = new Gson().getAdapter(JsonElement.class);
        try {
            strictAdapter.fromJson(json);
        } catch (JsonSyntaxException | IOException e) {
            return false;
        }
        return true;
    }
}
