package Services.Dependencies;

import Entity.*;
import Services.ResponseAnalyzer.AtomicObject;
import Services.ResponseAnalyzer.ResponseUnstructured;
import Services.ResponseAnalyzer.StructuredObject;

import java.net.URI;
import java.util.List;
/**
 * Handles the detection of dependencies based on the URL paths of HTTP requests.
 *
 * <p>This class inspects the path segments of the target request's URL and compares
 * them with previous responses to identify potential dependencies. Detected dependencies
 * are added to the {@link DependencyGraph}.
 */
public class UrlDependency {
    /**
     * Checks for URL-based dependencies for a given request.
     *
     * <p>For each segment in the target request's URL path, this method examines all
     * previous responses (from {@code first_index_response} to {@code req_index - 1})
     * to detect dependencies. Detected dependencies are added to the {@link DependencyGraph}.
     *
     * @param response list of previously analyzed unstructured responses
     * @param req_index index of the current request in the HAR sequence
     * @param dependencyGraph the graph to update with detected dependencies
     * @param to the target node representing the current request
     * @param first_index_response index of the first response to consider for dependency checks
     */
    public static void check_url_dependencies(List<ResponseUnstructured> response, int req_index, DependencyGraph dependencyGraph, MyNode to,int first_index_response){
        //System.out.println("URL:"+to.getRequest().getUrl());
        URI uri = URI.create(to.getRequest().getUrl());
        String path = uri.getPath();
        String [] subPathOfRequestURL = path.split("/");
        for(int i=1; i< subPathOfRequestURL.length ;i++){
            //System.out.println("URL DEP: "+subPathOfRequestURL[i]);
            for(int response_index = first_index_response;response_index<req_index;response_index++){
                MyNode from = dependencyGraph.getNodeByIndex(response_index);
                boolean is_dep = check_subpath(subPathOfRequestURL[i],response.get(response_index), dependencyGraph,to,from,subPathOfRequestURL[i-1],req_index,response_index);
            }
        }
    }


    private static boolean check_subpath(String path, Object response, DependencyGraph dependencyGraph, MyNode to , MyNode from, String possible_name,int req_index,int from_index){
        boolean res = false;
        if(response.getClass() == ResponseUnstructured.class){
            ResponseUnstructured responseUnstructured = (ResponseUnstructured)response;
            for(Object o : responseUnstructured.getObjects()){
                res = check_subpath_atomic_evaluation(o,path,from,dependencyGraph,res,to,null,possible_name,req_index,from_index);
            }
        }else if(response.getClass() == StructuredObject.class){
            StructuredObject structuredObject = (StructuredObject) response;
            for(Object o : structuredObject.getObjects()){
                res = check_subpath_atomic_evaluation(o,path,from,dependencyGraph,res,to,structuredObject,possible_name,req_index,from_index);
            }
        }
        return  res;
    }

    private static boolean check_subpath_atomic_evaluation(Object o, String path, MyNode from, DependencyGraph dependencyGraph, Boolean res, MyNode to, StructuredObject father, String possibile_name,int req_index,int from_index){
        if(o.getClass() == AtomicObject.class){
            AtomicObject atomicObject = (AtomicObject) o;
            if(path.equals(atomicObject.getValue())){
                AtomicDependencyValidator atomicDependencyValidator = new AtomicDependencyValidator();
                //System.out.println("found! : "+atomicObject);
                if(atomicDependencyValidator.evaluate_url_atomic_dep(atomicObject,from.getRequest(),path,father,possibile_name)) {
                    EdgeUrl edgeUrl = new EdgeUrl(from,to, path, atomicObject);
                    dependencyGraph.edges.add(edgeUrl);
                    edgeUrl.setTo_index(req_index);
                    edgeUrl.setFrom_index(from_index);
                    res = true;
                }
            }
        } else if (o.getClass() == StructuredObject.class) {
            res = res || check_subpath(path,o,dependencyGraph,to,from,possibile_name,req_index,from_index);
        }
        return  res;
    }
}
