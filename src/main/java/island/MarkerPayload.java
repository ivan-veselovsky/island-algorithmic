package island;

import lombok.Getter;
import lombok.Setter;

public class MarkerPayload {
    // TODO: replace with a boolean:
    @Getter @Setter
    Classification classification;

    int shoreWaterCardinality = 0;

    @Getter @Setter
    int lakeIndex = -1;

    @Override
    public String toString() {
        if (classification == null) {
            return null;
        } else if (lakeIndex >= 0) {
            return String.valueOf(lakeIndex);
        } else {
            return classification.toString();
        }
    }
}
