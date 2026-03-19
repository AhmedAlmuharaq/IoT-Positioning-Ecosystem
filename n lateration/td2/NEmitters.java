package td2;

import java.util.ArrayList;
import java.util.List;

public class NEmitters implements INEmitters {
    private final List<ICell> emitters = new ArrayList<>();

    @Override
    public void addEmitter(ICell cell) {
        emitters.add(cell);
    }

    @Override
    public ICell[] getEmitters() {
        return emitters.toArray(new ICell[0]);
    }

    @Override
    public int size() {
        return emitters.size();
    }
}