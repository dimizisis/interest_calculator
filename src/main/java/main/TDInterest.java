package main;

import java.util.*;

public class TDInterest {
	
	private final Set<JavaFile> javaFiles;
	private double totalInterest;

	private static final Double HOURLY_WAGE = 39.44;

	public TDInterest(Set<JavaFile> javaFiles) {
		totalInterest = 0.0;
		this.javaFiles = javaFiles;
		calculateTotal();
	}

	private void calculateTotal() {

		for (JavaFile jf : javaFiles) {
			// Calculate similarity
			AbstractQueue<Similarity> similarityOfFiles = new PriorityQueue<>(Collections.reverseOrder());
			for (JavaFile jf2 : javaFiles)
				if (!Objects.equals(jf, jf2))
                    similarityOfFiles.add(new Similarity(jf2, 1 - calculateSimilarityIndex(jf, jf2)));

			Set<JavaFile> topFiveNeighbors = new HashSet<>();

			// Keep top 5
			for (int i=0; i<5; ++i)
				topFiveNeighbors.add(Objects.requireNonNull(similarityOfFiles.poll()).getJf());

			// Get optimal metrics
			QualityMetrics optimalMetrics = getOptimalMetrics(topFiveNeighbors);

        	// Normalize (add one smoothing)
			optimalMetrics.normalize();
            
			// Calculate the interest per LOC
            // Get difference optimal to actual
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

            double avgInterestPerLOC = (sumInterestPerLOC) / 10;
            
            // Calculate the interest in AVG LOC
            double interestInAvgLOC = avgInterestPerLOC * Main.K;

            // Calculate the interest in hours
            double interestInHours = interestInAvgLOC / 25;

            // Calculate the interest in dollars
            double interestInEuros = interestInHours * HOURLY_WAGE;

            // Sum
            totalInterest += interestInEuros;
		}
	}

	private Double calculateSimilarityIndex(JavaFile jf, JavaFile jf2) {
		int jfClasses = (jf.getQualityMetrics().getClassesNum()==0) ? 1 : jf.getQualityMetrics().getClassesNum();
		int jfComplexity = (jf.getQualityMetrics().getComplexity()==0) ? 1 : jf.getQualityMetrics().getComplexity();
		double jfFunctions = (jf.getQualityMetrics().getWMC()==0) ? 1 : jf.getQualityMetrics().getWMC();
		int jfLOC = (jf.getQualityMetrics().getLOC()==0) ? 1 : jf.getQualityMetrics().getLOC();

		return (Math.abs(jf.getQualityMetrics().getClassesNum() - jf2.getQualityMetrics().getClassesNum()) * 1.0 / jfClasses
				+ Math.abs(jf.getQualityMetrics().getComplexity() - jf2.getQualityMetrics().getComplexity()) * 1.0 / jfComplexity
				+ Math.abs(jf.getQualityMetrics().getWMC() - jf2.getQualityMetrics().getWMC()) / jfFunctions
				+ Math.abs(jf.getQualityMetrics().getLOC() - jf2.getQualityMetrics().getLOC()) * 1.0 / jfLOC
				/ 4);
	}

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

	public Double getTotalInterest() {
		return totalInterest;
	}
}
