package buyerking.web;

public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static String success(String message, String jsonData) {
        return String.format("{\"success\": true, \"message\": \"%s\", \"data\": %s}", message,
                jsonData != null ? jsonData : "null");
    }

    public static String error(String message) {
        return String.format("{\"success\": false, \"message\": \"%s\", \"data\": null}", message);
    }

    // Simple JSON string builder helper to avoid big libraries for now
    public static String escape(String input) {
        if (input == null)
            return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
