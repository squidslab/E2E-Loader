package Services.Dependencies;

import Entity.*;
import Services.ResponseAnalyzer.AtomicObject;
import Services.ResponseAnalyzer.ResponseUnstructured;
import Services.ResponseAnalyzer.StructuredObject;

import java.util.List;
/**
 * Handles the analysis of cookies to detect dependencies between HTTP requests.
 *
 * <p>This class iterates over cookies in a request and checks if their values
 * depend on any previous responses, updating the {@link DependencyGraph} accordingly.
 */
public class CookieDependency {

    /**
     * Checks for cookie-based dependencies for a given request.
     *
     * <p>For each cookie in the request, it examines previous responses
     * (from {@code first_index_response} to {@code req_index - 1}) to detect
     * any dependency, and updates the {@link DependencyGraph}.
     *
     * @param responseUnstructuredList list of previously analyzed unstructured responses
     * @param req_index index of the current request in the HAR sequence
     * @param dependencyGraph the graph to update with detected dependencies
     * @param to the target node representing the current request
     * @param first_index_response index of the first response to consider for dependency checks
     */
    public static void check_cookie_dependency(List<ResponseUnstructured> responseUnstructuredList, int req_index, DependencyGraph dependencyGraph, MyNode to,int first_index_response) {
        for( Cookie cookie: to.getRequest().getCookies()){
            for (int response_index = first_index_response; response_index < req_index; response_index++) {
                MyNode from = dependencyGraph.getNodeByIndex(response_index);
                check_cookie(cookie, responseUnstructuredList.get(response_index),to,from,dependencyGraph);
            }
        }
    }

    private static void check_cookie(Cookie cookie, Object response, MyNode to, MyNode from, DependencyGraph dependencyGraph) {
        if(response.getClass() == ResponseUnstructured.class){
            ResponseUnstructured responseUnstructured = (ResponseUnstructured)response;
            for(Object o : responseUnstructured.getObjects()){
                check_cookie_atomic_evaluation(o,cookie,null,to,from,dependencyGraph);
            }
        }else if(response.getClass() == StructuredObject.class){
            StructuredObject structuredObject = (StructuredObject) response;
            for(Object o : structuredObject.getObjects()){
                check_cookie_atomic_evaluation(o,cookie,structuredObject,to,from,dependencyGraph);
            }
        }
    }

    private static void check_cookie_atomic_evaluation(Object o, Cookie cookie, StructuredObject father, MyNode to, MyNode from, DependencyGraph dependencyGraph) {
        if(o.getClass() == AtomicObject.class){
            AtomicObject atomicObject = (AtomicObject) o;
            AtomicDependencyValidator atomicDependencyValidator = new AtomicDependencyValidator();
            if(atomicDependencyValidator.evaluate_atomic_dependencies(atomicObject,cookie,father,from.getRequest())){
                EdgeCookie edgeCookie = new EdgeCookie(from,to,cookie.getName(),atomicObject);
                dependencyGraph.edges.add(edgeCookie);
            }
        } else if (o.getClass() == StructuredObject.class) {
            check_cookie(cookie,o,to,from,dependencyGraph);
        }
    }

}
