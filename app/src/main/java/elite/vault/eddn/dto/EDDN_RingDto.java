package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_RingDto {
    @SerializedName("Name") private String name;
    @SerializedName("RingClass") private String ringClass;   // "Metal Rich", "Icy", etc.
    @SerializedName("MassMT") private Double massMt;
    @SerializedName("InnerRad") private Double innerRad;
    @SerializedName("OuterRad") private Double outerRad;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRingClass() {
        return ringClass;
    }

    public void setRingClass(String ringClass) {
        this.ringClass = ringClass;
    }

    public Double getMassMt() {
        return massMt;
    }

    public void setMassMt(Double massMt) {
        this.massMt = massMt;
    }

    public Double getInnerRad() {
        return innerRad;
    }

    public void setInnerRad(Double innerRad) {
        this.innerRad = innerRad;
    }

    public Double getOuterRad() {
        return outerRad;
    }

    public void setOuterRad(Double outerRad) {
        this.outerRad = outerRad;
    }
}
