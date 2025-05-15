package com.nhncloud.pca.model.subject;

import lombok.Data;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class SubjectInfo {
    //국가 C
    private String country;
    //주,도 이름 ST
    private String stateOrProvince;
    //도시, 지역 이름 L
    private String locality;
    //회사 이름 O
    private String organization;
    //부서 이름 OU
    private String organizationalUnit;
    //CN
    private String commonName;
    //이메일 주소 E
    private String emailAddress;

    public String toDistinguishedName() {
        return Stream.of(
                Optional.ofNullable(this.getCountry()).map(value -> "C=" + value),
                Optional.ofNullable(this.getStateOrProvince()).map(value -> "ST=" + value),
                Optional.ofNullable(this.getLocality()).map(value -> "L=" + value),
                Optional.ofNullable(this.getOrganization()).map(value -> "O=" + value),
                Optional.ofNullable(this.getOrganizationalUnit()).map(value -> "OU=" + value),
                Optional.ofNullable(this.getCommonName()).map(value -> "CN=" + value),
                Optional.ofNullable(this.getEmailAddress()).map(value -> "E=" + value)
            )
            .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
            .collect(Collectors.joining(", "));
    }
}
