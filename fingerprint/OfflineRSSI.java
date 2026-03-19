package fingerprint;

import java.util.ArrayList;
import java.util.List;

public class OfflineRSSI implements I_OfflineRSSI {
    private final List<I_Cell> cells;

    public OfflineRSSI() {
        this.cells = new ArrayList<>();
    }

    @Override
    public void addCell(I_Cell cell) {
        this.cells.add(cell);
    }

    @Override
    public List<I_Cell> getCells() {
        return cells;
    }
}