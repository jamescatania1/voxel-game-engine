package james;

import java.lang.Math;

public class Color {
    public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Color BLACK = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color RED = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color GREEN = new Color(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f, 1.0f);

    public float red, green, blue, alpha;

    public int rgba_7_7_7_11;
    public int rgba_8_8_8_8;
    public int rgba_4_4_4_8;

    public Color(float red, float green, float blue, float alpha){
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;

        rgba_7_7_7_11 = getRGBA_7_7_7_11();
        rgba_8_8_8_8 = getRGBA_8_8_8_8();
        rgba_4_4_4_8 = getRGBA_4_4_4_8();
    }
    
    public Color(double red, double green, double blue, double alpha){
        this.red = (float)red;
        this.green = (float)green;
        this.blue = (float)blue;
        this.alpha = (float)alpha;
        
        rgba_7_7_7_11 = getRGBA_7_7_7_11();
        rgba_8_8_8_8 = getRGBA_8_8_8_8();
        rgba_4_4_4_8 = getRGBA_4_4_4_8();
    }
    
    public Color(int red, int green, int blue, int alpha){
        this.red = (float)red / 256.0f;
        this.green = (float)green / 256.0f;
        this.blue = (float)blue / 256.0f;
        this.alpha = (float)alpha / 256.0f;
        
        rgba_7_7_7_11 = getRGBA_7_7_7_11();
        rgba_8_8_8_8 = getRGBA_8_8_8_8();
        rgba_4_4_4_8 = getRGBA_4_4_4_8();
    }

    public Color(int rgba_8888){
        this.red = (float)((rgba_8888 >> 24) & 0xFF) / 256.0f;
        this.green = (float)((rgba_8888 >> 16) & 0xFF) / 256.0f;
        this.blue = (float)((rgba_8888 >> 8) & 0xFF) / 256.0f;
        this.alpha = (float)(rgba_8888 & 0xFF) / 256.0f;

        this.rgba_7_7_7_11 = getRGBA_7_7_7_11();
        this.rgba_8_8_8_8 = rgba_8888;
        this.rgba_4_4_4_8 = getRGBA_4_4_4_8();
    }

    /**
     * Creates a deep copy of the color.
     * @param color
     */
    public Color(Color color){
        this(color.red, color.blue, color.green, color.alpha);
    }

    /**
     * Creates a color with an alpha component of 1.0.
     * @param red
     * @param blue
     * @param green
     */
    public Color(int red, int blue, int green){
        this(red, blue, green, 256);
    }

    /**
     * Creates a color with an alpha component of 1.0.
     * @param red
     * @param blue
     * @param green
     */
    public Color(float red, float blue, float green){
        this(red, blue, green, 1.0f);
    }

    /**
     * Creates a color with an alpha component of 1.0.
     * @param red
     * @param blue
     * @param green
     */
    public Color(double red, double blue, double green){
        this(red, blue, green, 1.0);
    }

    /**
     * Linear interpolate between this color and target color.
     * 
     * @param target 
     *      - the color to interpolate to.
     * @param value 
     *      - in range [0, 1].
     */
    public void Interpolate(Color target, float value){
        red += value * (target.red - red);
        green += value * (target.green - green);
        blue += value * (target.blue - blue);
        alpha += value * (target.alpha - alpha);
        UpdateColorIndexes();
    }

    /**
     * Creates a copy of the color object.
     */
    public Color Clone(){
        return new Color(red, green, blue, alpha);
    }

    /**
     * Updates the color's indexed color values, i.e. RGBA_77711 & RGBA_8888.
     */
    public void UpdateColorIndexes(){
        rgba_7_7_7_11 = getRGBA_7_7_7_11();
        rgba_8_8_8_8 = getRGBA_8_8_8_8();
        rgba_4_4_4_8 = getRGBA_4_4_4_8();
    }
    
    private int getRGBA_7_7_7_11(){
        int r = Math.min((int)(red * 128.0f), 127);
        int g = Math.min((int)(green * 128.0f), 127);
        int b = Math.min((int)(blue * 128.0f), 127);
        int a = Math.min((int)(alpha * 2048.0f), 2047);
        return (r & 0x7F) << 25 | (g & 0x7F) << 18 | (b & 0x7F) << 11 | a & 0x7FF;
    }

    private int getRGBA_8_8_8_8(){
        int r = Math.min((int)(red * 256.0f), 255);
        int g = Math.min((int)(green * 256.0f), 255);
        int b = Math.min((int)(blue * 256.0f), 255);
        int a = Math.min((int)(alpha * 256.0f), 255);
        return (r & 0xFF) << 24 | (g & 0xFF) << 16 | (b & 0xFF) << 8 | a & 0xFF;
    }

    private int getRGBA_4_4_4_8(){
        int r = Math.min((int)(red * 16.0f), 15);
        int g = Math.min((int)(green * 16.0f), 15);
        int b = Math.min((int)(blue * 16.0f), 15);
        int a = Math.min((int)(alpha * 256.0f), 255);
        return (r & 0xF) << 16 | (g & 0xF) << 12 | (b & 0xF) << 8 | a & 0xFF;
    }

    public static Color Add(Color a, Color b){
        return new Color(Math.min(1.0f, a.red + b.red), Math.min(1.0f, a.green + b.green), Math.min(1.0f, a.blue + b.blue), Math.min(1.0f, a.alpha + b.alpha));
    }

    /**
     * Hashes the color based on the rgba_8_8_8_8 32-bit encoding.
     */
    public int hashCode(){
        return rgba_8_8_8_8;
    }

    /**
     * Returns the color's formatted value in rgba [0, 1] format.
     */
    public String toString(){
        return String.valueOf(red) + ", " + String.valueOf(green) + ", " + String.valueOf(blue) + ", " + String.valueOf(alpha);
    }
}
