package smartfloor.domain.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import smartfloor.domain.AnalysisCategory;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnalysisAnnotation {
    /**
     * TODO.
     */
    String name() default "";

    /**
     * TODO.
     */
    AnalysisCategory category() default AnalysisCategory.EXTRA_ANALYSIS;
}
