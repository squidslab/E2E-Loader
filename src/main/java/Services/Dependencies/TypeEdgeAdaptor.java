package Services.Dependencies;

import Entity.*;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Custom Gson deserializer for {@link Edge} objects.
 *
 * <p>This class allows polymorphic deserialization of {@link Edge} subclasses
 * based on the "type" field present in the JSON representation. Supported types
 * include:
 * <ul>
 *     <li>"bodyjson" → {@link EdgeBodyJSON}</li>
 *     <li>"bodyue" → {@link EdgeBodyUE}</li>
 *     <li>"cookie" → {@link EdgeCookie}</li>
 *     <li>"header" → {@link EdgeHeader}</li>
 *     <li>"queryparam" → {@link EdgeQueryParam}</li>
 *     <li>"url" → {@link EdgeUrl}</li>
 * </ul>
 *
 * <p>If an unknown type is encountered, a {@link JsonParseException} is thrown.
 */
public class TypeEdgeAdaptor implements JsonDeserializer<Edge> {
    /**
     * Deserializes a JSON element into the appropriate {@link Edge} subclass
     * based on its "type" field.
     *
     * @param jsonElement the JSON element to deserialize
     * @param type the type of the object to deserialize to (should be {@link Edge})
     * @param jsonDeserializationContext the Gson context for deserialization
     * @return the deserialized {@link Edge} subclass instance
     * @throws JsonParseException if the "type" field is missing or unknown
     */
    @Override
    public Edge deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String type_edge = jsonObject.get("type").getAsString();
        switch (type_edge){
            case "bodyjson":
                return jsonDeserializationContext.deserialize(jsonElement, EdgeBodyJSON.class);
            case "bodyue":
                return jsonDeserializationContext.deserialize(jsonElement, EdgeBodyUE.class);
            case "cookie":
                return jsonDeserializationContext.deserialize(jsonElement, EdgeCookie.class);
            case "header":
                return jsonDeserializationContext.deserialize(jsonElement, EdgeHeader.class);
            case "queryparam":
                return jsonDeserializationContext.deserialize(jsonElement, EdgeQueryParam.class);
            case "url":
                return jsonDeserializationContext.deserialize(jsonElement,EdgeUrl.class);
            default:
                throw new JsonParseException("Unknown type: " + type);
        }
    }
}
