package market.service;

import market.domain.AuditEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class AuditServiceImpl implements  AuditService {

    private final Path file = Paths.get("audit.log");

    @Override
    public void append(AuditEvent e){
        try (BufferedWriter bw = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(e.toString()); bw.newLine();
        } catch (IOException ex) {
            System.err.println("Audit write failed: " + ex.getMessage());
        }
    }
}
