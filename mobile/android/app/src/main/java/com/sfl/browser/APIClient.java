package com.sfl.browser;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class APIClient {
    private static final String TAG = "APIClient";
    private static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    /**
     * Fetches raw JSON from the farm API endpoint
     * @param apiUrl The full API URL to call
     * @param apiKey The API key for authentication (x-api-key header)
     * @return Raw JSON response as string, or null if request failed
     */
    public static String fetchRawJSON(String apiUrl, String apiKey) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "Starting API call to: " + apiUrl);
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-api-key", apiKey);
            Log.d(TAG, "Added x-api-key header for authentication");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(connection);
                Log.d(TAG, "API Response received. Size: " + response.length() + " bytes");
                return response;
            } else {
                Log.e(TAG, "API call failed with response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "API call failed with exception: " + e.getMessage(), e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads the response from an HTTP connection
     * @param connection The HttpURLConnection to read from
     * @return Response body as string
     */
    private static String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
