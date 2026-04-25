package com.druvu.jconsole.testjvm;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * Wraps {@link SampleMarkup} so that JMX introspection picks up markup-bearing
 * descriptions for parameters and operations. Without this wrapper a plain
 * Standard MBean would surface descriptions like {@code "p1"}, {@code "p2"},
 * which strips out our {@code {{combo:…}}}-style hints.
 */
public class SampleMarkupStandardMBean extends StandardMBean {

    private static final Map<String, String[]> PARAM_DESCRIPTIONS = new HashMap<>();
    private static final Map<String, String> OPERATION_DESCRIPTIONS = new HashMap<>();

    static {
        PARAM_DESCRIPTIONS.put(
                "setCurrencyPair", new String[] {"Currency pair {{combo:EURUSD,USDCHF,GBPUSD,USDJPY}}"});
        PARAM_DESCRIPTIONS.put("scheduleAt", new String[] {"Run-at date {{date:dd.MM.yyyy}}"});
        PARAM_DESCRIPTIONS.put("runQuery", new String[] {"Multi-line query {{text:rows=10}}"});
        PARAM_DESCRIPTIONS.put("uploadCsv", new String[] {"CSV file {{file:*.csv}}"});
        PARAM_DESCRIPTIONS.put("setVerbose", new String[] {"Verbose mode (boolean fallback — no markup)"});
        PARAM_DESCRIPTIONS.put("getConfig", new String[] {});
        PARAM_DESCRIPTIONS.put(
                "placeOrder",
                new String[] {
                    "FX pair {{combo:EURUSD,USDCHF,GBPUSD,USDJPY,AUDUSD}}",
                    "Direction {{combo:BUY,SELL}}",
                    "Order size (units)",
                    "Limit price",
                    "Trading account"
                });
        PARAM_DESCRIPTIONS.put(
                "scheduleReport",
                new String[] {
                    "Report name",
                    "From {{date:yyyy-MM-dd}}",
                    "To {{date:yyyy-MM-dd}}",
                    "Output format {{combo:CSV,JSON,XLSX,PDF}}"
                });
        PARAM_DESCRIPTIONS.put(
                "searchTrades",
                new String[] {
                    "FX pair {{combo:EURUSD,USDCHF,GBPUSD,USDJPY,AUDUSD,NZDUSD}}",
                    "Side {{combo:BUY,SELL,BOTH}}",
                    "Min quantity",
                    "Max quantity"
                });
        PARAM_DESCRIPTIONS.put(
                "transferFunds",
                new String[] {
                    "Source account",
                    "Destination account",
                    "Currency {{combo:USD,EUR,GBP,CHF,JPY}}",
                    "Amount",
                    "Memo / reference"
                });

        OPERATION_DESCRIPTIONS.put("getConfig", "Server config {{returns:format=json}}");
        OPERATION_DESCRIPTIONS.put("generatePdfReport", "Monthly report {{returns:mime=application/pdf}}");
        OPERATION_DESCRIPTIONS.put("generateCsvExport", "Trades export {{returns:mime=text/csv}}");
        OPERATION_DESCRIPTIONS.put(
                "generateUnknownBlob", "Diagnostic dump {{returns:mime=application/octet-stream}}");
        // generateNoHint intentionally has no description — exercises the
        // "no hint → fall through to existing array viewer" path.
    }

    public SampleMarkupStandardMBean(SampleMarkupMBean impl) throws NotCompliantMBeanException {
        super(impl, SampleMarkupMBean.class);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op) {
        String custom = OPERATION_DESCRIPTIONS.get(op.getName());
        if (custom != null) {
            return custom;
        }
        return super.getDescription(op);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        String[] descriptions = PARAM_DESCRIPTIONS.get(op.getName());
        if (descriptions != null && sequence < descriptions.length) {
            return descriptions[sequence];
        }
        return super.getDescription(op, param, sequence);
    }
}
