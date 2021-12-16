package infrastructure.interest;

import java.util.Objects;

public final class QualityMetrics {

    private String sha;
    private Integer classesNum;
    private Double complexity;
    private Integer DIT;
    private Integer NOCC;
    private Double RFC;
    private Double LCOM;
    private Double WMC;
    private Double NOM;
    private Double MPC;
    private Integer DAC;
    private Integer oldSIZE1;
    private Double CBO;
    private Integer SIZE1;
    private Integer SIZE2;

    public QualityMetrics(String sha, Integer classesNum, Double complexity, Integer DIT, Integer NOCC, Double RFC, Double LCOM, Double WMC, Double NOM, Double MPC, Integer DAC, Integer oldSIZE1, Double CBO, Integer SIZE1, Integer SIZE2) {
        this.sha = sha;
        this.classesNum = classesNum;
        this.complexity = complexity;
        this.DIT = DIT;
        this.NOCC = NOCC;
        this.RFC = RFC;
        this.LCOM = LCOM;
        this.WMC = WMC;
        this.NOM = NOM;
        this.MPC = MPC;
        this.DAC = DAC;
        this.oldSIZE1 = oldSIZE1;
        this.CBO = CBO;
        this.SIZE1 = SIZE1;
        this.SIZE2 = SIZE2;
    }

    public QualityMetrics() {
        this.sha = sha;
        this.classesNum = 0;
        this.complexity = 0.0;
        this.DIT = 0;
        this.NOCC = 0;
        this.RFC = 0.0;
        this.LCOM = 0.0;
        this.WMC = 0.0;
        this.NOM = 0.0;
        this.MPC = 0.0;
        this.DAC = 0;
        this.SIZE1 = 0;
        this.SIZE2 = 0;
        this.oldSIZE1 = 0;
    }

    public QualityMetrics(String sha) {
        this.sha = sha;
        this.classesNum = 0;
        this.complexity = 0.0;
        this.DIT = 0;
        this.NOCC = 0;
        this.RFC = 0.0;
        this.LCOM = 0.0;
        this.WMC = 0.0;
        this.NOM = 0.0;
        this.MPC = 0.0;
        this.DAC = 0;
        this.SIZE1 = 0;
        this.SIZE2 = 0;
        this.oldSIZE1 = 0;
    }

    public void normalize() {
        if (DIT <= 0)
            DIT = 1;

        if (NOCC <= 0)
            NOCC = 1;

        if (RFC <= 0)
            RFC = 1.0;

        if (LCOM <= 0)
            LCOM = 1.0;

        if (WMC <= 0)
            WMC = 1.0;

        if (NOM <= 0)
            NOM = 1.0;

        if (MPC <= 0)
            MPC = 1.0;

        if (DAC <= 0)
            DAC = 1;

        if (SIZE1 <= 0)
            SIZE1 = 1;

        if (SIZE2 <= 0)
            SIZE2 = 1;
    }

    public void zero() {
        this.classesNum = 0;
        this.complexity = 0.0;
        this.DIT = 0;
        this.NOCC = 0;
        this.RFC = 0.0;
        this.LCOM = 0.0;
        this.WMC = 0.0;
        this.NOM = 0.0;
        this.MPC = 0.0;
        this.DAC = 0;
        this.CBO = 0.0;
        this.SIZE1 = 0;
        this.SIZE2 = 0;
        this.oldSIZE1 = 0;
    }

    public Integer getClassesNum() {
        return classesNum;
    }

    public Double getComplexity() {
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

    public Double getMPC() {
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

    public Integer getOldSIZE1() {
        return oldSIZE1;
    }

    public Double getCBO() {
        return CBO;
    }

    public String getSha() {
        return this.sha;
    }

    public void setClassesNum(Integer classesNum) {
        this.classesNum = classesNum;
    }

    public void setComplexity(Double complexity) {
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

    public void setMPC(Double MPC) {
        this.MPC = MPC;
    }

    public void setDAC(Integer DAC) {
        this.DAC = DAC;
    }

    public void setOldSIZE1(Integer oldSIZE1) {
        this.oldSIZE1 = oldSIZE1;
    }

    public void setSIZE1(Integer SIZE1) {
        this.SIZE1 = SIZE1;
    }

    public void setSIZE2(Integer SIZE2) {
        this.SIZE2 = SIZE2;
    }

    public void setCBO(Double CBO) {
        this.CBO = CBO;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetrics that = (QualityMetrics) o;
        return Objects.equals(classesNum, that.classesNum) && Objects.equals(complexity, that.complexity) && Objects.equals(DIT, that.DIT) && Objects.equals(NOCC, that.NOCC) && Objects.equals(RFC, that.RFC) && Objects.equals(LCOM, that.LCOM) && Objects.equals(WMC, that.WMC) && Objects.equals(NOM, that.NOM) && Objects.equals(MPC, that.MPC) && Objects.equals(DAC, that.DAC) && Objects.equals(CBO, that.CBO) && Objects.equals(SIZE1, that.SIZE1) && Objects.equals(SIZE2, that.SIZE2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classesNum, complexity, DIT, NOCC, RFC, LCOM, WMC, NOM, MPC, DAC, CBO, SIZE1, SIZE2);
    }
}
