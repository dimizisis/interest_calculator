package infrastructure.interest;

import data.Globals;

import java.util.*;

public class JavaFile {
    private String path;
    private final QualityMetrics qualityMetrics;
    private final TDInterest interest;
    private Kappa k;

    public JavaFile(String path) {
        this.path = path;
        this.qualityMetrics = new QualityMetrics();
        this.interest = new TDInterest();
        this.setK(new Kappa());
    }

    public void calculateInterest() {
        this.getK().update(this.getQualityMetrics().getOldSIZE1());
        this.getInterest().calculate();
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public QualityMetrics getQualityMetrics() {
        return this.qualityMetrics;
    }

    public Double getInterestInEuros() {
        return this.interest.getInterestInHours();
    }

    public TDInterest getInterest() {
        return this.interest;
    }

    public Double getKappaValue() {
        return this.k.getValue();
    }

    public Kappa getK() {
        return this.k;
    }

    public void setK(Kappa k) {
        this.k = k;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaFile javaFile = (JavaFile) o;
        return path.equals(javaFile.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    class TDInterest {

        private final Double HOURLY_WAGE = 39.44;
        private Double interestInEuros;
        private Double interestInHours;
        private Double interestInAvgLOC;
        private Double avgInterestPerLOC;
        private Double sumInterestPerLOC;

        public TDInterest() {
            this.interestInEuros = 0.0;
            this.interestInHours = 0.0;
            this.interestInAvgLOC = 0.0;
            this.avgInterestPerLOC = 0.0;
            this.sumInterestPerLOC = 0.0;
        }

        /**
         * Calculates the interest for the file we are
         * referring to, finding the optimal metrics
         * and the top 5 neighbors.
         */
        private void calculate() {
            /* Calculate similarity */
            AbstractQueue<Similarity> similarityOfFiles = calculateSimilarities();

            /* No need to proceed to interest calculation */
            if (similarityOfFiles.isEmpty())
                return;

            /* Find Top 5 Neighbors */
            Set<JavaFile> topFiveNeighbors = findTopFiveNeighbors(similarityOfFiles);

            if (Objects.isNull(topFiveNeighbors))
                return;

            /* Get optimal metrics & normalize (add one smoothing) */
            QualityMetrics optimalMetrics = getOptimalMetrics(topFiveNeighbors);
            optimalMetrics.normalize();

			/* Calculate the interest per LOC
               Get difference optimal to actual */
            this.setSumInterestPerLOC(this.calculateInterestPerLoc(JavaFile.this, optimalMetrics));

            /* Calculate the average interest per line of code */
            this.setAvgInterestPerLOC(this.getSumInterestPerLOC() / 10);

            /* Calculate the interest in AVG LOC */
            this.setInterestInAvgLOC(this.getAvgInterestPerLOC() * JavaFile.this.getK().getValue());

            /* Calculate the interest in hours */
            this.setInterestInHours(this.getInterestInAvgLOC() / 25);

            /* Calculate the interest in dollars */
            this.setInterestInEuros(this.getInterestInHours() * this.HOURLY_WAGE);

            System.out.println("File: " + JavaFile.this.path + " | Interest: " + this.getInterestInEuros());
            System.out.println("Kappa: " + JavaFile.this.getK().getValue());
            System.out.println("Revisions: " + Globals.getRevisions());
        }

        /**
         * Calculates the interest per loc for the file we
         * are referring to, based on the optimal metrics
         * found from the top 5 neighbors.
         *
         * @param jf             the file we are referring to
         * @param optimalMetrics the optimal metrics object
         * @return the interest per line of code (double)
         */
        private Double calculateInterestPerLoc(JavaFile jf, QualityMetrics optimalMetrics) {
            double sumInterestPerLOC = 0.0;
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getDIT() - optimalMetrics.getDIT()) * 1.0 / optimalMetrics.getDIT();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getNOCC() - optimalMetrics.getNOCC()) * 1.0 / optimalMetrics.getNOCC();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getRFC() - optimalMetrics.getRFC()) / optimalMetrics.getRFC();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getLCOM() - optimalMetrics.getLCOM()) / optimalMetrics.getLCOM();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getWMC() - optimalMetrics.getWMC()) / optimalMetrics.getWMC();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getNOM() - optimalMetrics.getNOM()) / optimalMetrics.getNOM();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getMPC() - optimalMetrics.getMPC()) * 1.0 / optimalMetrics.getMPC();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getDAC() - optimalMetrics.getDAC()) * 1.0 / optimalMetrics.getDAC();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getSIZE1() - optimalMetrics.getSIZE1()) * 1.0 / optimalMetrics.getSIZE1();
            sumInterestPerLOC += Math.abs(jf.getQualityMetrics().getSIZE2() - optimalMetrics.getSIZE2()) * 1.0 / optimalMetrics.getSIZE2();
            return sumInterestPerLOC;
        }

        /**
         * Calculates the similarity between a file
         * and all the other files that are available
         *
         * @return the similarity of files (priority queue)
         */
        private AbstractQueue<Similarity> calculateSimilarities() {
            AbstractQueue<Similarity> similarityOfFiles = new PriorityQueue<>(Collections.reverseOrder());
            for (JavaFile jf : Globals.getJavaFiles())
                if (!Objects.equals(JavaFile.this, jf))
                    similarityOfFiles.add(new Similarity(JavaFile.this, jf, 1 - calculateSimilarityIndex(jf)));
            return similarityOfFiles;
        }

        /**
         * Finds the top five neighbors of the file
         * we are referring to, based on the similarity
         * index.
         *
         * @param similarityOfFiles all the similarity indexes
         * @return the top five neighbors (hash set)
         */
        private Set<JavaFile> findTopFiveNeighbors(AbstractQueue<Similarity> similarityOfFiles) {
            Set<JavaFile> topFiveNeighbors = new HashSet<>();
            if (similarityOfFiles.size() < 5)
                return null;
            /* Keep top 5 */
            for (int i = 0; i < 5; ++i)
                topFiveNeighbors.add(Objects.requireNonNull(similarityOfFiles.poll()).getJf2());
            return topFiveNeighbors;
        }

        /**
         * Calculates the similarity between two java files
         * (jf1, jf2) based on specific quality metrics
         *
         * @param jf2 the second java file
         * @return the similarity of these two files (double)
         */
        private Double calculateSimilarityIndex(JavaFile jf2) {
            int jfClasses = (JavaFile.this.getQualityMetrics().getClassesNum() == 0) ? 1 : JavaFile.this.getQualityMetrics().getClassesNum();
            double jfComplexity = (JavaFile.this.getQualityMetrics().getComplexity() == 0.0) ? 1.0 : JavaFile.this.getQualityMetrics().getComplexity();
            double jfFunctions = (JavaFile.this.getQualityMetrics().getWMC() == 0) ? 1 : JavaFile.this.getQualityMetrics().getWMC();
            int jfLOC = (JavaFile.this.getQualityMetrics().getSIZE1() == 0) ? 1 : JavaFile.this.getQualityMetrics().getSIZE1();

            return (Math.abs(JavaFile.this.getQualityMetrics().getClassesNum() - jf2.getQualityMetrics().getClassesNum()) * 1.0 / jfClasses
                    + Math.abs(JavaFile.this.getQualityMetrics().getComplexity() - jf2.getQualityMetrics().getComplexity()) / jfComplexity
                    + Math.abs(JavaFile.this.getQualityMetrics().getWMC() - jf2.getQualityMetrics().getWMC()) / jfFunctions
                    + Math.abs(JavaFile.this.getQualityMetrics().getSIZE1() - jf2.getQualityMetrics().getSIZE1()) * 1.0 / jfLOC
                    / 4);
        }

        /**
         * Returns the optimal metrics found from the set
         * of the top five neighbors.
         *
         * @param topFiveNeighbors the set of neighbors
         * @return a QualityMetrics object containing the 'holy grail' metrics
         */
        private QualityMetrics getOptimalMetrics(Set<JavaFile> topFiveNeighbors) {
            QualityMetrics optimalMetrics = new QualityMetrics();
            optimalMetrics.setDIT(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getDIT())
                    .min(Integer::compare)
                    .orElse(0));
            optimalMetrics.setNOCC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getNOCC())
                    .min(Integer::compare)
                    .orElse(0));
            optimalMetrics.setRFC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getRFC())
                    .min(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setLCOM(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getLCOM())
                    .min(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setWMC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getWMC())
                    .min(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setNOM(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getNOM())
                    .min(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setMPC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getMPC())
                    .min(Integer::compare)
                    .orElse(0));
            optimalMetrics.setDAC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getDAC())
                    .min(Integer::compare)
                    .orElse(0));
            optimalMetrics.setSIZE1(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getSIZE1())
                    .min(Integer::compare)
                    .orElse(0));
            optimalMetrics.setSIZE2(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getSIZE2())
                    .min(Integer::compare)
                    .orElse(0));
            return optimalMetrics;
        }

        public Double getInterestInHours() {
            return this.interestInHours;
        }

        public Double getInterestInAvgLOC() {
            return this.interestInAvgLOC;
        }

        public Double getAvgInterestPerLOC() {
            return this.avgInterestPerLOC;
        }

        public Double getSumInterestPerLOC() {
            return this.sumInterestPerLOC;
        }

        public Double getInterestInEuros() {
            return this.interestInEuros;
        }

        public void setInterestInEuros(Double interestInEuros) {
            this.interestInEuros = interestInEuros;
        }

        public void setInterestInHours(Double interestInHours) {
            this.interestInHours = interestInHours;
        }

        public void setInterestInAvgLOC(Double interestInAvgLOC) {
            this.interestInAvgLOC = interestInAvgLOC;
        }

        public void setAvgInterestPerLOC(Double avgInterestPerLOC) {
            this.avgInterestPerLOC = avgInterestPerLOC;
        }

        public void setSumInterestPerLOC(Double sumInterestPerLOC) {
            this.sumInterestPerLOC = sumInterestPerLOC;
        }
    }

    class Kappa {

        private Double value;

        public Kappa() {
            this.setValue(0.0);
        }

        public void update(Integer oldLOC) {
            this.setValue((this.getValue() * (Globals.getRevisions() - 1) + (Math.abs(JavaFile.this.getQualityMetrics().getSIZE1() - oldLOC))) / Globals.getRevisions());
        }

        public Double getValue() {
            return this.value;
        }

        public void setValue(Double newVal) {
            this.value = newVal;
        }
    }
}
