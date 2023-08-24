package james.UI;

/**
 * Contains static method wrappers and enum defines for library import.
 */
public class Library {

    //UI atlas image defines
    public static final int ATLASIMAGE_HOMEBUTTON = 0;
    public static final int ATLASIMAGE_SETTINGSBUTTON = 1;
    public static final int ATLASIMAGE_EXITBUTTON = 2;

    public static enum Anchor { Center, Top, TopRight, Right, BottomRight, Bottom, BottomLeft, Left, TopLeft};
    public static enum ConstraintDimension { ParentWidth, ParentHeight, Width, Height };
    public static enum ScalingFactor { Width, Height, WidthHeight, ParentWidth, ParentHeight, ParentWidthHeight };
    public static enum Alignment { Left, Right, Center, Top, Bottom };
    public static enum WrapMode { Overflow, Wrap };
    public static enum OverflowMode { Overflow, Clip };
    public static enum MeshUpdateMode { Static, Dynamic };
    public static enum ToggleMode { CanToggle, NoToggle, CanToggleOff };
    public static enum ClipDirection { ClipInside, ClipOutside };

    public static void AddPanel(Panel panel){
        UIManager.AddPanel(panel);
    }

    public static void AddImage(Image image){
        UIManager.AddImage(image);
    }

    public static void AddText(Text text){
        UIManager.AddText(text);
    }

    public static void AddFont(Font font){
        UIManager.AddFont(font);
    }

    public static void RefreshUIBuffer(){
        UIManager.RefreshUIBuffer();
    }

    public static void RegisterTransformAsCullRect(Transform transform){
        UIManager.RegisterTransformAsCullRect(transform);
    }
}