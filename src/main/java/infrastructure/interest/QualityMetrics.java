package infrastructure.interest;

public final class QualityMetrics {

    private Integer classesNum;
    private Integer LOC;
    private Integer complexity;
    private Integer DIT;
    private Integer NOCC;
    private Double RFC;
    private Double LCOM;
    private Double WMC;
    private Double NOM;
    private Integer MPC;
    private Integer DAC;
    private Integer SIZE1;
    private Integer SIZE2;

    public QualityMetrics() {
        this.classesNum = 0;
        this.LOC = 0;
        this.complexity = 0;
        this.DIT = -1;
        this.NOCC = -1;
        this.RFC = -1.0;
        this.LCOM = -1.0;
        this.WMC = -1.0;
        this.NOM = -1.0;
        this.MPC = -1;
        this.DAC = -1;
        this.SIZE1 = -1;
        this.SIZE2 = -1;
    }

    public QualityMetrics(Integer classesNum, Integer LOC, Integer complexity, Integer DIT, Integer NOCC,
                          Double RFC, Double LCOM, Double WMC, Double NOM, Integer MPC, Integer DAC, Integer SIZE1, Integer SIZE2) {
        this.classesNum = classesNum;
        this.LOC = LOC;
        this.complexity = complexity;
        this.DIT = DIT;
        this.NOCC = NOCC;
        this.RFC = RFC;
        this.LCOM = LCOM;
        this.WMC = WMC;
        this.NOM = NOM;
        this.MPC = MPC;
        this.DAC = DAC;
        this.SIZE1 = SIZE1;
        this.SIZE2 = SIZE2;
    }

    public void normalize() {
        if (DIT == 0)
            DIT = 1;

        if (NOCC == 0)
            NOCC = 1;

        if (RFC == 0)
            RFC = 1.0;

        if (LCOM == 0)
            LCOM = 1.0;

        if (WMC == 0)
            WMC = 1.0;

        if (NOM == 0)
            NOM = 1.0;

        if (MPC == 0)
            MPC = 1;

        if (DAC == 0)
            DAC = 1;

        if (SIZE1 == 0)
            SIZE1 = 1;

        if (SIZE2 == 0)
            SIZE2 = 1;
    }

    public Integer getClassesNum() {
        return classesNum;
    }

    public Integer getLOC() {
        return LOC;
    }

    public Integer getComplexity() {
        return complexity;
    }

    public Integer getDIT() {
        return DIT;
    }

    public Integer getNOCC() {
        return NOCC;
    }

    public Double getRFC() {
        return RFC;
    }

    public Double getLCOM() {
        return LCOM;
    }

    public Double getWMC() {
        return WMC;
    }

    public Double getNOM() {
        return NOM;
    }

    public Integer getMPC() {
        return MPC;
    }

    public Integer getDAC() {
        return DAC;
    }

    public Integer getSIZE1() {
        return SIZE1;
    }

    public Integer getSIZE2() {
        return SIZE2;
    }

    public void setClassesNum(Integer classesNum) {
        this.classesNum = classesNum;
    }

    public void setLOC(Integer LOC) {
        this.LOC = LOC;
    }

    public void setComplexity(Integer complexity) {
        this.complexity = complexity;
    }

    public void setDIT(Integer DIT) {
        this.DIT = DIT;
    }

    public void setNOCC(Integer NOCC) {
        this.NOCC = NOCC;
    }

    public void setRFC(Double RFC) {
        this.RFC = RFC;
    }

    public void setLCOM(Double LCOM) {
        this.LCOM = LCOM;
    }

    public void setWMC(Double WMC) {
        this.WMC = WMC;
    }

    public void setNOM(Double NOM) {
        this.NOM = NOM;
    }

    public void setMPC(Integer MPC) {
        this.MPC = MPC;
    }

    public void setDAC(Integer DAC) {
        this.DAC = DAC;
    }

    public void setSIZE1(Integer SIZE1) {
        this.SIZE1 = SIZE1;
    }

    public void setSIZE2(Integer SIZE2) {
        this.SIZE2 = SIZE2;
    }
}
