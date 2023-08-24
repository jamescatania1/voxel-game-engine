package james.UI;
import james.UI.Library.*;

/**
 * Constraint that returns proportion of its provided dimension. For correct aspect adjustments, the constraint should have exactly one parent
 * that contains the ScaledRelativeConstraint constraint for each dimension. 
 */
public class RelativeConstraint extends Constraint {
    private ConstraintDimension dimension;
    private float proportion;
    
    /**
     * Constructor for RelativeConstraint.
     * @param dimension
     *      - one of ConstraintDimension.{ Width, ParentWidth, Height, ParentHeight }.
     * @param proportion
     *      - any floating point value. A value of 1.0 represents a constraint that returns the same value as its relative dimension.
     */
    public RelativeConstraint(ConstraintDimension dimension, float proportion){
        this.dimension = dimension;
        this.proportion = proportion;
    }
    public void update(Transform transform){
        if (this.dimension == ConstraintDimension.Width){
            value = proportion * transform.widthConstraint.value;
        }
        else if(this.dimension == ConstraintDimension.Height){
            value = proportion * transform.heightConstraint.value;
        }
        else if(this.dimension == ConstraintDimension.ParentWidth){
            value = proportion * transform.parent.widthConstraint.value;
        }
        else if(this.dimension == ConstraintDimension.ParentHeight){
            value = proportion * transform.parent.heightConstraint.value;
        }
    }
}