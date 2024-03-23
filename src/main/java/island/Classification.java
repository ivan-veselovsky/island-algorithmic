package island;

import lombok.Getter;

// TODO: remove this class, as there is only one classification
public enum Classification {
    WATER_NEAR_ISLAND('~');

    @Getter
    private final char symbol;
    Classification(char c) {
        this.symbol = c;
    }

    @Override
    public String toString() {
        return String.valueOf(getSymbol());
    }
}
