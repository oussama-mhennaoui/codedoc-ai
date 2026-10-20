import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class FileReaderExample {

    public String readFile(String filename)
            throws IOException {

        Path path = Paths.get("/uploads", filename);
        return Files.readString(path);
    }
}