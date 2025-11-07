
package market.service;
import market.domain.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class AuthService {
    private final Map<String, User> users = new HashMap<>();
    private final Path file = Paths.get("users.csv");
    private User current;
    public AuthService(){
        load();
        if (users.isEmpty()) {
            users.put("admin", new User("admin","admin", Role.ADMIN));
            users.put("user",  new User("user","user", Role.USER));
            save();
        }
    }
    public Optional<User> login(String username, String password){
        User u = users.get(username);
        if (u!=null && Objects.equals(password, u.getPassword())) {
            current = u; return Optional.of(u);
        }
        return Optional.empty();
    }
    public void logout(){ current = null; }
    public Optional<User> current(){ return Optional.ofNullable(current); }
    public void register(String username, String password, Role role){
        if (users.containsKey(username)) throw new IllegalArgumentException("Username taken");
        users.put(username, new User(username, password, role));
        save();
    }
    private void load(){
        if (!Files.exists(file)) return;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                users.put(p[0], new User(p[0], p[1], Role.valueOf(p[2])));
            }
        } catch (IOException e){ System.err.println("users.csv load failed: "+e.getMessage()); }
    }
    private void save(){
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write("#username,password,role\n");
            for (User u: users.values()){
                bw.write(String.join(",", u.getUsername(), u.getPassword(), u.getRole().name()));
                bw.newLine();
            }
        } catch (IOException e){ System.err.println("users.csv save failed: "+e.getMessage()); }
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
