package island;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class Point<T> {
    private final int x;
    private final int y;
    private final boolean land;

    private final T mutablePayload;
    private final Set<Integer> visitedMarker = new HashSet<>();

    public boolean isVisited(Integer marker) {
        return visitedMarker.contains(marker);
    }
    public void markVisited(Integer marker) {
        boolean added = visitedMarker.add(marker);
        assert added;
    }

    @Override
    public String toString() {
        return x + ":" + y + " " + (land ? "#" : "~");
    }
}
