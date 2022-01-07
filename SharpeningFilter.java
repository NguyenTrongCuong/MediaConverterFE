package com.ntc.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class SharpeningFilter { 
	
	private final String INPUT_FILE_PATH = "";
	
	private final String OUTPUT_FILE_PATH = "";
	
	private final String OUTPUT_FILE_EXTENSION = "";
	
	// SET: BI = ORIGINAL IMAGE
	// SET: GB = GAUSSIAN BLUR OF BI
	// SET: INV(IMG) = COLOR INVERTED VERSION OF IMAGE
	
	// FORMULA: UNSHARP MASKING = BI + (BI - GB) - INV(BI + INV(GB)) 
	
	public void sharpen() throws IOException {
		
		BufferedImage bi = ImageIO.read(new File(this.INPUT_FILE_PATH));
		
		int rad = 5;
		
        double[][] weights = this.generateWeightMatrix(rad, Math.sqrt(rad));
		
		BufferedImage gaussianBlurBi = this.createGaussianImage(bi, weights, rad); // CALC GB OF BI
		
		BufferedImage unsharpMask = this.subtractImage1ByImage2(bi, gaussianBlurBi); // CALC UNSHARP MASK (EDGES OF ORIGINAL IMAGE)
		
		BufferedImage iGaussianBlurBi = this.invertImageColor(gaussianBlurBi); // CALC INV(BI)
		
		BufferedImage additionOfBiAndIGaussianBlurBi = this.addImage1ToImage2(bi, iGaussianBlurBi); // CALC BI + INV(GB)
		
		BufferedImage iAdditionOfBiAndIGaussianBlurBi = this.invertImageColor(additionOfBiAndIGaussianBlurBi); // CALC INV(BI + INV(GB))
		
		BufferedImage additionOfBiAndUnsharpMask = this.addImage1ToImage2(bi, unsharpMask); // CALC BI + UNSHARP MASK
		
		BufferedImage sharpenedBi = this.subtractImage1ByImage2(additionOfBiAndUnsharpMask, iAdditionOfBiAndIGaussianBlurBi); // FINAL CALC STEP
		
		sharpenedBi = this.transferColorSpaceOfBIToRGB(sharpenedBi); // CONVERT COLOR SPACE OF BI TO RGB TO AVOID CORRUPTION WHILE WRITTING IMAGE TO DISK
		
		ImageIO.write(sharpenedBi, this.OUTPUT_FILE_EXTENSION, new File(this.OUTPUT_FILE_PATH));
	}
	
	private BufferedImage transferColorSpaceOfBIToRGB(BufferedImage bi) {
		
		BufferedImage RGBbi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
		
        Graphics2D B2G = RGBbi.createGraphics();
        
        B2G.drawImage(bi, 0, 0, null);
        
        B2G.dispose();
        
        return RGBbi;
	}
	
	private BufferedImage subtractImage1ByImage2(BufferedImage image1, BufferedImage image2) {
		
		BufferedImage test = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_RGB);
		
		for (int y = 0; y < image1.getHeight(); y++) {
			for (int x = 0; x < image1.getWidth(); x++) {
				
				Color color1 = new Color(image1.getRGB(x, y));
				
				Color color2 = new Color(image2.getRGB(x, y));
				
				Color color = new Color(Math.abs(color1.getRed() - color2.getRed()), Math.abs(color1.getGreen() - color2.getGreen()), Math.abs(color1.getBlue() - color2.getBlue()));
				
		        test.setRGB(x, y, color.getRGB());
			}
		}
		
		return test;
	}
	
	private BufferedImage addImage1ToImage2(BufferedImage image1, BufferedImage image2) {
		BufferedImage test = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_RGB);
		
		for (int y = 0; y < image1.getHeight(); y++) {
			for (int x = 0; x < image1.getWidth(); x++) {
		        //Retrieving contents of a pixel
				
				Color color1 = new Color(image1.getRGB(x, y));
				
				Color color2 = new Color(image2.getRGB(x, y));
				
				int red = color1.getRed() + color2.getRed() > 255 ? 255 : color1.getRed() + color2.getRed();
				
				int green = color1.getGreen() + color2.getGreen() > 255 ? 255 : color1.getGreen() + color2.getGreen();
				
				int blue = color1.getBlue() + color2.getBlue() > 255 ? 255 : color1.getBlue() + color2.getBlue();
				
		        test.setRGB(x, y, new Color(red, green, blue).getRGB());
		        
			}
		}
		
		return test;
	}
	
	private BufferedImage invertImageColor(BufferedImage bi) {
		
		for (int x = 0; x < bi.getWidth(); x++) {
			for (int y = 0; y < bi.getHeight(); y++) {
				int rgb = bi.getRGB(x, y);
				Color color = new Color(rgb, true);
				color = new Color(255 - color.getRed(),
								  255 - color.getGreen(),
								  255 - color.getBlue());
				bi.setRGB(x, y, color.getRGB());
			}
		}
		
		return bi;
	}
	
	private double[][] generateWeightMatrix(int radius, double variance) {
        double[][] weights = new double[radius][radius];
        double summation = 0;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] = gaussianModel(i - radius / 2.0, j - radius / 2.0, variance);
                summation += weights[i][j];

            }
        }
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights.length; j++) {
                weights[i][j] /= summation;
            }
        }
        return weights;
    }
	
    private double gaussianModel(double x, double y, double variance) {
        return (1 / (2 * Math.PI * Math.pow(variance, 2))
                * Math.exp(-(Math.pow(x, 2) + Math.pow(y, 2)) / (2 * Math.pow(variance, 2))));
    }

    private BufferedImage createGaussianImage(BufferedImage source_image, double[][] weights, int radius) {
        BufferedImage bi = new BufferedImage(source_image.getWidth(), source_image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < source_image.getWidth(); x++) {
            for (int y = 0; y < source_image.getHeight(); y++) {

                double[][] distributedColorRed = new double[radius][radius];
                double[][] distributedColorGreen = new double[radius][radius];
                double[][] distributedColorBlue = new double[radius][radius];

                for (int weightX = 0; weightX < weights.length; weightX++) {
                    for (int weightY = 0; weightY < weights[weightX].length; weightY++) {
//                        CHia làm 2 khối
                        int sampleX = x + weightX - (weights.length / 2);
                        int sampleY = y + weightY - (weights.length / 2);

                        if (sampleX > source_image.getWidth() - 1) {
                            int error_offset = sampleX - source_image.getWidth() - 1;
                            sampleX = source_image.getWidth() - radius - error_offset;
                        }

                        if (sampleY > source_image.getHeight() - 1) {
                            int error_offset = sampleY - source_image.getHeight() - 1;
                            sampleY = source_image.getHeight() - radius - error_offset;
                        }
//                          Lấy đối xứng đảm bảo distribution weight
                        if (sampleX < 0) {
                            sampleX = Math.abs(sampleX);
                        }
                        if (sampleY < 0) {
                            sampleY = Math.abs(sampleY);
                        }

                        double currentWeight = weights[weightX][weightY];

                        Color sampledColor = new Color(source_image.getRGB(sampleX, sampleY));

                        distributedColorRed[weightX][weightY] = currentWeight * sampledColor.getRed();
                        distributedColorGreen[weightX][weightY] = currentWeight * sampledColor.getGreen();
                        distributedColorBlue[weightX][weightY] = currentWeight * sampledColor.getBlue();
                    }
                }
                bi.setRGB(x, y, new Color(getWeightedColorValue(distributedColorRed),
                        getWeightedColorValue(distributedColorGreen),
                        getWeightedColorValue(distributedColorBlue)).getRGB());
            }
        }
        return bi;
    }
    
    
    private int getWeightedColorValue(double[][] weightedColor) {
        double summation = 0;

        for (int i = 0; i < weightedColor.length; i++) {
            for (int j = 0; j < weightedColor[i].length; j++) {
                summation += weightedColor[i][j];
            }
        }
        return (int) summation;
    }

}
