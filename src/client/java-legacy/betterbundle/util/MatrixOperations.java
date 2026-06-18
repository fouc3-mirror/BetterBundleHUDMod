package betterbundle.util;

import net.minecraft.client.gui.DrawContext;

/**
 * MatrixOperations for Minecraft 1.21.2-1.21.5
 * Uses MatrixStack
 */
public final class MatrixOperations {
    
    private MatrixOperations() {}
    
    public static void pushMatrix(DrawContext graphics) {
        graphics.getMatrices().push();
    }
    
    public static void popMatrix(DrawContext graphics) {
        graphics.getMatrices().pop();
    }
    
    public static void translateMatrix(DrawContext graphics, float x, float y) {
        graphics.getMatrices().translate(x, y, 0);
    }
    
    public static void scaleMatrix(DrawContext graphics, float x, float y) {
        graphics.getMatrices().scale(x, y, x);
    }
}