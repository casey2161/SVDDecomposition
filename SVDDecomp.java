import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class SVDDecomp {
    public static void main(String[] args) throws Exception{
        JFileChooser fileChooser = new JFileChooser();
        int returnVal;

        Scanner in = new Scanner(System.in);        
        System.out.println("1. Compress an Image\n"
                + "2. Compress a matrix from an Image \n    in text file format(images can be converted using option 3)\n"
                + "3. Convert an image to a text file\n"
                + "4. SVD an input matrix from user input");
        int choice = Integer.parseInt(in.nextLine());
        if(choice == 1) {
            BufferedImage image;
            returnVal = fileChooser.showOpenDialog(null);
            File input = null;
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                input = fileChooser.getSelectedFile();
            } else {
                System.out.println("No file selected. Program is exiting");
                System.exit(0);
            }
            image = ImageIO.read(input);
            show(image, "Original Image", "No sigma values necessary");
            int k = 0;
            System.out.println("Enter a value for K");
            k = Integer.parseInt(in.nextLine());
            System.out.println("Compressing...\nLarger images will take longer");
            Object[] values = new Object[3];
            try {
                values = compress(image, k);
                image = (BufferedImage) values[2];
                show(image, "Compressed Image" + k,((String) values[0]) + " " + ((String) values[1]));
            } catch(ArrayIndexOutOfBoundsException aiobe) {
                System.out.println("K selected is too large, exiting");
                System.exit(0);
            }
        } else if(choice == 2) {
            System.out.println("Input file should be a text file in a format similar to below:");
            System.out.println("(r,g,b) (r,g,b) ... (r,g,b)\n(r,g,b) (r,g,b) ... (r,g,b)");
            returnVal = fileChooser.showOpenDialog(null);
            File input = null;
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                input = fileChooser.getSelectedFile();
            } else {
                System.out.println("No file selected. Program is exiting");
                System.exit(0);
            }
            try{
                BufferedImage image = parseFile(input);
                show(image, "Original Image", "No sigma values");
                           
                System.out.println("Enter a value for k");
                int k = Integer.parseInt(in.nextLine());
                Object[] values = new Object[3];
                values = compress(image, k);
                image = (BufferedImage) values[2];
                show(image, "Compressed Image" + k,((String) values[0]) + " " + ((String) values[1]));
            } catch(Exception e) {
                System.out.println("Something went wrong, Make sure the file is correct and"
                        + " that k is not too large. Exiting.");
                e.printStackTrace();
                System.exit(0);
            }
            
        } else if(choice == 3) {
            BufferedImage image = null;
            returnVal = fileChooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File input = fileChooser.getSelectedFile();
                image = ImageIO.read(input);
            }
            saveImageAsText(image);
        } else if(choice == 4) {
            matrix();
        } else {
            System.out.println("Your choice is invalid. The program is exiting");
            System.exit(0);
        }
        in.close();
    }
    
    private static void saveImageAsText(BufferedImage image) {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showSaveDialog(null);
        PrintWriter out = null;
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            try {
                out = new PrintWriter(new BufferedWriter(new FileWriter(fileChooser.getSelectedFile())));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        for(int i = 0; i < image.getHeight(); i++) {
            for(int j = 0; j < image.getWidth(); j++) {
                int rgb = image.getRGB(j, i);
                int red = rgb >> 16 & 0xFF;
                int green = rgb >> 8 & 0xFF;
                int blue = rgb & 0xFF;
                if(j > 0) {
                    out.print(" (" + red +"," + green + "," + blue + ")");
                } else {
                    out.print("(" + red +"," + green + "," + blue + ")");
                }
            }
            out.println();
        }
        out.close();
    }
    
    private static int[][] convertTo2D(BufferedImage image) {

        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
           final int pixelLength = 4;
           for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
              int argb = 0;
              argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
              argb += ((int) pixels[pixel + 1] & 0xff); // blue
              argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
              argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
              result[row][col] = argb;
              col++;
              if (col == width) {
                 col = 0;
                 row++;
              }
           }
        } else {
           final int pixelLength = 3;
           for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
               
              int argb = 0;
              argb += -16777216; // 255 alpha
              argb += ((int) pixels[pixel] & 0xff); // blue
              argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
              argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
              result[row][col] = argb;
              col++;
              if (col == width) {
                 col = 0;
                 row++;
              }
           }
        }

        return result;
     }
    
    private static Object[] compress(BufferedImage imageRef, int k) {
        Object[] ret = new Object[3];
        String sigma1 = "";
        String sigmak = "";
        ColorModel cm = imageRef.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = imageRef.copyData(null);
        BufferedImage image = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        
        Matrix red;
        Matrix blue;
        Matrix green;
        
        int[][] pixels = convertTo2D(image);
        red = new Matrix(image.getHeight(), image.getWidth());
        blue = new Matrix(image.getHeight(), image.getWidth());
        green = new Matrix(image.getHeight(), image.getWidth());
        for(int j = 0; j < red.getRowDimension(); j++) {
            for(int i = 0; i < red.getColumnDimension(); i++) {
                red.set(j, i, (pixels[j][i] >> 16) & 0xFF);
                green.set(j, i, (pixels[j][i] >>8 ) & 0xFF);
                blue.set(j, i, (pixels[j][i]) & 0xFF);
            }
        }

        SingularValueDecomposition redSVD = red.svd();
        SingularValueDecomposition blueSVD = blue.svd();
        SingularValueDecomposition greenSVD = green.svd();

        red = new Matrix(image.getHeight(), image.getWidth());
        green = new Matrix(image.getHeight(), image.getWidth());
        blue = new Matrix(image.getHeight(), image.getWidth());
        sigma1 = "Sigma 1: Red = " + redSVD.getSingularValues()[0];
        sigma1 += " Green = " + greenSVD.getSingularValues()[0];
        sigma1 += " Blue = " + blueSVD.getSingularValues()[0];
        ret[0] = sigma1;
        sigmak = "Sigma k: Red = " + redSVD.getSingularValues()[k - 1];
        sigmak += " Green = " + greenSVD.getSingularValues()[k - 1];
        sigmak += " Blue = " + blueSVD.getSingularValues()[k - 1];
        ret[1] = sigmak;
        for(int i = 0; i < k; i++) {
            double redS = redSVD.getSingularValues()[i];
            Matrix redU = redSVD.getU().getMatrix(0, redSVD.getU().getRowDimension()-1, new int[]{i});
            Matrix redV = redSVD.getV().getMatrix(0, redSVD.getV().getRowDimension()-1, new int[]{i});
            red = red.plus(redU.times(redV.transpose()).times(redS));
            
            double blueS = blueSVD.getSingularValues()[i];
            Matrix blueU = blueSVD.getU().getMatrix(0, blueSVD.getU().getRowDimension()-1, new int[]{i});
            Matrix blueV = blueSVD.getV().getMatrix(0, blueSVD.getV().getRowDimension()-1, new int[]{i});
            blue = blue.plus(blueU.times(blueV.transpose()).times(blueS));
            
            double greenS = greenSVD.getSingularValues()[i];
            Matrix greenU = greenSVD.getU().getMatrix(0, greenSVD.getU().getRowDimension()-1, new int[]{i});
            Matrix greenV = greenSVD.getV().getMatrix(0, greenSVD.getV().getRowDimension()-1, new int[]{i});
            green = green.plus(greenU.times(greenV.transpose()).times(greenS));
        }
        
        for(int i = 0; i < image.getWidth(); i++) {
            for(int j = 0; j < image.getHeight(); j++) {
                int rgb = (((int)red.get(j, i)&0xFF)<<16)|(((int) green.get(j, i)&0xFF)<<8)|((int)blue.get(j, i)&0xFF);
                
                image.setRGB(i, j, rgb);
            }
        }
        ret[2] = image;
        return ret;
    }
    
    private static BufferedImage parseFile(File input) throws FileNotFoundException {
        Scanner in = new Scanner(input);
        
        BufferedImage image = null;
        String line;
        String[] pixels;
        ArrayList<Integer> red = new ArrayList<Integer>();
        ArrayList<Integer> green = new ArrayList<Integer>();
        ArrayList<Integer> blue = new ArrayList<Integer>();
        int width = 0;
        int counter = 0;
        while(in.hasNextLine()) {
            line = in.nextLine();
            pixels = line.split(" ");
            for(int i = 0; i < pixels.length; i++) {
                String redString = pixels[i].split(",")[0].substring(1);
                String greenString = pixels[i].split(",")[1];
                String blueString = pixels[i].split(",")[2].substring(0, pixels[i].split(",")[2].length() - 1);
                int r = Integer.parseInt(redString);
                if(r > 255 || r < 0) {
                    in.close();
                    throw new IllegalArgumentException("Invalid number");
                }
                red.add(r);
                int g = Integer.parseInt(greenString);
                if(g > 255 || g < 0) {
                    in.close();
                    throw new IllegalArgumentException("Invalid number");
                }
                green.add(g);
                int b = Integer.parseInt(blueString);
                if(b > 255 || b < 0) {
                    in.close();
                    throw new IllegalArgumentException("Invalid number");
                }
                blue.add(b);
                if(width != 0 && width != pixels.length) {
                    in.close();
                    throw new IllegalArgumentException("Matrix is jagged");
                }
                width = pixels.length;
            }
            counter++;
        }
        image = new BufferedImage(width, counter, 5);

        counter = 0;
        for(int i = 0; i < image.getHeight(); i++) {
            for(int j = 0; j < image.getWidth(); j++) {
                int rgb = (((int)red.get(counter)&0xFF)<<16)|(((int) green.get(counter)&0xFF)<<8)|((int)blue.get(counter)&0xFF);
                counter++;
                image.setRGB(j, i, rgb);
            }
        }
        
        in.close();
        return image;
    }
    
    private static void show(BufferedImage image, String title, String sigma) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        frame.add(new JLabel(sigma), BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }
    
    private static void matrix() {
        System.out.println("Enter the number of rows");
        Scanner in = new Scanner(System.in);
        int n = Integer.parseInt(in.nextLine());
        System.out.println("Enter the number of columns");
        int m = Integer.parseInt(in.nextLine());
        Matrix mat = new Matrix(n, m);
        for(int i = 0; i < m; i++) {
            for(int j = 0; j < n; j++) {
                System.out.println("Input a value for A" + i + "," + j);
                mat.set(i, j, Double.parseDouble(in.nextLine()));
            }
        }
        
        SingularValueDecomposition svd = mat.svd();
        
        System.out.println("Input a value for k");
        int k = Integer.parseInt(in.nextLine());
        
        try {
            Matrix matsvd = new Matrix(n, m);
            for(int i = 0; i < k; i++) {
                double s = svd.getSingularValues()[i];
                Matrix u = svd.getU().getMatrix(0, svd.getU().getRowDimension()-1, new int[]{i});
                Matrix v = svd.getV().getMatrix(0, svd.getV().getRowDimension()-1, new int[]{i});
                matsvd = matsvd.plus(u.times(v.transpose()).times(s));
            }
            System.out.println("Original:");
            print(mat);
            System.out.println("SVD k = " + k + ":");
            print(matsvd);
        } catch(Exception e) {
            System.out.println("Value chosen for k is too large. Exiting...");
            System.exit(0);
        }
        
        in.close();
    }
    
    private static void print(Matrix m) {
        double[][] d = m.getArray();
        
        for(int i = 0; i < d.length; i++) {
            for(int j = 0; j < d[i].length; j++) {
                System.out.print(d[i][j] + " ");
            }
            System.out.println("");
        }
    }
}
