import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibraryMembers {
    public static void main(String[] args) {

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {

            System.out.println("--- Step 1 ---");
            MongoDatabase database = mongoClient.getDatabase("library");
            System.out.println("Switched to db: " + database.getName());

            MongoCollection<Document> members = database.getCollection("members");

            List<Document> memberDocs = Arrays.asList(
                new Document("name", "Ravi")
                    .append("address", new Document("city", "Chennai").append("pin", "600001"))
                    .append("borrowed", Arrays.asList("Python Basics", "MongoDB Guide"))
                    .append("fee", 0),
                new Document("name", "Sita")
                    .append("address", new Document("city", "Madurai").append("pin", "625001"))
                    .append("borrowed", Collections.singletonList("MongoDB Guide"))
                    .append("fee", 50),
                new Document("name", "Lakshmi")
                    .append("address", new Document("city", "Coimbatore").append("pin", "641001"))
                    .append("borrowed", Collections.singletonList("Python Basics"))
                    .append("fee", 120),
                new Document("name", "Kumar")
                    .append("address", new Document("city", "Chennai").append("pin", "600028"))
                    .append("borrowed", Collections.emptyList())
                    .append("fee", 0)
            );

            InsertManyResult insertResult = members.insertMany(memberDocs);
            System.out.println("Inserted IDs: " + insertResult.getInsertedIds());

            System.out.println("Databases:");
            for (String dbName : mongoClient.listDatabaseNames()) {
                System.out.println("  " + dbName);
            }

            System.out.println("Collections in library:");
            for (String colName : database.listCollectionNames()) {
                System.out.println("  " + colName);
            }

            System.out.println("\n--- Step 2 ---");
            members.find(Filters.eq("address.city", "Chennai"))
                   .projection(Projections.fields(
                       Projections.excludeId(),
                       Projections.include("name", "address")
                   ))
                   .forEach(doc -> System.out.println(doc.toJson()));

            System.out.println("\n--- Step 3 ---");
            members.find(Filters.eq("borrowed", "MongoDB Guide"))
                   .projection(Projections.fields(
                       Projections.excludeId(),
                       Projections.include("name", "borrowed")
                   ))
                   .forEach(doc -> System.out.println(doc.toJson()));

            System.out.println("\n--- Step 4 ---");
            members.find(Filters.all("borrowed", "Python Basics", "MongoDB Guide"))
                   .projection(Projections.fields(
                       Projections.excludeId(),
                       Projections.include("name")
                   ))
                   .forEach(doc -> System.out.println(doc.toJson()));

            System.out.println("\n--- Step 5 ---");
            members.find(Filters.size("borrowed", 0))
                   .projection(Projections.fields(
                       Projections.excludeId(),
                       Projections.include("name")
                   ))
                   .forEach(doc -> System.out.println(doc.toJson()));

            System.out.println("\n--- Step 6 ---");
            UpdateResult updateResult = members.updateOne(
                Filters.eq("name", "Sita"),
                Updates.set("address.city", "Chennai")
            );
            System.out.println("Matched: " + updateResult.getMatchedCount() +
                               ", Modified: " + updateResult.getModifiedCount());

            System.out.println("\n--- Step 7 ---");
            long feeCount = members.countDocuments(Filters.gt("fee", 0));
            System.out.println("Members owing fee > 0: " + feeCount);

            members.drop();
            System.out.println("Collection dropped successfully.");
        }
    }
}
