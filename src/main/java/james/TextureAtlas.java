package james;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL44C.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

@SuppressWarnings("all")
public class TextureAtlas {
    private int atlasCellWidth, atlasCellHeight;
    public int textureUnit;

    public TextureAtlas(String path, int atlasCellWidth, int atlasCellHeight, int textureUnit){
        this.atlasCellWidth = atlasCellWidth;
        this.atlasCellHeight = atlasCellHeight;

        int width;
        int height;
        int nrChannels;

        int[] widthBuffer = { 0 };
        int[] heightBuffer = { 0 };
        int[] nrChannelBuffer = { 0 };
        path = new File("").getAbsolutePath() + Main.RESOURCE_PATH + path;
        ByteBuffer dataBuffer = stbi_load(path, widthBuffer, heightBuffer, nrChannelBuffer, 0);
        if(dataBuffer == null) throw new Error("could not read file at " + path);
        width = widthBuffer[0];
        height = heightBuffer[0];
        nrChannels = nrChannelBuffer[0];
        
        byte[] data = new byte[width * height * nrChannels];
        dataBuffer.get(data);

        stbi_image_free(dataBuffer);
        
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        int atlas = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, atlas);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        //glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, 4);

        int xCt = width / atlasCellWidth;
        int yCt = height / atlasCellHeight;

        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA, atlasCellWidth, atlasCellHeight, xCt * yCt, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        
        for (int i = 0; i < xCt * yCt; i++) {
            int xOffset = (i % xCt) * atlasCellWidth;
            int yOffset = (i / yCt) * atlasCellHeight;
            
            ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(atlasCellWidth * atlasCellHeight * 4);
            
            for (int y = 0; y < atlasCellHeight; y++) {
                for (int x = 0; x < atlasCellWidth; x++) {
                    int atlasX = xOffset + x;
                    int atlasY = yOffset + y;
                    int pixelIndex = (atlasY * width + atlasX) * 4;
                    
                    pixelBuffer.put((y * atlasCellHeight + x) * 4, data[pixelIndex]);
                    pixelBuffer.put((y * atlasCellHeight + x) * 4 + 1, data[pixelIndex + 1]);
                    pixelBuffer.put((y * atlasCellHeight + x) * 4 + 2, data[pixelIndex + 2]);
                    pixelBuffer.put((y * atlasCellHeight + x) * 4 + 3, data[pixelIndex + 3]);
                }
            }
            
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, atlasCellWidth, atlasCellHeight, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        }
    }
}
