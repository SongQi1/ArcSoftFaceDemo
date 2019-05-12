package com.itboyst.facedemo.dto;

import com.arcsoft.face.Rect;
import lombok.Data;
import lombok.ToString;

/**
 *
 * 人脸识别返回DTO
 * @author songqi
 */
@Data
@ToString
public class FaceRecognizeResDto {
    private String faceId;
    private String name;
    private Integer similarValue;
    private Integer age;
    private String gender;
    private Rect rect;
}
