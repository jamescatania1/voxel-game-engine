package james.UI;

import james.Main;
import james.Color;
import james.UI.Font.FontMetaData;
import james.UI.Library.Alignment;
import james.UI.Library.ClipDirection;
import james.UI.Library.MeshUpdateMode;
import james.UI.Library.OverflowMode;
import james.UI.Library.WrapMode;

import static james.UI.Library.*;

import java.util.ArrayList;

/**
 * A GUI text object that uses the signed distance field rendering. Should be registered with the static AddText() method after creation.
 */
public class Text {
    private String text;
    public Font font;
    public Color color;
    public Transform transform;
    
    public Alignment xAlignment;
    public Alignment yAlignment;
    public WrapMode wrapMode;
    public OverflowMode overflowMode;
    public MeshUpdateMode meshUpdateMode; 

    /**
     * Used to cull mesh updates for dynamic text objects when an update is not needed.
     */
    private boolean meshUpdateReqested = true;

    private int[] clipRectIndicies;
    private ClipDirection[] clipDirections;

    public int charCt;
    public TextMeshData meshData;
    
    private ArrayList<Float> localLineWidths; 
    private ArrayList<Integer> localLineIndices;

    /**
     * clipOutsideRect (1 bit) | *cull (1 bit) | cullRect_1 (6 bits) | cullRect_2 (6 bits) | cullRect_3 (6 bits) | ---
     */
    private int packedInfo;

    private float fontSize;

    /**
     * Constructor for a new text object.
     * 
     * @param text
     *          - the text string that is to be displayed.
     * @param font
     *          - the name of the font.
     * @param fontSize
     *          - the font size (automatically scaled with screen size).
     * @param color
     *          - the text color.
     * @param xAlignment
     *          - the alignment of each line of text to its transform's box-bound.
     * @param yAlignment
     *          - the alignment of the text block to its transform's box bound. 
     * @param wrapMode
     *          - enables/disables text wrapping.
     * @param overflowMode
     *          - whether text overflows or clips when outside rect.
     * @param meshUpdateMode
     *          - specifies whether the mesh will be fully rebuilt each frame (dynamic) or will only update color and transformation scale/position that is bound to the transform (static).
     * @param transform
     *          - text transform.
     */
    public Text(String text, String font, float fontSize, Color color, Alignment xAlignment, Alignment yAlignment, WrapMode wrapMode, OverflowMode overflowMode, MeshUpdateMode meshUpdateMode, Transform transform){
        this.text = text;

        this.font = UIManager.GetFont(font);
        this.color = color;
        this.fontSize = fontSize;
        this.xAlignment = xAlignment;
        this.yAlignment = yAlignment;
        this.wrapMode = wrapMode;
        this.overflowMode = overflowMode;
        this.meshUpdateMode = meshUpdateMode;
        this.transform = transform;

        charCt = 0;
        for(int i = 0; i < text.length(); i++){
            if((int)text.charAt(i) == Font.FontMetaData.SPACE_ASCII) continue;
            charCt++;
        }
        if(meshUpdateMode == MeshUpdateMode.Dynamic) this.font.totalDynamicCharCt += charCt;
        else if(meshUpdateMode == MeshUpdateMode.Static) this.font.totalStaticCharCt += charCt;

        clipRectIndicies = new int[3];
        clipDirections = new ClipDirection[3];
        if(overflowMode == OverflowMode.Clip){
            AddCullRect(transform, ClipDirection.ClipOutside);
        }
        UpdateVertexInfo();

        meshData = null;
    }

    /**
     * Constructor for an empty text object with a specified maximum number of characters to buffer. Should use the dynamic mesh update mode in most normal implementations.
     * 
     * @param maxCharCt
     *          - the number of empty characters to buffer.
     * @param font
     *          - the name of the font.
     * @param fontSize
     *          - the font size (automatically scaled with screen size).
     * @param color
     *          - the text color.
     * @param xAlignment
     *          - the alignment of each line of text to its transform's box-bound.
     * @param yAlignment
     *          - the alignment of the text block to its transform's box bound. 
     * @param wrapMode
     *          - enables/disables text wrapping.
     * @param overflowMode
     *          - whether text overflows or clips when outside rect.
     * @param meshUpdateMode
     *          - specifies whether the mesh will be fully rebuilt each frame (dynamic) or will only update color and transformation scale/position that is bound to the transform (static).
     * @param transform
     *          - text transform.
     */
    public Text(int maxCharCount, String font, float fontSize, Color color, Alignment xAlignment, Alignment yAlignment, WrapMode wrapMode, OverflowMode overflowMode, MeshUpdateMode meshUpdateMode, Transform transform){
        charCt = maxCharCount;

        //initialize string to empty string
        char[] textChars = new char[maxCharCount];
        for(int i = 0; i < maxCharCount; i++){
            textChars[i] = '\0';
        }
        text = new String(textChars);

        this.font = UIManager.GetFont(font);
        this.color = color;
        this.fontSize = fontSize;
        this.xAlignment = xAlignment;
        this.yAlignment = yAlignment;
        this.wrapMode = wrapMode;
        this.overflowMode = overflowMode;
        this.meshUpdateMode = meshUpdateMode;
        this.transform = transform;

        charCt = 0;
        for(int i = 0; i < text.length(); i++){
            if((int)text.charAt(i) == Font.FontMetaData.SPACE_ASCII) continue;
            charCt++;
        }
        if(meshUpdateMode == MeshUpdateMode.Dynamic) this.font.totalDynamicCharCt += charCt;
        else if(meshUpdateMode == MeshUpdateMode.Static) this.font.totalStaticCharCt += charCt;

        clipRectIndicies = new int[3];
        clipDirections = new ClipDirection[3];
        if(overflowMode == OverflowMode.Clip){
            AddCullRect(transform, ClipDirection.ClipOutside);
        }
        UpdateVertexInfo();

        meshData = null;
    }

    /**
     * Sets the text object's string.
     * @param newString
     *      - new text string. Must be of size less or equal to that is buffered (charCt).
     */
    public void SetText(String newString) {
        if(newString.length() > charCt) throw new Error("text string is larger than buffer");
        char[] newChars = new char[charCt];
        for(int i = 0; i < charCt; i++){
            if(i < newString.length()) newChars[i] = newString.charAt(i);
            else newChars[i] = '\0';
        }
        this.text = new String(newChars);
        meshUpdateReqested = true;
    }

    public String GetText() {
        return text;
    }

    /**
     * Builds the text's mesh if necessary/desired, and updates its position/scale.
     */
    public void UpdateMesh(){
        if(meshData == null) {
            meshData = new TextMeshData(new float[charCt * 8], new float[charCt * 8], new float[charCt * 8]);
            BuildTextMesh();
        }
        else if(transform.transformChanged && wrapMode == WrapMode.Wrap){
            BuildTextMesh();
        }
        else if(meshUpdateMode == MeshUpdateMode.Dynamic){
            if(meshUpdateReqested) BuildTextMesh();
        }

        TransformMesh();
    }

    /**
     * Adds the cullTransform to one of the text's cull rect slots, if available. Updates vertex info at end, need not call
     * UpdateVertexInfo() after doing this.
     * @param cullTransform
     *      - the transform whose rect is to be registered.
     * @param clipDirection
     *      - whether to clip the parts of the text that are inside the cullTransform (ClipInside), or to
     *        clip the parts that are outside the cullTransform (Clipoutside).
     */
    public void AddCullRect(Transform cullTransform, ClipDirection clipDirection){
        if(clipRectIndicies[0] == 0) AddCullRect(cullTransform, clipDirection, 0);
        else if(clipRectIndicies[1] == 0) AddCullRect(cullTransform, clipDirection, 1);
        else if(clipRectIndicies[2] == 0) AddCullRect(cullTransform, clipDirection, 2);
    }
    /**
     * Adds the cullTransform as a clip rect for the text, i.e. a mask. Updates vertex info at end, need not call
     * UpdateVertexInfo() after doing this.
     * @param cullTransform
     *      - the transform whose rect is to be registered.
     * @param clipDirection
     *      - whether to clip the parts of the text that are inside the cullTransform (ClipInside), or to
     *        clip the parts that are outside the cullTransform (Clipoutside).
     * @param index
     *      - the text's index to store the cull rect index. Within range [0, 2].
     */
    public void AddCullRect(Transform cullTransform, ClipDirection clipDirection, int index){
        if(index < 0 || index >= 3) throw new Error("cull rect index is out of bounds. Must be in range [0, 2].");
        if(cullTransform.cullIndex == 0) RegisterTransformAsCullRect(cullTransform);
        clipRectIndicies[index] = cullTransform.cullIndex;
        clipDirections[index] = clipDirection;
        overflowMode = OverflowMode.Clip;
        UpdateVertexInfo();
    }

    /**
     * Updates static-packed data vertex attributes.
     */
    public void UpdateVertexInfo(){
        //set static parts (non-character dependent) of packed info attribute to be referenced
        packedInfo =
            ((overflowMode == OverflowMode.Clip ? 1 : 0) & 0x1) << 31 |
            (clipRectIndicies[0] & 0x3F) << 24 |
            (clipRectIndicies[1] & 0x3F) << 18 |
            (clipRectIndicies[2] & 0x3F) << 12 |
            ((clipDirections[0] == ClipDirection.ClipInside ? 1 : 0) & 0x1) << 11 |
            ((clipDirections[1] == ClipDirection.ClipInside ? 1 : 0) & 0x1) << 10 |
            ((clipDirections[2] == ClipDirection.ClipInside ? 1 : 0) & 0x1) << 9;
    }

    /**
     * Applies the transformation to the meshData's "vertices" that is a result of moving/scaling the transform component. Also updates color if necessary.
     */
    private void TransformMesh(){
        float fontSizeX = GetFontSizeX();
        float fontSizeY = GetFontSizeY();

        for(int i = 0; i < charCt; i++){
            for(int j = 0; j < 8; j += 2){
                meshData.vertices[i * 8 + j + 0] = meshData.localVertices[i * 8 + j + 0] * fontSizeX;
                meshData.vertices[i * 8 + j + 1] = meshData.localVertices[i * 8 + j + 1] * fontSizeY;
                
                meshData.vertices[i * 8 + j + 0] += transform.left + (transform.right - transform.left) / 2.0f;
                meshData.vertices[i * 8 + j + 1] += transform.bottom + (transform.top - transform.bottom) / 2.0f;
            }
        }

        //calculate height alignment adjustment
        float textHeight = localLineIndices.size() * Font.FontMetaData.LINE_HEIGHT * fontSizeY;
        float heightAlignmentOffset = 0.0f;
        if(yAlignment == Alignment.Center) heightAlignmentOffset = textHeight / 2.0f;
        else if(yAlignment == Alignment.Top) heightAlignmentOffset = (transform.top - transform.bottom) / 2.0f;
        else if(yAlignment == Alignment.Bottom) heightAlignmentOffset = (transform.bottom - transform.top) / 2.0f + textHeight;

        for(int i = 0; i < localLineIndices.size(); i++){
            int charIndex = localLineIndices.get(i);
            int nextLineIndex = i + 1 < localLineIndices.size() ? localLineIndices.get(i + 1) : charCt;

            //calculate width alignment adjustment
            float lineWidth = localLineWidths.get(i) * fontSizeX;
            float widthAligmentOffset = 0.0f;
            if(xAlignment == Alignment.Center) widthAligmentOffset = -lineWidth / 2.0f;
            else if(xAlignment == Alignment.Left) widthAligmentOffset = (transform.left - transform.right) / 2.0f;
            else if(xAlignment == Alignment.Right) widthAligmentOffset = (transform.right - transform.left) / 2.0f - lineWidth;

            for(int j = charIndex; j < nextLineIndex; j++){
                for(int k = 0; k < 8; k += 2) {
                    meshData.vertices[j * 8 + k] += widthAligmentOffset;
                    meshData.vertices[j * 8 + k + 1] += heightAlignmentOffset;
                }
            }
        }
    }

    /**
     * Creates the text's mesh, calculates text wrapping if needed and inserts line breaks.
     */
    private void BuildTextMesh(){
        meshUpdateReqested = false;

        localLineWidths = new ArrayList<>();
        localLineIndices = new ArrayList<>();

        //split lines by newline characters
        String[] rawlines = text.split("\\R");
        ArrayList<String> lines = new ArrayList<>();
        for(int i = 0; i < rawlines.length; i++) lines.add(rawlines[i]);

        //text wrapping
        if(wrapMode == WrapMode.Wrap){
            for(int i = 0; i < lines.size(); i++){
                String line = lines.get(i);
                if (MeasureStringWidth(line, true) > transform.right - transform.left) {
                    String[] lineWords = line.split(" ");
                    String newline = lineWords[0];
                    int nextWordIndex = 1;
                    while(nextWordIndex < lineWords.length 
                     && MeasureStringWidth(newline + " " + lineWords[nextWordIndex], true) < transform.right - transform.left) {
                        newline = newline + " " + lineWords[nextWordIndex];
                        nextWordIndex++;
                    }

                    lines.set(i, newline);
                    String nextLine = nextWordIndex < lineWords.length ? lineWords[nextWordIndex] : "";
                    nextWordIndex++;
                    for(int k = nextWordIndex; k < lineWords.length; k++){
                        nextLine += " " + lineWords[k];
                    }
                    if(!nextLine.isEmpty()) lines.add(i + 1, nextLine);
                }
            }
        }

        int charIndex = 0;
        float cursorX = 0.0f;
        float cursorY = 0.0f;
        for(int l = 0; l < lines.size(); l++){
            localLineIndices.add(charIndex);
            localLineWidths.add(MeasureStringWidth(lines.get(l), false));

            char[] chars = lines.get(l).toCharArray();
            cursorX = 0.0f;

            for(int i = 0; i < chars.length; i++){
                Font.FontMetaData.CharacterData c = font.metadata.data[(int)chars[i]];    

                if((int)chars[i] == FontMetaData.SPACE_ASCII) {
                    cursorX += font.metadata.spaceWidth;
                    continue;
                }

                float left = cursorX + c.xOffset;
                float right = left + c.sizeX;
                float bottom = cursorY - c.yOffset;
                float top = bottom - c.sizeY;

                meshData.localVertices[charIndex * 8 + 0] = left;
                meshData.localVertices[charIndex * 8 + 1] = top;
                meshData.localVertices[charIndex * 8 + 2] = right;
                meshData.localVertices[charIndex * 8 + 3] = top;
                meshData.localVertices[charIndex * 8 + 4] = right;
                meshData.localVertices[charIndex * 8 + 5] = bottom;
                meshData.localVertices[charIndex * 8 + 6] = left;
                meshData.localVertices[charIndex * 8 + 7] = bottom;

                meshData.textureCoords[charIndex * 8 + 0] = c.texCoordLeft;
                meshData.textureCoords[charIndex * 8 + 1] = c.texCoordTop;
                meshData.textureCoords[charIndex * 8 + 2] = c.texCoordRight;
                meshData.textureCoords[charIndex * 8 + 3] = c.texCoordTop;
                meshData.textureCoords[charIndex * 8 + 4] = c.texCoordRight;
                meshData.textureCoords[charIndex * 8 + 5] = c.texCoordBottom;
                meshData.textureCoords[charIndex * 8 + 6] = c.texCoordLeft;
                meshData.textureCoords[charIndex * 8 + 7] = c.texCoordBottom;

                cursorX += c.xAdvance;
                charIndex++;
            }
            cursorY -= Font.FontMetaData.LINE_HEIGHT;
        }
    }

    private float MeasureStringWidth(String string, boolean scaled){
        float fontSizeX = GetFontSizeX();

        float size = 0.0f;        

        char[] chars = string.toCharArray();
        for(int i = 0; i < chars.length; i++){
            Font.FontMetaData.CharacterData c = font.metadata.data[(int)chars[i]];    

            if((int)chars[i] == FontMetaData.SPACE_ASCII) {
                size += font.metadata.spaceWidth;
                continue;
            }

            size += c.xAdvance;
        }
        return scaled ? size * fontSizeX : size;
    }

    /**
     * Packs the text's texData attribute for the given character.
     * @param charIndex
     *      - index of character within the text.
     * @return
     *      - clipOutsideRect (1 bit) | cull (1 bit) | cullRect_1 (6 bits) | cullRect_2 (6 bits) | cullRect_3 (6 bits) | layer (7 bits) | ...
     */
    public int PackInfo(int charIndex){
        UpdateVertexInfo();
        return packedInfo | (((text.charAt(charIndex) == '\0' || transform.hidden) ? 1 : 0) << 30) | ((transform.layer & 0xFF) << 1);
    }

    public class TextMeshData {
        protected float[] localVertices;
        public float[] vertices;
        public float[] textureCoords;

        public TextMeshData(float[] localVertices, float[] vertices, float[] textureCoords){
            this.localVertices = localVertices;
            this.vertices = vertices;
            this.textureCoords = textureCoords;
        }
    }

    private float GetFontSizeX(){
        return fontSize * (float)UIManager.instance.scaleFactor
            * (float)Math.min(Main.DEFAULT_WINDOW_WIDTH, Main.DEFAULT_WINDOW_HEIGHT) / (float)Math.min(Main.windowX, Main.windowY)
            * (Main.windowX > Main.windowY ? (float)Main.windowY / (float)Main.windowX : 1.0f);
    }
    private float GetFontSizeY(){
        return fontSize * (float)UIManager.instance.scaleFactor
            * (float)Math.min(Main.DEFAULT_WINDOW_WIDTH, Main.DEFAULT_WINDOW_HEIGHT) / (float)Math.min(Main.windowX, Main.windowY) 
            * (Main.windowY > Main.windowX ? (float)Main.windowX / (float)Main.windowY : 1.0f);
    }
}
