package james.UI;
import james.Main;

/**
 * Constraint that returns proportion of its provided dimension such that it has a proportionally similar size on the physical screen.
 */
public class AspectConstraint extends Constraint {
    private float proportion;
    /**
     * Constructor for RelativeConstraint.
     * @param proportion
     *      - any floating point value. A value of 1.0 represents a constraint that returns the same size of its relative dimension.
     */
    public AspectConstraint(float proportion){
        this.proportion = proportion;
    }
    public void update(Transform transform){
        if (this == transform.heightConstraint){
            value = proportion * transform.widthConstraint.value * (float)Main.windowX / (float)Main.windowY;
        }
        else{
            value = proportion * transform.heightConstraint.value * (float)Main.windowY / (float)Main.windowX;
        }
    }
}