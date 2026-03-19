package fingerprint;

public class RSSIV implements I_RSSIV {
    private final int[] elements;

    public RSSIV(int[] elements) {
        this.elements = elements;
    }

    @Override
    public int[] getElements() { return elements; }

    @Override
    public int getSum() {
        int sum = 0;
        for (int val : elements) {
            sum += val;
        }
        return sum;
    }
}