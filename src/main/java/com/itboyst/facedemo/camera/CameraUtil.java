package com.itboyst.facedemo.camera;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import static org.bytedeco.javacpp.opencv_imgproc.LINE_AA;

/**
 * @author songqi
 */

public class CameraUtil {

    public static void initCamera(int cameraCount) {
        opencv_videoio.VideoCapture videoCapture = null;
        //遍历查找摄像头
        for(int index = 0; index<cameraCount; index++){
            videoCapture = new opencv_videoio.VideoCapture(index);
            if(videoCapture.grab()){
                //找到摄像头设备，退出遍历
                System.err.println("当前摄像头：" + index);
                // 水印文字位置
                opencv_core.Point point = new opencv_core.Point(10, 80);
                // 颜色，使用黄色
                opencv_core.Scalar scalar = new opencv_core.Scalar(0, 255, 255, 0);
                //数字格式化
                DecimalFormat df = new DecimalFormat("0.##");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                //使用java的JFrame显示图像
                CanvasFrame canvasFrame = new CanvasFrame("摄像头", CanvasFrame.getDefaultGamma()/2.2);
                //javacv提供的转换器，方便将mat转换为Frame
                OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
                Java2DFrameConverter javaConverter = new Java2DFrameConverter();
                opencv_core.Mat mat = new opencv_core.Mat();
                double start = System.currentTimeMillis();
                double end;
                // 图像透明权重值,0-1之间
                double alpha = 0.5;

                opencv_core.Mat logo = opencv_imgcodecs.imread("icon.png");


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
                                opencv_core.Point point1 = new opencv_core.Point(10+i, 50+i);
                                opencv_core.Point point2 = new opencv_core.Point(300+i, 200+i);
                                opencv_imgproc.rectangle(mat, point1, point2, scalar, 1, LINE_AA,2);

                                // 添加图片
                                opencv_core.Mat ROI = mat.apply(new opencv_core.Rect(10, 10,logo.cols(), logo.rows()));
                                opencv_core.addWeighted(ROI, alpha, logo, 1.0 - alpha, 0.0, ROI);
                            }
                            BufferedImage buffImg = javaConverter.convert(iplImageConverter.convert(mat));
                            //将bufferedImage对象输出到磁盘上
                            //  ImageIO.write(buffImg,"jpg",new File("img" + i +".jpg"));

                            //  opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                            canvasFrame.showImage(iplImageConverter.convert(mat));
                            start = end;
                        }
                        mat.release();//释放mat
                    }
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else if(videoCapture == null || !videoCapture.isOpened()){
                //没找到设备，释放资源
                videoCapture.close();
                System.err.println("无法找到摄像头["+ index+"]，请检查是否存在摄像头。" );
                return;
            }

        }
    }


}
