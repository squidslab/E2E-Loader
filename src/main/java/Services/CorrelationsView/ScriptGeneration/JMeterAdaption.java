package Services.CorrelationsView.ScriptGeneration;

import Services.ResponseAnalyzer.AtomicObject;
import Services.ResponseAnalyzer.StructuredObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import Entity.DependencyGraph;
import Entity.Edge;
import Entity.CSVNode;
import Entity.MyNode;
import Entity.EdgeUrl;
import Entity.EdgeHeader;
import Entity.EdgeQueryParam;
import Properties.Paths;
import Entity.EdgeBodyJSON;

/**
 * Handles the adaptation of a JMeter test plan generated from a HAR conversion.
 *
 * <p>This class takes the JMX skeleton produced by the {@code Converter} and
 * applies additional configuration defined by the tester, such as:
 * <ul>
 *   <li>Variable substitutions</li>
 *   <li>Web service–related parameters</li>
 *   <li>Response handling and dependency-based adaptations</li>
 * </ul>
 *
 * <p>The adaptation logic is driven by a {@link DependencyGraph}, which describes
 * relationships between requests and variables extracted during analysis.
 *
 * <p>The resulting JMX file is modified in-place or rewritten to include all
 * required configuration elements.
 */

public class JMeterAdaption {


    private static ArrayList<variable> variables = new ArrayList<>();
    private static Map<String,Map <Integer, TreeSet<variableWS>>> variablesWS = new HashMap<>();
    private static Set<Integer> SaveTotalResponse = new HashSet<>();

    public static void runJMeterAdaption(DependencyGraph dependencyGraph, String filenameJmx, String filename) throws Exception {
        JMeterAdaption ja = new JMeterAdaption();
        ja.replaceAdaption(dependencyGraph,filenameJmx,filename);
    }

    /**
     * Creates and configures the HeaderManager used for WebSocket requests.
     *
     * @param doc           XML document being modified
     * @param url           WebSocket endpoint URL
     * @param replacement   JSON object containing replacement values
     * @param FILENAME_HAR  name of the source HAR file
     * @return the created HeaderManager node
     */
    private Node createHeaderManagerWSS(Document doc,String url, JSONObject replacement,String FILENAME_HAR) throws  Exception{

        ArrayList<String> permissedItems = new ArrayList<>(Arrays.asList("Accept-Encoding","Accept-Language","Cache-Control","Cookie"));
        Node headerManager = doc.createElement("HeaderManager");
        ((Element)headerManager).setAttribute("guiclass","HeaderPanel");
        ((Element)headerManager).setAttribute("testclass","HeaderManager");
        ((Element)headerManager).setAttribute("testname","HTTP Header Manager");

        JSONParser parser = new JSONParser();
        try(FileReader fileReader = new FileReader(FILENAME_HAR, StandardCharsets.UTF_8))
        {
            Object obj = parser.parse(fileReader);
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray entries = (JSONArray)((JSONObject) jsonObject.get("log")).get("entries");
            for (JSONObject item : (Iterable<JSONObject>) entries)
            {
                JSONObject request  = (JSONObject)item.get("request");
                String url_request = request.get("url").toString();
                if(url_request.equals(url))
                {
                    Node collectionProp = doc.createElement("collectionProp");
                    ((Element)collectionProp).setAttribute("name","HeaderManager.headers");
                    JSONArray headers = (JSONArray) request.get("headers");
                    for(Object item_headers : headers) {
                        String name = ((JSONObject)item_headers).get("name").toString();
                        if(permissedItems.contains(name)) {
                            Node elementProp = doc.createElement("elementProp");
                            ((Element) elementProp).setAttribute("name", ((JSONObject) item_headers).get("name").toString());
                            ((Element) elementProp).setAttribute("elementType", "Header");

                            Node stringname = doc.createElement("stringProp");
                            ((Element) stringname).setAttribute("name", "Header.name");
                            stringname.setTextContent(((JSONObject) item_headers).get("name").toString());

                            Node stringvalue = doc.createElement("stringProp");
                            ((Element) stringvalue).setAttribute("name", "Header.value");
                            stringvalue.setTextContent(((JSONObject) item_headers).get("value").toString());
                            if ("Accept-Language".equals(name)) {
                                stringvalue.setTextContent("it,it-IT;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
                            }
                            elementProp.appendChild(stringname);
                            elementProp.appendChild(stringvalue);
                            if ("Cookie".equals(((JSONObject) item_headers).get("name").toString())) {
                                changeValueCookieHeaderParameter(stringvalue, (JSONArray) replacement.get("cookieWS"), (JSONArray) request.get("cookies"), ((JSONObject) item_headers).get("value").toString());
                            } else {
                                changeValueHeadersParameter(elementProp, (JSONArray) replacement.get("headers"));
                            }
                            collectionProp.appendChild(elementProp);
                        }
                    }
                    headerManager.appendChild(collectionProp);
                }
            }
        }
        return headerManager;
    }
    /**
     * Appends user-defined WebSocket variables to all thread groups.
     *
     * @param doc     XML document being modified
     * @param threads list of thread group nodes
     */
    private void appendWssUserDefineVariables(Document doc, NodeList threads) {

        for(int i=0;i<threads.getLength();i++) {
            Node thread = threads.item(i);
            Node hash = thread.getNextSibling().getNextSibling();
            if(hash.getNodeType()==Node.ELEMENT_NODE) {
                if(hash.getNodeName().equals("hashTree")) {
                    Node firstChild = hash.getFirstChild();
                    hash.insertBefore(createUserVariablesForWebSocket(doc), firstChild);
                    hash.insertBefore(doc.createElement("hashTree"), firstChild);
                }
            }
        }
    }
    /**
     * Appends CSV configuration elements required by dependency resolution.
     *
     * @param doc       XML document being modified
     * @param threads   thread groups where CSV configs are added
     * @param csv_deps  list of CSV dependency definitions
     */

    private void appendCSVConfig(Document doc, NodeList threads, List<CSVNode>csv_deps) {

        for(int i=0;i<threads.getLength();i++) {
            Node thread = threads.item(i);
            Node hash = thread.getNextSibling().getNextSibling();
            if(hash.getNodeType()==Node.ELEMENT_NODE) {
                if(hash.getNodeName().equals("hashTree")) {
                    Node firstChild = hash.getFirstChild();
                    for(CSVNode csvNode : csv_deps) {
                        hash.insertBefore(createCSVDataSetConfigElement(doc,csvNode), firstChild);
                        hash.insertBefore(doc.createElement("hashTree"), firstChild);
                    }
                }
            }
        }
    }
    /**
     * Creates a CSV Data Set Config element from a CSV dependency definition.
     *
     * @param doc XML document being modified
     * @param csv CSV dependency metadata
     * @return created CSV Data Set Config node
     */

    private Node createCSVDataSetConfigElement(Document doc, CSVNode csv) {

        Node csvDataNode = doc.createElement("CSVDataSet");
        ((Element)csvDataNode).setAttribute("guiclass","TestBeanGUI");
        ((Element)csvDataNode).setAttribute("testclass","CSVDataSet");
        ((Element)csvDataNode).setAttribute("testname","CSV Data Set Config "+csv.getFilename());
        ((Element)csvDataNode).setAttribute("enabled","true");

        Node stringPropFileName = doc.createElement("stringProp");
        ((Element)stringPropFileName).setAttribute("name", "filename");
        stringPropFileName.setTextContent(csv.getFilename());

        Node stringPropEncod = doc.createElement("stringProp");
        ((Element)stringPropEncod).setAttribute("name","fileEncoding");
        stringPropEncod.setTextContent(csv.getEncoding());

        Node stringPropVarNames = doc.createElement("stringProp");
        ((Element)stringPropVarNames).setAttribute("name","variableNames");
        stringPropVarNames.setTextContent(csv.getVariablesName());

        Node boolPropFirstLine = doc.createElement("boolProp");
        ((Element)boolPropFirstLine).setAttribute("name","ignoreFirstLine");
        boolPropFirstLine.setTextContent(String.valueOf(csv.isIgnorefirstLine()));

        Node stringPropDel = doc.createElement("stringProp");
        ((Element)stringPropDel).setAttribute("name","delimiter");
        stringPropDel.setTextContent(",");

        Node boolPropQuotedData = doc.createElement("boolProp");
        ((Element)boolPropQuotedData).setAttribute("name","quotedData");
        boolPropQuotedData.setTextContent("false");

        Node boolPropRecycle = doc.createElement("boolProp");
        ((Element)boolPropRecycle).setAttribute("name","recycle");
        boolPropRecycle.setTextContent("true");

        Node boolPropStopThread = doc.createElement("boolProp");
        ((Element)boolPropStopThread).setAttribute("name","stopThread");
        boolPropStopThread.setTextContent("false");

        Node stringPropShareMode = doc.createElement("stringProp");
        ((Element)stringPropShareMode).setAttribute("name","shareMode");
        stringPropShareMode.setTextContent("shareMode.all");

        csvDataNode.appendChild(stringPropFileName);
        csvDataNode.appendChild(stringPropEncod);
        csvDataNode.appendChild(stringPropVarNames);
        csvDataNode.appendChild(boolPropFirstLine);
        csvDataNode.appendChild(stringPropDel);
        csvDataNode.appendChild(boolPropQuotedData);
        csvDataNode.appendChild(boolPropRecycle);
        csvDataNode.appendChild(boolPropStopThread);
        csvDataNode.appendChild(stringPropShareMode);

        return csvDataNode;
    }

    /**
     * Creates user-defined variables used by WebSocket samplers.
     *
     * @param doc XML document being modified
     * @return node containing WebSocket user variables
     */
    private Node createUserVariablesForWebSocket(Document doc) {

        ArrayList<String> listName = new ArrayList<>(Arrays.asList("webSocketResponding"));
        ArrayList<String> listValue = new ArrayList<>(Arrays.asList("true"));
        Node Arguments = doc.createElement("Arguments");
        ((Element)Arguments).setAttribute("guiclass","ArgumentsPanel");
        ((Element)Arguments).setAttribute("testclass","Arguments");
        ((Element)Arguments).setAttribute("testname","User Defined Variables");
        ((Element)Arguments).setAttribute("enable","true");

        Node collectionProp = doc.createElement("collectionProp");
        ((Element)collectionProp).setAttribute("name","Arguments.arguments");

        for(int i=0;i<listName.size();i++) {
            Node elementProp= doc.createElement("elementProp");
            ((Element) elementProp).setAttribute("name", listName.get(i));
            ((Element) elementProp).setAttribute("elementType", "Argument");

            Node stringPropName = doc.createElement("stringProp");
            ((Element)stringPropName).setAttribute("name","Argument.name");
            stringPropName.setTextContent(listName.get(i));

            Node stringPropValue = doc.createElement("stringProp");
            ((Element)stringPropValue).setAttribute("name","Argument.value");
            stringPropValue.setTextContent(listValue.get(i));

            Node stringPropMeta = doc.createElement("stringProp");
            ((Element)stringPropMeta).setAttribute("name","Argument.metadata");
            stringPropMeta.setTextContent("=");

            elementProp.appendChild(stringPropName);
            elementProp.appendChild(stringPropValue);
            elementProp.appendChild(stringPropMeta);
            collectionProp.appendChild(elementProp);
        }
        Arguments.appendChild(collectionProp);
        return Arguments;
    }

    /**
     * Creates a WebSocket connection sampler node.
     *
     * @param doc            XML document being modified
     * @param hashTreeFrather parent hash tree node
     * @param url_request    WebSocket endpoint URL
     * @param replacement    JSON object containing replacement values
     * @param FILENAME_HAR   name of the source HAR file
     * @return created WebSocket connection node
     */
    private Node createWSSConnectionNode(Document doc,Node hashTreeFrather ,String url_request, JSONObject replacement,String FILENAME_HAR) throws Exception {

        Node hashTreeConnection = doc.createElement("hashTree");
        Node webSocketConnection = null;
        webSocketConnection = doc.createElement("eu.luminis.jmeter.wssampler.OpenWebSocketSampler");
        ((Element)webSocketConnection).setAttribute("guiclass","eu.luminis.jmeter.wssampler.OpenWebSocketSamplerGui");
        ((Element)webSocketConnection).setAttribute("testclass","eu.luminis.jmeter.wssampler.OpenWebSocketSampler");
        ((Element)webSocketConnection).setAttribute("testname","WebSocket Open Connection");
        ((Element)webSocketConnection).setAttribute("enable","true");

        URI url = URI.create(url_request);
        Node boolProp = doc.createElement("boolProp");
        ((Element)boolProp).setAttribute("name","TLS");
        boolProp.setTextContent("true");


        Node server = doc.createElement("stringProp");
        ((Element)server).setAttribute("name","server");
        server.setTextContent(url.getHost());
        Node port = doc.createElement("stringProp");
        ((Element)port).setAttribute("name","port");
        port.setTextContent("443");
        Node path = doc.createElement("stringProp");
        ((Element)path).setAttribute("name","path");
        path.setTextContent(url.getPath());
        Node connecttimeout = doc.createElement("stringProp");
        ((Element)connecttimeout).setAttribute("name","connectTimeout");
        connecttimeout.setTextContent("20000");
        Node readtimeout = doc.createElement("stringProp");
        ((Element)readtimeout).setAttribute("name","readTimeout");
        readtimeout.setTextContent("60000");

        webSocketConnection.appendChild(boolProp);
        webSocketConnection.appendChild(server);
        webSocketConnection.appendChild(port);
        webSocketConnection.appendChild(path);
        webSocketConnection.appendChild(connecttimeout);
        webSocketConnection.appendChild(readtimeout);

        hashTreeConnection.appendChild(webSocketConnection);
        hashTreeConnection.appendChild(doc.createElement("hashTree"));
        appendHeaderManagerWebSocketRequest(doc,hashTreeConnection,url_request,replacement,FILENAME_HAR);
        return hashTreeConnection;
    }

    /**
     * Appends a WebSocket connection sampler and its related configuration.
     *
     * @param doc            XML document being modified
     * @param hashTree       parent hash tree node
     * @param url_request    WebSocket endpoint URL
     * @param replacement    JSON object containing replacement values
     * @param FILENAME_HAR   name of the source HAR file
     */
    private void appendWebSocketconnection (Document doc, Node hashTree, String url_request , JSONObject replacement,String FILENAME_HAR ) throws Exception {
        Node genericControllerConnection = doc.createElement("GenericController");
        ((Element)genericControllerConnection).setAttribute("guiclass","LogicControllerGui");
        ((Element)genericControllerConnection).setAttribute("testclass","GenericController");
        ((Element)genericControllerConnection).setAttribute("testname","Connection");
        ((Element)genericControllerConnection).setAttribute("enabled","true");
        hashTree.appendChild(genericControllerConnection);
        hashTree.appendChild(createWSSConnectionNode(doc,hashTree,url_request,replacement,FILENAME_HAR));
    }

    /**
     * Appends a HeaderManager to a WebSocket request.
     *
     * @param doc            XML document being modified
     * @param hashTree       parent hash tree node
     * @param url_request    WebSocket endpoint URL
     * @param replacement    JSON object containing replacement values
     * @param FILENAME_HAR   name of the source HAR file
     */
    private void appendHeaderManagerWebSocketRequest(Document doc, Node hashTree, String url_request, JSONObject replacement,String FILENAME_HAR) throws Exception {
        hashTree.appendChild(createHeaderManagerWSS(doc,url_request,replacement,FILENAME_HAR));
        hashTree.appendChild(doc.createElement("hashTree"));
    }

    /**
     * Adds regular expression extractors based on dependency information.
     *
     * @param doc          XML document being modified
     * @param http         HTTP sampler node
     * @param dependencies list of dependency definitions
     * @param index        index of the current request
     */

    public void addRegExtractorMain(Document doc, Node http,List<Object> dependencies,int index) {
        String  url = http.getAttributes().getNamedItem("testname").getTextContent();
        String variable_names = "";
        String path_expression ="";
        String default_value="";
        for(Object object : dependencies)
        {
            if(object.getClass().equals(AtomicObject.class))
            {
                AtomicObject atomicObject = (AtomicObject) object;
                if(!atomicObject.from_set_cookie) {
                    if ("".equals(variable_names)) {
                        variable_names = atomicObject.name+"_"+index;
                    } else {
                        variable_names = variable_names + ";" + atomicObject.name+"_"+index;
                    }

                    if ("".equals(path_expression)) {
                        path_expression = atomicObject.xpath;
                    } else {
                        path_expression = path_expression + ";" + atomicObject.xpath;
                    }

                    if ("".equals(default_value)) {
                        default_value = "NOT_FOUND";
                    } else {
                        default_value = default_value + ";" + "NOT_FOUND";
                    }
                }
            }else{
                // TO DO
                StructuredObject structuredObject = (StructuredObject) object;
                if ("".equals(variable_names)) {
                    variable_names = structuredObject.name+"_"+index;
                } else {
                    variable_names = variable_names + ";" + structuredObject.name+"_"+index;
                }

                if ("".equals(path_expression)) {
                    path_expression = structuredObject.xpath;
                } else {
                    path_expression = path_expression + ";" + structuredObject.xpath;
                }

                if ("".equals(default_value)) {
                    default_value = "NOT_FOUND";
                } else {
                    default_value = default_value + ";" + "NOT_FOUND";
                }
            }
        }
        Node hashTree = http.getNextSibling().getNextSibling();
        addRegExtractor(doc, hashTree,variable_names,path_expression,default_value);
    }
    /**
     * Disables the default HeaderManager in the JMX document.
     *
     * @param doc XML document being modified
     */
    private void disableHeaderManager(Document doc) {
        NodeList listofHeaderManager = doc.getElementsByTagName("HeaderManager");
        System.out.println(listofHeaderManager.getLength());
        Node item  = listofHeaderManager.item(0);
        ((Element)item).setAttribute("enabled","false");
    }

    /**
     * Applies all adaptation rules to the JMX file based on the dependency graph.
     *
     * @param dependencyGraph dependency graph describing request relationships
     * @param FILENAME_JMX     JMX file to be modified
     * @param filename         original HAR filename
     */
    public void replaceAdaption(DependencyGraph dependencyGraph, String FILENAME_JMX,String filename) throws  Exception{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (InputStream is = new FileInputStream(FILENAME_JMX))
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            disableHeaderManager(doc);
            NodeList listOfHttp = doc.getElementsByTagName("HTTPSamplerProxy");
            NodeList threadsGroup = doc.getElementsByTagName("ThreadGroup");
            List<CSVNode> csv_dependencies = dependencyGraph.getCSVNodeDependencies();
            appendCSVConfig(doc,threadsGroup,csv_dependencies);
            //check addRegExpr http[0]
            Node http0 = listOfHttp.item(0);
            if(http0.getNodeType() == Node.ELEMENT_NODE)
            {
                MyNode first_node = dependencyGraph.getNodeByIndex(0);
                List<Edge> dependencies = dependencyGraph.getDependenciesByNode(first_node);
                List<Object> dependenciesbyfirstnode = dependencyGraph.getDependenciesToExtractToFromNode(first_node,0);
                analizeHttpNode(doc,http0,dependencies);
                if(!dependenciesbyfirstnode.isEmpty()) {
                    addRegExtractorMain(doc,http0,dependenciesbyfirstnode,0);
                }
            }
            for (int i = 1; i < listOfHttp.getLength(); i++)
            {
                Node http = listOfHttp.item(i);
                Node parent = http.getParentNode();
                Node nextSibling = http.getNextSibling();
                String  url = http.getAttributes().getNamedItem("testname").getTextContent();
                System.out.println("["+i+"]"+url);
                if (http.getNodeType() == Node.ELEMENT_NODE)
                {
                    if(url.startsWith("wss")|| url.startsWith("ws"))
                    {
                        /*http.getParentNode().removeChild(http);

                        Node genericControllerWSSRequest = doc.createElement("GenericController");
                        ((Element)genericControllerWSSRequest).setAttribute("guiclass","LogicControllerGui");
                        ((Element)genericControllerWSSRequest).setAttribute("testclass","GenericController");
                        ((Element)genericControllerWSSRequest).setAttribute("testname","WSS Request");
                        ((Element)genericControllerWSSRequest).setAttribute("enabled","true");

                        parent.insertBefore(genericControllerWSSRequest,nextSibling);
                        Node hash = nextSibling.getNextSibling();

                        if(hash.getNodeType() == Node.ELEMENT_NODE) {
                            if (hash.getNodeName().equals("hashTree")) {
                                while(hash.hasChildNodes()) {
                                    hash.removeChild(hash.getFirstChild());
                                }
                            }
                        }
                        appendWebSocketconnection(doc,hash,url,(JSONObject) replacements.get(i-1),FILENAME_HAR);
                        appendWssUserDefineVariables(doc,threadsGroup);
                        appendRequestsAndResponsesMessage(doc,hash,url,(JSONObject)replacements.get(i-1),i,FILENAME_HAR);
                        appendCloseWssConnection(doc,hash);
                        */
                    }
                    else
                    {
                        MyNode node = dependencyGraph.getNodeByIndex(i);
                        //System.out.println("REPLACEMENT ["+(i-1)+"]");
                        List<Edge> dependencies = dependencyGraph.getDependenciesByNode(node);
                        List<Object> dependentByMe = dependencyGraph.getDependenciesToExtractToFromNode(node,i);
                        //System.out.println(replacements.get(i-1));
                        analizeHttpNode(doc,http,dependencies);
                        if (!dependentByMe.isEmpty())
                        {
                            addRegExtractorMain(doc,http,dependentByMe,i);
                        }
                        /*if(checkTotalSaveResponse(i)) {
                            addPostProcessorSaveAllResponse(doc,http,i);
                        }*/
                    }
                }
            }
            String out_filename = filename.substring(0,filename.indexOf("."));
            try(FileOutputStream outputStream = new FileOutputStream(Paths.scripts_saved_path+"/"+out_filename+".jmx"))
            {
                writeXml(doc,outputStream);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
//}

    /**
     * Appends a sampler that closes an active WebSocket connection.
     *
     * @param doc  XML document being modified
     * @param hash parent hash tree node
     */
    private void appendCloseWssConnection(Document doc, Node hash) {
        Node close = doc.createElement("eu.luminis.jmeter.wssampler.CloseWebSocketSampler");
        ((Element)close).setAttribute("guiclass","eu.luminis.jmeter.wssampler.CloseWebSocketSamplerGui");
        ((Element)close).setAttribute("testclass","eu.luminis.jmeter.wssampler.CloseWebSocketSampler");
        ((Element)close).setAttribute("testname","WebSocket Close");
        ((Element)close).setAttribute("enabled","true");

        Node statusCode = doc.createElement("stringProp");
        ((Element)statusCode).setAttribute("name","statusCode");
        statusCode.setTextContent("1000");
        Node readT = doc.createElement("stringProp");
        ((Element)readT).setAttribute("name","readTimeout");
        readT.setTextContent("6000");

        close.appendChild(statusCode);
        close.appendChild(readT);

        hash.appendChild(close);
        hash.appendChild(doc.createElement("hashTree"));
    }

    /**
     * Adds a post-processor that saves the full response content.
     *
     * @param doc   XML document being modified
     * @param http  HTTP sampler node
     * @param index request index
     */

    private void addPostProcessorSaveAllResponse(Document doc, Node http,int index) {
        Node postProcessor = null;
        Node hashTree = http.getNextSibling().getNextSibling();
        postProcessor = doc.createElement("JSR223PostProcessor");
        ((Element)postProcessor).setAttribute("guiclass","TestBeanGUI");
        ((Element)postProcessor).setAttribute("testclass","JSR223PostProcessor");
        ((Element)postProcessor).setAttribute("testname","SaveResponse"+index);
        ((Element)postProcessor).setAttribute("enabled","true");

        Node scriptLenguage = doc.createElement("stringProp");
        ((Element)scriptLenguage).setAttribute("name","scriptLanguage");
        scriptLenguage.setTextContent("groovy");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cackeKey = doc.createElement("stringProp");
        ((Element)cackeKey).setAttribute("name","cacheKey");
        cackeKey.setTextContent("true");
        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");
        script.setTextContent("" +
                "var responsedata = prev.getResponseDataAsString();\n" +
                "vars.put(\"Request"+index+"_Response\",responsedata);");

        postProcessor.appendChild(scriptLenguage);
        postProcessor.appendChild(parameters);
        postProcessor.appendChild(filename);
        postProcessor.appendChild(cackeKey);
        postProcessor.appendChild(script);
        //hashTree.appendChild(doc.createElement("hashTree"));
        hashTree.appendChild(postProcessor);

    }
    /**
     * Writes the modified XML document to the provided output stream.
     *
     * @param doc    XML document to write
     * @param output destination output stream
     */
    private void writeXml(Document doc, OutputStream output) throws Exception {
        doc.getDocumentElement().normalize();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT,"yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source,result);
    }
    /**
     * Adds a regular expression extractor to the specified sampler.
     *
     * @param doc      XML document being modified
     * @param hashTree parent hash tree node
     * @param name     variable name to store the extracted value
     * @param path     regular expression or JSON path
     * @param defa     default value if extraction fails
     */
    public  void addRegExtractor(Document doc, Node hashTree,String name, String path, String defa) {

        Node regExtr = null;
        regExtr= doc.createElement("JSONPostProcessor");
        ((Element)regExtr).setAttribute("guiclass","JSONPostProcessorGui");
        ((Element)regExtr).setAttribute("testclass","JSONPostProcessor");
        ((Element)regExtr).setAttribute("testname","JSON Extractor");
        ((Element)regExtr).setAttribute("enable","true");

        Node refname = doc.createElement("stringProp");
        ((Element)refname).setAttribute("name","JSONPostProcessor.referenceNames");
        refname.setTextContent(name);

        Node regex = doc.createElement("stringProp");
        ((Element)regex).setAttribute("name","JSONPostProcessor.jsonPathExprs");
        regex.setTextContent(path);

        Node num = doc.createElement("stringProp");
        ((Element)num).setAttribute("name","JSONPostProcessor.match_numbers");
        num.setTextContent("");

        Node def = doc.createElement("stringProp");
        ((Element)def).setAttribute("name","JSONPostProcessor.defaultValues");
        def.setTextContent(defa);

        regExtr.appendChild(refname);
        regExtr.appendChild(regex);
        regExtr.appendChild(num);
        regExtr.appendChild(def);
        hashTree.appendChild(regExtr);
        hashTree.appendChild(doc.createElement("hashTree"));

    }
    /**
     * Checks whether the response for the given request index has already been saved.
     *
     * @param index_req request index
     * @return {@code true} if the response was already saved, {@code false} otherwise
     */
    public boolean checkRegExtractor(Node http,int index_req) {
        String  url = http.getAttributes().getNamedItem("testname").getTextContent();
        for(int i=0; i<variables.size();i++) {
            if(variables.get(i).from.equals(url)&& variables.get(i).num_req==index_req) {return true;}
        }
        return false;
    }

    /**
     * Checks whether the response for the given request index has already been saved.
     *
     * @param index_req request index
     * @return {@code true} if the response was already saved, {@code false} otherwise
     */
    public boolean checkTotalSaveResponse(int index_req){
        for(Integer i : SaveTotalResponse) {
            if(i == index_req)
                return true;
        }
        return false;
    }
    /**
     * Analyzes an HTTP sampler and applies dependency-based replacements.
     *
     * @param doc          XML document being modified
     * @param http         HTTP sampler node
     * @param dependencies list of dependency edges
     */
    public void analizeHttpNode(Document doc, Node http, List<Edge> dependencies) throws MalformedURLException, URISyntaxException, ParseException {
        NodeList childNodes = http.getChildNodes();
        for(int j=0 ; j< childNodes.getLength();j++) {
            Node item = childNodes.item(j);
            if(item.getNodeType() == Node.ELEMENT_NODE) {

                if("stringProp".equals(item.getNodeName()) ){
                    if("HTTPSampler.path".equals(item.getAttributes().getNamedItem("name").getTextContent())){
                        replacementsInURL(item,dependencies);
                    }
                }

                if("elementProp".equals(item.getNodeName())){
                    if(item.getAttributes().getNamedItem("name").getNodeValue().equals("HTTPSampler.header_manager")) {
                        replacementsInHeaders(item,dependencies);
                    }else {
                        if (item.getAttributes().getNamedItem("name").getNodeValue().equals("HTTPsampler.Arguments")) {
                            //System.out.println(replacement);
                            replacementsInQueryParameters_PostData(item, dependencies, "queryParameters");
                            //System.out.println(replacement.containsKey("postData"));
                            if(!dependencies.isEmpty()){
                                MyNode node = dependencies.get(0).to;
                                if (node.request.getMethod().equals("POST")|| node.request.getMethod().equals("PUT")) {
                                    String mimeType = node.request.getPostData().getMimeType();
                                    if(mimeType.equals("application/x-www-form-urlencoded")) {
                                        replacementsInQueryParameters_PostData(item, dependencies, "postData");
                                    }else if(mimeType.equals("application/json")){
                                        replacementInPostDataBodyJson(doc,http,item,dependencies);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Converts a JSONPath expression from bracket notation to dot notation.
     *
     * @param path JSONPath expression
     * @return converted path using dot notation
     */
    private String convertBracketJsonPathToDotNotation(String path) {
        StringBuilder result = new StringBuilder();
        String regex = "\\['([^\\]]+)'\\]|\\[(\\d+)]";
        Matcher matcher = Pattern.compile(regex).matcher(path);

        // Start with "$"
        result.append("$");

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Key: ['key'] → .key
                result.append(".").append(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // Index: [number] → [number]
                result.append("[").append(matcher.group(2)).append("]");
            }
        }

        return result.toString();
    }
    /**
     * Recursively searches a JSON structure to find the path of a given key.
     *
     * @param json        JSON object or array to inspect
     * @param key         key to search for
     * @param currentPath current traversal path
     * @return full JSON path to the key, or {@code null} if not found
     */
    private String findJsonPathRecursive(Object json, String key, String currentPath) {
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            for (Object k : jsonObject.keySet()) {
                String keyStr = (String) k;
                Object value = jsonObject.get(keyStr);
                String newPath = currentPath + "['" + keyStr + "']";

                if (keyStr.equals(key)) {
                    return newPath;
                }

                String result = findJsonPathRecursive(value, key, newPath);
                if (result != null) {
                    return result;
                }
            }
        } else if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) json;
            for (int i = 0; i < jsonArray.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                Object value = jsonArray.get(i);

                String result = findJsonPathRecursive(value, key, newPath);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    /**
     * Replaces values inside a JSON request body using dependency information.
     *
     * @param doc          XML document being modified
     * @param http         HTTP sampler node
     * @param item         XML node containing the request body
     * @param dependencies list of dependency edges
     */
    private void replacementInPostDataBodyJson(Document doc,Node http, Node item, List<Edge> dependencies) throws ParseException {
        NodeList elementPropChild = item.getChildNodes();
        for (int k = 0; k < elementPropChild.getLength(); k++) {
            Node collectionProp = elementPropChild.item(k);
            if (collectionProp.getNodeType() == Node.ELEMENT_NODE) {
                if ("collectionProp".equals(collectionProp.getNodeName())) {
                    NodeList elementsPropCollection = collectionProp.getChildNodes();
                    for (int i = 0; i < elementsPropCollection.getLength(); i++) {
                        Node elementProp = elementsPropCollection.item(i);
                        if (elementProp.getNodeType() == Node.ELEMENT_NODE) {
                            if ("elementProp".equals(elementProp.getNodeName())) {
                                NodeList argumentsChilds = elementProp.getChildNodes();
                                for(int j=0;j< argumentsChilds.getLength();j++){
                                    Node stringProp = argumentsChilds.item(j);
                                    if(stringProp.getNodeType()== Node.ELEMENT_NODE) {
                                        if("stringProp".equals(stringProp.getNodeName())) {
                                            if(stringProp.getAttributes().getNamedItem("name").getNodeValue().equals("Argument.value"))
                                            {
                                                String data = stringProp.getTextContent();
                                                List<EdgeBodyJSON> list = IsDipendentFromPreviousAllResponse(dependencies);
                                                if(list!=null) {
                                                    if (list.size()==1){
                                                        EdgeBodyJSON isDipFromPrevResp = list.get(0);
                                                        String name = isDipFromPrevResp.structuredObject.name;
                                                        data = "${"+name+"_"+isDipFromPrevResp.from_index+"}";
                                                    }else{
                                                        // bisogna estrarre la risposta e modificarla in base alle altre dipedenze.
                                                        appendPrePocessorNodeToModifyResponseData(doc,http,list);
                                                        data = "${modifiedBody_"+ list.get(0).to.indexs+"}";
                                                    }
                                                }
                                                else {
                                                    for (Edge edge : dependencies) {
                                                        if(edge.getClass().equals(EdgeBodyJSON.class))
                                                        {
                                                            EdgeBodyJSON edgeBodyJSON = (EdgeBodyJSON)edge;
                                                            JSONParser parser = new JSONParser();
                                                            String jsonString = edgeBodyJSON.to.request.getPostData().getText();
                                                            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
                                                            String xpath = findJsonPathRecursive(jsonObject,edgeBodyJSON.name,"$");

                                                            if(edgeBodyJSON.dependency!=null){ // CASO ATOMICOBJECT
                                                                String name = edgeBodyJSON.dependency.name;
                                                                if(!name.startsWith("$")){
                                                                    if(!edgeBodyJSON.dependency.value.equals("manually_inserted") && !edgeBodyJSON.dependency.value.equals("manually_csv"))
                                                                        name="${"+name+"_"+edgeBodyJSON.from_index+"}";
                                                                    else {
                                                                        //name="${"+name+"}";
                                                                    }
                                                                }
                                                                data = replaceAtomicJSONObject(data, xpath,name);
                                                            }else
                                                            {
                                                                String name = edgeBodyJSON.structuredObject.name;
                                                                if(!name.startsWith("$")){
                                                                    name="${"+name+"_"+edgeBodyJSON.from_index+"}";
                                                                }
                                                                data = replaceStructureJSONObject(data,xpath,edgeBodyJSON.name,name);
                                                            }
                                                        }
                                                    }
                                                }
                                                stringProp.setTextContent(data);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void appendPrePocessorNodeToModifyResponseData(Document doc, Node http, List<EdgeBodyJSON> listEdgeBodyJSON) throws ParseException {
        Node preProcessor = null;
        Node hashTree = http.getNextSibling().getNextSibling();
        preProcessor = doc.createElement("JSR223PreProcessor");
        ((Element)preProcessor).setAttribute("guiclass","TestBeanGUI");
        ((Element)preProcessor).setAttribute("testclass","JSR223PreProcessor");
        ((Element)preProcessor).setAttribute("testname","Modify previously saved json response");
        ((Element)preProcessor).setAttribute("enabled","true");

        Node scriptLenguage = doc.createElement("stringProp");
        ((Element)scriptLenguage).setAttribute("name","scriptLanguage");
        scriptLenguage.setTextContent("groovy");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cackeKey = doc.createElement("stringProp");
        ((Element)cackeKey).setAttribute("name","cacheKey");
        cackeKey.setTextContent("true");
        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");

        String scriptString = "import com.jayway.jsonpath.JsonPath\n"+
        "import com.jayway.jsonpath.DocumentContext\n";
        // get previous the index of the request we're extracting all the response
        int previous_all_req_index=0;
        for(Edge edge : listEdgeBodyJSON) {
            if(edge.getClass().equals(EdgeBodyJSON.class)){
                EdgeBodyJSON edgeBodyJSON = (EdgeBodyJSON) edge;
                if ( edgeBodyJSON.structuredObject!= null && edgeBodyJSON.name.equals("All")){
                    previous_all_req_index = edgeBodyJSON.from_index;
                }
            }
        }
        scriptString+="def originalJson = vars.get(\"All_"+previous_all_req_index+"\")\n"+
                "DocumentContext jsonDoc = JsonPath.parse(originalJson)\n"+
                "def substitutions = [\n";
        for (Edge edge : listEdgeBodyJSON){
            if (edge.getClass().equals(EdgeBodyJSON.class)){
                EdgeBodyJSON edgeBodyJSON = (EdgeBodyJSON)edge;
                if (!edgeBodyJSON.name.equals("All"))
                {
                    JSONParser parser = new JSONParser();
                    String jsonString = edgeBodyJSON.to.request.getPostData().getText();
                    JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
                    String xpath_array = findJsonPathRecursive(jsonObject,edgeBodyJSON.name,"$");
                    String xpath = convertBracketJsonPathToDotNotation(xpath_array);
                    System.out.println(xpath);

                    if(!edgeBodyJSON.dependency.value.equals("manually_inserted") && !edgeBodyJSON.dependency.value.equals("manually_csv"))
                    {
                        if(edgeBodyJSON.structuredObject!=null){
                            System.out.println(edgeBodyJSON.structuredObject);
                            scriptString+="[path:\'"+xpath+"\', newValue:\'${"+edgeBodyJSON.structuredObject.name+"_"+edgeBodyJSON.from_index+"}\'],\n";
                        }else{
                            System.out.println(edgeBodyJSON.dependency);
                            scriptString+="[path:\'"+xpath+"\', newValue:\'${"+edgeBodyJSON.dependency.name+"_"+edgeBodyJSON.from_index+"}\'],\n";
                        }
                    }
                    else{
                        if(edgeBodyJSON.structuredObject!=null){
                            scriptString+="[path:\'"+xpath+"\', newValue:\'"+edgeBodyJSON.dependency.name+"\'],\n";
                        }else{
                            scriptString+="[path:\'"+xpath+"\', newValue:\'"+edgeBodyJSON.dependency.name+"\'],\n";
                        }
                    }
                }
            }
        }

        scriptString+="]\n"+
                "substitutions.each { sub ->\n" +
                "    jsonDoc.set(sub.path, sub.newValue)\n" +
                "}\n";
        scriptString+="String modifiedJson = jsonDoc.jsonString()\n"+
                "vars.put(\"modifiedBody_"+ listEdgeBodyJSON.get(0).to.indexs+"\",modifiedJson)";
        script.setTextContent(scriptString);
        preProcessor.appendChild(scriptLenguage);
        preProcessor.appendChild(parameters);
        preProcessor.appendChild(filename);
        preProcessor.appendChild(cackeKey);
        preProcessor.appendChild(script);
        //hashTree.appendChild(doc.createElement("hashTree"));
        hashTree.appendChild(preProcessor);
    }

    private List<EdgeBodyJSON> IsDipendentFromPreviousAllResponse(List<Edge> dependencies) {
        List<EdgeBodyJSON> res = new ArrayList<EdgeBodyJSON>();
        Boolean found = false;
        for(Edge edge : dependencies) {
            if(edge.getClass().equals(EdgeBodyJSON.class)){
                EdgeBodyJSON edgeBodyJSON = (EdgeBodyJSON) edge;
                res.add(edgeBodyJSON);
                if ( edgeBodyJSON.structuredObject!= null && edgeBodyJSON.name.equals("All")){
                    found = true;
                }
            }
        }
        if(found)
            return res;
        else
            return null;
    }

    private String replaceAtomicJSONObject(String data, String xpath, String value) {
        System.out.println("XPath: " + xpath);
        System.out.println("Data: " + data);
        try {
            final Configuration configuration = Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .mappingProvider(new JacksonMappingProvider())
                    .build();

            JsonNode updatedJson = JsonPath.using(configuration).parse(data).set(xpath, value).json();
            return updatedJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update JSON. Check if `data` is valid JSON and `xpath` is correct.");
        }
    }

    private static String replaceFieldValue(String json, String key, String value) {
        // Creazione della stringa da cercare e della stringa sostitutiva
        String toReplace = "\"" + key + "\":\"" + value + "\"";
        String replacement = "\"" + key + "\":" + value;
        // Sostituzione
        String modifiedJsonString = json.replace(toReplace, replacement);
        return modifiedJsonString;
    }
    private String replaceStructureJSONObject(String data, String xpath,String key, String value){
        String json = replaceAtomicJSONObject(data,xpath,value);
        return  replaceFieldValue(json,key,value);
    }


    public void replacementsInURL(Node item, List<Edge> dependencies) throws MalformedURLException, URISyntaxException {
        if(!dependencies.isEmpty()) {
            String originalUrl = dependencies.get(0).to.getRequest().getUrl();
            URI uri = new URI(originalUrl);
            String path = uri.getPath();
            String[] subpaths = path.split("/");
            int changed =0;
            for (Edge edge : dependencies) {
                if (edge.getClass().equals(EdgeUrl.class)) {
                    EdgeUrl edgeUrl = (EdgeUrl) edge;
                    for (int i = 0; i < subpaths.length; i++) {
                        if (subpaths[i].equals(edgeUrl.subPath)) {
                            changed=1;
                            if(!edgeUrl.dependency.value.equals("manually_inserted") && !edgeUrl.dependency.value.equals("manually_csv"))
                                subpaths[i] = "${" + edgeUrl.dependency.name+"_"+edgeUrl.from_index+"}";
                            else
                                subpaths[i] = edgeUrl.dependency.name;

                            break;
                        }
                    }
                }
            }
            if (changed==0){return;}
            // Ricostruisci il percorso
            StringBuilder newPath = new StringBuilder();
            for (String subPath : subpaths) {
                if (!subPath.isEmpty()) {
                    newPath.append("/").append(subPath);
                }
            }
            /*
            // Ricostruisci l'URI con il nuovo percorso
            URI newUri = new URI(uri.getScheme(), uri.getAuthority(), newPath.toString(), uri.getQuery(), uri.getFragment());
            String newUrl = newUri.toString();

            item.setTextContent(newUrl);*/
            // Ricostruisci l'URL manualmente senza URLEncoding
            StringBuilder newUrl = new StringBuilder();
            newUrl.append(uri.getScheme())
                    .append("://")
                    .append(uri.getAuthority())
                    .append(newPath);

            if (uri.getQuery() != null) {
                newUrl.append("?").append(uri.getQuery());
            }

            if (uri.getFragment() != null) {
                newUrl.append("#").append(uri.getFragment());
            }

            item.setTextContent(newUrl.toString());
        }
    }

    public void replacementsInHeaders(Node item, List<Edge> dependencies) {
        NodeList elementPropChild = item.getChildNodes();
        for(int k=0 ; k< elementPropChild.getLength();k++) {
            Node collectionProp  = elementPropChild.item(k);
            if (collectionProp.getNodeType() == Node.ELEMENT_NODE) {
                if ("collectionProp".equals(collectionProp.getNodeName())) {
                    NodeList elementsPropCollection = collectionProp.getChildNodes();
                    for(int i =0; i < elementsPropCollection.getLength();i++) {
                        Node elementProp = elementsPropCollection.item(i);
                        if(elementProp.getNodeType() == Node.ELEMENT_NODE) {
                            if("elementProp".equals(elementProp.getNodeName())){
                                changeValueHeadersParameter(elementProp,dependencies);
                            }
                        }
                    }
                }
            }
        }
    }

    public void replacementsInQueryParameters_PostData(Node item, List<Edge> dependencies,String name) {
        NodeList elementPropChild = item.getChildNodes();
        for(int k=0 ; k< elementPropChild.getLength();k++) {
            Node collectionProp  = elementPropChild.item(k);
            if (collectionProp.getNodeType() == Node.ELEMENT_NODE) {
                if ("collectionProp".equals(collectionProp.getNodeName())) {
                    NodeList elementsPropCollection = collectionProp.getChildNodes();
                    for(int i =0; i < elementsPropCollection.getLength();i++) {
                        Node elementProp = elementsPropCollection.item(i);
                        if(elementProp.getNodeType() == Node.ELEMENT_NODE) {
                            if("elementProp".equals(elementProp.getNodeName())){
                                if(name.equals("postData")) {
                                    changeValueQueryParameter(elementProp,dependencies);
                                }else {
                                    changeValueQueryParameter(elementProp, dependencies);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void changeValueHeadersParameter(Node elementProp, List<Edge> dependencies){
        String name = elementProp.getAttributes().getNamedItem("name").getTextContent();
        String value= "";
        for(Edge edge: dependencies) {
            if(edge.getClass().equals(EdgeHeader.class)){
                EdgeHeader edgeHeader = (EdgeHeader)edge;
                if(name.equals(edgeHeader.header_name)) {
                    if(!edgeHeader.dependency.value.equals("manually_inserted") && !edgeHeader.dependency.value.equals("manually_csv")) {
                        if (!name.equals("Authorization"))
                            value = "${" + edgeHeader.dependency.name + "_" + edgeHeader.from_index + "}";
                        else
                            value = "Bearer ${" + edgeHeader.dependency.name + "_" + edgeHeader.from_index + "}";
                    }
                    else
                        value = edgeHeader.dependency.name;

                    break;
                }
            }
        }

        if("".equals(value)) {return;}

        NodeList stringProp = elementProp.getChildNodes();
        for(int i=0; i< stringProp.getLength();i++) {
            Node item = stringProp.item(i);
            if(item.getNodeType() == Node.ELEMENT_NODE) {
                if("Header.value".equals(item.getAttributes().getNamedItem("name").getTextContent())) {
                    item.setTextContent(value);
                }
            }
        }
    }


    public void changeValueCookieHeaderParameter(Node stringvalue, JSONArray cookiesReplacements, JSONArray cookies ,String value)
    {
        for(Object item_cookies : cookies) {
            String name_item_cookies = ((JSONObject)item_cookies).get("name").toString();
            String value_item_cookies = ((JSONObject)item_cookies).get("value").toString();
            for(Object item_repCookie: cookiesReplacements) {
                String name_item_repCookie = ((JSONObject)item_repCookie).get("name").toString();
                String value_item_repCookie = ((JSONObject)item_repCookie).get("value").toString();
                if(name_item_cookies.equals(name_item_repCookie)) {
                    value = value.replace(value_item_cookies,value_item_repCookie);
                    break;
                }
            }
        }
        stringvalue.setTextContent(value);
    }

    public void changeValueQueryParameter (Node elementProp, List<Edge> dependencies) {
        NodeList stringProp = elementProp.getChildNodes();
        for(int i=0; i< stringProp.getLength();i++) {
            Node item = stringProp.item(i);
            String value= "";
            if(item.getNodeType() == Node.ELEMENT_NODE) {
                if("Argument.value".equals(item.getAttributes().getNamedItem("name").getTextContent())) {
                    String name = elementProp.getAttributes().getNamedItem("name").getTextContent();
                    // TO DO : CHECK UE POST DATA
                    for(Edge edge: dependencies) {
                        if(edge.getClass().equals(EdgeQueryParam.class)){
                            EdgeQueryParam edgeQueryParam = (EdgeQueryParam) edge;
                            if(name.equals(edgeQueryParam.query_param_name)) {
                                if(!edgeQueryParam.dependency.value.equals("manually_inserted")&&!edgeQueryParam.dependency.value.equals("manually_csv"))
                                    value = "${"+edgeQueryParam.dependency.name+"_"+edgeQueryParam.from_index+"}";
                                else
                                    value = edgeQueryParam.dependency.name;
                                break;
                            }
                        }

                    }
                    if(!"".equals(value)) {
                        item.setTextContent(value);
                    }
                }

                if("HTTPArgument.always_encode".equals(item.getAttributes().getNamedItem("name").getTextContent())){
                    item.setTextContent("false");
                }
            }
        }
    }

    public void addVariableAtList(ArrayList<String> varNameList, ArrayList<String> fromList, ArrayList<Integer> numReqList ,String name, String from,String num_req) {
        if(varNameList.contains(name)) {
            int[] indexes = IntStream.range(0, varNameList.size())
                    .filter(i -> varNameList.get(i).equals(name))
                    .toArray();

            for (int i = 0; i < indexes.length; i++) {
                if (fromList.get(indexes[i]).equals(from) && numReqList.get(indexes[i]) == Integer.parseInt(num_req)) {
                    return;
                }
            }
        }
        varNameList.add(name);
        fromList.add(from);
        numReqList.add(Integer.parseInt(num_req));

    }

    private void addVariablesWS(JSONObject item, String url ) {
        String from= item.get("From").toString();
        String fromResWS=item.get("FromResWS").toString();
        String varName= item.get("Name").toString();
        String regEx= item.get("regEx").toString();
        String value = item.get("Value").toString();
        variableWS vws = new variableWS(from,fromResWS,varName,regEx,value.substring(2,value.length()-1));
        if(variablesWS.get(url).containsKey(Integer.parseInt(fromResWS))){
            variablesWS.get(url).get(Integer.parseInt(fromResWS)).add(vws);
        }else {
            TreeSet<variableWS> list = new TreeSet <>(new Comparator<variableWS>() {
                @Override
                public int compare(variableWS v1, variableWS v2) {
                    if (
                            v1.fromResWS.equals(v2.fromResWS) &&
                                    v1.varName.equals(v2.varName)
                    )
                        return 0;
                    else
                        return -1;
                }
            });
            list.add(vws);
            variablesWS.get(url).put(Integer.parseInt(fromResWS),list);
        }
    }

    public String getNameFromValue ( String value) {
        int pos_g1 = value.indexOf("{");
        int pos_d =  value.indexOf("$");
        int pos_g2 = value.indexOf("}");
        if(pos_d+1 == pos_g1) {
            return  value.substring(pos_g1+1,pos_g2);
        }
        return "";
    }
    class variableWS {
        public String from;
        public String fromResWS;
        public String varName;
        public String regEx;
        public String pathExpr;
        public String value;

        public String calculatePathExprByValue(String value) {
            return "$."+value.replace("-",".");
        }

        public variableWS(String from, String fromResWS, String varName, String regEx,String value) {
            this.from= from;
            this.fromResWS=fromResWS;
            this.varName= varName;
            this.regEx=regEx;
            this.value=value;
            this.pathExpr=calculatePathExprByValue(value);
        }
        @Override
        public String toString(){
            return from+" "+fromResWS+" "+varName;
        }

    }

    class variable implements Comparable<variable>{
        public String from;
        public String varName;
        public String ApplyTo;
        public String FiledToCheck;
        public String pathExpr;
        public int num_req;

        public variable(String from, String varName, String applyTo, String filedToCheck, String pathExpr,int num_req) {
            this.from = from;
            this.varName = varName;
            this.ApplyTo = applyTo;
            this.FiledToCheck = filedToCheck;
            this.pathExpr = pathExpr;
            this.num_req =num_req;
        }
        public int compareTo(variable v) {

            if (this.from.equals(v.from) &&
                    this.pathExpr.equals(v.pathExpr) &&
                    this.varName.equals(v.varName) &&
                    this.num_req==v.num_req
            )
                return 0;
            else
                return -1;
        }

        @Override
        public String toString() {
            return this.varName+" \n"+this.from;
        }
    }

    private void appendRequestsAndResponsesMessage(Document doc, Node hashTree, String url, JSONObject replacement,int index_request_har,String FILENAME_HAR) {
        JSONParser parser = new JSONParser();
        try(FileReader fileReader = new FileReader(FILENAME_HAR, StandardCharsets.UTF_8)) {
            Object obj = parser.parse(fileReader);
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray entries = (JSONArray) ((JSONObject) jsonObject.get("log")).get("entries");
            int index_actual_requst = 0;
            for (JSONObject item : (Iterable<JSONObject>) entries) {
                JSONObject request = (JSONObject) item.get("request");
                String url_request = request.get("url").toString();
                if (url_request.equals(url)&& index_request_har==index_actual_requst) {
                    JSONArray messages =(JSONArray) item.get("_webSocketMessages");
                    int index_request = 0;
                    for(int i=0; i< messages.size();i++) {
                        JSONObject message = (JSONObject) messages.get(i);
                        String type = message.get("type").toString();
                        if("request".equals(type)) {
                            appendRequestAndResponse(doc,hashTree,message,replacement,index_request,url);
                            index_request++;
                        }
                    }
                }
                index_actual_requst++;
            }
        }catch (Exception e) {System.out.println(e);}
    }


    private void appendRequestAndResponse(Document doc, Node hashTree, JSONObject message, JSONObject replacement, int index,String url) {
        Node genericControllerRequest = doc.createElement("GenericController");
        ((Element)genericControllerRequest).setAttribute("guiclass","LogicControllerGui");
        ((Element)genericControllerRequest).setAttribute("testclass","GenericController");
        ((Element)genericControllerRequest).setAttribute("testname","Request "+index);
        ((Element)genericControllerRequest).setAttribute("enabled","true");
        hashTree.appendChild(genericControllerRequest);

        Node hashTreeRequest = doc.createElement("hashTree");
        Node singleWrite = doc.createElement("eu.luminis.jmeter.wssampler.SingleWriteWebSocketSampler");
        ((Element)singleWrite).setAttribute("guiclass","eu.luminis.jmeter.wssampler.SingleWriteWebSocketSamplerGui");
        ((Element)singleWrite).setAttribute("testclass","eu.luminis.jmeter.wssampler.SingleWriteWebSocketSampler");
        ((Element)singleWrite).setAttribute("testname","Request");
        ((Element)singleWrite).setAttribute("enabled","true");

        Node boolPropTLS = doc.createElement("boolProp");
        ((Element)boolPropTLS).setAttribute("name","TLS");
        boolPropTLS.setTextContent("false");

        Node server = doc.createElement("stringProp");
        ((Element)server).setAttribute("name","server");
        server.setTextContent("");

        Node port = doc.createElement("stringProp");
        ((Element)port).setAttribute("name","port");
        port.setTextContent("80");

        Node path = doc.createElement("stringProp");
        ((Element)path).setAttribute("name","path");
        path.setTextContent("");

        Node connecttimeout = doc.createElement("stringProp");
        ((Element)connecttimeout).setAttribute("name","connectTimeout");
        connecttimeout.setTextContent("20000");

        Node boolPropBP = doc.createElement("boolProp");
        ((Element)boolPropBP).setAttribute("name","binaryPayload");
        boolPropBP.setTextContent("false");

        Node requestData = doc.createElement("stringProp");
        ((Element)requestData).setAttribute("name","requestData");
        String data =(String)message.get("data");
        requestData.setTextContent(replacementsInWSRequestData(data,replacement,index));

        Node newConnection = doc.createElement("boolProp");
        ((Element)newConnection).setAttribute("name","createNewConnection");
        newConnection.setTextContent("false");

        Node loadDataFromFile = doc.createElement("boolProp");
        ((Element)loadDataFromFile).setAttribute("name","loadDataFromFile");
        loadDataFromFile.setTextContent("false");

        Node dataFile = doc.createElement("stringProp");
        ((Element)dataFile).setAttribute("name","dataFile");
        dataFile.setTextContent("");

        singleWrite.appendChild(boolPropTLS);
        singleWrite.appendChild(server);
        singleWrite.appendChild(port);
        singleWrite.appendChild(path);
        singleWrite.appendChild(connecttimeout);
        singleWrite.appendChild(boolPropBP);
        singleWrite.appendChild(requestData);
        singleWrite.appendChild(newConnection);
        singleWrite.appendChild(loadDataFromFile);
        singleWrite.appendChild(dataFile);

        hashTreeRequest.appendChild(singleWrite);
        hashTreeRequest.appendChild(doc.createElement("hashTree"));
        hashTree.appendChild(hashTreeRequest);

        Node whileWSResponding = doc.createElement("WhileController");
        ((Element)whileWSResponding).setAttribute("guiclass","WhileControllerGui");
        ((Element)whileWSResponding).setAttribute("testclass","WhileController");
        ((Element)whileWSResponding).setAttribute("testname","While WebSocket is responding");
        ((Element)whileWSResponding).setAttribute("enabled","true");
        Node condition = doc.createElement("stringProp");
        ((Element)condition).setAttribute("name","WhileController.condition");
        condition.setTextContent("${__groovy(vars.get(\"webSocketResponding\") != \"false\")}");
        whileWSResponding.appendChild(condition);
        hashTreeRequest.appendChild(whileWSResponding);
        hashTreeRequest.appendChild(createHashTreeReadMessageLoop(doc,index,url));
        hashTreeRequest.appendChild(updateParamAfterRequestMessageNode(doc));
        hashTreeRequest.appendChild(doc.createElement("hashTree"));
        if(variablesWS.get(url).containsKey(index)) {
            for(variableWS vws :variablesWS.get(url).get(index)) {
                hashTreeRequest.appendChild(createJSONExtractor(doc,vws));
                hashTreeRequest.appendChild(doc.createElement("hashTree"));
            }
        }
    }

    private Node createJSONExtractor(Document doc, variableWS vws) {
        Node jpp = doc.createElement("JSONPostProcessor");
        ((Element)jpp).setAttribute("guiclass","JSONPostProcessorGui");
        ((Element)jpp).setAttribute("testClass","JSONPostProcessor");
        ((Element)jpp).setAttribute("testname",vws.varName+" Extractor");
        ((Element)jpp).setAttribute("enabled","true");

        Node referencenames = doc.createElement("stringProp");
        ((Element)referencenames).setAttribute("name","JSONPostProcessor.referenceNames");
        referencenames.setTextContent(vws.value);

        Node pathexpr = doc.createElement("stringProp");
        ((Element)pathexpr).setAttribute("name","JSONPostProcessor.jsonPathExprs");
        pathexpr.setTextContent(vws.pathExpr);

        Node num = doc.createElement("stringProp");
        ((Element)num).setAttribute("name","JSONPostProcessor.match_numbers");
        num.setTextContent("1");

        Node scope = doc.createElement("stringProp");
        ((Element)scope).setAttribute("name","Sample.scope");
        scope.setTextContent("variable");

        Node scopeVariable= doc.createElement("stringProp");
        ((Element)scopeVariable).setAttribute("name","Scope.variable");
        scopeVariable.setTextContent(vws.varName+"_dataResponse");

        Node defaultValue= doc.createElement("stringProp");
        ((Element)defaultValue).setAttribute("name","JSONPostProcessor.defaultValues");
        defaultValue.setTextContent("NOT_FOUND");

        jpp.appendChild(referencenames);
        jpp.appendChild(pathexpr);
        jpp.appendChild(num);
        jpp.appendChild(scope);
        jpp.appendChild(scopeVariable);
        jpp.appendChild(defaultValue);
        return jpp;
    }

    private Node updateParamAfterRequestMessageNode(Document doc) {
        Node jpp = doc.createElement("JSR223Sampler");
        ((Element)jpp).setAttribute("guiclass","TestBeanGUI");
        ((Element)jpp).setAttribute("testclass","JSR223Sampler");
        ((Element)jpp).setAttribute("testname","Update Params After Request Message");
        ((Element)jpp).setAttribute("enabled","true");
        Node language = doc.createElement("stringProp");
        ((Element)language).setAttribute("name","scriptLanguage");
        language.setTextContent("groovy");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cachekey = doc.createElement("stringProp");
        ((Element)cachekey).setAttribute("name","cacheKey");
        cachekey.setTextContent("true");

        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");
        script.setTextContent("//init webSocketResponding\n" +
                "vars.put(\"webSocketResponding\",\"true\");");
        jpp.appendChild(language);
        jpp.appendChild(cachekey);
        jpp.appendChild(parameters);
        jpp.appendChild(filename);
        jpp.appendChild(script);
        return jpp;
    }

    private String replacementsInWSRequestData(String oldData, JSONObject replacement,int index) {
        JSONArray requestWS = (JSONArray) replacement.get("RequestWS");
        String newData = oldData;
        if(replacement.size()==0) return oldData;
        for(Object item_request : requestWS) {
            Long num_item_req = (Long) ((JSONObject)item_request).get("NumReqWS");
            if(num_item_req == (index)) {
                String value = ((JSONObject)item_request).get("Value").toString();
                String name = (String) ((JSONObject)item_request).get("Name");
                newData = replaceAtomicJSONObject(newData,name,value);
            }
        }
        return newData;
    }

    private Node getSimpleReadNode(Document doc) {
        Node read = doc.createElement("eu.luminis.jmeter.wssampler.SingleReadWebSocketSampler");
        ((Element)read).setAttribute("guiclass","eu.luminis.jmeter.wssampler.SingleReadWebSocketSamplerGui");
        ((Element)read).setAttribute("testclass","eu.luminis.jmeter.wssampler.SingleReadWebSocketSampler");
        ((Element)read).setAttribute("testname","Response");
        ((Element)read).setAttribute("enabled","true");

        Node boolPropTLS = doc.createElement("boolProp");
        ((Element)boolPropTLS).setAttribute("name","TLS");
        boolPropTLS.setTextContent("false");

        Node server = doc.createElement("stringProp");
        ((Element)server).setAttribute("name","server");
        server.setTextContent("");

        Node port = doc.createElement("stringProp");
        ((Element)port).setAttribute("name","port");
        port.setTextContent("80");

        Node path = doc.createElement("stringProp");
        ((Element)path).setAttribute("name","path");
        path.setTextContent("");

        Node connecttimeout = doc.createElement("stringProp");
        ((Element)connecttimeout).setAttribute("name","connectTimeout");
        connecttimeout.setTextContent("20000");

        Node datatype = doc.createElement("stringProp");
        ((Element)datatype).setAttribute("name","dataType");
        datatype.setTextContent("Text");

        Node newConnection = doc.createElement("boolProp");
        ((Element)newConnection).setAttribute("name","createNewConnection");
        newConnection.setTextContent("false");


        Node readTimeout = doc.createElement("stringProp");
        ((Element)readTimeout).setAttribute("name","readTimeout");
        readTimeout.setTextContent("10000");

        Node optional = doc.createElement("boolProp");
        ((Element)optional).setAttribute("name","optional");
        optional.setTextContent("true");

        read.appendChild(boolPropTLS);
        read.appendChild(server);
        read.appendChild(port);
        read.appendChild(path);
        read.appendChild(connecttimeout);
        read.appendChild(datatype);
        read.appendChild(newConnection);
        read.appendChild(readTimeout);
        read.appendChild(optional);

        return read;
    }

    private Node createHashTreeReadMessageLoop (Document doc,int index,String url) {
        Node hashTree = doc.createElement("hashTree");
        Node read = getSimpleReadNode(doc);
        hashTree.appendChild(read);
        hashTree.appendChild(appendSaveTemporanealyNode(doc));
        hashTree.appendChild(IfNotisLastFrameNode(doc));
        hashTree.appendChild(hashTreeConcatenateResponse(doc));
        hashTree.appendChild(IfIsLastFrame(doc));
        if(variablesWS.get(url).containsKey(index)){
            hashTree.appendChild(hashTreeSave(doc,index,url));
        }
        return hashTree;
    }

    private Node hashTreeSave(Document doc,int index,String url) {
        Node hashTree = doc.createElement("hashTree");
        Node jpp = doc.createElement("JSR223Sampler");
        ((Element)jpp).setAttribute("guiclass","TestBeanGUI");
        ((Element)jpp).setAttribute("testclass","JSR223Sampler");
        ((Element)jpp).setAttribute("testname","Save");
        ((Element)jpp).setAttribute("enabled","true");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cachekey = doc.createElement("stringProp");
        ((Element)cachekey).setAttribute("name","cacheKey");
        cachekey.setTextContent("true");
        Node language = doc.createElement("stringProp");
        ((Element)language).setAttribute("name","scriptLanguage");
        language.setTextContent("javascript");

        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");
        String scriptText =" var str = vars.get(\"tmp\");\n" +
                "var re =/"+variablesWS.get(url).get(index).first().regEx+"/;\n" +
                "var found = str.match(re);\n" +
                "if(found != null) {\n" +
                "\tvars.put(\""+variablesWS.get(url).get(index).first().varName+"_dataResponse\",str);\n" +
                "\t}";
        Iterator<variableWS> iteratorWs = variablesWS.get(url).get(index).iterator();
        iteratorWs.next();
        while(iteratorWs.hasNext()) {
            variableWS item = iteratorWs.next();
            scriptText=scriptText+"\n"+
                    "re =/"+item.regEx+"/;\n" +
                    "found = str.match(re);\n" +
                    "if(found != null) {\n" +
                    "\tvars.put(\""+item.varName+"_dataResponse\",str);\n" +
                    "\t}";
        }
        script.setTextContent(scriptText);
        jpp.appendChild(cachekey);
        jpp.appendChild(parameters);
        jpp.appendChild(filename);
        jpp.appendChild(language);
        jpp.appendChild(script);
        hashTree.appendChild(jpp);
        hashTree.appendChild(doc.createElement("hashTree"));
        return hashTree;
    }

    private Node IfIsLastFrame(Document doc){
        Node Ifcontroller = doc.createElement("IfController");
        ((Element)Ifcontroller).setAttribute("guiclass","IfControllerPanel");
        ((Element)Ifcontroller).setAttribute("testclass","IfController");
        ((Element)Ifcontroller).setAttribute("testname","IF Is Last Frame");
        ((Element)Ifcontroller).setAttribute("enabled","true");
        Node condition = doc.createElement("stringProp");
        ((Element)condition).setAttribute("name","IfController.condition");
        condition.setTextContent("${__groovy(vars.get(\"websocket.last_frame_final\").equals(\"true\"),)}");
        Node evaluateAll = doc.createElement("boolProp");
        ((Element)evaluateAll).setAttribute("name","IfController.evaluateAll");
        evaluateAll.setTextContent("false");
        Node useExpression = doc.createElement("boolProp");
        ((Element)useExpression).setAttribute("name","IfController.useExpression");
        useExpression.setTextContent("true");
        Ifcontroller.appendChild(condition);
        Ifcontroller.appendChild(evaluateAll);
        Ifcontroller.appendChild(useExpression);
        return Ifcontroller;
    }

    private Node IfNotisLastFrameNode(Document doc) {
        Node Ifcontroller = doc.createElement("IfController");
        ((Element)Ifcontroller).setAttribute("guiclass","IfControllerPanel");
        ((Element)Ifcontroller).setAttribute("testclass","IfController");
        ((Element)Ifcontroller).setAttribute("testname","IF Not is Last Frame");
        ((Element)Ifcontroller).setAttribute("enabled","true");
        Node condition = doc.createElement("stringProp");
        ((Element)condition).setAttribute("name","IfController.condition");
        condition.setTextContent("${__groovy(!vars.get(\"websocket.last_frame_final\").equals(\"true\"),)}");
        Node evaluateAll = doc.createElement("boolProp");
        ((Element)evaluateAll).setAttribute("name","IfController.evaluateAll");
        evaluateAll.setTextContent("false");
        Node useExpression = doc.createElement("boolProp");
        ((Element)useExpression).setAttribute("name","IfController.useExpression");
        useExpression.setTextContent("true");
        Ifcontroller.appendChild(condition);
        Ifcontroller.appendChild(evaluateAll);
        Ifcontroller.appendChild(useExpression);
        return Ifcontroller;
    }

    private Node hashTreeConcatenateResponse(Document doc) {
        Node hashTree = doc.createElement("hashTree");
        Node whileIsNotTheLastFrame = doc.createElement("WhileController");
        ((Element)whileIsNotTheLastFrame).setAttribute("guiclass","WhileControllerGui");
        ((Element)whileIsNotTheLastFrame).setAttribute("testclass","WhileController");
        ((Element)whileIsNotTheLastFrame).setAttribute("testname","While is no the last frame");
        ((Element)whileIsNotTheLastFrame).setAttribute("enabled","true");
        Node condition = doc.createElement("stringProp");
        ((Element)condition).setAttribute("name","WhileController.condition");
        condition.setTextContent("${__groovy(!vars.get(\"websocket.last_frame_final\").equals(\"true\"),)}");
        whileIsNotTheLastFrame.appendChild(condition);
        hashTree.appendChild(whileIsNotTheLastFrame);
        Node hashTreeWhile= doc.createElement("hashTree");
        Node read = getSimpleReadNode(doc);
        hashTreeWhile.appendChild(read);
        Node hashTreeConcatenate = doc.createElement("hashTree");
        Node jpp = doc.createElement("JSR223PostProcessor");
        ((Element)jpp).setAttribute("guiclass","TestBeanGUI");
        ((Element)jpp).setAttribute("testclass","JSR223PostProcessor");
        ((Element)jpp).setAttribute("testname","Concatenate");
        ((Element)jpp).setAttribute("enabled","true");

        Node scriptL = doc.createElement("stringProp");
        ((Element)scriptL).setAttribute("name","scriptLanguage");
        scriptL.setTextContent("javascript");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cachekey = doc.createElement("stringProp");
        ((Element)cachekey).setAttribute("name","cacheKey");
        cachekey.setTextContent("true");

        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");
        script.setTextContent("var responsedata = prev.getResponseDataAsString();\n" +
                "var responsecode = ctx.getPreviousResult().getResponseCode(); \n" +
                "\n" +
                "vars.put(\"tmp\",vars.get(\"tmp\")+\'\'+responsedata);\n" +
                "if(responsecode === \"No response\") {\n" +
                "\tvars.put(\"webSocketResponding\",\"false\");\n" +
                "\t} ");
        jpp.appendChild(scriptL);
        jpp.appendChild(parameters);
        jpp.appendChild(filename);
        jpp.appendChild(cachekey);
        jpp.appendChild(script);

        hashTreeConcatenate.appendChild(jpp);
        hashTreeConcatenate.appendChild(doc.createElement("hashTree"));
        hashTreeWhile.appendChild(hashTreeConcatenate);
        hashTree.appendChild(hashTreeWhile);
        return hashTree;
    }


    private Node appendSaveTemporanealyNode(Document doc) {
        Node hashTree = doc.createElement("hashTree");
        Node jpp = doc.createElement("JSR223PostProcessor");
        ((Element)jpp).setAttribute("guiclass","TestBeanGUI");
        ((Element)jpp).setAttribute("testclass","JSR223PostProcessor");
        ((Element)jpp).setAttribute("testname","Save temporanealy");
        ((Element)jpp).setAttribute("enabled","true");

        Node scriptL = doc.createElement("stringProp");
        ((Element)scriptL).setAttribute("name","scriptLanguage");
        scriptL.setTextContent("javascript");

        Node parameters = doc.createElement("stringProp");
        ((Element)parameters).setAttribute("name","parameters");
        parameters.setTextContent("");

        Node filename = doc.createElement("stringProp");
        ((Element)filename).setAttribute("name","filename");
        filename.setTextContent("");

        Node cachekey = doc.createElement("stringProp");
        ((Element)cachekey).setAttribute("name","cacheKey");
        cachekey.setTextContent("true");

        Node script = doc.createElement("stringProp");
        ((Element)script).setAttribute("name","script");
        script.setTextContent("var responsedata = prev.getResponseDataAsString();\n" +
                "var responsecode = ctx.getPreviousResult().getResponseCode(); \n" +
                "\n" +
                "vars.put(\"tmp\",responsedata);\n" +
                "\n" +
                "if(responsecode === \"No response\") {\n" +
                "\n" +
                "\tvars.put(\"webSocketResponding\",\"false\");\n" +
                "\t}");
        jpp.appendChild(scriptL);
        jpp.appendChild(parameters);
        jpp.appendChild(filename);
        jpp.appendChild(cachekey);
        jpp.appendChild(script);
        hashTree.appendChild(jpp);
        hashTree.appendChild(doc.createElement("hashTree"));
        return hashTree;
    }


    public static void main(String[]args) {

    }
}

