package com.circle.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.circle.models.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.json.JSONObject;
import org.json.JSONTokener;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GetUserHandler implements RequestStreamHandler {
  private static final String USERS_TABLE_NAME = System.getenv("USERS_TABLE_NAME");

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    // Read the request from the input stream
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    JSONObject responseJson = new JSONObject();

    // Create a DynamoDbClient object
    DynamoDbClient ddb = DynamoDbClient.builder().region(Region.US_WEST_1).build();
    DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();

    // Parse the JSON input
    // If the request is valid, then create a new item in the table
    User result = null;

    try {
      JSONTokener tokener = new JSONTokener(reader);
      JSONObject event = new JSONObject(tokener);
      JSONObject responseBody = new JSONObject();
      DynamoDbTable<User> userTable =
          enhancedClient.table(USERS_TABLE_NAME, TableSchema.fromClass(User.class));

      if (event.get("pathParameters") != null) {

        JSONObject pathParameters = (JSONObject) event.get("pathParameters");

        if (pathParameters.get("id") != null) {

          String id = (String) pathParameters.get("id");
          result =
              userTable.getItem(
                  (GetItemEnhancedRequest.Builder requestBuilder) ->
                      requestBuilder.key(Key.builder().partitionValue(id).build()));
        }
      } else if (event.get("queryStringParameters") != null) {

        JSONObject queryStringParameters = (JSONObject) event.get("queryStringParameters");
        if (queryStringParameters.get("id") != null) {

          String id = (String) queryStringParameters.get("id");
          result =
              userTable.getItem(
                  (GetItemEnhancedRequest.Builder requestBuilder) ->
                      requestBuilder.key(Key.builder().partitionValue(id).build()));
        }
      }

      if (result != null) {
        User user = new User();
        user.setId(result.getId());
        user.setName(result.getName());
        user.setEmail(result.getEmail());
        user.setPhone(result.getPhone());

        responseBody.put("User", user);
        responseJson.put("statusCode", "200");
      } else {
        responseBody.put("message", "User not found");
        responseJson.put("statusCode", "404");
      }

      JSONObject headerJson = new JSONObject();
      headerJson.put("x-custom-header", "my custom header value");
      responseJson.put("headers", headerJson);
      responseJson.put("body", responseBody.toString());

    } catch (Exception pex) {
      responseJson.put("statusCode", "400");
      responseJson.put("exception", pex);
    }

    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    writer.write(responseJson.toString());
    writer.close();
  }
}
