
// FIXED Student Base64
import java.io.*;
import java.util.Base64;

public class StudentsFrame {

    public String encodeImage(String path) throws Exception {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
