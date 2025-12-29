package Services.CorrelationsView.ScriptGeneration;
import Properties.Paths;
import okhttp3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Handles the conversion of HAR files into JMeter JMX scripts using
 * the BlazeMeter Converter public API.
 *
 * <p>The conversion workflow is:
 * <ol>
 *   <li>Upload a HAR files</li>
 *   <li>Trigger the conversion process</li>
 *   <li>Poll conversion status until finished</li>
 *   <li>Download the generated JMX file</li>
 *   <li>Save it locally</li>
 * </ol>
 *
 * <p>This class acts as an HTTP client wrapper and orchestrator
 * for the full conversion lifecycle.
 */

public class Converter {
    /**
     * Main orchestration method that converts a HAR file into a JMX script.
     *
     * <p>The method:
     * <ul>
     *   <li>Uploads the HAR file</li>
     *   <li>Starts the conversion process</li>
     *   <li>Polls the status endpoint until conversion is finished</li>
     *   <li>Downloads the resulting JMX file</li>
     *   <li>Saves it to the configured scripts directory</li>
     * </ul>
     *
     * @param filename name of the HAR file
     * @param filehar  HAR file identifier
     * @param path     directory where the HAR file is located
     */

    public static void runMain (String filename, String filehar,String path) throws IOException, ParseException, InterruptedException, ParserConfigurationException, TransformerException, SAXException {
        Response uploadResponse = UploadRequest(filehar,path+"/hars");
        String bodyresponse = uploadResponse.body().string();
        Response converResponse = convertRequest(bodyresponse);
        String convertResponse = converResponse.body().string();
        Response statusResponse = statusRequest(convertResponse);
        String statusR = statusResponse.body().string();
        while(!getStatus(statusR).equals("FINISHED")){
            TimeUnit.SECONDS.sleep(5);
            statusResponse = statusRequest(convertResponse);
            statusR = getStatus(statusResponse.body().string());
        }
        String downResponse = downloadRequest(getoUrl(statusR));
        saveJMXFile(downResponse,filename);
    }

    /**
     * Saves the converted JMX content to disk.
     *
     * @param jmx      JMX file content returned by the converter
     * @param filename original HAR filename (used to derive output name)
     */
    public static void saveJMXFile(String jmx,String filename) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        /*// Parse the given input
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(jmx)));

        // Write the parsed document to an xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        StreamResult result =  new StreamResult(new File("icst-e2e-loader\\"+"src\\SINGLE_SCRIPTS_BEHAVIOR\\"+getNamefileHar(filename)+".jmx"));
        transformer.transform(source, result);
    */
        System.out.println(Paths.scripts_saved_path+"/"+getNamefileHar(filename)+".jmx");
        try(FileWriter fw = new FileWriter(Paths.scripts_saved_path+"/"+getNamefileHar(filename)+".jmx")) {
            fw.write(jmx);
        }catch (IOException e){
            //System.out.println("eccezzione catturata: "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Uploads a HAR file to the BlazeMeter converter service.
     *
     * @param filename HAR file name
     * @param path     directory containing the HAR file
     * @return HTTP response containing a public token
     */
    public static Response UploadRequest(String filename, String path) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file",filename,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(path+"/"+filename)))
                .build();
        Request request = new Request.Builder()
                .url("https://converter.blazemeter.com/api/converter/v1/upload")
                .method("POST", body)
                .addHeader("authority", "converter.blazemeter.com")
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("accept-language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("origin", "https://converter.blazemeter.com")
                .addHeader("referer", "https://converter.blazemeter.com/")
                .addHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Google Chrome\";v=\"102\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"macOS\"")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.61 Safari/537.36")
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    /**
     * Triggers the conversion process for a previously uploaded HAR file.
     *
     * @param res upload response JSON containing the public token
     * @return HTTP response for the conversion request
     */
    public static Response convertRequest (String res) throws ParseException, IOException {
        String token = getToken(res);
        OkHttpClient client = new OkHttpClient().newBuilder()

                .build();

        MediaType mediaType = MediaType.parse("text/plain");

        RequestBody body = RequestBody.create(mediaType, "");

        Request request = new Request.Builder()

                .url("https://converter.blazemeter.com/api/converter/v1/"+token+"/convert")

                .method("POST", body)

                .addHeader("authority", "converter.blazemeter.com")

                .addHeader("accept", "application/json, text/plain, */*")

                .addHeader("accept-language", "it,it-IT;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")

                .addHeader("content-length", "0")

                .addHeader("origin", "https://converter.blazemeter.com")

                .addHeader("referer", "https://converter.blazemeter.com/")

                .addHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Microsoft Edge\";v=\"102\"")

                .addHeader("sec-ch-ua-mobile", "?0")

                .addHeader("sec-ch-ua-platform", "\"Windows\"")

                .addHeader("sec-fetch-dest", "empty")

                .addHeader("sec-fetch-mode", "cors")

                .addHeader("sec-fetch-site", "same-origin")

                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.39")

                .build();

                Response response =  client.newCall(request).execute();
                return response;
    }

    /**
     * Downloads the generated JMX file from the given URL.
     *
     * @param url download URL returned by the converter
     * @return JMX file content as a string
     */

    public static String downloadRequest(String url) throws ParseException, IOException {
        String bodyString="";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .method("GET",  null)
                .build();
        try (Response response = client.newCall(request).execute()){
            if(!response.isSuccessful()){
                throw new IOException("Unexpected code " + response);
            }
            ResponseBody responsebody = response.body();
            if(responsebody != null){
                bodyString = responsebody.string();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return bodyString;
    }
    /**
     * Retrieves the conversion status from the API.
     *
     * @param res response JSON containing the public token
     * @return HTTP response with status information
     */
    public static Response statusRequest(String res) throws ParseException, IOException {
        String token = getToken(res);
        OkHttpClient client = new OkHttpClient().newBuilder()

                .build();

        MediaType mediaType = MediaType.parse("text/plain");

        //RequestBody body = RequestBody.create(mediaType, "");

        Request request = new Request.Builder()

                .url("https://converter.blazemeter.com/api/converter/v1/"+token+"/status")

                .method("GET", null)

                .addHeader("authority", "converter.blazemeter.com")

                .addHeader("accept", "application/json, text/plain, */*")

                .addHeader("accept-language", "it,it-IT;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")

                .addHeader("referer", "https://converter.blazemeter.com/")

                .addHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Microsoft Edge\";v=\"102\"")

                .addHeader("sec-ch-ua-mobile", "?0")

                .addHeader("sec-ch-ua-platform", "\"Windows\"")

                .addHeader("sec-fetch-dest", "empty")

                .addHeader("sec-fetch-mode", "cors")

                .addHeader("sec-fetch-site", "same-origin")

                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.39")

                .build();

        Response response = client.newCall(request).execute();
        return response;
    }
    /**
     * Extracts the public token from a JSON response.
     *
     * @param response JSON response returned by the API
     * @return public token used to track conversion
     */
    public static String getToken(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);
        return json.get("publicToken").toString();
    }

    /**
     * Returns the base filename without extension.
     *
     * @param filename input file name
     * @return filename without extension
     */

    public static String getNamefileHar(String filename) {
        return filename.split("\\.")[0];
    }
    /**
     * Extracts the conversion status from a JSON response.
     *
     * @param response status response JSON
     * @return conversion status (e.g. FINISHED)
     */
    public static String getStatus(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);
        JSONObject data = (JSONObject) json.get("data");
        String status = (String)data.get("status");
        return status;
    }

    /**
     * Extracts the output download URL from the API response.
     *
     * @param response JSON response containing conversion result data
     * @return URL used to download the generated JMX file
     */
    public static String getoUrl(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);
        JSONObject data = (JSONObject) json.get("data");
        String ourl = (String)data.get("oUrl");
        return ourl;
    }

}
