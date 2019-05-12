package com.itboyst.facedemo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.itboyst.facedemo.dao.mapper.UserFaceInfoMapper;
import com.itboyst.facedemo.domain.UserFaceInfo;
import com.itboyst.facedemo.dto.FaceRecognizeResDto;
import com.itboyst.facedemo.dto.ProcessInfo;
import com.itboyst.facedemo.factory.FaceEngineFactory;
import com.itboyst.facedemo.service.FaceEngineService;
import com.itboyst.facedemo.dto.FaceUserInfo;
import com.itboyst.facedemo.base.ImageInfo;
import com.arcsoft.face.*;
import com.arcsoft.face.enums.ImageFormat;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@Service
public class FaceEngineServiceImpl implements FaceEngineService {

    public final static Logger logger = LoggerFactory.getLogger(FaceEngineServiceImpl.class);

    @Value("${config.freesdk.app-id}")
    public String appId;

    @Value("${config.freesdk.sdk-key}")
    public String sdkKey;

    @Value("${config.freesdk.thread-pool-size}")
    public Integer threadPoolSize;


    private Integer passRate = 80;

    private ExecutorService executorService;

    private LoadingCache<Integer, List<FaceUserInfo>> faceGroupCache;

    @Autowired
    private UserFaceInfoMapper userFaceInfoMapper;

    private GenericObjectPool<FaceEngine> extractFaceObjectPool;
    private GenericObjectPool<FaceEngine> compareFaceObjectPool;

    @PostConstruct
    public void init() {
        initCache();
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(threadPoolSize);
        poolConfig.setMaxTotal(threadPoolSize);
        poolConfig.setMinIdle(threadPoolSize);
        poolConfig.setLifo(false);
        extractFaceObjectPool = new GenericObjectPool(
                new FaceEngineFactory(appId, sdkKey,
                        FunctionConfiguration.builder()
                                .supportFaceDetect(true)
                                .supportFaceRecognition(true)
                                .supportAge(true)
                                .supportGender(true)
                                .supportFace3dAngle(true)
                                .build()), poolConfig);//底层库算法对象池
        compareFaceObjectPool = new GenericObjectPool(
                new FaceEngineFactory(appId, sdkKey,
                        FunctionConfiguration.builder()
                                .supportFaceRecognition(true).build()), poolConfig);//底层库算法对象池

    }

    /**
     * 初始化缓存
     */
    private void initCache() {
        this.faceGroupCache = CacheBuilder
                .newBuilder()
                .maximumSize(100)
                .expireAfterAccess(2, TimeUnit.HOURS)
                .build(new CacheLoader<Integer, List<FaceUserInfo>>() {
                    @Override
                    public List<FaceUserInfo> load(Integer groupId) {
                        UserFaceInfo userFaceInfo = new UserFaceInfo();
                        userFaceInfo.setGroupId(groupId);
                        List<UserFaceInfo> userFaceInfoList = userFaceInfoMapper.selectList(userFaceInfo);

                        List<FaceUserInfo> userFaceInfoListTarget = Lists.newLinkedList();
                        userFaceInfoList.forEach(k -> {
                            FaceUserInfo info = new FaceUserInfo();
                            BeanUtil.copyProperties(k, info);
                            userFaceInfoListTarget.add(info);
                        });

                        return userFaceInfoListTarget;
                    }
                });
    }


    private int plusHundred(Float value) {
        BigDecimal target = new BigDecimal(value);
        BigDecimal hundred = new BigDecimal(100f);
        return target.multiply(hundred).intValue();

    }

    @Override
    public void addFaceToCache(Integer groupId, FaceUserInfo faceUserInfo) throws ExecutionException {
        List<FaceUserInfo> userFaceInfoList = faceGroupCache.get(groupId);
        userFaceInfoList.add(faceUserInfo);
    }

    @Override
    public List<FaceInfo> detectFaces(ImageInfo imageInfo) {
        FaceEngine faceEngine = null;
        try {
            //获取引擎对象
            faceEngine = extractFaceObjectPool.borrowObject();

            //人脸检测得到人脸列表
            List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();

            //人脸检测
            faceEngine.detectFaces(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList);
            return faceInfoList;
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            if (faceEngine != null) {
                //释放引擎对象
                extractFaceObjectPool.returnObject(faceEngine);
            }

        }

        return null;
    }


    @Override
    public List<ProcessInfo> process(ImageInfo imageInfo){
        FaceEngine faceEngine = null;
        try {
            //获取引擎对象
            faceEngine = extractFaceObjectPool.borrowObject();
            //人脸检测得到人脸列表
            List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
            //人脸检测
            faceEngine.detectFaces(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList);
            int processResult = faceEngine.process(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList, FunctionConfiguration.builder().supportAge(true).supportGender(true).build());
            List<ProcessInfo> processInfoList=Lists.newLinkedList();

            List<GenderInfo> genderInfoList = new ArrayList<GenderInfo>();
            //性别提取
            int genderCode = faceEngine.getGender(genderInfoList);
            //年龄提取
            List<AgeInfo> ageInfoList = new ArrayList<AgeInfo>();
            int ageCode = faceEngine.getAge(ageInfoList);
            for (int i = 0; i <genderInfoList.size() ; i++) {
                ProcessInfo processInfo=new ProcessInfo();
                processInfo.setGender(genderInfoList.get(i).getGender());
                processInfo.setAge(ageInfoList.get(i).getAge());
                processInfoList.add(processInfo);
            }
            return processInfoList;

        } catch (Exception e) {
            logger.error("", e);
        } finally {
            if (faceEngine != null) {
                //释放引擎对象
                extractFaceObjectPool.returnObject(faceEngine);
            }
        }

        return null;

    }
    /**
     * 人脸特征
     * @param imageInfo
     * @return
     */
    @Override
    public byte[] extractFaceFeature(ImageInfo imageInfo) throws InterruptedException {

        FaceEngine faceEngine = null;
        try {
            //获取引擎对象
            faceEngine = extractFaceObjectPool.borrowObject();

            //人脸检测得到人脸列表
            List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();

            //人脸检测
            int i = faceEngine.detectFaces(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList);

            if (CollectionUtil.isNotEmpty(faceInfoList)) {
                FaceFeature faceFeature = new FaceFeature();
                //提取人脸特征
                faceEngine.extractFaceFeature(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList.get(0), faceFeature);

                return faceFeature.getFeatureData();
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            if (faceEngine != null) {
                //释放引擎对象
                extractFaceObjectPool.returnObject(faceEngine);
            }

        }

        return null;
    }

    @Override
    public List<FaceUserInfo> compareFaceFeature(byte[] faceFeature, Integer groupId) throws InterruptedException, ExecutionException {
        List<FaceUserInfo> resultFaceInfoList = Lists.newLinkedList();//识别到的人脸列表

        FaceFeature targetFaceFeature = new FaceFeature();
        targetFaceFeature.setFeatureData(faceFeature);
        List<FaceUserInfo> faceInfoList = faceGroupCache.get(groupId);//从缓存中提取人脸库
        //分成1000一组，多线程处理
        List<List<FaceUserInfo>> faceUserInfoPartList = Lists.partition(faceInfoList, 1000);
        CompletionService<List<FaceUserInfo>> completionService = new ExecutorCompletionService(executorService);
        for (List<FaceUserInfo> part : faceUserInfoPartList) {
            completionService.submit(new CompareFaceTask(part, targetFaceFeature));
        }
        for (int i = 0; i < faceUserInfoPartList.size(); i++) {
            List<FaceUserInfo> faceUserInfoList = completionService.take().get();
            if (CollectionUtil.isNotEmpty(faceInfoList)) {
                resultFaceInfoList.addAll(faceUserInfoList);
            }
        }

        resultFaceInfoList.sort((h1, h2) -> h2.getSimilarValue().compareTo(h1.getSimilarValue()));//从大到小排序

        return resultFaceInfoList;
    }

    @Override
    public List<FaceRecognizeResDto> recognizeFaces(ImageInfo imageInfo) throws InterruptedException {
        List<FaceRecognizeResDto> recognizedFaceList = new ArrayList<>();
        //人脸检测
        List<FaceInfo> faceInfoList = this.detectFaces(imageInfo);
        if(faceInfoList != null && faceInfoList.size() > 0){
            FaceEngine faceEngine = null;
            try {
                //获取引擎对象
                faceEngine = extractFaceObjectPool.borrowObject();
                for(FaceInfo face : faceInfoList ){
                    FaceRecognizeResDto recognizedFace = new FaceRecognizeResDto();
                    recognizedFace.setRect(face.getRect());

                    FaceFeature faceFeature = new FaceFeature();
                    //提取人脸特征
                    faceEngine.extractFaceFeature(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, face, faceFeature);


                    /**
                    注意：这个groupId是方便从数据库中拉取人脸特征数据到缓存中。
                    例如：如果规定每个groupId只能放1000个人脸数据，在人脸注册的时候，可以由程序自动控制groupId的值。
                     在人脸对比的时候，如果某一个groupId没有查出相似的人脸数据，则应该换下一个groupId，直到所有groupid
                     都遍历完
                    这里由于测试，统一将groupId设置为101。
                     */
                    //到人脸数据库中对比
                    List<FaceUserInfo> similarList = compareFaceFeature(faceFeature.getFeatureData(), 101);
                    if(similarList != null && similarList.size() > 0){
                        //取相似度最高的那个
                        FaceUserInfo similarFace = similarList.get(0);
                        /*//调用年龄、性别、三维角度检测
                        faceEngine.process(imageInfo.getRgbData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList, FunctionConfiguration.builder().supportAge(true).supportGender(true).build());
                        //性别提取
                        List<GenderInfo> genderInfoList = new ArrayList<GenderInfo>();
                        faceEngine.getGender(genderInfoList);

                        //年龄提取
                        List<AgeInfo> ageInfoList = new ArrayList<>();
                        faceEngine.getAge(ageInfoList);

                        //三维角度提取*/
                        BeanUtil.copyProperties(similarFace, recognizedFace);
                        recognizedFaceList.add(recognizedFace);
                    }else{
                        //换下一个grouupId继续对比
                        //TODO

                        //最终都添加到识别的列表中。
                        recognizedFaceList.add(recognizedFace);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (faceEngine != null) {
                    //释放引擎对象
                    extractFaceObjectPool.returnObject(faceEngine);
                }
            }
        }
        return recognizedFaceList;
    }


    private class CompareFaceTask implements Callable<List<FaceUserInfo>> {

        //从数据库中拉取到缓存中的人脸列表
        private List<FaceUserInfo> faceUserInfoList;
        //待识别的人脸列表
        private FaceFeature targetFaceFeature;


        public CompareFaceTask(List<FaceUserInfo> faceUserInfoList, FaceFeature targetFaceFeature) {
            this.faceUserInfoList = faceUserInfoList;
            this.targetFaceFeature = targetFaceFeature;
        }

        @Override
        public List<FaceUserInfo> call() throws Exception {
            FaceEngine faceEngine = null;
            List<FaceUserInfo> resultFaceInfoList = Lists.newLinkedList();//识别到的人脸列表
            try {
                faceEngine = compareFaceObjectPool.borrowObject();
                for (FaceUserInfo faceUserInfo : faceUserInfoList) {
                    FaceFeature sourceFaceFeature = new FaceFeature();
                    sourceFaceFeature.setFeatureData(faceUserInfo.getFaceFeature());
                    FaceSimilar faceSimilar = new FaceSimilar();
                    faceEngine.compareFaceFeature(targetFaceFeature, sourceFaceFeature, faceSimilar);
                    Integer similarValue = plusHundred(faceSimilar.getScore());//获取相似值
                    if (similarValue > passRate) {//相似值大于配置预期，加入到识别到人脸的列表

                        FaceUserInfo info = new FaceUserInfo();
                        info.setName(faceUserInfo.getName());
                        info.setFaceId(faceUserInfo.getFaceId());
                        info.setSimilarValue(similarValue);
                        resultFaceInfoList.add(info);
                    }
                }
            } catch (Exception e) {
                logger.error("", e);
            } finally {
                if (faceEngine != null) {
                    compareFaceObjectPool.returnObject(faceEngine);
                }
            }

            return resultFaceInfoList;
        }

    }
}
