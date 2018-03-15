import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import me.samng.myreads.api.MainVerticle;
import me.samng.myreads.api.entities.ReadingListElementEntity;
import me.samng.myreads.api.entities.TagEntity;
import me.samng.myreads.api.entities.UserEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ReadingListElementRouteTest {
    private Vertx vertx;
    private long userId = -1;
    private long rleId = -1;
    private long tagId = -1;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName(),
            context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void getAllRLEs(TestContext context) {
        final Async async = context.async();

        WebClient client = WebClient.create(vertx);

        UserEntity entity = new UserEntity();
        entity.email = "listtest@test.com";
        entity.name = "testuser";
        entity.userId = "testId";

        Future<Long> postFut = TestHelper.postUser(context, client, entity, 201);
        Future<Long> getAllFut = postFut.compose(userId -> {
                return TestHelper.getAllReadingListElements(context, client, userId, 200).map(userId);
            });
        getAllFut.compose(userId -> {
            return TestHelper.deleteUser(context, client, userId, 204);
        })
            .setHandler(x -> { async.complete(); });
    }

    @Test
    public void postRLE(TestContext context) {
        final Async async = context.async();

        WebClient client = WebClient.create(vertx);

        UserEntity entity = new UserEntity();
        entity.email = "PostUserTest@test.com";
        entity.name = "testuser";
        entity.userId = "testId";

        Future<Long> postFut = TestHelper.postUser(context, client, entity, 201);
        Future<Long> postRLEFut = postFut.compose(userId -> {
            ReadingListElementEntity rle = new ReadingListElementEntity();
            rle.userId = userId;
            rle.description = "description";
            rle.name = "rleName";
            rle.amazonLink = "some amazon link";

            this.userId = userId;

            return TestHelper.postReadingListElement(context, client, rle, userId, 201); });
        Future<ReadingListElementEntity> getFut = postRLEFut.compose(rleId -> {
            return TestHelper.getReadingListElement(context, client, this.userId, rleId, 200);
        });
        Future<Long> deleteListFut = getFut.compose(e -> {
            context.assertEquals("description", e.description);
            context.assertEquals("rleName", e.name);
            context.assertEquals("some amazon link", e.amazonLink);
            context.assertEquals(this.userId, e.userId);

            return TestHelper.deleteReadingListElement(context, client, this.userId, e.id, 204).map(e.id);
        });
        Future<ReadingListElementEntity> failGetFut = deleteListFut.compose(listId -> {
            return TestHelper.getReadingListElement(context, client, this.userId, listId, 404);
        });
        failGetFut.compose(x -> {
            return TestHelper.deleteUser(context, client, this.userId, 204);
        })
            .setHandler(x -> { async.complete(); });
    }

    @Test
    public void putRLE(TestContext context) {
        final Async async = context.async();

        WebClient client = WebClient.create(vertx);

        UserEntity entity = new UserEntity();
        entity.email = "putUserTest@test.com";
        entity.name = "testuser";
        entity.userId = "testId";

        Future<Long> postFut = TestHelper.postUser(context, client, entity, 201);
        Future<Long> postRLEFut = postFut.compose(userId -> {
            ReadingListElementEntity rleEntity = new ReadingListElementEntity();
            rleEntity.userId = userId;
            rleEntity.description = "description";
            rleEntity.name = "RLEName";
            rleEntity.amazonLink = "some amazon Link from Put";

            this.userId = userId;

            return TestHelper.postReadingListElement(context, client, rleEntity, userId, 201); });
        Future<Long> putFut = postRLEFut.compose(rleId -> {
            ReadingListElementEntity putEntity = new ReadingListElementEntity();
            putEntity.id = rleId;
            putEntity.userId = this.userId;
            putEntity.description = "newdescription";
            putEntity.name = "newRLEName";
            putEntity.amazonLink = "amazon from Put";

            return TestHelper.putReadingListElement(context, client, putEntity, this.userId,204).map(rleId);
        });
        Future<ReadingListElementEntity> getFut = putFut.compose(listId -> { return TestHelper.getReadingListElement(context, client, userId, listId, 200); });
        Future<Void> deleteFut = getFut.compose(e -> {
            context.assertEquals("newdescription", e.description);
            context.assertEquals("newRLEName", e.name);
            context.assertEquals("amazon from Put", e.amazonLink);

            return TestHelper.deleteReadingListElement(context, client, this.userId, e.id, 204);
        });
        deleteFut.compose(x -> {
            return TestHelper.deleteUser(context, client, this.userId, 204);
        })
            .setHandler(x -> { async.complete(); });
    }

    @Test
    public void tagTest(TestContext context) {
        final Async async = context.async();

        WebClient client = WebClient.create(vertx);

        UserEntity entity = new UserEntity();
        entity.email = "tagReadingList@test.com";
        entity.name = "testuser";
        entity.userId = "testId";

        Future<Long> postFut = TestHelper.postUser(context, client, entity, 201);
        Future<Long> postListFut = postFut.compose(userId -> {
            ReadingListElementEntity rle = new ReadingListElementEntity();
            rle.userId = userId;
            rle.description = "description";
            rle.name = "listName";
            rle.amazonLink = "tag test amazon link";

            this.userId = userId;

            return TestHelper.postReadingListElement(context, client, rle, userId, 201); });
        Future<Long> postTagFut = postListFut.compose(rleId -> {
            TagEntity tagEntity = new TagEntity();
            tagEntity.tagName = "testReadingListElementTag";

            this.rleId = rleId;

            return TestHelper.postTag(context, client, tagEntity, 201);
        });
        Future<Long> addTagToListFut = postTagFut.compose(tagId -> {
            long[] tagIds = { tagId };

            this.tagId = tagId;

            return TestHelper.addTagToReadingListElement(context, client, this.userId, this.rleId, tagIds, 200).map(tagId);
        });
        Future<TagEntity[]> getTagFut = addTagToListFut.compose(tagId -> {
            return TestHelper.getTagsForReadingListElement(context, client, this.userId, this.rleId, 200);
        });
        Future<Void> removeTagFut = getTagFut.compose(tagEntities -> {
            context.assertEquals(tagEntities.length, 1);
            context.assertEquals(tagEntities[0].id, tagId);
            context.assertEquals(tagEntities[0].tagName, "testReadingListElementTag");

            return TestHelper.removeTagFromReadingListElement(context, client, this.userId, this.rleId, this.tagId, 204);
        });
        Future<TagEntity[]> checkTagFut = removeTagFut.compose(tagId -> {
            return TestHelper.getTagsForReadingListElement(context, client, this.userId, this.rleId, 200);
        });
        Future<Void> deleteTagFut = checkTagFut.compose(tagEntities -> {
            context.assertEquals(tagEntities.length, 0);

            return TestHelper.deleteTag(context, client, this.tagId, 204);
        });
        Future<Void> deleteListFut = deleteTagFut.compose(x -> {
            return TestHelper.deleteReadingListElement(context, client, this.userId, this.rleId, 204);
        });
        deleteListFut.compose(x -> {
            return TestHelper.deleteUser(context, client, this.userId, 204);
        })
            .setHandler(x -> { async.complete(); });
    }
}
