package james;

import java.util.HashMap;

import static org.lwjgl.opengl.GL44C.*;

/**
 * Color palette singleton manager used for automatic static packing of colors into a 8-bit index, 
 * and necessary update of said index to a UBO object for reference in GLSL.
 * 
 * For the underlying hashing, colors are hashed in the rgba_8888 format (see james\\Color.java).
 * 
 * An index of "0" will always be used to refer to the clear/null color (RGBA 0,0,0,0)
 * Maximum of 255 colors, although may be changed if needed.
 */
public class ColorPalette {
    public static ColorPalette instance;

    private HashMap<Integer, Integer> colorIndexMap;

    private int colorDataUBO;
    private boolean reqBufferUpdate;
    private Color[] colors;

    /**
     * Initialize the color palette. Should call at initialization of scene that utilizes the palette, in a standard implementation.
     * If re-initalizing for some reason, will simply replace the active static instance.
     */
    public ColorPalette(){
        instance = this;

        colorIndexMap = new HashMap<>(256);
        colors = new Color[256];
        colorIndexMap.put(0, new Color(0.0, 0.0, 0.0, 0.0).rgba_8_8_8_8);

        //create global buffer for color array
        colorDataUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, colorDataUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)(256 * 16), GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 3, colorDataUBO);

        UpdateColorUBO();
    }

    /**
     * Gets the color's index in the palette. Adds to palette if not already entered, returning "0" if the palette is full.
     * @param color
     *      - the color whose index is in question.
     * @return
     *      - \in [0, 255]\qed
     */
    public static int GetColorPaletteIndex(Color color){
        return GetColorPaletteIndex(color.rgba_8_8_8_8);
    }

    /**
     * Gets the color's index in the palette. Adds to palette if not already entered, returning "0" if the palette is full.
     * UpdateColorUBO() must be called for any changes to the  palette to be reflected in the uniform GLSL buffer.
     * @param rgba_8888
     *      - the color whose index is in question, in the format rgba_8888.
     * @return
     *      - \in [0, 255]\qed
     */
    public static int GetColorPaletteIndex(Integer rgba_8888){
        if(!instance.colorIndexMap.containsKey(rgba_8888)){
            if(instance.colorIndexMap.size() >= 255) return 0;
            instance.colors[instance.colorIndexMap.size()] = new Color(rgba_8888);
            instance.colorIndexMap.put(rgba_8888, instance.colorIndexMap.size());
            instance.reqBufferUpdate = true;
            return instance.colorIndexMap.size() - 1;
        }
        return instance.colorIndexMap.get(rgba_8888);
    }

    /**
     * Update the color uniform buffer if necessary. This must be called manually.
     */
    public static void UpdateColorUBO(){
        if(!instance.reqBufferUpdate) return;
        instance.reqBufferUpdate = false;
        
        float[] colorPaletteData = new float[256 * 4];
        for(int i = 1; i < 256; i++){
            if(instance.colors[i] == null) continue;
            //System.out.println(String.valueOf(i) + " -- " + colors[i].toString());
            colorPaletteData[i * 4 + 0] = instance.colors[i].red;
            colorPaletteData[i * 4 + 1] = instance.colors[i].green;
            colorPaletteData[i * 4 + 2] = instance.colors[i].blue;
            colorPaletteData[i * 4 + 3] = instance.colors[i].alpha;
        }
        glBindBuffer(GL_UNIFORM_BUFFER, instance.colorDataUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, colorPaletteData);
    }
}
