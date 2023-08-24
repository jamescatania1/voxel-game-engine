package james.UI;
import james.*;
import james.UI.Library.*;
import james.UI.Text.TextMeshData;

import static org.lwjgl.opengl.GL44C.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main class for GUI library. To initialize, create new instance and register it as a gameobject.
 * Static functions wrap the singleton class and may be used once initialized.
 */
@SuppressWarnings("all")
public class UIManager implements GameObject {

    private int panelVAO;
    private int panelVBO;
    private int dynamicTextVAO;
    private int dynamicTextVBO;
    private int staticTextVAO;
    private int staticTextVBO;
    private ArrayList<Panel> panels;
    private ArrayList<Image> images;
    private Shader imageShader;

    private int windowInfoUBO;
    private int cullRectsUBO;
    private ArrayList<Transform> cullRectTranforms;
    private float[] cullRects;
    
    //panel/image vertex attributes
    private float[] panelImageVertices;
    private int[] panelImageVertexColors;
    private int[] panelImageVertexData;
    private int[] panelImageRectSizes;
    
    //dynamic text vertex attributes
    private float[] dynamicTextVertices;
    private int[] dynamicTextColors;
    private float[] dynamicTextTextureCoords;
    private int[] dynamicTextData;

    //static text vertex attributes
    private float[] staticTextVertices;
    private int[] staticTextColors;
    private float[] staticTextTextureCoords;
    private int[] staticTextData;

    private ArrayList<Text> dynamicTexts;
    private ArrayList<Text> staticTexts;
    private Shader textShader;
    private int totalDynamicCharCt;
    private int totalStaticCharCt;

    private int prevWindowX = 0;
    private int prevWindowY = 0;

    private HashMap<String, Font> fontIndex;
    private ArrayList<Font> fonts;

    private TextureAtlas imageAtlas;

    /**
     * Transform rect which covers the entire window [-1, 1] x [-1, 1] and is the root parent for the GUI transform tree.
     * In a common implementation, all immediate children of the canvas should use a ScaledRelativeConstraint such that appropriate
     * scaling aspects are maintained.
     */
    public Transform canvas;

    /**
     * Value by which all UI elements (including text) are scaled by. This value may be user controlled.
     */
    public double scaleFactor;

    public static UIManager instance;

    /**
     * Initializes UIManager instance.
     */
    public UIManager(){
        instance = this;

        // create global buffer object for window info
        windowInfoUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, windowInfoUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)8, GL_DYNAMIC_DRAW); //2 * 4 bytes
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, windowInfoUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);

        // create global buffer object for cull rects
        cullRectsUBO = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, cullRectsUBO);
		glBufferData(GL_UNIFORM_BUFFER, (long)1024, GL_DYNAMIC_DRAW); //4 * 4 * 64 bytes
		glBindBufferBase(GL_UNIFORM_BUFFER, 1, cullRectsUBO);
		glBindBuffer(GL_UNIFORM_BUFFER, 0);
        cullRectTranforms = new ArrayList<>();
        cullRects = new float[64 * 4];
        
        //initialize image atlas
        imageAtlas = new TextureAtlas("ui_atlas.png", 128, 128, Game.currentScene.GetNextTextureUnit());
        
        panels = new ArrayList<>();
        images = new ArrayList<>();
        imageShader = new Shader("image.vert", "image.frag");
        imageShader.SetInt("atlas", GL_TEXTURE0 + imageAtlas.textureUnit);
        
        fontIndex = new HashMap<String, Font>();
        fonts = new ArrayList<>();
        
        textShader = new Shader("text.vert", "text.frag");
        
        //set the UBO block binding points for the image shader (and other shaders if needed in future)
        imageShader.Use();
        int windowInfoBindingIndex = glGetUniformBlockIndex(imageShader.id, "windowInfo");
        glUniformBlockBinding(imageShader.id, windowInfoBindingIndex, 0);
        int cullRectsBindingIndex = glGetUniformBlockIndex(imageShader.id, "cullRects");
        glUniformBlockBinding(imageShader.id, cullRectsBindingIndex, 1);
        
        staticTexts = new ArrayList<>();
        dynamicTexts = new ArrayList<>();
        //initialize canvas
        canvas = new Transform(null, Anchor.Center, Anchor.Center, 0, new AbsoluteConstraint(2.0f), new AbsoluteConstraint(2.0f));

        scaleFactor = 1.0;
    }

    public static void AddPanel(Panel panel){
        instance.panels.add(panel);
    }

    public static void AddImage(Image image){
        instance.images.add(image);
    }

    public static void AddText(Text text){
        if(text.meshUpdateMode == MeshUpdateMode.Dynamic) instance.dynamicTexts.add(text.font.dynamicTextIndexLocation, text);
        else if(text.meshUpdateMode == MeshUpdateMode.Static) instance.staticTexts.add(text.font.staticTextIndexLocation, text);

        //update the offset and stride text index values for the fonts that come after that of the added text
        boolean afterTargFont = false;
        for(int i = 0; i < instance.fonts.size(); i++){
            if(instance.fonts.get(i) == text.font){
                afterTargFont = true;
                continue;
            } 
            if(!afterTargFont) continue;
            Font cmpFont = instance.fonts.get(i);
            if(text.meshUpdateMode == MeshUpdateMode.Dynamic) {
                cmpFont.dynamicTextIndexLocation++;
                cmpFont.dynamicBatchIndexOffset += text.charCt;
            }
            if(text.meshUpdateMode == MeshUpdateMode.Static) {
                cmpFont.staticTextIndexLocation++;
                cmpFont.staticBatchIndexOffset += text.charCt;
            }
        }
    }

    public static void AddFont(Font font){
        instance.fonts.add(font);
        font.dynamicTextIndexLocation = instance.dynamicTexts.size();
        font.staticTextIndexLocation = instance.staticTexts.size();
        instance.fontIndex.put(font.name, font);
    }

    public static Font GetFont(String fontName){
        return instance.fontIndex.get(fontName);
    }

    public static void RefreshUIBuffer(){
        instance.RefreshBuffer();
    }

    public static void RegisterTransformAsCullRect(Transform transform){
        transform.cullIndex = instance.cullRectTranforms.size() + 1;
        if(transform.cullIndex >= 63) throw new Error("cull rect overflow--cannot register more than 63 cull rects");
        instance.cullRectTranforms.add(transform);
    }

    /**
     * Builds all the GUI meshes and buffers them.
     */
    private void RefreshBuffer(){
        //panels/images
        int[] indices = new int[6 * (panels.size() + images.size())];
        for(int i = 0; i < panels.size() + images.size(); i++){
            indices[6 * i + 0] = 4 * i + 0;
            indices[6 * i + 1] = 4 * i + 3;
            indices[6 * i + 2] = 4 * i + 1;
            indices[6 * i + 3] = 4 * i + 3;
            indices[6 * i + 4] = 4 * i + 2;
            indices[6 * i + 5] = 4 * i + 1;
        }

        int[] VAO = { 0 };
        int[] VBO = { 0 };
        int[] EBO = { 0 };
        glGenVertexArrays(VAO);
        glGenBuffers(VBO);
        glGenBuffers(EBO);
        glBindVertexArray(VAO[0]);

        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        int[] vertices = new int[4 * 7 * (panels.size() + images.size())];
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, (long)0);
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, (long)(8 * 4 * (panels.size() + images.size())));
        glVertexAttribIPointer(2, 3, GL_UNSIGNED_INT, 0, (long)(12 * 4 * (panels.size() + images.size())));
        glVertexAttribIPointer(3, 1, GL_UNSIGNED_INT, 0, (long)(24 * 4 * (panels.size() + images.size())));
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0); 

        glBindVertexArray(0); 

        panelVAO = VAO[0];
        panelVBO = VBO[0];

        panelImageVertices = new float[8 * (panels.size() + images.size())];
        panelImageVertexColors = new int[4 * (panels.size() + images.size())];
        panelImageVertexData = new int[12 * (panels.size() + images.size())];
        panelImageRectSizes = new int[4 * (panels.size() + images.size())];

        //text
        totalDynamicCharCt = 0;
        totalStaticCharCt = 0;
        for(Text text : dynamicTexts) totalDynamicCharCt += text.charCt;
        for(Text text : staticTexts) totalStaticCharCt += text.charCt;

        //create dynamic text buffers
        int[] dynamicIndices = new int[6 * totalDynamicCharCt];
        for(int i = 0; i < totalDynamicCharCt; i++){
            dynamicIndices[6 * i + 0] = 4 * i + 0;
            dynamicIndices[6 * i + 1] = 4 * i + 1;
            dynamicIndices[6 * i + 2] = 4 * i + 3;
            dynamicIndices[6 * i + 3] = 4 * i + 1;
            dynamicIndices[6 * i + 4] = 4 * i + 2;
            dynamicIndices[6 * i + 5] = 4 * i + 3;
        }

        VAO = new int[]{ 0 };
        VBO = new int[]{ 0 };
        EBO = new int[]{ 0 };
        glGenVertexArrays(VAO);
        glGenBuffers(VBO);
        glGenBuffers(EBO);
        glBindVertexArray(VAO[0]);

        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        int[] dynamicVertices = new int[6 * 4 * totalDynamicCharCt];
        glBufferData(GL_ARRAY_BUFFER, dynamicVertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, dynamicIndices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, (long)0);
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, (long)(8 * 4 * totalDynamicCharCt));
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, (long)(12 * 4 * totalDynamicCharCt));
        glVertexAttribIPointer(3, 1, GL_UNSIGNED_INT, 0, (long)(20 * 4 * totalDynamicCharCt));
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0); 

        glBindVertexArray(0); 

        dynamicTextVAO = VAO[0];
        dynamicTextVBO = VBO[0];

        dynamicTextVertices = new float[8 * totalDynamicCharCt];
        dynamicTextColors = new int[4 * totalDynamicCharCt];
        dynamicTextTextureCoords = new float[8 * totalDynamicCharCt];
        dynamicTextData = new int[4 * totalDynamicCharCt];

        //create static text buffers
        int[] staticIndices = new int[6 * totalStaticCharCt];
        for(int i = 0; i < totalStaticCharCt; i++){
            staticIndices[6 * i + 0] = 4 * i + 0;
            staticIndices[6 * i + 1] = 4 * i + 1;
            staticIndices[6 * i + 2] = 4 * i + 3;
            staticIndices[6 * i + 3] = 4 * i + 1;
            staticIndices[6 * i + 4] = 4 * i + 2;
            staticIndices[6 * i + 5] = 4 * i + 3;
        }

        VAO = new int[]{ 0 };
        VBO = new int[]{ 0 };
        EBO = new int[]{ 0 };
        glGenVertexArrays(VAO);
        glGenBuffers(VBO);
        glGenBuffers(EBO);
        glBindVertexArray(VAO[0]);

        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        int[] staticVertices = new int[6 * 4 * totalStaticCharCt];
        glBufferData(GL_ARRAY_BUFFER, staticVertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, staticIndices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, (long)0);
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, (long)(8 * 4 * totalStaticCharCt));
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, (long)(12 * 4 * totalStaticCharCt));
        glVertexAttribIPointer(3, 1, GL_UNSIGNED_INT, 0, (long)(20 * 4 * totalStaticCharCt));
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0); 

        glBindVertexArray(0); 

        staticTextVAO = VAO[0];
        staticTextVBO = VBO[0];

        staticTextVertices = new float[8 * totalStaticCharCt];
        staticTextColors = new int[4 * totalStaticCharCt];
        staticTextTextureCoords = new float[8 * totalStaticCharCt];
        staticTextData = new int[4 * totalStaticCharCt];
        
        // Set static vertex buffer attributes (colors, texcoords, textdata). May update more frequenly if needed.
        UpdateVertexBuffersStatic();
    }

    /**
     * To be called in the draw function. Updates dynamic vertex buffers.
     */
    private void UpdateVertexBuffersDynamic(){
        //update panel/image vertices
        for (int i = 0; i < panels.size(); i++){
            Panel panel = panels.get(i);
            
            panelImageVertices[i * 8 + 4] = panel.transform.left;
            panelImageVertices[i * 8 + 5] = panel.transform.top;
            
            panelImageVertices[i * 8 + 6] = panel.transform.right;
            panelImageVertices[i * 8 + 7] = panel.transform.top;
            
            panelImageVertices[i * 8 + 0] = panel.transform.right;
            panelImageVertices[i * 8 + 1] = panel.transform.bottom;
            
            panelImageVertices[i * 8 + 2] = panel.transform.left;
            panelImageVertices[i * 8 + 3] = panel.transform.bottom;
        }
        for (int i = panels.size(); i < panels.size() + images.size(); i++){
            Image image = images.get(i - panels.size());
            
            panelImageVertices[i * 8 + 4] = image.transform.left;
            panelImageVertices[i * 8 + 5] = image.transform.top;
            
            panelImageVertices[i * 8 + 6] = image.transform.right;
            panelImageVertices[i * 8 + 7] = image.transform.top;
            
            panelImageVertices[i * 8 + 0] = image.transform.right;
            panelImageVertices[i * 8 + 1] = image.transform.bottom;

            panelImageVertices[i * 8 + 2] = image.transform.left;
            panelImageVertices[i * 8 + 3] = image.transform.bottom;
        }
        for (int i = 0; i < panels.size(); i++){
            Panel panel = panels.get(i);
            for(int j = 0; j < 4; j++){
                panelImageVertexColors[i * 4 + j] = panel.color.rgba_7_7_7_11;

                panelImageVertexData[i * 12 + j * 3 + 0] = panel.PackVertex(j, 0);
                panelImageVertexData[i * 12 + j * 3 + 1] = panel.PackVertex(j, 1);
                panelImageVertexData[i * 12 + j * 3 + 2] = panel.PackVertex(j, 2);

                panelImageRectSizes[i * 4 + j] = panel.transform.PackSize();
            }
        }
        for (int i = panels.size(); i < panels.size() + images.size(); i++){
            Image image = images.get(i - panels.size());
            for(int j = 0; j < 4; j++){
                panelImageVertexColors[i * 4 + j] = image.color.rgba_7_7_7_11;

                panelImageVertexData[i * 12 + j * 3 + 0] = image.PackVertex(j, 0);
                panelImageVertexData[i * 12 + j * 3 + 1] = image.PackVertex(j, 1);
                panelImageVertexData[i * 12 + j * 3 + 2] = image.PackVertex(j, 2);
                
                panelImageRectSizes[i * 4 + j] = image.transform.PackSize();
            }
        }

        //buffer panel/image vertices
        glBindVertexArray(panelVAO);
        glBindBuffer(GL_ARRAY_BUFFER, panelVBO);

        glBufferSubData(GL_ARRAY_BUFFER, (long)0, panelImageVertices);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(4 * 4 * 2 * (panels.size() + images.size())), panelImageVertexColors);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(4 * 6 * 2 * (panels.size() + images.size())), panelImageVertexData);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(4 * 12 * 2 * (panels.size() + images.size())), panelImageRectSizes);

        //update dynamic text vertices
        int charIndex = 0;
        for(Text text : dynamicTexts){
            text.UpdateMesh();
            TextMeshData mesh = text.meshData; 
            for(int i = 0; i < text.charCt; i++){
                for(int k = 0; k < 8; k++) {
                    dynamicTextVertices[charIndex * 8 + i * 8 + k] = mesh.vertices[i * 8 + k];
                    dynamicTextTextureCoords[charIndex * 8 + i * 8 + k] = mesh.textureCoords[i * 8 + k];
                }
                for(int k = 0; k < 4; k++) {
                    dynamicTextColors[charIndex * 4 + i * 4 + k] = text.color.rgba_7_7_7_11;
                    dynamicTextData[charIndex * 4 + i * 4 + k] = text.PackInfo(i);
                }
            }
            charIndex += text.charCt;
        }
        
        //buffer dynamic text vertices
        glBindVertexArray(dynamicTextVAO);
        glBindBuffer(GL_ARRAY_BUFFER, dynamicTextVBO);

        glBufferSubData(GL_ARRAY_BUFFER, (long)0, dynamicTextVertices);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalDynamicCharCt * 4 * 8), dynamicTextColors);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalDynamicCharCt * 4 * 12), dynamicTextTextureCoords);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalDynamicCharCt * 4 * 20), dynamicTextData);


        //update static text vertex positions if needed
        boolean reqUpdateStaticTextVertices = false;
        if (prevWindowX != Main.windowX || prevWindowY != Main.windowY) reqUpdateStaticTextVertices = true;
        else for(Text text : staticTexts) if(text.transform.transformChanged) reqUpdateStaticTextVertices = true;
        prevWindowX = Main.windowX;
        prevWindowY = Main.windowY;

        if(reqUpdateStaticTextVertices) {
            charIndex = 0;
            for(Text text : staticTexts){
                text.UpdateMesh();
                TextMeshData mesh = text.meshData; 
                for(int i = 0; i < text.charCt; i++){
                    for(int k = 0; k < 8; k++) {
                        staticTextVertices[charIndex * 8 + i * 8 + k] = mesh.vertices[i * 8 + k];
                    }
                }
                charIndex += text.charCt;
            }
            
            //buffer dynamic text vertices
            glBindVertexArray(staticTextVAO);
            glBindBuffer(GL_ARRAY_BUFFER, staticTextVBO);
    
            glBufferSubData(GL_ARRAY_BUFFER, (long)0, staticTextVertices);
        }
    }

    /**
     * To be called at initialization, or sparsely. Updates static vertex buffers.
     */
    private void UpdateVertexBuffersStatic(){
        //update static text vertex attributes
        int charIndex = 0;
        for(Text text : staticTexts){
            text.UpdateMesh();
            TextMeshData mesh = text.meshData; 
            for(int i = 0; i < text.charCt; i++){
                for(int k = 0; k < 8; k++) {
                    staticTextTextureCoords[charIndex * 8 + i * 8 + k] = mesh.textureCoords[i * 8 + k];
                }
                for(int k = 0; k < 4; k++) {
                    staticTextColors[charIndex * 4 + i * 4 + k] = text.color.rgba_7_7_7_11;
                    staticTextData[charIndex * 4 + i * 4 + k] = text.PackInfo(i);
                }
            }
            charIndex += text.charCt;
        }
        
        //buffer static text vertices
        glBindVertexArray(staticTextVAO);
        glBindBuffer(GL_ARRAY_BUFFER, staticTextVBO);

        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalStaticCharCt * 4 * 8), staticTextColors);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalStaticCharCt * 4 * 12), staticTextTextureCoords);
        glBufferSubData(GL_ARRAY_BUFFER, (long)(totalStaticCharCt * 4 * 20), staticTextData);
    }

    public void Update() {
        //update UI transformations
        canvas.UpdateTransformRecursive();

        //set the global buffer object data for window info
        int[] windowInfo = new int[]{Main.windowX, Main.windowY};
        glBindBuffer(GL_UNIFORM_BUFFER, windowInfoUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, windowInfo);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, windowInfoUBO);

        // set the global buffer object data for cull rects
        for(int i = 0; i < cullRectTranforms.size(); i++){
            Transform transform = cullRectTranforms.get(i);
            cullRects[(i + 1) * 4 + 0] = transform.top;
            cullRects[(i + 1) * 4 + 1] = transform.right;
            cullRects[(i + 1) * 4 + 2] = transform.bottom;
            cullRects[(i + 1) * 4 + 3] = transform.left;
        }
        glBindBuffer(GL_UNIFORM_BUFFER, cullRectsUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, cullRects);
        glBindBufferBase(GL_UNIFORM_BUFFER, 1, cullRectsUBO);
    }

    public void FixedUpdate() {
    }

    public void Draw() {
        UpdateVertexBuffersDynamic();

        //draw panels/images
        glBindVertexArray(panelVAO);
        glBindBuffer(GL_ARRAY_BUFFER, panelVBO);
        imageShader.Use();
        glDrawElements(GL_TRIANGLES, 6 * (panels.size() + images.size()), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0); 

        //draw dynamic text
        glBindVertexArray(dynamicTextVAO);
        glBindBuffer(GL_ARRAY_BUFFER, dynamicTextVBO);
        for(int i = 0; i < fonts.size(); i++){
            Font font = fonts.get(i);
            textShader.SetInt("atlas", font.textureUnit);
            textShader.SetFloat("glyphWidth", font.width);
            textShader.SetFloat("glyphEdge", font.edge);
            glDrawElementsBaseVertex(GL_TRIANGLES, 6 * font.totalDynamicCharCt, GL_UNSIGNED_INT, 0, 4 * font.dynamicBatchIndexOffset);
        }
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        //draw static text
        glBindVertexArray(staticTextVAO);
        glBindBuffer(GL_ARRAY_BUFFER, staticTextVBO);
        for(int i = 0; i < fonts.size(); i++){
            Font font = fonts.get(i);
            textShader.SetInt("atlas", font.textureUnit);
            textShader.SetFloat("glyphWidth", font.width);
            textShader.SetFloat("glyphEdge", font.edge);
            glDrawElementsBaseVertex(GL_TRIANGLES, 6 * font.totalStaticCharCt, GL_UNSIGNED_INT, 0, 4 * font.staticBatchIndexOffset);
        }
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public void UpdateUITransforms(){
        canvas.UpdateTransformRecursive();
    }
}
