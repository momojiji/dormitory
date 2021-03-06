package util;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Random;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

/**
 * 提供二维码生成和解析工具类
 * @author Administrator
 *
 */
public class QRCodeUtils {

    private static final String CHARSET = "utf-8";
    private static final String FORMAT_NAME = "JPG";
    private static final int QRCODE_SIZE = 300; // 二维码尺寸，宽度和高度均是300
    private static final int WIDTH = 80;  //LOGO宽度
    private static final int HEIGHT = 80; //LOGO高度
    private static String pdestPath="qrcode";
    @Value("qrcode")
    private static String qrcodePath;

    /**
     * 生成二维码的方法
     * @param content 目标url
     * @param imgPath logo图片地址
     * @param needCompress 是否压缩logo
     * @return 二维码图片
     * @throws Exception
     */
    private static BufferedImage createImage(String content, String imgPath,boolean needCompress) throws Exception {
        Hashtable hints = new Hashtable();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);//指定二维码的纠错等级为高级
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);//指定字符编码为“utf-8”
        hints.put(EncodeHintType.MARGIN, 1); //设置图片的边距
        //参数1：内容，目标url,参数2：固定写法，参数3：二维码的宽度，参数4：二维码的高度，参数5：二维码属性设置
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content,BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, hints);
        int width = bitMatrix.getWidth();//二维码的宽度
        int height = bitMatrix.getHeight();//二维码的高度
        BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);//生成的二维码image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000: 0xFFFFFFFF);
            }
        }
        if (imgPath == null || "".equals(imgPath)) {//logo图片地址为null或空时
            return image; //只返回二维码图片，无中间的logo
        }
        // 插入logo图片  参数1：二维码图片，参数2：logo图片地址，参数3：压缩图片
        QRCodeUtils.insertImage(image, imgPath, needCompress);
        return image;
    }
    /**
     * 插入logo图片
     * @param source 二维码图片
     * @param imgPath LOGO图片地址
     * @param needCompress 是否压缩
     * @throws Exception
     */
    private static void insertImage(BufferedImage source, String imgPath,boolean needCompress) throws Exception {
        File file = new File(imgPath);//logo图片地址放入文件
        if (!file.exists()) {
            System.err.println("" + imgPath + " 该文件不存在！");
            return;
        }
        Image src = ImageIO.read(new File(imgPath));
        int width = src.getWidth(null);
        int height = src.getHeight(null);
        if (needCompress) { // 压缩LOGO
            if (width > WIDTH) { //如果上传logo宽 >60
                width = WIDTH;
            }
            if (height > HEIGHT) { //如果上传logo高>60
                height = HEIGHT;
            }
            Image image = src.getScaledInstance(width, height,Image.SCALE_SMOOTH);
            BufferedImage tag = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图
            g.dispose();
            src = image;
        }
        // 插入LOGO
        Graphics2D graph = source.createGraphics();
        int x = (QRCODE_SIZE - width) / 2;
        int y = (QRCODE_SIZE - height) / 2;
        graph.drawImage(src, x, y, width, height, null);
        Shape shape = new RoundRectangle2D.Float(x, y, width, width, 6, 6);
        graph.setStroke(new BasicStroke(3f));
        graph.draw(shape);
        graph.dispose();
    }
    /**
     * 生成二维码(内嵌LOGO)
     * @param content  内容
     * @param imgPath  logo地址
     * @param destPath 存放目录
     * @param needCompress 是否压缩logo
     * @throws Exception
     */
    public static String encode(String content, String imgPath, String destPath, boolean needCompress) throws Exception {
        BufferedImage image = QRCodeUtils.createImage(content, imgPath,needCompress); //生成二维码
        if(destPath==null||"".equals(destPath)){
            destPath=pdestPath;
        }
        mkdirs(destPath);
        String filename = new Random().nextInt(99999) + ".jpg";
        File file=new File(destPath + "/" + filename);
        String filepath=file.getAbsolutePath();
        ImageIO.write(image, FORMAT_NAME, new File(destPath + "/" + filename));
        return  filepath;
    }
    /**
     * 当文件夹不存在时，mkdirs会自动创建多层目录，区别于mkdir．(mkdir如果父目录不存在则会抛出异常)
     * @param destPath 存放目录
     */
    public static void mkdirs(String destPath) {
        File file = new File(destPath);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
    }
    /**
     * 生成二维码(内嵌LOGO),没有压缩
     *
     * @param content  内容
     * @param imgPath  LOGO地址
     * @param destPath 存储地址
     * @throws Exception
     */
    public static String encode(String content, String imgPath, String destPath)throws Exception {

      return   QRCodeUtils.encode(content, imgPath, destPath, false);
    }

    /**
     * 生成二维码，无内嵌logo
     *
     * @param content      内容
     * @param destPath     存储地址
     * @param needCompress 是否压缩LOGO
     * @throws Exception
     */
    public static String encode(String content, String destPath,boolean needCompress) throws Exception {
      return   QRCodeUtils.encode(content, null, destPath, needCompress);
    }
    /**
     * 生成二维码
     *
     * @param content  内容
     * @param destPath 存储地址
     * @throws Exception
     */
    public static String encode(String content, String destPath) throws Exception {
      return   QRCodeUtils.encode(content, null, destPath, false);
    }

    /**
     * 生成二维码(内嵌LOGO)
     *
     * @param content      内容
     * @param imgPath      LOGO地址
     * @param output       输出流
     * @param needCompress 是否压缩LOGO
     * @throws Exception
     */
    public static String encode(String content, String imgPath,OutputStream output, boolean needCompress) throws Exception {
        BufferedImage image = QRCodeUtils.createImage(content, imgPath,needCompress);
        ImageIO.write(image, FORMAT_NAME, output);
        return ImageIO.getCacheDirectory().getCanonicalPath();
    }
    /**
     * 生成二维码
     *
     * @param content 内容
     * @param output  输出流
     * @throws Exception
     */
    public static String encode(String content, OutputStream output)throws Exception {
        return  QRCodeUtils.encode(content, null, output, false);
    }

    /**
     * 解析二维码
     * 按文件参数解析
     * @param file 二维码图片
     * @return
     * @throws Exception
     */
    public static String decode(File file) throws Exception {
        BufferedImage image;
        image = ImageIO.read(file);
        if (image == null) {
            return null;
        }
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result;
        Hashtable hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET, CHARSET);//指定字符编码为“utf-8”
        result = new MultiFormatReader().decode(bitmap, hints);
        String resultStr = result.getText();//二维码文本内容
        return resultStr;
    }

    /**
     * 解析二维码
     * 按二维码图片地址解析
     * @param path 二维码图片地址
     * @return  不是二维码的内容返回null,是二维码直接返回识别的结果
     * @throws Exception
     */
    public static String decode(String path) throws Exception {
        return QRCodeUtils.decode(new File(path));
    }
}

