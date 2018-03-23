package me.samng.myreads.api.routes;

import com.google.cloud.datastore.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import me.samng.myreads.api.DatastoreHelpers;
import me.samng.myreads.api.entities.FollowedListEntity;

import java.util.List;

public class FollowedListRoute {
    private FollowedListEntity getFollowedListIfUserOwnsIt(
        Datastore datastore,
        long userId,
        long listId) {

        Key key = DatastoreHelpers.newFollowedListKey(listId);
        Entity entity = datastore.get(key);
        if (entity == null) {
            return null;
        }
        FollowedListEntity followedListEntity = FollowedListEntity.fromEntity(entity);
        if (followedListEntity.userId != userId) {
            return null;
        }
        return followedListEntity;
    }

    // Get all followed lists for a given user - /users/{userId}/followedLists
    public void getAllFollowedLists(RoutingContext routingContext) {
        long userId;
        try {
            userId = Long.decode(routingContext.request().getParam("userId"));
        }
        catch (Exception e) {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "text/plain")
                .end("Invalid request parameters");
            return;
        }

        Datastore datastore = DatastoreHelpers.getDatastore();
        List<FollowedListEntity> results = DatastoreHelpers.getAllFollowedListsForUser(datastore, userId);

        routingContext.response()
            .putHeader("content-type", "text/plain")
            .end(Json.encode(results.toArray()));
    }

    // Post a new followed list - /users/{userId}/followedLists
    public void postFollowedList(RoutingContext routingContext) {
        FollowedListEntity followedListEntity;
        try {
            followedListEntity = Json.decodeValue(routingContext.getBody(), FollowedListEntity.class);
        }
        catch (Exception e) {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "text/plain")
                .end("Invalid request body");
            return;
        }
        long userId = Long.decode(routingContext.request().getParam("userId"));

        if (userId == followedListEntity.ownerId()) {
            // Can't follow your own list.
            routingContext.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "text/plain")
                .end("Can't follow your own list");
            return;
        }

        FullEntity<IncompleteKey> insertEntity = Entity.newBuilder(DatastoreHelpers.newFollowedListKey())
            .set("userId", userId)
            .set("listId", followedListEntity.listId())
            .set("ownerId", followedListEntity.ownerId())
            .build();
        Datastore datastore = DatastoreHelpers.getDatastore();
        Entity addedEntity = datastore.add(insertEntity);

        routingContext.response()
            .setStatusCode(HttpResponseStatus.CREATED.code())
            .putHeader("content-type", "text/plain")
            .end(Long.toString(addedEntity.getKey().getId()));
    }

    // Delete a user, /users/{userId}/readingLists/{readingListId}
    public void deleteFollowedList(RoutingContext routingContext) {
        long listId;
        long userId;
        try {
            listId = Long.decode(routingContext.request().getParam("followedListId"));
            userId = Long.decode(routingContext.request().getParam("userId"));
        }
        catch (Exception e) {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "text/plain")
                .end("Invalid request parameters");
            return;
        }

        Datastore datastore = DatastoreHelpers.getDatastore();
        if (getFollowedListIfUserOwnsIt(datastore, userId, listId) == null) {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                .putHeader("content-type", "text/plain")
                .end();
            return;
        }
        datastore.delete(DatastoreHelpers.newFollowedListKey(listId));

        routingContext.response()
            .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
            .putHeader("content-type", "text/plain")
            .end();
    }
}
