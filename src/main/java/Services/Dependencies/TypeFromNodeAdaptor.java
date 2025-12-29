package Services.Dependencies;

import Entity.CSVNode;
import Entity.MyNode;
import com.google.gson.*;

import java.lang.reflect.Type;
/**
 * Custom Gson deserializer for {@link MyNode} objects.
 *
 * <p>This deserializer handles polymorphic deserialization based on the
 * presence of specific fields in the JSON. If the JSON object contains
 * the field "ignorefirstLine", it is deserialized as a {@link CSVNode};
 * otherwise, it is deserialized as a regular {@link MyNode}.
 *
 * <p>This is useful for converting JSON representations of nodes in a
 * dependency graph where some nodes may represent CSV data.
 */
public class TypeFromNodeAdaptor implements JsonDeserializer<MyNode> {
    /**
     * Deserializes a JSON element into either a {@link MyNode} or {@link CSVNode}
     * depending on the presence of the "ignorefirstLine" field.
     *
     * @param jsonElement the JSON element to deserialize
     * @param type the type of the object to deserialize to (should be {@link MyNode})
     * @param jsonDeserializationContext the Gson context for deserialization
     * @return the deserialized {@link MyNode} or {@link CSVNode} instance
     * @throws JsonParseException if deserialization fails
     */
    @Override
    public MyNode deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if(jsonObject.has("ignorefirstLine")){
            return  jsonDeserializationContext.deserialize(jsonElement, CSVNode.class);
        }else{
            return  deserializeNode(jsonObject);
        }
    }
    /**
     * Deserializes a JSON object into a standard {@link MyNode}.
     *
     * @param jsonObject the JSON object representing a node
     * @return the deserialized {@link MyNode} instance
     */
    private MyNode deserializeNode(JsonObject jsonObject) {
        Gson gson = new Gson();
        return gson.fromJson(jsonObject, MyNode.class);
    }
}
