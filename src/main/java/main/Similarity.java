package main;

public final class Similarity implements Comparable<Similarity>{

    private final JavaFile jf;
    private final Double similarity;

    public Similarity(JavaFile jf, Double similarity) {
        this.jf = jf;
        this.similarity = similarity;
    }

    public JavaFile getJf() { return jf; }

    public Double getSimilarity() { return similarity; }

    @Override
    public int compareTo(Similarity s) {
        return this.getSimilarity().compareTo(s.getSimilarity());
    }
}
