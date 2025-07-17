package com.nhncloud.pca.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountKeyResponse {

    @JsonProperty("kty")
    private String keyType;

    // RSA 키 필드들
    @JsonProperty("n")
    private String modulus;

    @JsonProperty("e")
    private String exponent;

    @JsonProperty("d")
    private String privateExponent;

    @JsonProperty("p")
    private String firstPrimeFactor;

    @JsonProperty("q")
    private String secondPrimeFactor;

    @JsonProperty("dp")
    private String firstFactorCrtExponent;

    @JsonProperty("dq")
    private String secondFactorCrtExponent;

    @JsonProperty("qi")
    private String firstCrtCoefficient;

    // EC 키 필드들
    @JsonProperty("crv")
    private String curve;

    @JsonProperty("x")
    private String xCoordinate;

    @JsonProperty("y")
    private String yCoordinate;

    // RSA 키용 생성자
    public AccountKeyResponse(String keyType, String modulus, String exponent, String privateExponent,
                             String firstPrimeFactor, String secondPrimeFactor, String firstFactorCrtExponent,
                             String secondFactorCrtExponent, String firstCrtCoefficient) {
        this.keyType = keyType;
        this.modulus = modulus;
        this.exponent = exponent;
        this.privateExponent = privateExponent;
        this.firstPrimeFactor = firstPrimeFactor;
        this.secondPrimeFactor = secondPrimeFactor;
        this.firstFactorCrtExponent = firstFactorCrtExponent;
        this.secondFactorCrtExponent = secondFactorCrtExponent;
        this.firstCrtCoefficient = firstCrtCoefficient;
    }

    // EC 키용 생성자
    public AccountKeyResponse(String keyType, String curve, String xCoordinate, String yCoordinate, String privateExponent) {
        this.keyType = keyType;
        this.curve = curve;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.privateExponent = privateExponent;
    }
}