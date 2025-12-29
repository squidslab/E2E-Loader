package Services.ResponseAnalyzer;

import Entity.Header;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
/**
 * Analyzes HTTP responses and extracts structured and atomic objects from JSON responses
 * and response headers.
 *
 * <p>This class is responsible for converting JSON responses into a tree of {@link StructuredObject}
 * and {@link AtomicObject} instances. It also extracts relevant information from response headers
 * such as "Set-Cookie" headers.
 */
public class ResponseAnalyzer {

    String url;
    Integer num_req;
    /**
     * Constructs a ResponseAnalyzer for a specific URL and request number.
     *
     * @param url the URL of the HTTP request
     * @param num_req the index or number of the request
     */
    public ResponseAnalyzer (String url, Integer num_req)
    {
        this.url=url;
        this.num_req=num_req;
    }
    /**
     * Default constructor.
     */
    public ResponseAnalyzer(){}
    /**
     * Converts a JSON response string into a {@link ResponseUnstructured} object containing
     * {@link StructuredObject} and {@link AtomicObject} instances.
     *
     * @param json_response the JSON string to analyze
     * @return a ResponseUnstructured object containing the parsed structure
     */
    public ResponseUnstructured getUnstructuredResponse(String json_response) {
        ResponseUnstructured responseUnstructured = new ResponseUnstructured();
        StructuredObject structuredObject = new StructuredObject("All",json_response,"$");
        responseUnstructured.getObjects().add(structuredObject);
        if(json_response.startsWith("{")) {
            JsonObject jsonObject = new JsonParser().parse(json_response).getAsJsonObject();
            visitNode(jsonObject, "$",  responseUnstructured.getObjects());
        } else if(json_response.startsWith("[")) {
            JsonArray jsonArray = new JsonParser().parse(json_response).getAsJsonArray();
            visitNode(jsonArray,"$",responseUnstructured.getObjects());
        }
        return responseUnstructured;
    }
    /**
     * Recursively visits a JSON node and converts it into {@link StructuredObject} and {@link AtomicObject}.
     *
     * @param object the JSON node (can be {@link JsonObject} or {@link JsonArray})
     * @param xPath the current path in the JSON tree
     * @param objectList the list to populate with atomic and structured objects
     */
    private void visitNode(Object object, String xPath, List<Object> objectList){
        if(object.getClass() == JsonObject.class)
        {
            JsonObject node = (JsonObject)object;
            if(node.isJsonNull()) return;
            for(String key : node.keySet()){
                JsonElement value = node.get(key);
                if(value.isJsonPrimitive() || value.isJsonNull()) {
                   // atomic node
                    if(!value.isJsonNull()){
                        objectList.add(new AtomicObject(value.getAsString(),key,String.format("%s.%s", xPath,key)));
                    }else{
                        //objectList.add(new AtomicObject(value.getAsString(),key,String.format("%s.%s", xPath,key)));
                    }

                } else if (value.isJsonObject()) {
                    StructuredObject structuredObject = null;
                    structuredObject = new StructuredObject(key,value.getAsJsonObject().toString(),String.format("%s.%s", xPath,key));
                    objectList.add(structuredObject);
                    //visitNode(value.getAsJsonObject(), String.format("%s.%s", xPath,key), "".equals(varName) ? key.toString() :varName+"-"+key.toString(),structuredObject.getObjects());
                    visitNode(value.getAsJsonObject(), String.format("%s.%s", xPath,key), structuredObject.getObjects());
                }else if (value.isJsonArray()){
                    JsonArray jsonArray = value.getAsJsonArray();
                    StructuredObject structuredObject = new StructuredObject(key,jsonArray.toString(),String.format("%s.%s", xPath,key));
                    objectList.add(structuredObject);
                    iterateJSONArray(jsonArray,String.format("%s.%s", xPath,key),key,structuredObject.getObjects());
                }
            }
        }else if(object.getClass() == JsonArray.class){
            JsonArray jsonArray = (JsonArray)object;
            StructuredObject structuredObject = null;
            structuredObject = new StructuredObject("",jsonArray.toString(),xPath);
            objectList.add(structuredObject);
            iterateJSONArray(jsonArray,xPath,"",structuredObject.getObjects());
        }
    }
    /**
     * Iterates through a JSON array, converting each element into atomic or structured objects.
     *
     * @param jsonArray the JSON array to iterate
     * @param xPath the current path in the JSON tree
     * @param varName the variable name associated with array elements
     * @param objectsList the list to populate with parsed objects
     */
    private void iterateJSONArray(JsonArray jsonArray,String xPath, String varName, List<Object> objectsList){
        int id = 0;
        for(JsonElement element : jsonArray){
            //String forname =""+id+"";
            if(element.isJsonPrimitive()){
                objectsList.add(new AtomicObject(element.getAsString(),varName,String.format("%s.%s[%d]", xPath,varName,id)));
            } else if (element.isJsonObject()){
                StructuredObject structuredObject = null;
                structuredObject = new StructuredObject(varName,element.getAsJsonObject().toString(),String.format("%s[%d]", xPath, id));
                objectsList.add(structuredObject);
                visitNode(element.getAsJsonObject(), String.format("%s[%d]", xPath, id),structuredObject.getObjects());
            }
            id++;
        }
    }
    /**
     * Analyzes HTTP response headers and extracts atomic objects for headers such as "Set-Cookie".
     *
     * @param headers the array of HTTP headers to analyze
     * @param responseUnstructured the ResponseUnstructured object to add extracted atomic objects
     */
    public void analyzeResponseHeader(Header[] headers, ResponseUnstructured responseUnstructured) {
        for(int i=0;i<headers.length;i++){
            Header header = headers[i];
            if(header.getName().equals("Set-Cookie")){
                String obj = header.getValue().split(";")[0];
                String [] name_value = obj.split("=");
                AtomicObject atomicObject;
                if(name_value.length==2)
                     atomicObject = new AtomicObject(name_value[1],name_value[0],null,true);
                else
                     atomicObject = new AtomicObject("",name_value[0],null,true);

                responseUnstructured.getObjects().add(atomicObject);
            }
        }
    }
}