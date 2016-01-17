import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by code8 on 1/15/16.
 */

public class CacheTester {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java CacheTester {type}");
            System.err.println("where {type} is one of: jdk|custom");
            System.exit(1);
        }

        test(args[0], false);
        test(args[0], true);
        concurrentTest(args[0], false);
        concurrentTest(args[0], true);
    }


    private static void test(String type, boolean lru) {
        Cache<Integer, String> cache = Cache.make(type, Object::toString, 3, lru);

        Stream.of(1,2,1,3,4).forEach(cache::read);

        assert cache.usage() == 3                               : "constant capacity fail";
        assert cache.get(lru ? 2 : 1) == null                   : "stale element deletion fail";
        assert cache.get(lru ? 1 : 2).equals(lru ? "1" : "2")   : "consistency check fail";
        assert cache.get(3).equals("3")                         : "consistency check fail";

        System.out.println(cache.getClass().getSimpleName() + (lru ? " LRU" : " FIFO") + " strategy checked single thread");
    }

    private static void concurrentTest(String type, boolean lru) {
        Cache<Integer, String> cache = Cache.make(type, Object::toString, 16, lru);

        if (cache instanceof CustomCache) {
            boolean consisted = IntStream.range(1,128)
                    .boxed()
                    .parallel()
                    .map(k -> {
                        int i = 10000;
                        boolean checked = true;
                        while (i-- > 0) {
                            checked &= Objects.equals(cache.read(k), k.toString());
                        }
                        return checked;
                    })
                    .allMatch(checked -> true);

            assert cache.usage() == 16 : "duplication check fail";
            assert consisted           : "consistency check fail";

            System.out.println(cache.getClass().getSimpleName() + (lru ? " LRU" : " FIFO") + " strategy checked multi thread");
        }
    }
}
