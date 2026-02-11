package buyerking;

public class TestOrderProcessing {

    public static void main(String[] args) {
        System.out.println("Testing order processing...\n");

        ChatbotInterface chatbot = new ChatbotInterface();

        // Test case 1: Single item
        System.out.println("=== Test 1: Single Item ===");
        String order1 = "ORDER:[{\"id\":\"1\",\"name\":\"Nasi Padang (Beef Rendang)\",\"price\":\"Rp25,000\"}]";
        String response1 = chatbot.processCommand(order1);
        System.out.println(response1);
        System.out.println();

        // Test case 2: Multiple items
        System.out.println("=== Test 2: Multiple Items ===");
        String order2 = "ORDER:[{\"id\":\"1\",\"name\":\"Nasi Padang (Beef Rendang)\",\"price\":\"Rp25,000\"},{\"id\":\"7\",\"name\":\"Nasi Goreng Special\",\"price\":\"Rp20,000\"},{\"id\":\"25\",\"name\":\"Chocolate Chip Cookies\",\"price\":\"Rp25,000\"}]";
        String response2 = chatbot.processCommand(order2);
        System.out.println(response2);
        System.out.println();

        // Test case 3: Expensive items
        System.out.println("=== Test 3: Electronics Order ===");
        String order3 = "ORDER:[{\"id\":\"15\",\"name\":\"MacBook Air M1\",\"price\":\"Rp11,500,000\"},{\"id\":\"19\",\"name\":\"AirPods Pro\",\"price\":\"Rp3,500,000\"}]";
        String response3 = chatbot.processCommand(order3);
        System.out.println(response3);
        System.out.println();
    }
}
