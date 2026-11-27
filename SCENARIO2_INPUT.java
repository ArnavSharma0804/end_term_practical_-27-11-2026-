import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertManyResult;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

public class SalesReport {

    public static void main(String[] args) {
        // Connect to local MongoDB instance
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            
            // Step 1: Select database 'sales' and collection 'orders', then insert 6 orders
            MongoDatabase database = mongoClient.getDatabase("sales");
            MongoCollection<Document> orders = database.getCollection("orders");
            
            // Optional: Clear existing documents if re-running
            orders.drop();

            List<Document> orderList = Arrays.asList(
                new Document("orderId", 1).append("customer", "Arun").append("product", "Laptop").append("category", "Electronics").append("qty", 1).append("price", 55000),
                new Document("orderId", 2).append("customer", "Divya").append("product", "Mouse").append("category", "Electronics").append("qty", 2).append("price", 500),
                new Document("orderId", 3).append("customer", "Arun").append("product", "Notebook").append("category", "Stationery").append("qty", 10).append("price", 40),
                new Document("orderId", 4).append("customer", "Meena").append("product", "Pen").append("category", "Stationery").append("qty", 20).append("price", 10),
                new Document("orderId", 5).append("customer", "Divya").append("product", "Headphones").append("category", "Electronics").append("qty", 1).append("price", 1500),
                new Document("orderId", 6).append("customer", "Karthik").append("product", "Office Chair").append("category", "Furniture").append("qty", 2).append("price", 3500)
            );

            InsertManyResult insertResult = orders.insertMany(orderList);
            System.out.println("--- Step 1 ---");
            System.out.println("Acknowledged: " + insertResult.wasAcknowledged());
            System.out.println("Inserted IDs: " + insertResult.getInsertedIds());

            // Step 2: $match and $project for Electronics orders (show orderId and product)
            System.out.println("\n--- Step 2 ---");
            orders.aggregate(Arrays.asList(
                Aggregates.match(Filters.eq("category", "Electronics")),
                Aggregates.project(Projections.fields(
                    Projections.excludeId(),
                    Projections.include("orderId", "product")
                ))
            )).forEach(doc -> System.out.println(doc.toJson()));

            // Step 3: $project with $multiply to calculate lineTotal (qty * price)
            System.out.println("\n--- Step 3 ---");
            orders.aggregate(Arrays.asList(
                Aggregates.project(Projections.fields(
                    Projections.excludeId(),
                    Projections.include("orderId", "customer"),
                    Projections.computed("lineTotal", new Document("$multiply", Arrays.asList("$qty", "$price")))
                ))
            )).forEach(doc -> System.out.println(doc.toJson()));

            // Step 4: $group with $sum for total quantity sold per category
            System.out.println("\n--- Step 4 ---");
            orders.aggregate(Arrays.asList(
                Aggregates.group("$category", Accumulators.sum("totalQty", "$qty"))
            )).forEach(doc -> System.out.println(doc.toJson()));

            // Step 5: Revenue and average price per category, sorted highest revenue first
            System.out.println("\n--- Step 5 ---");
            Document lineTotalExpr = new Document("$multiply", Arrays.asList("$qty", "$price"));
            orders.aggregate(Arrays.asList(
                Aggregates.group("$category",
                    Accumulators.sum("revenue", lineTotalExpr),
                    Accumulators.avg("avgPrice", "$price")
                ),
                Aggregates.sort(Sorts.descending("revenue"))
            )).forEach(doc -> System.out.println(doc.toJson()));

            // Step 6: Top two customers by total spend ($group, $sort, $limit)
            System.out.println("\n--- Step 6 ---");
            orders.aggregate(Arrays.asList(
                Aggregates.group("$customer",
                    Accumulators.sum("spent", lineTotalExpr)
                ),
                Aggregates.sort(Sorts.descending("spent")),
                Aggregates.limit(2)
            )).forEach(doc -> System.out.println(doc.toJson()));
        }
    }
}