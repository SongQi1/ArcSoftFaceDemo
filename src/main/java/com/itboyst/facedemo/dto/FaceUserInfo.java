package com.itboyst.facedemo.dto;


import lombok.Data;

@Data
public class FaceUserInfo {

    private String faceId;
    private String name;
    private Integer similarValue;
    private Integer age;
    private Short gender;
    private byte[] faceFeature;


}
