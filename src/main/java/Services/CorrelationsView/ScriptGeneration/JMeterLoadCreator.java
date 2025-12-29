package Services.CorrelationsView.ScriptGeneration;

import Entity.UltimateThreadGroup;
import Properties.Paths;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
/**
 * Creates a complete JMeter test plan wrapping multiple user behaviors (E2E GUI tests)
 * chosen by the tester.
 *
 * <p>This class generates a single JMX file containing multiple Ultimate Thread Groups
 * corresponding to different user scripts.
 *
 * <p>It handles:
 * <ul>
 *   <li>TestPlan creation</li>
 *   <li>CookieManager addition</li>
 *   <li>UltimateThreadGroup creation</li>
 *   <li>Thread group mapping and aggregation</li>
 *   <li>Writing the final JMX file to disk</li>
 * </ul>
 */
public class JMeterLoadCreator {
    /**
     * Adds an HTTP Cookie Manager to the test plan.
     *
     * @param document XML document being modified
     * @param hashTree parent hashTree element where the CookieManager is appended
     */
    private static final String testplan = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.4.3\">\n" +
            "  <hashTree>\n" +
            "    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan (E2ELoader is helping you)\" enabled=\"true\">\n" +
            "      <stringProp name=\"TestPlan.comments\">Script generated through E2ELoader tool :D</stringProp>\n" +
            "      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n" +
            "      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n" +
            "      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n" +
            "      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n" +
            "        <collectionProp name=\"Arguments.arguments\"/>\n" +
            "      </elementProp>\n" +
            "      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n" +
            "    </TestPlan>\n" +
            "    <hashTree/>\n" +
            "  </hashTree>\n" +
            "</jmeterTestPlan>";


    /**
     * Adds an HTTP Cookie Manager to the test plan.
     *
     * @param document XML document being modified
     * @param hashTree parent hashTree element where the CookieManager is appended
     */

    private static void addCookieManager(Document document,Element hashTree){
        /*
        <CookieManager guiclass="CookiePanel" testclass="CookieManager" testname="HTTP Cookie Manager" enabled="true">
        <collectionProp name="CookieManager.cookies"/>
        <boolProp name="CookieManager.clearEachIteration">true</boolProp>
        <boolProp name="CookieManager.controlledByThreadGroup">false</boolProp>
      </CookieManager>
      <hashTree/>*/
        Element cookieManager = document.createElement("CookieManager");
        cookieManager.setAttribute("guiclass","CookiePanel");
        cookieManager.setAttribute("testclass","CookieManager");
        cookieManager.setAttribute("testname","HTTP Cookie Manager");
        cookieManager.setAttribute("enalbed","true");

        Element collectionProp = document.createElement("collectionProp");
        collectionProp.setAttribute("name","CookieManager.cookies");

        Element boolProp1 = document.createElement("boolProp");
        boolProp1.setAttribute("name","CookieManager.clearEachIteration");
        boolProp1.setTextContent("true");

        Element boolProp2 = document.createElement("boolProp");
        boolProp2.setAttribute("name","CookieManager.controlledByThreadGroup");
        boolProp2.setTextContent("false");

        cookieManager.appendChild(collectionProp);
        cookieManager.appendChild(boolProp1);
        cookieManager.appendChild(boolProp2);

        hashTree.appendChild(cookieManager);

    }
    /**
     * Converts a string representation of XML to a Document object.
     *
     * @param xml XML string (not used directly in current implementation)
     * @return parsed Document object or null on error
     */
    private static Document converStringToXMLDocument(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try{
            builder=factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(testplan)));
            return document;
        }catch (Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public static void runJMeterCreator(ArrayList<UltimateThreadGroup> threadsgrup, String outfile) throws ParserConfigurationException, IOException, TransformerException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element jmeterTestPlan = document.createElement("jmeterTestPlan");
        jmeterTestPlan.setAttribute("version","1.2");
        jmeterTestPlan.setAttribute("properties","5.0");
        jmeterTestPlan.setAttribute("jmeter","5.4.3");
        document.appendChild(jmeterTestPlan);
        Element hashTreeJmeterTestPlan = document.createElement("hashTree");
        jmeterTestPlan.appendChild(hashTreeJmeterTestPlan);
        hashTreeJmeterTestPlan.appendChild(returnTestPlanNode(document));
        Element hashTree = document.createElement("hashTree");
        hashTreeJmeterTestPlan.appendChild(hashTree);
        addCookieManager(document,hashTreeJmeterTestPlan);
        HashMap<String,ArrayList<UltimateThreadGroup>> threadGroupsMap = ConvertThreadGroupArrayToMap(threadsgrup);
        for(String filename : threadGroupsMap.keySet()) {
            addUltimateThreadGroup(document,hashTree,filename,threadGroupsMap.get(filename));
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult( new File(Paths.scripts_saved_path +"/"+outfile+".jmx"));
        transformer.transform(domSource,streamResult);

    }
    /**
     * Adds an Ultimate Thread Group to the test plan.
     *
     * @param doc       XML document being modified
     * @param hashTree  parent hashTree element to append the thread group
     * @param filename  name of the script / user behavior
     * @param list      list of UltimateThreadGroup objects to add
     * @throws IOException in case of file reading errors
     */
    private static void  addUltimateThreadGroup (Document doc, Element hashTree,String filename ,ArrayList<UltimateThreadGroup> list) throws IOException {

        Element ultimateThreadTroup = doc.createElement("kg.apc.jmeter.threads.UltimateThreadGroup");
        ultimateThreadTroup.setAttribute("guiclass","kg.apc.jmeter.threads.UltimateThreadGroupGui");
        ultimateThreadTroup.setAttribute("testclass","kg.apc.jmeter.threads.UltimateThreadGroup");
        ultimateThreadTroup.setAttribute("testname",filename);
        ultimateThreadTroup.setAttribute("enabled","true");

        Element collectionPropRoot = doc.createElement("collectionProp");
        collectionPropRoot.setAttribute("name","ultimatethreadgroupdata");
        for(UltimateThreadGroup td : list) {

            Element collectionProp = doc.createElement("collectionProp");
            collectionProp.setAttribute("name", String.valueOf(new Random().nextInt(1000000)));
            Element stringpropTC = doc.createElement("stringProp");
            stringpropTC.setAttribute("name", td.getThreadsCount());
            stringpropTC.setTextContent(td.getThreadsCount());
            Element stripropID = doc.createElement("stringProp");
            stripropID.setAttribute("name", td.getInitialDelay());
            stripropID.setTextContent(td.getInitialDelay());

            Element stringpropST = doc.createElement("stringProp");
            stringpropST.setTextContent(td.getStartupTime());
            stringpropST.setAttribute("name", td.getStartupTime());

            Element stringpropHL = doc.createElement("stringProp");
            stringpropHL.setAttribute("name", td.getHoldLoadFor());
            stringpropHL.setTextContent(td.getHoldLoadFor());

            Element stringPropSW = doc.createElement("stringProp");
            stringPropSW.setTextContent(td.getShutDownTime());
            stringPropSW.setAttribute("name", td.getShutDownTime());

            collectionProp.appendChild(stringpropTC);
            collectionProp.appendChild(stripropID);
            collectionProp.appendChild(stringpropST);
            collectionProp.appendChild(stringpropHL);
            collectionProp.appendChild(stringPropSW);
            collectionPropRoot.appendChild(collectionProp);
        }
        ultimateThreadTroup.appendChild(collectionPropRoot);

        Element elementProp = doc.createElement("elementProp");
        elementProp.setAttribute("name","ThreadGroup.main_controller");
        elementProp.setAttribute("elementType","LoopController");
        elementProp.setAttribute("guiclass","LoopControllerPanel");
        elementProp.setAttribute("testclass","LoopController");
        elementProp.setAttribute("testname","Loop Controller");
        elementProp.setAttribute("enabled","true");

        Element boolProp = doc.createElement("boolProp");
        boolProp.setAttribute("name","LoopController.continue_forever");
        boolProp.setTextContent("false");

        Element intProp = doc.createElement("intProp");
        intProp.setAttribute("name","LoopController.loops");
        intProp.setTextContent("-1");

        elementProp.appendChild(boolProp);
        elementProp.appendChild(intProp);
        ultimateThreadTroup.appendChild(elementProp);

        Element stringPropError = doc.createElement("stringProp");
        stringPropError.setAttribute("name","ThreadGroup.on_sample_error");
        stringPropError.setTextContent("continue");
        ultimateThreadTroup.appendChild(stringPropError);

        hashTree.appendChild(ultimateThreadTroup);
        Node importedNode = doc.importNode(returnNodehashScriptByName(filename),true);
        hashTree.appendChild(importedNode);
    }

    /**
     * Converts an array of UltimateThreadGroup objects into a map keyed by filename.
     *
     * @param threadGroups list of UltimateThreadGroup objects
     * @return map with filename as key and list of thread groups as value
     */

    public static HashMap<String,ArrayList<UltimateThreadGroup>> ConvertThreadGroupArrayToMap(ArrayList<UltimateThreadGroup> threadGroups) {
        HashMap<String,ArrayList<UltimateThreadGroup>> result = new HashMap<>();
        for(UltimateThreadGroup td : threadGroups) {
            if(result.containsKey(td.getFilename())) {
                result.get(td.getFilename()).add(td);
            }else {
                result.put(td.getFilename(),new ArrayList<UltimateThreadGroup>());
                result.get(td.getFilename()).add(td);
            }
        }
        return result;
    }
    /**
     * Returns the hashTree node from an existing JMX script by filename.
     *
     * @param filename name of the JMX script
     * @return Node representing the hashTree or null if not found
     * @throws IOException if file cannot be read
     */
    private static Node returnNodehashScriptByName(String filename) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try(InputStream is = new FileInputStream(Paths.scripts_saved_path+"/"+filename))
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            NodeList thread = doc.getElementsByTagName("ThreadGroup");
            for(int i=0;i< thread.getLength();i++) {
                Node t = thread.item(i);
                Node hash  = t.getNextSibling().getNextSibling();
                if(hash.getNodeType() == Node.ELEMENT_NODE) {
                    if(hash.getNodeName().equals("hashTree")) {
                        return hash;
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Returns a TestPlan element for the JMeter test plan.
     *
     * @param doc XML document being modified
     * @return TestPlan element
     */
    private static Element returnTestPlanNode(Document doc) {
        Element TestPlan = doc.createElement("TestPlan");
        TestPlan.setAttribute("guiclass","TestPlanGui");
        TestPlan.setAttribute("testclass","TestPlan");
        TestPlan.setAttribute("testname","Test Plan (E2ELoader is helping you)");
        TestPlan.setAttribute("enabled","true");

        Element stringPropComments = doc.createElement("stringProp");
        stringPropComments.setAttribute("name","TestPlan.comments");
        stringPropComments.setTextContent("Script generated through E2ELoader tool :D ");

        Element boolPropFMode = doc.createElement("boolProp") ;
        boolPropFMode.setAttribute("name","TestPlan.functional_mode");
        boolPropFMode.setTextContent("false");

        Element boolProptd = doc.createElement("boolProp") ;
        boolProptd.setAttribute("name","TestPlan.serialize_threadgroups");
        boolProptd.setTextContent("false");

        Element elementProp = doc.createElement("elementProp");
        elementProp.setAttribute("name","TestPlan.user_defined_variables");
        elementProp.setAttribute("elementType","Arguments");
        elementProp.setAttribute("enabled","true");
        elementProp.setAttribute("guiclass","ArgumentsPanel");
        elementProp.setAttribute("testclass","Arguments");
        elementProp.setAttribute("testname","User Defined Variables");

        Element collectionProp = doc.createElement("collectionProp");
        collectionProp.setAttribute("name","Arguments.arguments");
        elementProp.appendChild(collectionProp);

        TestPlan.appendChild(stringPropComments);
        TestPlan.appendChild(boolPropFMode);
        TestPlan.appendChild(boolProptd);
        TestPlan.appendChild(elementProp);
        return TestPlan;
    }
}
