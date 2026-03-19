package fingerprint;

public class Cell implements I_Cell {
    private final I_Position centrum;
    private final float width;
    private final float height;
    private final I_RSSIV rssiv;

    public Cell(I_Position centrum, float width, float height, I_RSSIV rssiv) {
        this.centrum = centrum;
        this.width = width;
        this.height = height;
        this.rssiv = rssiv;
    }

    @Override
    public I_Position getCentrum() { return centrum; }

    @Override
    public float getWidth() { return width; }

    @Override
    public float getHeight() { return height; }

    @Override
    public I_RSSIV getRssiv() { return rssiv; }
}