
package market.service;
import market.domain.AuditEvent;
import java.io.*;
import java.nio.file.*;
public class AuditService {
    private final Path file = Paths.get("audit.log");
    public void append(AuditEvent e){
        try (BufferedWriter bw = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(e.format()); bw.newLine();
        } catch (IOException ex) {
            System.err.println("Audit write failed: " + ex.getMessage());
        }
    }
}
