import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Consume a secured API deployed in WSO2 API Cloud using Client Credential Grant type
 */
class APIConsumer {

    private static LocalDateTime expiringTimeStamp;
    private static String accessToken;

    /**
     * Invoke token API and get the access token.
     */
    static void getAccessToken() {

        // client-id and client-secret of the application
        // replace these two values with the values of your application
        String clientId = "Izsy6jDa7sprF_miGCMTsfllF1Aa";
        String clientSecret = "LjZrwoC_ZoiOBtsewbN98cyBcJga";

        // token API URL
        String tokenUrl = "https://gateway.api.cloud.wso2.com/token";

        String auth = clientId + ":" + clientSecret;
        String content = "grant_type=client_credentials";
        BufferedReader reader = null;
        HttpsURLConnection connection = null;
        String authentication = Base64.getEncoder().encodeToString(auth.getBytes());

        try {
            URL url = new URL(tokenUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + authentication);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "application/json");
            PrintStream os = new PrintStream(connection.getOutputStream());
            os.print(content);
            os.close();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.lines().collect(Collectors.joining());
            // This response contains the access-token and the expiring period in seconds in a JSON response
            //{"access_token":"ff0062dc-2269-3407-a7ba-f893613640d7","scope":"am_application_scope default","token_type":"Bearer","expires_in":3372}

            JSONObject jsonOb = new JSONObject(response);

            // Extract access token from the response and set the value
            accessToken = jsonOb.getString("access_token");
            System.out.println(accessToken);
            // Extract the expiring period from the response
            int tokenValidityPeriod = jsonOb.getInt("expires_in");

            // Set the expiring time stamp by adding the validity period to the current time stamp
            expiringTimeStamp = LocalDateTime.now().plusSeconds(tokenValidityPeriod);

        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            connection.disconnect();

        }
    }

    /**
     * Gets the validity of the access token.
     *
     * @return the validity of access token.
     */
    static boolean isAccessTokenValid() {
        LocalDateTime currentTimeStamp = LocalDateTime.now();

        // Check curent time stamp is less than the token expiring time stamp
        if (expiringTimeStamp != null && currentTimeStamp.isBefore(expiringTimeStamp)) {
            return true;
        } else {
            return false;
        }
    }

    static void invokeAPI() {

        // Check the validity of the access-token
        // If the access-token is expired, request a new token
        boolean isAccessTokenValid = isAccessTokenValid();
        if (!isAccessTokenValid) {
            getAccessToken();
        }

        // Gateway URL
        String gatewayURL = "https://gateway.api.cloud.wso2.com/t/";

        // Replace the tenantDomain with the the value of your Tenant Domain
        String tenantDomain = "testliveorg/";
        // Replace the API context according to your API
        String apiContext = "wb/1.0.0/countries/usa";

        HttpsURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(gatewayURL + tenantDomain + apiContext);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.lines().collect(Collectors.joining());

            // Print the API response
            System.out.println(response);

        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            connection.disconnect();
        }
    }

    public static void main(String[] args) {

        // Invoke the API first time
        // In this case it will get an access token from the token API and then invoke the API
        invokeAPI();

        // Invoke the API second time
        // This time it will use the existing access token (if it is not expired) and then invoke the API
        invokeAPI();

    }

}
