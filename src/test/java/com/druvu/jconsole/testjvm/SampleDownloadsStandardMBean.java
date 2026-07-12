package com.druvu.jconsole.testjvm;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * Wraps {@link SampleDownloads} so the {@code {{returns:mime=…}}} operation descriptions actually reach JMX
 * introspection — a plain interface-based Standard MBean would surface {@code "generatePdfReport"} instead of the
 * markup, and JCB would never route the byte[] result through {@code MimeHandler}.
 */
public class SampleDownloadsStandardMBean extends StandardMBean {

    private static final Map<String, String> OPERATION_DESCRIPTIONS = new HashMap<>();

    static {
        OPERATION_DESCRIPTIONS.put("generatePdfReport", "Monthly report {{returns:mime=application/pdf}}");
        OPERATION_DESCRIPTIONS.put(
                "generateCsvExport", "Trades export {{returns:mime=text/csv,filename=monthly-trades.csv}}");
        OPERATION_DESCRIPTIONS.put("generateUnknownBlob", "Diagnostic dump {{returns:mime=application/octet-stream}}");
        // generateNoHint intentionally has no description — exercises the
        // "no hint → fall through to existing array viewer" path.
    }

    public SampleDownloadsStandardMBean(SampleDownloadsMBean impl) throws NotCompliantMBeanException {
        super(impl, SampleDownloadsMBean.class);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op) {
        String custom = OPERATION_DESCRIPTIONS.get(op.getName());
        if (custom != null) {
            return custom;
        }
        return super.getDescription(op);
    }
}
