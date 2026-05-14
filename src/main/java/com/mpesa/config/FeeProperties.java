package com.mpesa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.fee")
public class FeeProperties {

    private BigDecimal tier1 = new BigDecimal("100");
    private BigDecimal tier2 = new BigDecimal("500");
    private BigDecimal tier3 = new BigDecimal("1000");
    private BigDecimal rate1 = BigDecimal.ZERO;
    private BigDecimal rate2 = new BigDecimal("13");
    private BigDecimal rate3 = new BigDecimal("25");
    private BigDecimal rate4 = new BigDecimal("30");

    public BigDecimal getTier1() { return tier1; }
    public void setTier1(BigDecimal tier1) { this.tier1 = tier1; }
    public BigDecimal getTier2() { return tier2; }
    public void setTier2(BigDecimal tier2) { this.tier2 = tier2; }
    public BigDecimal getTier3() { return tier3; }
    public void setTier3(BigDecimal tier3) { this.tier3 = tier3; }
    public BigDecimal getRate1() { return rate1; }
    public void setRate1(BigDecimal rate1) { this.rate1 = rate1; }
    public BigDecimal getRate2() { return rate2; }
    public void setRate2(BigDecimal rate2) { this.rate2 = rate2; }
    public BigDecimal getRate3() { return rate3; }
    public void setRate3(BigDecimal rate3) { this.rate3 = rate3; }
    public BigDecimal getRate4() { return rate4; }
    public void setRate4(BigDecimal rate4) { this.rate4 = rate4; }
}
