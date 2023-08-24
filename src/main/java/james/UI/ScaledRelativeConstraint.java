package james.UI;
import james.Main;

/**
 * Constraint that appropriately adjusts for screen aspect. This should only be used for transforms that have no other
 * parents of tied proportionality that also have this constraint, such that the value is not over-adjusted for.
 */
public class ScaledRelativeConstraint extends Constraint {
    private float proportion;
    private boolean isHeight;
    
    /**
     * Constructor for ScaledRelativeConstraint.
     * @param proportion
     *      - any floating point value. In a standard implementation, in range [0, 1].
     * @param isHeight
     *      - set to true if is used as height constraint, false if used as width constraint.
     */
    public ScaledRelativeConstraint(float proportion, boolean isHeight){
        this.proportion = proportion;
        this.isHeight = isHeight;
    }
    public void update(Transform transform){
        value = (float)UIManager.instance.scaleFactor * (float)Math.min(Main.DEFAULT_WINDOW_WIDTH, Main.DEFAULT_WINDOW_HEIGHT) / (float)Math.min(Main.windowX, Main.windowY) * proportion * transform.parent.widthConstraint.value;
        if(isHeight) {
            value *= Main.windowY > Main.windowX ? (float)Main.windowX / (float)Main.windowY : 1.0f;
        }
        else {
            value /= Main.windowX > Main.windowY ? (float)Main.windowX / (float)Main.windowY : 1.0f;
        }
    }
}