package buyerking;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting BuyerKing Tender System...");
        System.out.println("==================================");

        // Initialize sample data
        initializeSampleData();

        // Start Web Server
        System.out.println("Starting Web Server...");
        buyerking.web.WebServer webServer = new buyerking.web.WebServer();
        webServer.start();

        // Start Console Chatbot
        System.out.println("Use the Web Interface at http://localhost:8080");
        System.out.println("Press Ctrl+C to stop.");

        ChatbotInterface chatbot = new ChatbotInterface();
        chatbot.start();
    }

    private static void initializeSampleData() {
        try {
            // Check and create sample data if needed
            String checkBuyers = "SELECT COUNT(*) as count FROM buyers";
            var rs = DatabaseManager.executeQuery(checkBuyers);

            if (rs.next() && rs.getInt("count") == 0) {
                System.out.println("Creating sample data...");

                // Data is already in schema.sql, but let's add sample tenders
                TenderManager tenderManager = new TenderManager();

                // Create sample tenders
                tenderManager.createTender(1, "Catering for Company Event",
                        "Need catering (Nasi Padang) for 50 people", 50, 850000, 3);

                tenderManager.createTender(2, "Laptops for School Lab",
                        "Need 10 laptops for computer lab", 10, 70000000, 7);

                System.out.println("Sample data created successfully!");
            }

        } catch (Exception e) {
            System.err.println("Error initializing sample data: " + e.getMessage());
        }
    }
}