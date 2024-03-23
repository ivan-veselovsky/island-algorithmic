package island;

import lombok.Getter;
import lombok.NonNull;

import java.util.function.Consumer;

@Getter
public class MinMaxPointTracker implements Consumer<Point<?>> {
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;

    public void accept(@NonNull Point<?> point) {
        minX = Math.min(minX, point.getX());
        minY = Math.min(minY, point.getY());

        maxX = Math.max(maxX, point.getX());
        maxY = Math.max(maxY, point.getY());
    }

    public boolean isOnBoundingBox(Point<?> point) {
        return minX == point.getX() || maxX == point.getX()
            || minY == point.getY() || maxY == point.getY();
    }
}
