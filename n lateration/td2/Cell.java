package td2;

public class Cell implements ICell {
    private final IPosition position;
    private final double distance;

    public Cell(IPosition position, double distance) {
        this.position = position;
        this.distance = distance;
    }

    @Override
    public IPosition getPosition() {
        return position;
    }

    @Override
    public double getDistance() {
        return distance;
    }
}