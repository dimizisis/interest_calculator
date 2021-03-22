package infrastructure.interest;

import data.Globals;
import main.Main;

import java.util.*;

public class JavaFile {
    private String path;
    private QualityMetrics qualityMetrics;
    private TDInterest interest;

    public JavaFile(String path) {
        this.path = path;
        qualityMetrics = new QualityMetrics();
        this.interest = new TDInterest();
    }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public QualityMetrics getQualityMetrics() { return qualityMetrics; }

    public void setQualityMetrics(QualityMetrics qualityMetrics) { this.qualityMetrics = qualityMetrics; }

    public TDInterest getInterest() { return interest; }

    public void setInterest(TDInterest interest) { this.interest = interest; }

    class TDInterest {

        private final Double HOURLY_WAGE = 39.44;
        private Double interestInEuros;
        private Double interestInHours;
        private Double interestInAvgLOC;
        private Double avgInterestPerLOC;
        private Double sumInterestPerLOC;

        public TDInterest() {
            this.interestInEuros = 0.0;
//            this.calculate();
        }

        public void calculate() {
            /* Calculate similarity */
            AbstractQueue<Similarity> similarityOfFiles = calculateSimilarities(JavaFile.this);

            /* Find Top 5 Neighbors */
            Set<JavaFile> topFiveNeighbors = findTopFiveNeighbors(similarityOfFiles);

            /* Get optimal metrics & normalize (add one smoothing) */
            QualityMetrics optimalMetrics = getOptimalMetrics(topFiveNeighbors);
            optimalMetrics.normalize();

			/* Calculate the interest per LOC
               Get difference optimal to actual */
			this.sumInterestPerLOC = calculateInterestPerLoc(JavaFile.this, optimalMetrics);

			this.avgInterestPerLOC = (sumInterestPerLOC) / 10;

            /* Calculate the interest in AVG LOC */
			this.interestInAvgLOC = avgInterestPerLOC * Main.K;

            /* Calculate the interest in hours */
            this.interestInHours = interestInAvgLOC / 25;

            /* Calculate the interest in dollars */
            this.interestInEuros = interestInHours * HOURLY_WAGE;

			System.out.println("File: " + JavaFile.this.path + " | Interest: " + this.interestInEuros);
        }

        /**
         * Calculates the interest per loc for the file we
         * are referring to, based on the optical metrics
         * found from the top 5 neighbors.
         *
         * @param jf             the file we are referring to
         * @param optimalMetrics the optical metrics object
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
         * Calculates the similarity between a file (jf)
         * and all the other files that are available
         *
         * @param jf the file we are referring to
         * @return the similarity of files (priority queue)
         */
        private AbstractQueue<Similarity> calculateSimilarities(JavaFile jf) {
            AbstractQueue<Similarity> similarityOfFiles = new PriorityQueue<>(Collections.reverseOrder());
            for (JavaFile jf2 : Globals.getJavaFiles())
                if (!Objects.equals(jf, jf2))
                    similarityOfFiles.add(new Similarity(jf, jf2, 1 - calculateSimilarityIndex(jf, jf2)));
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
            // Keep top 5
            for (int i = 0; i < 5; ++i)
                topFiveNeighbors.add(Objects.requireNonNull(similarityOfFiles.poll()).getJf2());
            return topFiveNeighbors;
        }

        /**
         * Calculates the similarity between two java files
         * (jf1, jf2) based on specific quality metrics
         *
         * @param jf1 the first java file
         * @param jf2 the second java file
         * @return the similarity of these two files (double)
         */
        private Double calculateSimilarityIndex(JavaFile jf1, JavaFile jf2) {
            int jfClasses = (jf1.getQualityMetrics().getClassesNum() == 0) ? 1 : jf1.getQualityMetrics().getClassesNum();
            int jfComplexity = (jf1.getQualityMetrics().getComplexity() == 0) ? 1 : jf1.getQualityMetrics().getComplexity();
            double jfFunctions = (jf1.getQualityMetrics().getWMC() == 0) ? 1 : jf1.getQualityMetrics().getWMC();
            int jfLOC = (jf1.getQualityMetrics().getLOC() == 0) ? 1 : jf1.getQualityMetrics().getLOC();

            return (Math.abs(jf1.getQualityMetrics().getClassesNum() - jf2.getQualityMetrics().getClassesNum()) * 1.0 / jfClasses
                    + Math.abs(jf1.getQualityMetrics().getComplexity() - jf2.getQualityMetrics().getComplexity()) * 1.0 / jfComplexity
                    + Math.abs(jf1.getQualityMetrics().getWMC() - jf2.getQualityMetrics().getWMC()) / jfFunctions
                    + Math.abs(jf1.getQualityMetrics().getLOC() - jf2.getQualityMetrics().getLOC()) * 1.0 / jfLOC
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
                    .max(Integer::compare)
                    .orElse(0));
            optimalMetrics.setNOCC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getNOCC())
                    .max(Integer::compare)
                    .orElse(0));
            optimalMetrics.setRFC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getRFC())
                    .max(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setLCOM(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getLCOM())
                    .max(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setWMC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getWMC())
                    .max(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setNOM(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getNOM())
                    .max(Double::compare)
                    .orElse(0.0));
            optimalMetrics.setMPC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getMPC())
                    .max(Integer::compare)
                    .orElse(0));
            optimalMetrics.setDAC(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getDAC())
                    .max(Integer::compare)
                    .orElse(0));
            optimalMetrics.setSIZE1(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getSIZE1())
                    .max(Integer::compare)
                    .orElse(0));
            optimalMetrics.setSIZE2(topFiveNeighbors
                    .stream()
                    .map(n -> n.getQualityMetrics().getSIZE2())
                    .max(Integer::compare)
                    .orElse(0));
            return optimalMetrics;
        }

        public Double getInterestInEuros() {
            return interestInEuros;
        }
    }
}
