package Services.ResponseAnalyzer;

import com.squareup.okhttp.*;

import java.io.IOException;
/**
 * Provides utilities to generate a JSON Schema from a JSON string.
 *
 * <p>This class uses an external service (running on localhost:4000) to convert a JSON payload
 * into a corresponding JSON Schema representation.
 */
public class JsonSchemaGenerator {
    /**
     * Generates a JSON Schema for the given JSON string by sending it to an external service.
     *
     * @param json the JSON string to generate a schema for
     * @return the JSON Schema as a string
     * @throws IOException if there is a network or I/O error while contacting the schema service
     */
    public static String generateJSONSchema(String json) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        Request request = new Request.Builder()
                .url("http://localhost:4000/schema")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
