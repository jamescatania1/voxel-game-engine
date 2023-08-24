package james.UI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;

import james.Main;

import static org.lwjgl.opengl.GL44C.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

/**
 * An object representing a font and its associated metadata. Should be registered with the static AddFont() method after being created.
 */
public class Font {
    public int textureAtlas;
    
    /**
     * Index location of the first dynamic GUI text object that has this font.
     */
    public int dynamicTextIndexLocation = 0;

    /**
     * Index location of the first static GUI text object that has this font.
     */
    public int staticTextIndexLocation = 0;

    /**
     * Offset, in vertices, of the dynamic texts' vertices that have this font in the UIManager's dynamic text vertex array.
     */
    public int dynamicBatchIndexOffset = 0;

    /** 
     * Offset, in vertices, of the static texts' vertices that have this font in the UIManager's static text vertex array.
     */
    public int staticBatchIndexOffset = 0;

    public int totalDynamicCharCt = 0;
    public int totalStaticCharCt = 0;
    
    public String name;
    //texture unit that the font's atlas is bound to
    public int textureUnit;
    //uniforms used in shader for sampling the distance field
    public float width, edge;
	
	private int atlasWidth, atlasHeight;
    public FontMetaData metadata;

    /**
     * Constructor for a font object.
     * @param name
     *      - name of the font that will be used as a key in the lookup table.
     * @param atlasPath
     *      - path of the font's signed distance field .png atlas.
     * @param metadataPath
     *      - path of the font's metadata .fnt file.
     * @param textureUnit
     *      - the OpenGL texture unit of the font's distance field atlas. The atlas will be bound to GL_TEXTURE*textureUnit*.
     * @param width
     *      - the distance that represents a positive test while sampling from the distance field, i.e. the thickness of the font characters.
     * @param edge
     *      - the distance from the width at which a negative test occurs while sampling form the distance field, i.e. the smoothing outside of the font characters.
     * @param padding
     *      - the padding of the glyph characters in the distance field, in pixels.
     */
	public Font(String name, String atlasPath, String metadataPath, int textureUnit, float width, float edge, int padding) {
        this.name = name;
        this.textureUnit = textureUnit;
        this.width = width;
        this.edge = edge;

        textureAtlas = LoadTexture(atlasPath);
		metadata = new FontMetaData(metadataPath, padding);
	}

	private int LoadTexture(String path) {
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer nrChannelBuffer = BufferUtils.createIntBuffer(1);
        path = new File("").getAbsolutePath() + "\\src\\main\\resources\\" + path;
        ByteBuffer dataBuffer = stbi_load(path, widthBuffer, heightBuffer, nrChannelBuffer, 0);
        if(dataBuffer == null) throw new Error("could not read file at " + path);
        atlasWidth = widthBuffer.get();
        atlasHeight = heightBuffer.get();
        //atlasNrChannels = nrChannelBuffer.get();
        
        glActiveTexture(GL_TEXTURE0 + textureUnit);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
		
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, dataBuffer);

		stbi_image_free(dataBuffer);

        glActiveTexture(GL_TEXTURE0);

		return texture;
    }

    public class FontMetaData {
        public static final int SPACE_ASCII = 32;
        private static final int PAD_TOP = 0, PAD_LEFT = 1, PAD_BOTTOM = 2, PAD_RIGHT = 3;
        public static final float LINE_HEIGHT = 0.03f;
        private static final int MAX_ID = 128;
        
        private BufferedReader reader;
        
        public float verticalPerPixelSlice;
        public float horizontalPerPixelSlice;
        public float spaceWidth;
        public int paddingDesired;
        public int[] padding;
        public int paddingWidth;
        public int paddingHeight;
        public CharacterData[] data = new CharacterData[MAX_ID];

        private int imageSize;

        public FontMetaData(String path, int paddingDesired) {
            path = new File("").getAbsolutePath() + Main.RESOURCE_PATH + path;
            this.paddingDesired = paddingDesired;
            try {
                reader = new BufferedReader(new FileReader(path));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("could not read font metadata file " + path);
            }
            HashMap<String, String> currentLine = readNextLine();
            padding = readIntegerList(currentLine.get("padding"));
            paddingWidth = padding[PAD_RIGHT] + padding[PAD_LEFT];
            paddingHeight = padding[PAD_TOP] + padding[PAD_BOTTOM];

            currentLine = readNextLine();
            int lineHeightPixels = Integer.parseInt(currentLine.get("lineHeight")) - paddingHeight;
            verticalPerPixelSlice = LINE_HEIGHT / (float)lineHeightPixels;
            horizontalPerPixelSlice = verticalPerPixelSlice;
            imageSize = Integer.parseInt(currentLine.get("scaleW"));

            readNextLine();
            readNextLine();

            currentLine = readNextLine();
            while(currentLine != null){
                CharacterData c = new CharacterData(currentLine);
                if(c.id != SPACE_ASCII && c.id >= 0){
                    data[c.id] = c;
                }
                currentLine = readNextLine();
            }

            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private HashMap<String, String> readNextLine() {
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {}
            if(line == null) return null;
            
            HashMap<String, String> result = new HashMap<>();
            for (String var : line.split(" ")) {
                String[] valPairs = var.split("=");
                if(valPairs.length == 2){
                    result.put(valPairs[0], valPairs[1]);
                }
            }
            return result;
        }
        private int[] readIntegerList(String in){
            String[] vars = in.split(",");
            int[] vals = new int[vars.length];
            for(int i = 0; i < vals.length; i++){
                vals[i] = Integer.parseInt(vars[i]);
            }
            return vals;
        }

        public class CharacterData {
            public int id;
            public float texCoordLeft;
            public float texCoordRight;
            public float texCoordBottom;
            public float texCoordTop;
            public float xOffset;
            public float yOffset;
            public float sizeX;
            public float sizeY;
            public float xAdvance;

            public CharacterData(HashMap<String, String> line){
                id = -1;
                if(!line.containsKey("id")) return;
                id = Integer.parseInt(line.get("id"));
                if (id == SPACE_ASCII) {
                    spaceWidth = (Integer.parseInt(line.get("xadvance")) - paddingWidth) * horizontalPerPixelSlice;
                    return;
                }
                texCoordLeft = ((float)Integer.parseInt(line.get("x")) + (padding[PAD_LEFT] - paddingDesired)) / imageSize;
                texCoordBottom = ((float)Integer.parseInt(line.get("y")) + (padding[PAD_TOP] - paddingDesired)) / imageSize;
                int charWidth = Integer.parseInt(line.get("width")) - (paddingWidth - (2 * paddingDesired));
                int charHeight = Integer.parseInt(line.get("height")) - (paddingHeight - (2 * paddingDesired));
                sizeX = (float)charWidth * (float)horizontalPerPixelSlice;
                sizeY = (float)charHeight * (float)verticalPerPixelSlice;
                texCoordRight = texCoordLeft + (float)charWidth / imageSize;
                texCoordTop = texCoordBottom + (float)charHeight / imageSize;
                xOffset = (Integer.parseInt(line.get("xoffset")) + padding[PAD_LEFT] - paddingDesired) * horizontalPerPixelSlice;
                yOffset = (Integer.parseInt(line.get("yoffset")) + padding[PAD_TOP] - paddingDesired) * verticalPerPixelSlice;
                xAdvance = (Integer.parseInt(line.get("xadvance")) - paddingWidth) * horizontalPerPixelSlice;
            }
        }
    }
}