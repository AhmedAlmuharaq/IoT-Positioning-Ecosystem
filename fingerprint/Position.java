package fingerprint;

public class Position implements I_Position {
    private final double x;
    private final double y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public double getX() { return x; }

    @Override
    public double getY() { return y; }
}