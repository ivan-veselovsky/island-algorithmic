package island;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;

// TODO:
//  1) count map boundaries as sea;
//  2) support diagonal connectivity;
//  3)
class IslandMapTest {

//    @SneakyThrows
//    static String readContent(String resource) {
//        URL url = IslandMapTest.class.getClassLoader().getResource(resource);
//        return Files.readString(Paths.get(url.toURI()));
//    }

    @SneakyThrows
    static Path resourcePath(String resource) {
        URL url = IslandMapTest.class.getClassLoader().getResource(resource);
        return Paths.get(url.toURI());
    }

    @SneakyThrows
    static String readResourceAsString(Path resourcePath) {
        return Files.readString(resourcePath);
    }

    private String example0() {
        return readResourceAsString(resourcePath("island0.txt"));
    }

    @Test
    void testReadFromString() {
        IslandMap<?> islandMap = IslandMap.readFromString('.', example0(),
                (x, y) -> new MarkerPayload());
        then(islandMap.getHeight()).isEqualTo(9);
        then(islandMap.getWidth()).isEqualTo(48);
    }

    @Test
    void testExport() {
        IslandMap<MarkerPayload> islandMap = IslandMap.readFromString('.', example0(),
                (x, y) -> new MarkerPayload());
        String actualExported = islandMap.export('.', 'X');
        System.out.println(actualExported);
        String expected = example0().replace('#', 'X');
        then(actualExported).isEqualTo(expected);
    }

    @Test
    void test_island0() {
        Path resourcePath = resourcePath("island0.txt");
        IslandMap<MarkerPayload> islandMap = IslandMap.readFromResource('.', resourcePath,
                (x, y) -> new MarkerPayload());

        final Traversal<MarkerPayload> traversal = new Traversal<>(islandMap);
        final Point<MarkerPayload> startPoint = islandMap.getPoint(22, 1);
        then(startPoint.isLand()).isTrue();

        final Set<Point<MarkerPayload>> shorePoints = new HashSet<>();
        final MinMaxPointTracker minMaxPointTracker = new MinMaxPointTracker();
        final int count = traversal.traverseBFS(startPoint,
                p -> {
                    System.out.println("visit: " + p);
                    return true;
                }, (p, n) -> {
                    if (n.isLand()) {
                        return true;
                    } else {
                        n.getMutablePayload().setClassification(Classification.WATER_NEAR_ISLAND);
                        p.getMutablePayload().shoreWaterCardinality++;
                        System.out.println("          Water near island: " + n);

                        shorePoints.add(n);

                        minMaxPointTracker.accept(n);

                        return false;
                    }
                }, p -> {
                    //assert p.getMutablePayload().shoreWaterCardinality <= 3;
                    if (p.getMutablePayload().shoreWaterCardinality > 1) {
                        Point<MarkerPayload>[] allNeighbours = islandMap.getRegularAndDiagonalNeighbours(p);
                        assert allNeighbours.length == 8;
                        for (int i = 0; i < 8; i += 2) {
                            int i90 = (i + 2) & 7;
                            int i45 = (i + 1) & 7;
                            if (allNeighbours[i] != null
                                    && allNeighbours[i].getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                                    && allNeighbours[i90].getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                                    && allNeighbours[i90] != null
                                    && !allNeighbours[i45].isLand()) {
                                allNeighbours[i45].getMutablePayload().setClassification(Classification.WATER_NEAR_ISLAND);
                                //System.out.println("set diagoinal ~ for " + allNeighbours[i45]);
                            }
                        }
                    }
                    return true;
                });
        System.out.println("visited count = " + count);

        String actual = islandMap.export('.', 'X');
        System.out.println(actual);
        System.out.println("shore points = " + shorePoints.size());

        String expected = """
..............~~~~~~~~~~~~~~~~~~~~~.............
...........~~~~XXXXXXXXXXXXXXXXXXX~~~~..........
........~~~~XXXXXX~~~XXXX~XXX~~~~XXXX~~~~.......
........~XXXXX~~~~~.~~XX~~~~X~..~~~XXXXX~.......
......~~~XXXXX~~~~~~~~XX~~~~X~~~..~XXXXX~.......
......~XXXXXXXXXXXXXXXXXX~~X~~X~~~~~X~~~~~~.....
......~~~XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX~.....
........~~~~~XXXXXXXXXXXXXXXXXXXX~~~~~~~~~~.....
............~~~~~~~~~~~~~~~~~~~~~~..............
""";
        then(actual).isEqualTo(expected);

        then(minMaxPointTracker.getMinX()).isEqualTo(6);
        then(minMaxPointTracker.getMaxX()).isEqualTo(42);

        then(minMaxPointTracker.getMinY()).isEqualTo(0);
        then(minMaxPointTracker.getMaxY()).isEqualTo(8);

        // ########################## Part 2
        val lakeCount = new AtomicInteger();

        final int count2 = traversal.traverseBFS(startPoint,
                p -> true, (p, n) -> {
                    if (n.isLand()) {
                        return true;
                    } else if (n.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                            && n.getMutablePayload().getLakeIndex() < 0) {
                        System.out.println("#### starting shore traversal from point " + n);

                        final AtomicReference<Point<MarkerPayload>> outerShoreFound = new AtomicReference<>();

                        traversal.traverseBFS(n, x -> {
                                    if (minMaxPointTracker.isOnBoundingBox(x)) {
                                        System.out.println("### outer shore detected at node " + x);
                                        boolean changed = outerShoreFound.compareAndSet(null, x);
                                        assert changed;
                                        return false; // this is outer shore, restart and mark.
                                    }
                                    return true;
                                }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                                x -> true);

                        final Point<MarkerPayload> outerStart = outerShoreFound.get();
                        if (outerStart != null) {
                            System.out.println("#### marking outer shore " + outerStart);
                            int outerMarked = traversal.traverseBFS(outerStart, x -> {
                                        x.getMutablePayload().setLakeIndex(0); // outer shore
                                        return true;
                                    }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                                    x -> true);
                            System.out.println("Outer marked = " + outerMarked);
                        } else {
                            val lakeIndex = lakeCount.incrementAndGet();
                            System.out.println("#### Found lake #" + lakeIndex);
                            traversal.traverseBFS(n, x -> {
                                        x.getMutablePayload().setLakeIndex(lakeIndex);
                                        return true;
                                    }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                                    x -> true);
                        }
                        assert n.getMutablePayload().getLakeIndex() >= 0 : "#### " + n;
                    }
                    return false;
                }, p -> true);
        System.out.println("visited count = " + count);
        then(count2).isEqualTo(count);

        System.out.println("lake count = " + lakeCount.get());

        String actual2 = islandMap.export('=', '.');
        System.out.println(actual2);
    }


    @Test
    void test_island_1() {
        Path island1Path = resourcePath("island1.txt");

        IslandMap<MarkerPayload> islandMap = IslandMap.readFromResource('.', island1Path,
                (x, y) -> new MarkerPayload());
        then(islandMap.getWidth()).isEqualTo(256);
        then(islandMap.getHeight()).isEqualTo(256);

        final Traversal<MarkerPayload> traversal = new Traversal<>(islandMap);
        final Point<MarkerPayload> startPoint = islandMap.getPoint(2, 7);
        then(startPoint.isLand()).isTrue();

        final Set<Point<MarkerPayload>> shorePoints = new HashSet<>();
        final MinMaxPointTracker minMaxPointTracker = new MinMaxPointTracker();
        final int count = traversal.traverseBFS(startPoint,
                p -> {
            System.out.println("visit: " + p);
            return true;
        }, (p, n) -> {
            if (n.isLand()) {
                return true;
            } else {
                n.getMutablePayload().setClassification(Classification.WATER_NEAR_ISLAND);
                p.getMutablePayload().shoreWaterCardinality++;
                System.out.println("          Water near island: " + n);

                shorePoints.add(n);

                minMaxPointTracker.accept(n);

                return false;
            }
        }, p -> {
            //assert p.getMutablePayload().shoreWaterCardinality <= 3;
            if (p.getMutablePayload().shoreWaterCardinality > 1) {
                Point<MarkerPayload>[] allNeighbours = islandMap.getRegularAndDiagonalNeighbours(p);
                assert allNeighbours.length == 8;
                for (int i = 0; i < 8; i += 2) {
                    int i90 = (i + 2) & 7;
                    int i45 = (i + 1) & 7;
                    if (allNeighbours[i] != null
                        && allNeighbours[i].getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                        && allNeighbours[i90].getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                        && allNeighbours[i90] != null
                        && !allNeighbours[i45].isLand()) {
                        allNeighbours[i45].getMutablePayload().setClassification(Classification.WATER_NEAR_ISLAND);
                        //System.out.println("set diagoinal ~ for " + allNeighbours[i45]);
                    }
                }
            }
            return true;
        });
        System.out.println("visited count = " + count);

        String actual = islandMap.export('.', 'X');
        System.out.println(actual);
        System.out.println("shore points = " + shorePoints.size());

//        String expected = """
//              ~~~~~~~~~~~~~~~~~~~~~            .
//           ~~~~XXXXXXXXXXXXXXXXXXX~~~~         .
//        ~~~~XXXXXX~~~XXXX~XXX~~~~XXXX~~~~      .
//        ~XXXXX~~~~~ ~~XX~~~~X~  ~~~XXXXX~      .
//      ~~~XXXXX~~~~~~~~XX~~~~X~~~  ~XXXXX~      .
//      ~XXXXXXXXXXXXXXXXXX~~X~~X~~~~~X~~~~~~    .
//      ~~~XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX~    .
//        ~~~~~XXXXXXXXXXXXXXXXXXXX~~~~~~~~~~    .
//            ~~~~~~~~~~~~~~~~~~~~~~             .
//""";
//        then(actual).isEqualTo(expected);

//        then(minMaxPointTracker.getMinX()).isEqualTo(6);
//        then(minMaxPointTracker.getMaxX()).isEqualTo(42);
//
//        then(minMaxPointTracker.getMinY()).isEqualTo(0);
//        then(minMaxPointTracker.getMaxY()).isEqualTo(8);

        // ########################## Part 2
        val lakeCount = new AtomicInteger();

        final int count2 = traversal.traverseBFS(startPoint,
                p -> true, (p, n) -> {
                    if (n.isLand()) {
                        return true;
                    } else if (n.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND
                            && n.getMutablePayload().getLakeIndex() < 0) {
                        System.out.println("#### starting shore traversal from point " + n);

                        final AtomicReference<Point<MarkerPayload>> outerShoreFound = new AtomicReference<>();

                        traversal.traverseBFS(n, x -> {
                            if (minMaxPointTracker.isOnBoundingBox(x)) {
                                System.out.println("### outer shore detected at node " + x);
                                boolean changed = outerShoreFound.compareAndSet(null, x);
                                assert changed;
                                return false; // this is outer shore, restart and mark.
                            }
                            return true;
                        }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                        x -> true);

                        final Point<MarkerPayload> outerStart = outerShoreFound.get();
                        if (outerStart != null) {
                            System.out.println("#### marking outer shore " + outerStart);
                            int outerMarked = traversal.traverseBFS(outerStart, x -> {
                                x.getMutablePayload().setLakeIndex(0); // outer shore
                                return true;
                            }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                            x -> true);
                            System.out.println("Outer marked = " + outerMarked);
                        } else {
                            val lakeIndex = lakeCount.incrementAndGet();
                            System.out.println("#### Found lake #" + lakeIndex);
                            traversal.traverseBFS(n, x -> {
                                x.getMutablePayload().setLakeIndex(lakeIndex);
                                return true;
                            }, (x, y) -> y.getMutablePayload().getClassification() == Classification.WATER_NEAR_ISLAND,
                            x -> true);
                        }
                        assert n.getMutablePayload().getLakeIndex() >= 0 : "#### " + n;
                    }
                    return false;
                }, p -> true);
        System.out.println("visited count = " + count);
        then(count2).isEqualTo(count);

        System.out.println("lake count = " + lakeCount.get());

        String actual2 = islandMap.export('.', '*');
        System.out.println(actual2);

        then(lakeCount.get()).isEqualTo(4);
    }

}