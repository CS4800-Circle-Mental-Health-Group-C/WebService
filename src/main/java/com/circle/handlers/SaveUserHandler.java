package com.circle.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.circle.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.Collections;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class SaveUserHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private static final String USERS_TABLE_NAME = System.getenv("USERS_TABLE_NAME");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent request, Context context) {

    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

    LambdaLogger logger = context.getLogger();

    String body = request.getBody();

    logger.log("body: " + body);

    // Create a DynamoDbClient object
    DynamoDbClient ddb = DynamoDbClient.builder().region(Region.US_WEST_1).build();
    DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();

    // Parse the JSON input
    // If the request is valid, then create a new item in the table
    try {

      // If the request body is not empty, then create a new item in the table
      if (body != null) {
        User user = new User();
        Gson gson = new Gson();
        User requestObj = gson.fromJson(body, User.class);
        user.setId(requestObj.getId());
        user.setName(requestObj.getName());
        user.setEmail(requestObj.getEmail());
        user.setPhone(requestObj.getPhone());

        DynamoDbTable<User> userTable =
            enhancedClient.table(USERS_TABLE_NAME, TableSchema.fromClass(User.class));

        userTable.putItem(user);

        return response
            .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
            .withStatusCode(200)
            .withBody(gson.toJson(user));
      } else {
        return response
            .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
            .withStatusCode(403)
            .withBody("Invalid request body");
      }

    } catch (JsonParseException ex) {
      response.setStatusCode(400);
      response.setBody("Failed to parse JSON: " + ex.getMessage());
      return response;
    }
  }
}
