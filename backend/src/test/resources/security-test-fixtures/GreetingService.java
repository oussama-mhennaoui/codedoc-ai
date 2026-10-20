public class GreetingService {

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, guest!";
        }

        return "Hello, " + name + "!";
    }
}