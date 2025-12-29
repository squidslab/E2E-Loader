package Services.ResponseAnalyzer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * Represents an unstructured HTTP response containing a collection of parsed objects.
 *
 * <p>This class holds a list of objects extracted from a response. The list can include
 * {@link AtomicObject} and {@link StructuredObject} instances, representing atomic values
 * or nested structured parts of the response, respectively.
 *
 * <p>Typically, instances of this class are populated by {@link ResponseAnalyzer#getUnstructuredResponse(String)}.
 */
public class ResponseUnstructured implements Serializable {

    /**
     * Constructs an empty ResponseUnstructured object with an initialized list of objects.
     */
    public ResponseUnstructured (){
        this.objects = new ArrayList<>();
    }
    /**
     * Returns the list of objects extracted from the HTTP response.
     *
     * @return a list of parsed objects, which may include {@link AtomicObject} and {@link StructuredObject}
     */
    public List<Object> getObjects() {
        return objects;
    }
    /**
     * The list of objects extracted from the response.
     */
    List<Object> objects;

}
