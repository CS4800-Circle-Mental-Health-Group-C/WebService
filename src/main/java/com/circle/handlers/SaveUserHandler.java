package com.circle.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.circle.models.User;
import com.google.gson.Gson;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class SaveUserHandler implements RequestStreamHandler {
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
    try {
      JSONTokener tokener = new JSONTokener(reader);
      JSONObject event = new JSONObject(tokener);

      // If the request body is not empty, then create a new item in the table
      if (event.get("body") != null) {
        User user = new User();
        Gson gson = new Gson();
        User request = gson.fromJson((String) event.get("body"), User.class);
        user.setId(request.getId());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        DynamoDbTable<User> userTable =
            enhancedClient.table(USERS_TABLE_NAME, TableSchema.fromClass(User.class));

        userTable.putItem(user);
      }

      JSONObject responseBody = new JSONObject();
      responseBody.put("message", "User saved successfully");

      JSONObject headerJson = new JSONObject();
      headerJson.put("x-custom-header", "my custom header value");

      responseJson.put("statusCode", "200");
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
