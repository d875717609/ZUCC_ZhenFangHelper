package xyz.zhzh;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeProcess {
    private static Map<BufferedImage, String> trainMap = null;

    public static void main(String[] args) throws Exception {
        startOCR();
        System.out.println("图示识别完成");
    }

    private static void startOCR() throws Exception {
        int sameCount = 0;
        for (int i = 0; i < 10000; i++) {
            String resultStr = getAllOcr(MySetting.IMG_DOWN + "Code" + i + ".gif");
            System.out.println(i + ".gif = " + resultStr);
            File source = new File(MySetting.IMG_DOWN + "Code" + i + ".gif");
            File dest = new File(MySetting.IMG_RESULT + resultStr + ".gif");
            if (dest.exists()) {
                sameCount++;
            } else {
                Files.copy(source.toPath(), dest.toPath());
            }
        }
        System.out.println("重复验证码有：" + sameCount);
    }

    /*
     * 获得所有验证码图片路径
     */
    static String getAllOcr(String file) throws Exception {
        BufferedImage img = removeBackgroud(file);
        List<BufferedImage> listImg = splitImage(img);
        Map<BufferedImage, String> map = loadModelData();
        if (map == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (BufferedImage bIMG : listImg) {
            result.append(getSingleCharOcr(bIMG, map));
        }
        return result.toString();
    }

    /*
     * 因为四个字符的颜色均为纯蓝色，所以通过把图片蓝色设置为黑色，其他所有颜色设置为白色来把图片二值化并去噪
     */
    private static BufferedImage removeBackgroud(String picFile) throws Exception {
        BufferedImage img = ImageIO.read(new File(picFile));
        int width = img.getWidth();
        int height = img.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (isBlue(img.getRGB(x, y)) == 1) {
                    img.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    img.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }
        return img;
    }

    /*
     * 因为所有验证码都是蓝色的
     */
    private static int isBlue(int colorInt) {
        Color color = new Color(colorInt);
        int rgb = color.getRed() + color.getGreen() + color.getBlue();
        if (rgb == 153) {
            return 1;
        }
        return 0;
    }

    /*
     * 切割验证码图片
     */
    private static List<BufferedImage> splitImage(BufferedImage img){
        List<BufferedImage> subImgs = new ArrayList<BufferedImage>();
        subImgs.add(img.getSubimage(5, 0, 12, 23));
        subImgs.add(img.getSubimage(17, 0, 12, 23));
        subImgs.add(img.getSubimage(29, 0, 12, 23));
        subImgs.add(img.getSubimage(41, 0, 12, 23));
        return subImgs;
    }

    private static Map<BufferedImage, String> loadModelData() throws Exception {
        if (trainMap == null) {
            Map<BufferedImage, String> map = new HashMap<BufferedImage, String>();
            File dir = new File(MySetting.MODEL_ROOT);
            File[] files = dir.listFiles();
            if (files == null) {
                System.out.println(dir.getName() + "下没有字模！");
                return null;
            }
            for (File file : files) {
                map.put(ImageIO.read(file), file.getName().charAt(0) + "");
            }
            trainMap = map;
        }
        return trainMap;
    }

    /*
     * 识别切割的单个字符
     */
    private static String getSingleCharOcr(BufferedImage img, Map<BufferedImage, String> map) {
        String result = "#";
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height;
        for (BufferedImage bi : map.keySet()) {
            int count = 0;
            if (Math.abs(bi.getWidth() - width) > 2)
                continue;
            int widthmin = width < bi.getWidth() ? width : bi.getWidth();
            int heightmin = height < bi.getHeight() ? height : bi.getHeight();
            Label1: for (int x = 0; x < widthmin; ++x) {
                for (int y = 0; y < heightmin; ++y) {
                    if (isBlack(img.getRGB(x, y)) != isBlack(bi.getRGB(x, y))) {
                        count++;
                        if (count >= min)
                            break Label1;
                    }
                }
            }
            if (count < min) {
                min = count;
                result = map.get(bi);
            }
        }
        return result;
    }

    /*
     * 识别已被处理成黑色的字符
     */

    private static int isBlack(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= 100) {
            return 1;
        }
        return 0;
    }
}
