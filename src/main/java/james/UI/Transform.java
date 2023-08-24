package james.UI;

import james.Main;
import james.UI.Library.*;

import org.joml.*;

import java.util.ArrayList;

/**
 * GUI transform rect object.
 */
public class Transform {
    public Anchor anchor;
    public Anchor anchorPoint;
    public int layer;
    public Vector2f position;
    public Transform parent;
    public Constraint widthConstraint;
    public Constraint heightConstraint;
    public ArrayList<Transform> children;
    public Vector4f edgeOffset;
    public Vector2f positionOffset;
    public ScalingFactor positionOffsetFactor;
    public boolean hidden;
    
    public float left, right, top, bottom;
    
    /**
     * Index of the transform object as in the cullRects UBO array. Must be initialized externally with RegisterAsCullRect().
     */
    public int cullIndex; 
    
    private float prevLeft, prevRight, prevTop, prevBottom;
    private boolean prevHidden;
    public boolean transformChanged;

    private int packedSize;

    /**
     * Constructor for a GUI transform.
     * @param parent
     *      - the transform's parent transform. Must be null, unless it is the root (canvas).
     * @param anchor
     *      - point on parent rect to anchor to. One of Anchor.{ Center, Top, TopRight, Right, BottomRight, BottomLeft, Left, TopLeft }.
     * @param anchorPoint
     *      - self's point to anchor about. One of Anchor.{ Center, Top, TopRight, Right, BottomRight, BottomLeft, Left, TopLeft }.
     * @param layer
     *      - the ordering layer for the transform. One of [0, 127].
     * @param widthConstraint
     *      - one of { AbsouteConstraint, RelativeConstraint, ScaledRelativeConstraint }.
     * @param heightConstraint
     *      - one of { AbsoluteConstraint, RelativeConstraint, ScaledRelativeConstraint }.
     */
    public Transform(Transform parent, Anchor anchor, Anchor anchorPoint, int layer, Constraint widthConstraint, Constraint heightConstraint){
        if(parent != null ) parent.children.add(this);
        this.parent = parent;
        this.anchor = anchor;
        this.layer = layer;
        this.anchorPoint = anchorPoint;
        this.widthConstraint = widthConstraint;
        this.heightConstraint = heightConstraint;
        this.positionOffset = new Vector2f(0.0f);
        this.positionOffsetFactor = ScalingFactor.WidthHeight;
        this.edgeOffset = new Vector4f(0.0f);
        this.children = new ArrayList<>();
        this.position = new Vector2f(0.0f);
        this.cullIndex = 0;
    }

    /**
     * Constructor for a GUI transform.
     * @param parent
     *      - the transform's parent transform. Must be null, unless it is the root (canvas).
     * @param anchor
     *      - point on parent rect to anchor to. One of Anchor.{ Center, Top, TopRight, Right, BottomRight, BottomLeft, Left, TopLeft }.
     * @param anchorPoint
     *      - self's point to anchor about. One of Anchor.{ Center, Top, TopRight, Right, BottomRight, BottomLeft, Left, TopLeft }.
     * @param layer
     *      - the ordering layer for the transform. One of [0, 127].
     * @param widthConstraint
     *      - one of { AbsouteConstraint, RelativeConstraint, ScaledRelativeConstraint }.
     * @param heightConstraint
     *      - one of { AbsoluteConstraint, RelativeConstraint, ScaledRelativeConstraint }.
     * @param positionOffset
     *      - the rect's position offset.
     * @param positionOffsetFactor
     *      - the scaling factor of the offset. One of ScalingFactor.{ Width, Height, WidthHeight, ParentWidth, ParentHeight, ParentWidthHeight }. 
     *        WidthHeight represents scaling the positionOffset as a proportion of its respective dimensions.
     * @param edgeOffset
     *      - the offset (top, right, bottom, left) as a proportion of the rect's respective dimensions.
     */
    public Transform(Transform parent, Anchor anchor, Anchor anchorPoint, int layer, Constraint widthConstraint, Constraint heightConstraint, Vector2f positionOffset, ScalingFactor positionOffsetFactor, Vector4f edgeOffset){
        if(parent != null ) parent.children.add(this);
        this.parent = parent;
        this.anchor = anchor;
        this.anchorPoint = anchorPoint;
        this.layer = layer;
        this.widthConstraint = widthConstraint;
        this.heightConstraint = heightConstraint;
        this.positionOffset = positionOffset;
        this.positionOffsetFactor = positionOffsetFactor;
        this.edgeOffset = edgeOffset;
        this.children = new ArrayList<>();
        this.position = new Vector2f(0.0f);
        this.cullIndex = 0;
    }

    public void UpdateTransformRecursive(){

        this.widthConstraint.update(this);
        this.heightConstraint.update(this);
        this.widthConstraint.update(this);

        if(parent == null) position = new Vector2f(0.0f);
        else {
            position = new Vector2f(parent.position.x, parent.position.y);
            switch(anchor) {
                case Center : 
                    break;
                case Left :
                    position.x -= parent.widthConstraint.value / 2.0f;
                    break;
                case TopLeft :
                    position.x -= parent.widthConstraint.value / 2.0f;
                    position.y += parent.heightConstraint.value / 2.0f;
                    break;
                case Top :
                    position.y += parent.heightConstraint.value / 2.0f;
                    break;
                case TopRight :
                    position.x += parent.widthConstraint.value / 2.0f;
                    position.y += parent.heightConstraint.value / 2.0f;
                    break;
                case Right :
                    position.x += parent.widthConstraint.value / 2.0f;
                    break;
                case BottomRight :
                    position.x += parent.widthConstraint.value / 2.0f;
                    position.y -= parent.heightConstraint.value / 2.0f;
                    break;
                case Bottom :
                    position.y -= parent.heightConstraint.value / 2.0f;
                    break;
                case BottomLeft :
                    position.x -= parent.widthConstraint.value / 2.0f;
                    position.y -= parent.heightConstraint.value / 2.0f;
                    break;
            }

            switch(anchorPoint) {
                case Center : 
                    break;
                case Left :
                    position.x += widthConstraint.value / 2.0f;
                    break;
                case TopLeft :
                    position.x += widthConstraint.value / 2.0f;
                    position.y -= heightConstraint.value / 2.0f;
                    break;
                case Top :
                    position.y -= heightConstraint.value / 2.0f;
                    break;
                case TopRight :
                    position.x -= widthConstraint.value / 2.0f;
                    position.y -= heightConstraint.value / 2.0f;
                    break;
                case Right :
                    position.x -= widthConstraint.value / 2.0f;
                    break;
                case BottomRight :
                    position.x -= widthConstraint.value / 2.0f;
                    position.y += heightConstraint.value / 2.0f;
                    break;
                case Bottom :
                    position.y += heightConstraint.value / 2.0f;
                    break;
                case BottomLeft :
                    position.x += widthConstraint.value / 2.0f;
                    position.y += heightConstraint.value / 2.0f;
                    break;
            }
        }

        if(positionOffsetFactor == ScalingFactor.Width) {
            position.x += widthConstraint.value * positionOffset.x;
            position.y += widthConstraint.value * positionOffset.y;
        }
        else if(positionOffsetFactor == ScalingFactor.Height) {
            position.x += heightConstraint.value * positionOffset.x;
            position.y += heightConstraint.value * positionOffset.y;
        }
        else if(positionOffsetFactor == ScalingFactor.WidthHeight) {
            position.x += widthConstraint.value * positionOffset.x;
            position.y += heightConstraint.value * positionOffset.y;
        }
        else if(positionOffsetFactor == ScalingFactor.ParentWidth) {
            position.x += parent.widthConstraint.value * positionOffset.x;
            position.y += parent.widthConstraint.value * positionOffset.y;
        }
        else if(positionOffsetFactor == ScalingFactor.ParentHeight) {
            position.x += parent.heightConstraint.value * positionOffset.x;
            position.y += parent.heightConstraint.value * positionOffset.y;
        }
        else if(positionOffsetFactor == ScalingFactor.ParentWidthHeight) {
            position.x += parent.widthConstraint.value * positionOffset.x;
            position.y += parent.heightConstraint.value * positionOffset.y;
        }

        left = position.x - 0.5f * widthConstraint.value * (1.0f - edgeOffset.w);
        right = position.x + 0.5f * widthConstraint.value * (1.0f - edgeOffset.y);
        top = position.y + 0.5f * heightConstraint.value * (1.0f - edgeOffset.x);
        bottom = position.y - 0.5f * heightConstraint.value * (1.0f - edgeOffset.z);

        transformChanged = (prevLeft != left || prevRight != prevRight || prevTop != prevTop || prevBottom != bottom || prevHidden != hidden);

        prevLeft = left;
        prevRight = right;
        prevTop = top;
        prevBottom = bottom;
        prevHidden = hidden;

        packedSize = ((((int)((right - left) * 0.5f * (float)Main.windowX)) & 0xFFFF) << 16)
            | (((int)((top - bottom) * 0.5f * (float)Main.windowY)) & 0xFFFF);
        
        for(Transform child : children){
            child.UpdateTransformRecursive();
        }
    }

    /**
     * Sets "hidden" to the given value for the transform and all of its children.
     */
    public void SetAllHidden(boolean value){
        hidden = value;
        for(Transform child : children){
            child.SetAllHidden(value);
        }
    }

    /**
     * Get the packed size attribute of the rect, in pixel coordinates.
     * @return
     *      - rect pixel width (16 bits) | rect pixel height (16 bits)
     */
    public int PackSize(){
        return packedSize;
    }
}