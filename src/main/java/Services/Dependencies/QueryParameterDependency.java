package Services.Dependencies;

import Entity.*;
import Services.ResponseAnalyzer.AtomicObject;
import Services.ResponseAnalyzer.ResponseUnstructured;
import Services.ResponseAnalyzer.StructuredObject;

import java.util.List;
/**
 * Handles analysis of query parameters to detect dependencies between HTTP requests.
 *
 * <p>This class iterates over query parameters of a request and compares them with
 * previous responses to identify dependencies. Detected dependencies are added
 * to the {@link DependencyGraph}.
 */
public class QueryParameterDependency {
    /**
     * Checks for query parameter-based dependencies for a given request.
     *
     * <p>For each query parameter in the target request, this method examines all
     * previous responses (from {@code first_index_response} to {@code req_index - 1})
     * to detect dependencies, updating the {@link DependencyGraph} if a dependency is found.
     *
     * @param responseUnstructuredList list of previously analyzed unstructured responses
     * @param req_index index of the current request in the HAR sequence
     * @param dependencyGraph the graph to update with detected dependencies
     * @param to the target node representing the current request
     * @param first_index_response index of the first response to consider for dependency checks
     */
    public static void check_queryParams_dependency(List<ResponseUnstructured> responseUnstructuredList, int req_index, DependencyGraph dependencyGraph, MyNode to,int first_index_response) {
        for( QueryParam queryParam: to.getRequest().getQueryParams()){
            for (int response_index = first_index_response; response_index < req_index; response_index++) {
                MyNode from = dependencyGraph.getNodeByIndex(response_index);
                check_queryParam(queryParam, responseUnstructuredList.get(response_index),from,to,dependencyGraph,req_index,response_index);
            }
        }
    }

    private static void check_queryParam(QueryParam queryParam, Object response, MyNode from, MyNode to, DependencyGraph dependencyGraph,int req_index,int from_index) {
        if(response.getClass() == ResponseUnstructured.class){
            ResponseUnstructured responseUnstructured = (ResponseUnstructured)response;
            for(Object o : responseUnstructured.getObjects()){
                check_queryparam_atomic_evaluation(o,queryParam,null,from,to,dependencyGraph,req_index,from_index);
            }
        }else if(response.getClass() == StructuredObject.class){
            StructuredObject structuredObject = (StructuredObject) response;
            for(Object o : structuredObject.getObjects()){
                check_queryparam_atomic_evaluation(o,queryParam,structuredObject,from,to,dependencyGraph,req_index,from_index);
            }
        }
    }

    private static void check_queryparam_atomic_evaluation(Object o, QueryParam queryParam, StructuredObject father, MyNode from, MyNode to, DependencyGraph dependencyGraph,int req_index,int from_index) {
        if(o.getClass() == AtomicObject.class){
            AtomicObject atomicObject = (AtomicObject) o;
            AtomicDependencyValidator atomicDependencyValidator = new AtomicDependencyValidator();
            if(atomicDependencyValidator.evaluate_atomic_dependencies(atomicObject,queryParam,father,from.getRequest())){
                EdgeQueryParam edgeQueryParam = new EdgeQueryParam(from,to,queryParam.getName(),atomicObject);
                dependencyGraph.edges.add(edgeQueryParam);
                edgeQueryParam.setTo_index(req_index);
                edgeQueryParam.setFrom_index(from_index);
            }
        } else if (o.getClass() == StructuredObject.class) {
            check_queryParam(queryParam,o,from,to,dependencyGraph,req_index,from_index);
        }
    }
}
