package Services.ResponseAnalyzer;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
/**
 * Represents a single atomic value extracted from an HTTP response.
 *
 * <p>An atomic object is a primitive value (string) with an associated name
 * and optionally an XPath expression identifying its location in a structured response.
 * It can also represent values originating from a Set-Cookie header.
 */
public class AtomicObject implements Serializable {

    /**
     * Constructs an AtomicObject with the given value, name, and XPath.
     *
     * @param value the value of the atomic object
     * @param name the name of the atomic object
     * @param xpath the XPath identifying the location of this value in the response
     */
    public AtomicObject (String value, String name, String xpath){
        this.value=value;
        this.name=name;
        this.xpath=xpath;
    }
    /**
     * Constructs an AtomicObject with the given value, name, XPath, and Set-Cookie origin flag.
     *
     * @param value the value of the atomic object
     * @param name the name of the atomic object
     * @param xpath the XPath identifying the location of this value in the response
     * @param from_set_cookie true if this value comes from a Set-Cookie header; false otherwise
     */
    public AtomicObject(String value, String name, String xpath, boolean from_set_cookie){
        this.value=value;
        this.name=name;
        this.xpath=xpath;
        this.from_set_cookie=from_set_cookie;
    }
    /** @return the value of the atomic object */
    public String getValue() {
        return value;
    }
    /** @return the name of the atomic object */
    public String getName() {
        return name;
    }
    /** @return the XPath of the atomic object */
    public String getXpath() {
        return xpath;
    }
    /** @return true if the object originates from a Set-Cookie header */
    public boolean getFromSetCookie(){return this.from_set_cookie;}
    /** Sets the value of the atomic object */
    public void setValue(String value) {
        this.value = value;
    }
    /** Sets the name of the atomic object */
    public void setName(String name) {
        this.name = name;
    }
    /** Sets the XPath of the atomic object */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }
    /** Sets the from_set_cookie flag */
    public void setFrom_set_cookie(boolean from_set_cookie) {
        this.from_set_cookie = from_set_cookie;
    }

    @SerializedName("value")
    public  String value;
    @SerializedName("name")
    public  String name;
    @SerializedName("xpath")
    public String xpath;
    @SerializedName("from_set_cookie")
    public boolean from_set_cookie =false;

    @Override
    public String toString(){
        if(!from_set_cookie)
            return "[ATOMIC] name: "+name+" value:"+value+" xpath:"+xpath;
        else
            return "[SET COOKIE] name:"+name+" value:"+value;
    }

    @Override
    public boolean equals(Object o){
        if(this==o){
            return true;
        }

        if(o == null || getClass() != o.getClass()){
            return false;
        }

        AtomicObject atomicObject = (AtomicObject) o;

        if(this.xpath == null || ((AtomicObject) o).xpath == null){
            return this.name.equals(atomicObject.name) && this.value.equals(atomicObject.value);
        }

        return this.name.equals(atomicObject.name) && this.value.equals(atomicObject.value) && this.xpath.equals(atomicObject.xpath);


    }
}
