package Hmm;

public class Cellule {
    public int nb;
    public float statNext; // (Row Probability)
    public float statPrev; //   (Column Probability)

    public Cellule(int _nb) {
        this.nb = _nb;
        this.statNext = 0.0f;
        this.statPrev = 0.0f;
    }
}