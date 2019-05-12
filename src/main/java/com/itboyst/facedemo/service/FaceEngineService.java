package com.itboyst.facedemo.service;

import com.itboyst.facedemo.base.ImageInfo;
import com.itboyst.facedemo.dto.FaceRecognizeResDto;
import com.itboyst.facedemo.dto.FaceUserInfo;
import com.itboyst.facedemo.dto.ProcessInfo;
import com.arcsoft.face.FaceInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;


public interface FaceEngineService {

    void addFaceToCache(Integer groupId, FaceUserInfo userFaceInfo) throws ExecutionException;


    /**
     * 人脸检测
     * @param imageInfo
     * @return
     */
    List<FaceInfo> detectFaces(ImageInfo imageInfo);


    /**
     * 年龄、性别、三维角度检测
     * @param imageInfo
     * @return
     */
    List<ProcessInfo> process(ImageInfo imageInfo);

    /**
     * 人脸特征
     * @param imageInfo
     * @return
     */
    byte[] extractFaceFeature(ImageInfo imageInfo) throws InterruptedException;

    /**
     * 人脸比对
     * @param groupId
     * @param faceFeature
     * @return
     */
    List<FaceUserInfo> compareFaceFeature(byte[] faceFeature, Integer groupId) throws InterruptedException, ExecutionException;


    /**
     * 人脸识别
     * @param imageInfo
     * @return
     * @throws InterruptedException
     */
    List<FaceRecognizeResDto> recognizeFaces(ImageInfo imageInfo) throws InterruptedException;
}
