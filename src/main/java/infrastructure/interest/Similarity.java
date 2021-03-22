package infrastructure.interest;

public final class Similarity implements Comparable<Similarity>{

    private final JavaFile jf1;
    private final JavaFile jf2;
    private final Double similarity;

    public Similarity(JavaFile jf1, JavaFile jf2, Double similarity) {
        this.jf1 = jf1;
        this.jf2 = jf2;
        this.similarity = similarity;
    }

    public JavaFile getJf1() { return jf1; }
    public JavaFile getJf2() { return jf2; }

    public Double getSimilarity() { return similarity; }

    @Override
    public int compareTo(Similarity s) {
        return this.getSimilarity().compareTo(s.getSimilarity());
    }
}
