package james.UI;

import james.Color;

import static james.UI.Library.*;

import org.joml.Vector3i;

/**
 * A GUI panel that fills its transform rect. Should be registered with the static AddPanel() method after creation.
 */
public class Panel {
    public Color color;
    public Transform transform;

    private Vector3i packedVertex;
    private OverflowMode clipMode;
    private int[] clipRectIndicies;
    private ClipDirection[] clipDirections;
    private int[] cornerPixelRadii;
    private int borderInnerRadius;
    private int borderOuterRadius;
    private Color borderColor;

    /**
     * Panel constructor.
     * @param color
     *      - the panel's color.
     * @param transform
     *      - the panel's transform object.
     */
    public Panel(Color color, Transform transform){
        this.transform = transform;
        this.color = color;

        clipMode = OverflowMode.Overflow;
        clipRectIndicies = new int[3];
        clipDirections = new ClipDirection[3];
        packedVertex = new Vector3i();
        cornerPixelRadii = new int[4];
        borderInnerRadius = 0;
        borderOuterRadius = 0;
        borderColor = Color.WHITE.Clone();
        UpdateVertexInfo();
    }

    /**
     * Adds the cullTransform to one of the panel's cull rect slots, if available. Updates vertex info at end, need not call
     * UpdateVertexInfo() after doing this.
     * @param cullTransform
     *      - the transform whose rect is to be registered.
     * @param clipDirection
     *      - whether to clip the parts of the panel that are inside the cullTransform (ClipInside), or to
     *        clip the parts that are outside the cullTransform (Clipoutside).
     */
    public void AddCullRect(Transform cullTransform, ClipDirection clipDirection){
        if(clipRectIndicies[0] == 0) AddCullRect(cullTransform, clipDirection, 0);
        else if(clipRectIndicies[1] == 0) AddCullRect(cullTransform, clipDirection, 1);
        else if(clipRectIndicies[2] == 0) AddCullRect(cullTransform, clipDirection, 2);
    }
    /**
     * Adds the cullTransform as a clip rect for the panel, i.e. a mask. Updates vertex info at end, need not call
     * UpdateVertexInfo() after doing this.
     * @param cullTransform
     *      - the transform whose rect is to be registered.
     * @param clipDirection
     *      - whether to clip the parts of the panel that are inside the cullTransform (ClipInside), or to
     *        clip the parts that are outside the cullTransform (Clipoutside).
     * @param index
     *      - the panel's index to store the cull rect index. Within range [0, 2].
     */
    public void AddCullRect(Transform cullTransform, ClipDirection clipDirection, int index){
        if(index < 0 || index >= 3) throw new Error("cull rect index is out of bounds. Must be in range [0, 2].");
        if(cullTransform.cullIndex == 0) RegisterTransformAsCullRect(cullTransform);
        clipRectIndicies[index] = cullTransform.cullIndex;
        clipDirections[index] = clipDirection;
        clipMode = OverflowMode.Clip;
        UpdateVertexInfo();
    }

    /**
     * Update static packed vertex attributes (clip information) make public if more mutable attributes that require external update are added.
     */
    private void UpdateVertexInfo(){
        packedVertex.x = 
            (255 & 0xFF) << 22 |  //value of 255 means no texture here
            ((clipMode == OverflowMode.Clip ? 1 : 0) & 0x1) << 21 |
            (clipRectIndicies[0] & 0x3F) << 15 |
            (clipRectIndicies[1] & 0x3F) << 9 |
            (clipRectIndicies[2] & 0x3F) << 3 |
            ((clipDirections[0] == ClipDirection.ClipInside ? 1 : 0) & 0x1) << 2 |
            ((clipDirections[1] == ClipDirection.ClipInside ? 1 : 0) & 0x1) << 1 |
            ((clipDirections[2] == ClipDirection.ClipInside ? 1 : 0) & 0x1);
        packedVertex.y =
            (cornerPixelRadii[0] & 0x3F) << 26 |
            (cornerPixelRadii[1] & 0x3F) << 20 |
            (cornerPixelRadii[2] & 0x3F) << 14 |
            (cornerPixelRadii[3] & 0x3F) << 8 |
            (transform.layer & 0x7F) << 1;
        packedVertex.z = 
            (borderInnerRadius & 0x3F) << 26 |
            (borderOuterRadius & 0x3F) << 20 |
            borderColor.rgba_4_4_4_8;
    }

    /**
     * Sets the radii, in pixels, of the rounded corners of the panel.
     * @param sw
     *      - the southwest radius.
     * @param se
     *      - the southeast radius.
     * @param ne
     *      - the northeast radius.
     * @param nw
     *      - the northwest radius.
     */
    public void SetCornerRadii(int sw, int se, int ne, int nw){
        cornerPixelRadii[3] = sw;
        cornerPixelRadii[0] = se;
        cornerPixelRadii[1] = ne;
        cornerPixelRadii[2] = nw;
        UpdateVertexInfo();
    }

    /**
     * Sets the panel's border.
     * @param innerRadius
     *      - in range [0, 63].
     * @param outerRadius
     *      - in range [0, 63].
     * @param borderColor
     *      - will be compressed in RGBA_4448 format.
     */
    public void SetBorder(int innerRadius, int outerRadius, Color borderColor){
        borderInnerRadius = innerRadius;
        borderOuterRadius = outerRadius;
        this.borderColor = borderColor;
        UpdateVertexInfo();
    }

    /**
     * Returns the packed vertex data.
     * @param texCornerIndex
     *      - the index of the corner of the image whose data is being fetched.
     * @param attribute
     *      - the index of the vertex data that should be packed, element of {0, 1}.
     */
    public int PackVertex(int texCornerIndex, int attribute){
        if(attribute == 0) return ((texCornerIndex & 0x3) << 30) | packedVertex.x;
        else if(attribute == 1) return packedVertex.y | (transform.hidden ? 1 : 0);
        else return packedVertex.z;
    }
}