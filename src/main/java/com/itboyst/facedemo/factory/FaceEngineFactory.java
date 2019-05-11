package com.itboyst.facedemo.factory;

import com.arcsoft.face.EngineConfiguration;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectOrient;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @Author: st7251
 * @Date: 2018/10/16 13:47
 */
public class FaceEngineFactory extends BasePooledObjectFactory<FaceEngine> {

    private String appId;
    private String sdkKey;
    private FunctionConfiguration functionConfiguration;

    /**
     * 设置引擎最多能检测出的人脸数，推荐有效值范围[1,50]
     */
    private Integer detectFaceMaxNum=10;

    /**
     *设置人脸相对于所在图片的长边的占比，
     * 在视频模式(ASF_DETECT_MODE_VIDEO)下有效值范围[2，16]，
     * 在图像模式(ASF_DETECT_MODE_IMAGE)下有效值范围[2，32]
     */
    private Integer detectFaceScaleVal=16;

    /**
     * 设置检测模式：图片模式和视频模式
     */
    private DetectMode detectMode= DetectMode.ASF_DETECT_MODE_IMAGE;

    /**
     * 设置人脸检测方向的优先级，
     * 支持仅0度(ASF_OP_0_ONLY)，
     * 仅90度(ASF_OP_90_ONLY)，
     * 仅180度(ASF_OP_180_ONLY)，
     * 仅270度(ASF_OP_270_ONLY)，
     * 多方向检测(ASF_OP_0_HIGHER_EXT)，
     * 建议使用单一指定方向检测，性能比多方向检测更佳
     */
    private DetectOrient detectFaceOrientPriority= DetectOrient.ASF_OP_270_ONLY;


    public FaceEngineFactory(String appId, String sdkKey, FunctionConfiguration functionConfiguration) {
        this.appId = appId;
        this.sdkKey = sdkKey;
        this.functionConfiguration = functionConfiguration;
    }



    @Override
    public FaceEngine create() throws Exception {

        EngineConfiguration engineConfiguration= EngineConfiguration.builder()
                .functionConfiguration(functionConfiguration)
                .detectFaceMaxNum(detectFaceMaxNum)
                .detectFaceScaleVal(detectFaceScaleVal)
                .detectMode(detectMode)
                .detectFaceOrientPriority(detectFaceOrientPriority)
                .build();
        FaceEngine faceEngine =new FaceEngine();
        faceEngine.active(appId,sdkKey);
        faceEngine.init(engineConfiguration);

        return faceEngine;
    }

    @Override
    public PooledObject<FaceEngine> wrap(FaceEngine faceEngine) {
        return new DefaultPooledObject<>(faceEngine);
    }


    @Override
    public void destroyObject(PooledObject<FaceEngine> p) throws Exception {
        FaceEngine faceEngine = p.getObject();
        int result = faceEngine.unInit();
        super.destroyObject(p);
    }
}
