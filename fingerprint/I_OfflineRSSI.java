package fingerprint;

import java.util.List;

public interface I_OfflineRSSI {
    List<I_Cell> getCells();
    void addCell(I_Cell cell);
}