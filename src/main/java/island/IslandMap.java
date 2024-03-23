package island;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class IslandMap<T> {
    @Getter
    private final int width;
    @Getter
    private final int height;

    private final Point[][] mapData;

    Point<T> getPoint(int x, int y) {
        return mapData[y][x];
    }

    @SneakyThrows
    public static <T> IslandMap<T> readFromResource(char waterSymbol, Path path, BiFunction<Integer, Integer, T> payloadSupplier) {
        Stream<String> lineStream = Files.lines(path);
        return readFromStringStream(waterSymbol, lineStream, payloadSupplier);
    }

    @SneakyThrows
    public static <T> IslandMap<T> readFromString(char waterSymbol, String mapContent, BiFunction<Integer, Integer, T> payloadSupplier) {
        String[] lineArray = mapContent.split("\n");
        Stream<String> lineStream = Arrays.stream(lineArray);
        return readFromStringStream(waterSymbol, lineStream, payloadSupplier);
    }

    private static <T> IslandMap<T> readFromStringStream(char waterSymbol, Stream<String> mapContentStream,
                                                         BiFunction<Integer, Integer, T> payloadSupplier) {
        val y = new AtomicInteger();
        val width = new AtomicInteger(-1);
        final Point<T>[][] data
                = mapContentStream.map(x -> {
                    Preconditions.checkArgument(!x.endsWith("\n"));
                    return x;
                }).map(line -> {
                    if (!width.compareAndSet(-1, line.length())) {
                        Preconditions.checkArgument(line.length() == width.get(),
                                "Line y=" + y + " length mismatch: expected " + width + ", but found " + line.length());
                    }
                    return makeRow(waterSymbol, y.getAndIncrement(), line, payloadSupplier);
        }).toArray(Point[][]::new);

        return new IslandMap<>(width.get(), data.length, data);
    }

    public static <T> IslandMap<T> buildFromPoints(int width, int height,
                List<Point<T>> list, BiFunction<Integer, Integer, T> payloadSupplier) {
        final Point<T>[][] data = new Point[height][width];
        for (Point<T> point: list) {
            val x = point.getX();
            val y = point.getY();
            data[y][x] = new Point<>(x, y, true, payloadSupplier.apply(x, y));
        }
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (data[y][x] == null) {
                    data[y][x] = new Point<>(x, y, false, payloadSupplier.apply(x, y));
                }
            }
        }
        return new IslandMap<>(width, height, data);
    }

    private static <P> Point<P>[] makeRow(char waterSymbol, int y, String line, BiFunction<Integer, Integer, P> payloadSupplier) {
        Point<P>[] row = new Point[line.length()];
        for (int x = 0; x<line.length(); x++) {
            char c = line.charAt(x);
            boolean isLand = (c != waterSymbol);
            P payload = payloadSupplier.apply(x, y);
            row[x] = new Point<>(x, y, isLand, payload);
        }
        return row;
    }

    String export(char waterSymbol, char landSymbol) {
        StringBuilder sb = new StringBuilder(height * (width + 1));
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                Point<T> point = getPoint(x, y);
                T payload = point.getMutablePayload();
                String payloadSymbol = payload.toString();
                if (payloadSymbol == null) {
                    sb.append(point.isLand() ? landSymbol : waterSymbol);
                } else {
                    sb.append(payloadSymbol);
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    Optional<Point<T>> safeGetPoint(int x, int y) {
        if (x < 0 || x >= width) {
            return Optional.empty();
        }
        if (y < 0 || y >= height) {
            return Optional.empty();
        }
        return Optional.of(getPoint(x, y));
    }

    Point<T>[] getNeighbours(Point<T> point) {
        Point<T>[] list = new Point[4];
        val x = point.getX();
        val y = point.getY();
        list[0] = safeGetPoint(x, y - 1).orElse(null); // north
        list[1] = safeGetPoint(x + 1, y).orElse(null); // east
        list[2] = safeGetPoint(x, y + 1).orElse(null); // south
        list[3] = safeGetPoint(x - 1, y).orElse(null); // west
        return list;
    }

    Point<T>[] getRegularAndDiagonalNeighbours(Point<T> point) {
        Point<T>[] list = new Point[8];
        val x = point.getX();
        val y = point.getY();
        list[0] = safeGetPoint(x, y - 1).orElse(null); // north
            list[1] = safeGetPoint(x + 1, y - 1).orElse(null); // north - east
        list[2] = safeGetPoint(x + 1, y).orElse(null); // east
            list[3] = safeGetPoint(x + 1, y + 1).orElse(null); // south - east
        list[4] = safeGetPoint(x, y + 1).orElse(null); // south
            list[5] = safeGetPoint(x - 1, y + 1).orElse(null); // south - west
        list[6] = safeGetPoint(x - 1, y).orElse(null); // west
            list[7] = safeGetPoint(x - 1, y - 1).orElse(null); // north - west
        return list;
    }
}
