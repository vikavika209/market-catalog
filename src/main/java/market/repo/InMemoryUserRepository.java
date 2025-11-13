package market.repo;

import market.domain.Role;
import market.domain.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory репозиторий для сущности {@link User}.
 * Данные хранятся в HashMap и исчезают после завершения программы.
 */
public class InMemoryUserRepository implements UserRepository{
    private final Map<String, User> users = new HashMap<>();
    private final Path file = Paths.get("users.csv");

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public void saveUser(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public boolean exists(String username) {
        return users.containsKey(username);
    }

    @Override
    public void load() throws IOException {
        if (!Files.exists(file)) return;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\uFEFF","").trim();
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                if (p.length < 3) continue;
                users.put(p[0], new User(p[0], p[1], Role.valueOf(p[2])));
            }
        }
    }

    @Override
    public void persist() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            bw.write("#username,password,role\n");
            for (User u : users.values()) {
                bw.write(String.join(",", u.getUsername(), u.getPassword(), u.getRole().name()));
                bw.newLine();
            }
        }
    }
}
