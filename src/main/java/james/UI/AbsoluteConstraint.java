package james.UI;

/**
 * Basic constraint which returns its given value regardless of GUI context state. 
 * Should be used by root canvas with width and height as 2.0 in a standard implementation.
 */
public class AbsoluteConstraint extends Constraint {
    /**
     * Constructor for AbsoluteConstraint.
     * @param value
     *      - screen size value. Often in range [0, 2].
     */
    public AbsoluteConstraint(float value){
        this.value = value;
    }
    public void update(Transform transform){
    }
}