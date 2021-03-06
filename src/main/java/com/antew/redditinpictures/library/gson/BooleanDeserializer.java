/*
 * Copyright (C) 2014 Antew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antew.redditinpictures.library.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

/**
 * This is used to deserialize booleans from Flickr because they come across and either 1 or 0
 *
 * @author Antew
 */
public class BooleanDeserializer implements JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        return element.getAsInt() == 1 ? true : false;
    }
}
