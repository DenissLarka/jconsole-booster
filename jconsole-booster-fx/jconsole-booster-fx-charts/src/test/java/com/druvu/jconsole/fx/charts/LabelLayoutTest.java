package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Checks that the current-value labels keep apart and inside the plot. */
public class LabelLayoutTest {

    private static final double GAP = 20;
    private static final double TOP = 0;
    private static final double BOTTOM = 100;

    @Test
    public void labelsAlreadyApartStayPut() {
        assertThat(LabelLayout.spread(new double[] {10, 50}, GAP, TOP, BOTTOM)).containsExactly(10, 50);
    }

    @Test
    public void labelsAtTheSameHeightSpreadDownwards() {
        assertThat(LabelLayout.spread(new double[] {40, 40}, GAP, TOP, BOTTOM)).containsExactly(40, 60);
    }

    @Test
    public void labelsNearTheBottomArePushedUp() {
        assertThat(LabelLayout.spread(new double[] {95, 100}, GAP, TOP, BOTTOM)).containsExactly(80, 100);
    }

    @Test
    public void aLabelAboveTheTopComesDown() {
        assertThat(LabelLayout.spread(new double[] {-10}, GAP, TOP, BOTTOM)).containsExactly(0);
    }

    @Test
    public void placesComeBackInTheOrderAsked() {
        assertThat(LabelLayout.spread(new double[] {50, 45}, GAP, TOP, BOTTOM)).containsExactly(65, 45);
    }
}
