
package market.repo;
public class IdGenerator {
    private Long seq;
    public IdGenerator(long start){ this.seq = start; }
    public long next(){ return seq += 1L; }
    public long peek(){ return seq; }
}
