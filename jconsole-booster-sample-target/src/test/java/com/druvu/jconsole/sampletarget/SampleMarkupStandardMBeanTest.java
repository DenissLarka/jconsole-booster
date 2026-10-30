package com.druvu.jconsole.sampletarget;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Checks that the markup in {@link SampleMarkupMBean}'s descriptions reaches the {@code MBeanInfo}. */
public class SampleMarkupStandardMBeanTest {

    @Test
    public void parameterDescriptionsCarryMarkup() throws NotCompliantMBeanException {
        SampleMarkupStandardMBean mbean = new SampleMarkupStandardMBean(new SampleMarkup());
        MBeanInfo info = mbean.getMBeanInfo();

        for (MBeanOperationInfo op : info.getOperations()) {
            switch (op.getName()) {
                case "setCurrencyPair" -> assertParamDescription(op, 0, "{{combo:EURUSD");
                case "scheduleAt" -> assertParamDescription(op, 0, "{{date:dd.MM.yyyy}}");
                case "runQuery" -> assertParamDescription(op, 0, "{{text:rows=10}}");
                case "uploadCsv" -> assertParamDescription(op, 0, "{{file:*.csv}}");
                case "getConfig" ->
                    Assert.assertTrue(
                            op.getDescription().contains("{{returns:format=json}}"),
                            "operation description: " + op.getDescription());
                default -> {
                    // the other operations carry no markup to check
                }
            }
        }
    }

    private static void assertParamDescription(MBeanOperationInfo op, int idx, String expectedSubstring) {
        MBeanParameterInfo[] params = op.getSignature();
        Assert.assertTrue(
                params.length > idx, op.getName() + " has only " + params.length + " parameters, wanted index " + idx);
        Assert.assertTrue(
                params[idx].getDescription().contains(expectedSubstring),
                op.getName()
                        + " param "
                        + idx
                        + " description was '"
                        + params[idx].getDescription()
                        + "', expected to contain '"
                        + expectedSubstring
                        + "'");
    }
}
