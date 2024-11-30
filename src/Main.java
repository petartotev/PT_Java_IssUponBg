import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        String currentDir = System.getProperty("user.dir");
        System.out.println(currentDir);

        setupLogging("./res/application.log");

        // Timer to repeat task
        Timer timer = new Timer();
        long interval = 30000; // 30 seconds

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkISSLocation();
            }
        }, 0, interval);
    }

    private static void setupLogging(String logFileName) {
        try {
            PrintStream logStream = new PrintStream(new LoggingStream(System.out, logFileName), true);
            System.setOut(logStream);
            System.setErr(logStream); // Also log errors
        } catch (IOException e) {
            System.err.println("Error setting up logging: " + e.getMessage());
        }
    }

    private static void checkISSLocation() {
        String apiUrl = "http://api.open-notify.org/iss-now.json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONObject position = jsonResponse.getJSONObject("iss_position");
            double latitude = position.getDouble("latitude");
            double longitude = position.getDouble("longitude");
            String dateLocalNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.print(dateLocalNow + " | ");
            System.out.println((isAboveBulgaria(latitude, longitude) ?
                    "Hooray! ISS is currently above Bulgaria!" : "NO!") +
                    (" Latitude: " + latitude + ", Longitude: " + longitude));
        } catch (Exception e) {
            System.err.println("Error fetching ISS location: " + e.getMessage());
        }
    }

    private static boolean isAboveBulgaria(double latitude, double longitude) {
        return (latitude >= 41 && latitude <= 44) && (longitude >= 22 && longitude <= 29);
    }

    private static class LoggingStream extends java.io.OutputStream {
        private final PrintStream consoleStream;
        private final FileWriter fileWriter;

        public LoggingStream(PrintStream consoleStream, String logFileName) throws IOException {
            this.consoleStream = consoleStream;
            this.fileWriter = new FileWriter(logFileName, true); // Append to existing log file
        }

        @Override
        public void write(int b) throws IOException {
            consoleStream.write(b); // Write to console
            fileWriter.write(b); // Write to log file
        }

        @Override
        public void flush() throws IOException {
            consoleStream.flush();
            fileWriter.flush();
        }

        @Override
        public void close() throws IOException {
            consoleStream.close();
            fileWriter.close();
        }
    }
}
