package com.druvu.jconsole.testjvm;

/**
 * Demonstrates the Phase 2.A1 markup vocabulary in JMX parameter descriptions.
 * Exposed via {@link SampleMarkupStandardMBean} so descriptions actually reach
 * the wire (a plain interface-based Standard MBean would lose them).
 */
public interface SampleMarkupMBean {

    String setCurrencyPair(String pair);

    String scheduleAt(String date);

    String runQuery(String sql);

    String uploadCsv(byte[] payload);

    String getConfig();

    String setVerbose(boolean verbose);

    byte[] generatePdfReport();

    byte[] generateCsvExport();

    byte[] generateUnknownBlob();

    byte[] generateNoHint();

    javax.management.openmbean.TabularData loadOrderBook();

    String placeOrder(String symbol, String side, long quantity, double price, String account);

    String scheduleReport(String name, String fromDate, String toDate, String format);

    String searchTrades(String symbol, String side, long minQty, long maxQty);

    String transferFunds(String fromAccount, String toAccount, String currency, double amount, String memo);
}
