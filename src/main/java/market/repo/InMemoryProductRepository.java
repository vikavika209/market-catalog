package market.repo;


import market.domain.Category;
import market.domain.Product;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

public class InMemoryProductRepository implements ProductRepository {
    private final Map<Long, Product> store = new HashMap<>();
    private final Path file = Paths.get("products.csv");
    private final IdGenerator ids = new IdGenerator(0);

    @Override
    public Product save(Product p){

        if (p.getId()==0)
            p.setId(nextId());

        store.put(p.getId(), p);

        return p;
    }

    @Override
    public Optional<Product> findById(long id){
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean deleteById(long id){
        return store.remove(id)!=null;
    }

    @Override
    public List<Product> findAll(){
        return new ArrayList<>(store.values());
    }

    @Override
    public long nextId(){
        return ids.next();
    }

    @Override
    public void load(){
        store.clear();

        if (!Files.exists(file)) return;

        try (BufferedReader br = Files.newBufferedReader(file)){
            String line;
            long maxId = 0;

            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                List<String> parts = parseCsvLine(line);
                long id = Long.parseLong(parts.get(0));
                Product p = new Product(
                        id,
                        parts.get(1),
                        parts.get(2),
                        Category.valueOf(parts.get(3)),
                        Double.parseDouble(parts.get(4)),
                        parts.get(5)
                );
                p.setActive(Boolean.parseBoolean(parts.get(6)));
                store.put(id, p);
                if (id>maxId) maxId = id;
            }
            if (maxId>0) while (ids.peek()<maxId) ids.next();
        } catch (IOException e) {
            System.err.println("Failed to load products.csv: " + e.getMessage());
        }
    }
    @Override public void flush(){
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write("#id,name,brand,category,price,description,active\n");
            for (Product p: store.values()){
                bw.write(String.join("," ,
                        Long.toString(p.getId()),
                        CsvUtil.esc(p.getName()),
                        CsvUtil.esc(p.getBrand()),
                        p.getCategory().name(),
                        Double.toString(p.getPrice()),
                        CsvUtil.esc(p.getDescription()),
                        Boolean.toString(p.isActive())
                ));
                bw.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to write products.csv: " + e.getMessage());
        }
    }
    private static List<String> parseCsvLine(String line){
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i=0;i<line.length();i++){
            char c = line.charAt(i);
            if (c=='"') {
                if (quoted && i+1<line.length() && line.charAt(i+1)=='"'){ cur.append('"'); i++; }
                else quoted = !quoted;
            } else if (c==',' && !quoted) {
                out.add(unesc(cur.toString())); cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(unesc(cur.toString()));
        return out;
    }
    private static String unesc(String s){
        String v = s.trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length()-1).replace("\"\"", "\"");
        }
        return v;
    }
}
