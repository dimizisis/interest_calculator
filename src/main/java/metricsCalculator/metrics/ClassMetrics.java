package metricsCalculator.metrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassMetrics extends Metrics {
	protected int noc;
	private float rfc;
	private int dit;
	private boolean isPublicClass;
	private boolean isAbstract;
	private boolean visited;
	private final Set<String> afferentCoupledClasses; // Ca

	public ClassMetrics() {
		this.setNoc(0);
		this.setDit(-1);
		this.setRfc(0);
		this.setClassesNum(0);
		this.afferentCoupledClasses = ConcurrentHashMap.newKeySet();
	}

	public void setGMOODLowLevel() {
		this.setDsc(1);
		this.setNom(this.getWmc());
		if (this.getNoc() > 0 && this.getAna() == 0)
			setNoh(1);
	}
	
	public void setQMOODHighLevel() {
		this.setReusability(-0.25 * this.getDcc() + 0.25 * this.getCamc() + 0.5 * this.getNpm() + 0.5 * this.getDsc());
	    this.setFlexibility(-0.25 * this.getDcc() + 0.25 * this.getDam() + 0.5 * this.getMoa() + 0.5 * this.getNop());
	    this.setUnderstandability(-0.33 * this.getAna() + 0.33 * this.getDam() + 0.33 * this.getCamc() - 0.33 * this.getDcc() - 0.33 * this.getNop() - 0.33 * this.getNom() - 0.33 * this.getDsc());
	    this.setFunctionality(0.12 * this.getCamc() + 0.22 * this.getNop() + 0.22 * this.getNpm() + 0.22 * this.getDsc() + 0.22 * this.getNoh());
	    this.setExtendibility(0.5 * this.getAna() - 0.5 * this.getDcc() + 0.5 * this.getMfa() + 0.5 * this.getNop());
	    this.setEffectiveness(0.2 * this.getAna() + 0.2 * this.getDam() + 0.2 * this.getMoa() + 0.2 * this.getMfa() + 0.2 * this.getNop());
	}

	public float getDsc() { return this.dsc; }
	public float getRfc() { return rfc; }
	public int getNoc() { return this.noc; }
	public void setRfc(float r) { this.rfc = r; }
	public void setDit(int d) { this.dit = d; }
	public int getDit() { return this.dit; }
	public int getCa() { return this.afferentCoupledClasses.size(); }
	public void setDsc (int dsc){ this.dsc = dsc; }
	public void setNoc (int noc){ this.noc = noc; }
	public void setRfc (int rfc){ this.rfc = rfc; }
	public void setClassesNum (int classesNum){ this.classesNum = classesNum; }
	public int getClassesNum() { return this.classesNum; }

	public void addAfferentCoupling(String name) { this.afferentCoupledClasses.add(name); }

	public boolean isPublic() { return this.isPublicClass; }
	public void setPublic() { this.isPublicClass = true; }

	public void setAbstract() { this.isAbstract = true; }
	public boolean isAbstract() { return this.isAbstract; }

	public void setVisited() { this.visited = true; }

	public void incNpm() { this.npm += 1.0F; }
	public void incNoc() { this.setNoc(this.getNoc() + 1); }

	@Override
	public String toString() {
		float size = 1.0F;
		float hierarchies = -1.0F;
		float abstraction = this.getMfa();
		float encapsulation = this.getDam();
		float coupling = this.getCbo();
		float cohesion = this.getLcom();
		float composition = this.getMoa();
		float ancestors = this.getAna();
		float polymorphism = this.getNop();
		float messaging = getNpm();
		float complexity = this.getWmc();
		return "\t\t" + size + "\t\t" + hierarchies + "\t\t" + abstraction
				+ "\t\t" + encapsulation + "\t\t\t" + coupling + "\t\t"
				+ cohesion + "\t\t" + composition + "\t\t" + ancestors + "\t\t"
				+ polymorphism + "\t\t" + messaging + "\t\t" + complexity;
	}

	public String getCsvOutput(){
		StringBuilder builder = new StringBuilder();

		/*
	 	* Chidamber & Kemerer metrics
	 	*/
		builder.append(this.getWmc()).append(";");
		builder.append(this.getDit()).append(";");
		builder.append(this.getNoc()).append(";");
		builder.append(this.getRfc()).append(";");
		builder.append(this.getLcom()).append(";");
		/*
		 *  Li & Henry metrics
		 */
		if (this.getWmcCC() == -1) builder.append("-1000;");
			else builder.append(this.getWmcCC()).append(";");
		builder.append(this.getWmc()).append(";"); // (NOM: number of methods) as in WMC of C&K metrics
		builder.append(this.getMpc()).append(";");
		builder.append(this.getDac()).append(";");
		builder.append(this.getSize1()).append(";");
		builder.append(this.getSize2()).append(";");
		builder.append(this.getCbo()).append(";");
		builder.append(this.getClassesNum());

		return builder.toString();
	}
}
