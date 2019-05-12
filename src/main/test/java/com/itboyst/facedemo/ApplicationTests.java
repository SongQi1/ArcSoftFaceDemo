package com.itboyst.facedemo;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.druid.util.HttpClientUtils;
import com.arcsoft.face.FaceInfo;
import com.itboyst.facedemo.camera.CameraUtil;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.bytedeco.javacpp.opencv_imgproc.LINE_4;
import static org.bytedeco.javacpp.opencv_imgproc.LINE_AA;


public class ApplicationTests {

    @Test
    public void testCamera() throws InterruptedException, FrameGrabber.Exception {
        //开始获取摄像头数据
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();

        //新建一个窗口
        CanvasFrame canvas = new CanvasFrame("摄像头");
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        while (true) {
            //窗口是否关闭
            if (!canvas.isDisplayable()) {
                //停止抓取
                grabber.stop();
                //退出
                System.exit(-1);
            }

            Frame frame = grabber.grab();

            //获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像
            canvas.showImage(frame);
            //50毫秒刷新一次图像
            Thread.sleep(50);
        }



    }

    @Test
    public void testCamera1() throws FrameGrabber.Exception, InterruptedException {
        VideoInputFrameGrabber grabber = VideoInputFrameGrabber.createDefault(0);
        grabber.start();
        CanvasFrame canvasFrame = new CanvasFrame("摄像头");
        canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvasFrame.setAlwaysOnTop(true);
        while (true) {
            if (!canvasFrame.isDisplayable()) {
                grabber.stop();
                System.exit(-1);
            }
            Frame frame = grabber.grab();



            canvasFrame.showImage(frame);
            Thread.sleep(30);
        }
    }

    @Test
    public void testRecordVedio() throws Exception {




        Loader.load(opencv_objdetect.class);
        //本机摄像头默认0，这里使用javacv的抓取器，至于使用的是ffmpeg还是opencv，请自行查看源码
        FrameGrabber grabber = FrameGrabber.createDefault(0);
        grabber.start();//开启抓取器

        //转换器
        OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
        //抓取一帧视频并将其转换为图像，至于用这个图像用来做什么？加水印，人脸识别等等自行添加
        IplImage grabbedImage = iplImageConverter.convert(grabber.grabFrame());
        int width = grabbedImage.width();
        int height = grabbedImage.height();

        //录制器
        FrameRecorder recorder = FrameRecorder.createDefault("output.flv", width, height);
        //AV_CODEC_ID_H264，编码
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        //封装格式，如果是推送到rtmp就必须是flv封装格式
        recorder.setFormat("flv");
        recorder.setFrameRate(25);
        recorder.start();//开启录制器

        long startTime=0;
        long videoTS;
        CanvasFrame canvasFrame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvasFrame.setAlwaysOnTop(true);

        // 水印文字位置
        Point point1 = new Point(10, 50);
        Point point2 = new Point(200, 200);
        Point point3 = new Point(200, 240);
        // 颜色
        Scalar scalar1 = new Scalar(0, 255, 255, 0);
        Scalar scalar2 = new Scalar(255, 0, 0, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Java2DFrameConverter javaConverter = new Java2DFrameConverter();
        //不知道为什么这里不做转换就不能推到rtmp
        Frame rotatedFrame;
        int index =0;
        while (canvasFrame.isVisible() && (grabbedImage = iplImageConverter.convert(grabber.grabFrame())) != null) {
            rotatedFrame = iplImageConverter.convert(grabbedImage);
            BufferedImage buffImg = javaConverter.convert(grabber.grab());

            //将bufferedImage对象输出到磁盘上
//            ImageIO.write(buffImg,"jpg",new File("img"+index +".jpg"));


            Mat mat = iplImageConverter.convertToMat(grabber.grabFrame());
            // 加文字水印，opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，文字反锯齿，是否翻转文字）
            opencv_imgproc.putText(mat, "songqi", point2, opencv_imgproc.CV_FONT_VECTOR0, 2.2, scalar2, 1, 0,false);
            // 翻转字体，文字平滑处理（即反锯齿）
            opencv_imgproc.putText(mat, "songqi", point3, opencv_imgproc.CV_FONT_VECTOR0, 2.2, scalar2, 1, 20,true);
            opencv_imgproc.putText(mat, sdf.format(new Date()), point1, opencv_imgproc.CV_FONT_ITALIC, 0.8, scalar1,2, 20, false);
            //在窗口显示处理后的图像
            canvasFrame.showImage(iplImageConverter.convert(mat));


            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            recorder.setTimestamp(videoTS);
            recorder.record(rotatedFrame);
            if (index == 0) {
                // 保存第一帧图片到本地
                opencv_imgcodecs.imwrite("eguid.jpg", mat);
            }
            Thread.sleep(40);
            index++;
        }

        System.out.println("共录制了"+ index + "帧。");
        // 销毁窗口
        canvasFrame.dispose();
        //停止记录
        recorder.stop();
        //停止抓取
        grabber.stop();

        // 手动释放资源
        scalar1.close();
        scalar2.close();
        point1.close();
        point2.close();
        point3.close();
    }

    @Test
    public void testOpenCV() throws InterruptedException, IOException {

        RestTemplate restTemplate = new RestTemplate();
        opencv_videoio.VideoCapture videoCapture = null;
        //遍历查找摄像头

        for(int index = 0; index<2; index++){
            videoCapture = new opencv_videoio.VideoCapture(index);
            if(videoCapture.grab()){
                //找到摄像头设备，退出遍历
                System.err.println("当前摄像头：" + index);
                // 水印文字位置
                Point point = new Point(10, 80);
                // 颜色，使用黄色
                Scalar scalar = new Scalar(0, 255, 255, 0);
                //数字格式化
                DecimalFormat df = new DecimalFormat("0.##");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                //使用java的JFrame显示图像
                CanvasFrame canvasFrame = new CanvasFrame("摄像头", CanvasFrame.getDefaultGamma()/2.2);
                //javacv提供的转换器，方便将mat转换为Frame
                OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
                Java2DFrameConverter javaConverter = new Java2DFrameConverter();
                Mat mat = new Mat();
                double start = System.currentTimeMillis();
                double end;
                // 图像透明权重值,0-1之间
                double alpha = 0.5;

                Mat logo = opencv_imgcodecs.imread("icon.png");


                for(int i=0; canvasFrame.isVisible() ;i++) {
                    //重新获取mat
                    videoCapture.retrieve(mat);
                    //是否采集到摄像头数据
                    if (videoCapture.grab()) {
                        //读取一帧mat图像
                        if (videoCapture.read(mat)) {
                            end = System.currentTimeMillis();
                            if(mat != null){
                                //添加水印文字
                                String msg = sdf.format(new Date()) + " frames:"+ i + " fps:" + df.format((end-start)/1000.0);
                                opencv_imgproc.putText(mat, msg, point, opencv_imgproc.CV_FONT_VECTOR0, 0.5, scalar, 1, 20, false);

                                //添加矩形框
                                Point point1 = new Point(10+i, 50+i);
                                Point point2 = new Point(300+i, 200+i);
                                opencv_imgproc.rectangle(mat, point1, point2, scalar, 1, LINE_AA,2);

                                // 添加图片
                                Mat ROI = mat.apply(new opencv_core.Rect(10, 10,logo.cols(), logo.rows()));
                                opencv_core.addWeighted(ROI, alpha, logo, 1.0 - alpha, 0.0, ROI);
                            }

                            //开始人脸检测
                            BufferedImage buffImg = javaConverter.convert(iplImageConverter.convert(mat));
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            ImageIO.write(buffImg, "jpg", outputStream);
                            byte[] bytes1 = outputStream.toByteArray();
                            String imageBase64Str = Base64Utils.encodeToUrlSafeString(bytes1);
                            MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
                            request.add("image", imageBase64Str);
                            List<?> resp = restTemplate.postForObject("http://127.0.0.1:8080/detectFaces", request, List.class);
                            if(resp != null && resp.size() > 0){
                                for(Object obj : resp){
                                    FaceInfo faceInfo = BeanUtil.mapToBean((Map<?, ?>) obj, FaceInfo.class, true);
                                    System.out.println("检测到人脸。faceInfo : " + faceInfo.toString());
                                }
                            }


                            //将bufferedImage对象输出到磁盘上
                            //  ImageIO.write(buffImg,"jpg",new File("img" + i +".jpg"));

                            //  opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                            canvasFrame.showImage(iplImageConverter.convert(mat));
                            start = end;
                        }
                        mat.release();//释放mat
                    }
                    Thread.sleep(45);
                }
            }else if(videoCapture == null || !videoCapture.isOpened()){
                //没找到设备，释放资源
                videoCapture.close();
                System.err.println("无法找到摄像头["+ index+"]，请检查是否存在摄像头。" );
                return;
            }

        }
    }

    @Test
    public void testInitCamera(){

        CameraUtil.initCamera(1);

    }

}