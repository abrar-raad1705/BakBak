import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GeneratePattern {
    public static void main(String[] args) {
        try {
            // 1. Light pattern
            BufferedImage lightImg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gLight = lightImg.createGraphics();
            setupGraphics(gLight);
            
            // Draw shapes in light theme color: rgba(51, 144, 236, 0.12)
            Color lightColor = new Color(51, 144, 236, 30); // 30 out of 255 is ~0.12 opacity
            drawShapes(gLight, lightColor);
            gLight.dispose();
            
            // 2. Dark pattern
            BufferedImage darkImg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gDark = darkImg.createGraphics();
            setupGraphics(gDark);
            
            // Draw shapes in dark theme color: rgba(255, 255, 255, 0.05)
            Color darkColor = new Color(255, 255, 255, 13); // 13 out of 255 is ~0.05 opacity
            drawShapes(gDark, darkColor);
            gDark.dispose();
            
            // Save files
            File outputDir = new File(args[0]);
            outputDir.mkdirs();
            ImageIO.write(lightImg, "png", new File(outputDir, "chat_pattern_light.png"));
            ImageIO.write(darkImg, "png", new File(outputDir, "chat_pattern_dark.png"));
            System.out.println("Patterns generated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.0f));
    }

    private static void drawShapes(Graphics2D g, Color color) {
        g.setColor(color);
        
        // Ring 1 at (8, 8)
        g.drawOval(1, 1, 14, 14);
        
        // Ring 2 at (40, 40)
        g.drawOval(33, 33, 14, 14);
        
        // Cross 1 at (40, 8)
        g.drawLine(34, 2, 46, 14);
        g.drawLine(34, 14, 46, 2);
        
        // Cross 2 at (8, 40)
        g.drawLine(2, 34, 14, 46);
        g.drawLine(2, 46, 14, 34);
    }
}
