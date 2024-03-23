package island;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class Traversal<P> {

    private final IslandMap<P> islandMap;

    private final AtomicInteger marker = new AtomicInteger();

    public int traverseBFS(Point<P> startPoint,
                            Predicate<Point<P>> pointPreConsumer,
                            BiPredicate<Point<P>, Point<P>> businessNeighbourFilter,
                           Predicate<Point<P>> pointPostConsumer) {
        final Integer uniqueMark = marker.incrementAndGet();
        final Deque<Point<P>> deque = new LinkedList<>();
        assert !startPoint.isVisited(uniqueMark);
        deque.offer(startPoint);
        int visitedCount = 0;
        while (true) {
            Point<P> point = deque.poll();
            if (point == null) {
                break; // finish
            }
            if (!point.isVisited(uniqueMark)) {
                boolean result = pointPreConsumer.test(point);
                visitedCount++;
                point.setVisitedMarker(uniqueMark);
                assert point.isVisited(uniqueMark);
                if (!result) {
                    break; // business decided to stop traverse
                }
                Point<P>[] neighbourPoints = islandMap.getNeighbours(point);
                //System.out.println(point + ", neighbours: " + Arrays.toString(neighbourPoints));
                Arrays.stream(neighbourPoints)
                        .filter(Objects::nonNull)
                        .filter(n -> !n.isVisited(uniqueMark))
                        .filter(n -> businessNeighbourFilter.test(point, n))
                        .forEachOrdered(deque::offer);
                result = pointPostConsumer.test(point);
                if (!result) {
                    break; // business decided to stop traverse
                }
            }
        }
        return visitedCount;
    }

}
