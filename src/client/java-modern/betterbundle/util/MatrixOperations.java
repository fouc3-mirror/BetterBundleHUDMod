package betterbundle.util;

import net.minecraft.client.gui.DrawContext;

/**
 * MatrixOperations for Minecraft 1.21.6+
 * Uses Matrix3x2fStack
 */
public final class MatrixOperations {
    
    private MatrixOperations() {}
    
    public static void pushMatrix(DrawContext graphics) {
        graphics.getMatrices().pushMatrix();
    }
    
    public static void popMatrix(DrawContext graphics) {
        graphics.getMatrices().popMatrix();
    }
    
    public static void translateMatrix(DrawContext graphics, float x, float y) {
        graphics.getMatrices().translate(x, y);
    }
    
    public static void scaleMatrix(DrawContext graphics, float x, float y) {
        graphics.getMatrices().scale(x, y);
    }
}