package Services.ResponseAnalyzer;


import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static Services.ResponseAnalyzer.JsonSchemaGenerator.generateJSONSchema;
/**
 * Represents a structured part of an HTTP response.
 *
 * <p>A {@code StructuredObject} typically corresponds to a JSON object or array within
 * an HTTP response. It can contain nested {@link AtomicObject} or {@link StructuredObject}
 * instances in its {@link #objects} list.
 *
 * <p>Upon creation, this class attempts to generate a JSON schema representation of its
 * {@link #value} by calling {@link JsonSchemaGenerator#generateJSONSchema(String)}.
 */
public class StructuredObject implements Serializable {

    /**
     * Constructs a StructuredObject with the specified name, value, and XPath.
     *
     * @param name  the name of the object, typically corresponding to the JSON key
     * @param value the raw JSON string or value of the object
     * @param xpath the XPath-like path representing the location of the object within the JSON hierarchy
     * @throws RuntimeException if the JSON schema generation fails
     */
    public StructuredObject(String name, String value,String xpath)  {
        this.name = name;
        this.value = value;
        this.xpath=xpath;
        this.objects = new ArrayList<>();
       // if (value.startsWith("{")) {
            try {
              this.schemaString=generateJSONSchema(value);
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        //}
    }
    /** Returns the name of the object. */
    public String getName() {
        return name;
    }
    /** Returns the value of the object. */
    public String getValue() {
        return value;
    }
    /** Returns the list of child objects contained within this structured object. */
    public List<Object> getObjects() {
        return objects;
    }
    /** Sets the name of the object. */
    public void setName(String name) {
        this.name = name;
    }
    /** Sets the value of the object. */
    public void setValue(String value) {
        this.value = value;
    }
    /** Sets the list of child objects for this structured object. */
    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    public void setSchemaString(String schemaString) {
        this.schemaString = schemaString;
    }

    @SerializedName("name")
    public String name;
    @SerializedName("value")
    public String value;
    /** Returns the XPath-like path of the object within the JSON hierarchy. */
    public String getXpath() {
        return xpath;
    }
    /** Sets the XPath-like path of the object within the JSON hierarchy. */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    @SerializedName("xpath")
    public String xpath;
    @SerializedName("objects")
    public List<Object> objects;
    /** Returns the JSON schema string generated for this object. */
    public String getSchemaString() {
        return schemaString;
    }

    String schemaString;
    /**
     * Returns a string representation of the object, including its name, value, number of child objects,
     * JSON schema, and XPath.
     */
    public String toString(){
        return "[STRUCTURED] name: "+name+" value:"+value+" size:"+objects.size()+"\n [JSON_SCHEMA]: "+getSchemaString()+" XPATH:"+xpath;
    }



}
