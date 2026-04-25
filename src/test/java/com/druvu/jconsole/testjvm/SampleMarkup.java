package com.druvu.jconsole.testjvm;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class SampleMarkup implements SampleMarkupMBean {

    private static final TabularType ORDER_BOOK_TYPE = buildOrderBookType();
    private static final TabularData ORDER_BOOK = buildOrderBook();

    private static TabularType buildOrderBookType() {
        try {
            CompositeType row = new CompositeType(
                    "OrderEntry",
                    "Resting order",
                    new String[] {
                        "orderId",
                        "symbol",
                        "side",
                        "price",
                        "quantity",
                        "notional",
                        "broker",
                        "account",
                        "status",
                        "timeInForce",
                        "placedAt"
                    },
                    new String[] {
                        "Order ID",
                        "FX pair",
                        "BUY/SELL",
                        "Limit price",
                        "Size",
                        "Price × Size",
                        "Counterparty",
                        "Account ID",
                        "Order state",
                        "Time in force",
                        "Placement time (ISO)"
                    },
                    new OpenType<?>[] {
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.DOUBLE,
                        SimpleType.LONG,
                        SimpleType.DOUBLE,
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.STRING
                    });
            return new TabularType("OrderBook", "Resting orders indexed by (symbol, side, orderId)", row, new String[] {
                "symbol", "side", "orderId"
            });
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TabularData buildOrderBook() {
        Map<String, Double> midPrice = Map.ofEntries(
                Map.entry("EURUSD", 1.0850),
                Map.entry("USDCHF", 0.9125),
                Map.entry("GBPUSD", 1.2650),
                Map.entry("USDJPY", 154.15),
                Map.entry("AUDUSD", 0.6585),
                Map.entry("NZDUSD", 0.5985),
                Map.entry("USDCAD", 1.3620),
                Map.entry("EURGBP", 0.8575),
                Map.entry("EURJPY", 167.25),
                Map.entry("GBPJPY", 195.05));
        String[] symbols = midPrice.keySet().toArray(new String[0]);
        java.util.Arrays.sort(symbols);
        String[] sides = {"BUY", "SELL"};
        String[] brokers = {"BARC", "CITI", "DBK", "GS", "HSBC", "JPMC", "MS", "UBS"};
        String[] statuses = {"RESTING", "PARTIAL", "FILLED"};
        String[] tifs = {"GTC", "IOC", "FOK", "DAY"};

        Random rnd = new Random(42);
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 4, 25, 9, 0, 0);
        int orderSeq = 1;

        TabularDataSupport tabular = new TabularDataSupport(ORDER_BOOK_TYPE);
        for (String symbol : symbols) {
            for (String side : sides) {
                double mid = midPrice.get(symbol);
                double pip = symbol.endsWith("JPY") ? 0.01 : 0.0001;
                for (int i = 0; i < 10; i++) {
                    String orderId = String.format("ORD-%05d", orderSeq++);
                    double offset = (rnd.nextInt(40) + 1) * pip;
                    double price = "BUY".equals(side) ? mid - offset : mid + offset;
                    price = Math.round(price * 1_000_000d) / 1_000_000d;
                    long quantity = (rnd.nextInt(50) + 1) * 100_000L;
                    double notional = Math.round(price * quantity * 100d) / 100d;
                    String broker = brokers[rnd.nextInt(brokers.length)];
                    String account = String.format("ACCT-%04d", 1000 + rnd.nextInt(9000));
                    String status = statuses[rnd.nextInt(statuses.length)];
                    String tif = tifs[rnd.nextInt(tifs.length)];
                    String placedAt = now.plusSeconds(rnd.nextInt(3600)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    Map<String, Object> values = new HashMap<>();
                    values.put("orderId", orderId);
                    values.put("symbol", symbol);
                    values.put("side", side);
                    values.put("price", price);
                    values.put("quantity", quantity);
                    values.put("notional", notional);
                    values.put("broker", broker);
                    values.put("account", account);
                    values.put("status", status);
                    values.put("timeInForce", tif);
                    values.put("placedAt", placedAt);
                    try {
                        tabular.put(new CompositeDataSupport(ORDER_BOOK_TYPE.getRowType(), values));
                    } catch (OpenDataException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return tabular;
    }

    @Override
    public String setCurrencyPair(String pair) {
        return "set: " + pair;
    }

    @Override
    public String scheduleAt(String date) {
        try {
            LocalDate parsed = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            return "scheduled: " + parsed;
        } catch (Exception e) {
            return "could not parse date '" + date + "': " + e.getMessage();
        }
    }

    @Override
    public String runQuery(String sql) {
        if (sql == null) {
            return "received 0 chars across 0 lines";
        }
        int lines = sql.isEmpty() ? 0 : sql.split("\n", -1).length;
        return "received " + sql.length() + " chars across " + lines + " lines";
    }

    @Override
    public String uploadCsv(byte[] payload) {
        if (payload == null) {
            return "received 0 bytes";
        }
        return "received " + payload.length + " bytes ("
                + new String(payload, StandardCharsets.UTF_8).split("\n", -1).length + " lines)";
    }

    @Override
    public String getConfig() {
        return """
                {
                  "service": "sample",
                  "version": "1.0",
                  "limits": { "rate": 100, "burst": 20 },
                  "features": ["alpha", "beta"]
                }
                """.strip();
    }

    @Override
    public String setVerbose(boolean verbose) {
        return "verbose=" + verbose;
    }

    @Override
    public byte[] generatePdfReport() {
        // Minimal valid PDF — a one-page empty document. Source: hand-rolled,
        // approx 470 bytes, parseable by every PDF viewer.
        return ("""
                %PDF-1.4
                1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
                2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj
                3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj
                4 0 obj<</Length 44>>stream
                BT /F1 18 Tf 72 720 Td (Sample report) Tj ET
                endstream
                endobj
                5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
                xref
                0 6
                0000000000 65535 f
                0000000009 00000 n
                0000000054 00000 n
                0000000099 00000 n
                0000000189 00000 n
                0000000283 00000 n
                trailer<</Size 6/Root 1 0 R>>
                startxref
                344
                %%EOF
                """).getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] generateCsvExport() {
        return ("""
                symbol,side,quantity,price
                EURUSD,BUY,1000000,1.0850
                USDCHF,SELL,500000,0.9130
                GBPUSD,BUY,250000,1.2640
                """).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] generateUnknownBlob() {
        byte[] bytes = new byte[64];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    @Override
    public byte[] generateNoHint() {
        return new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    }

    @Override
    public TabularData loadOrderBook() {
        return ORDER_BOOK;
    }

    @Override
    public String placeOrder(String symbol, String side, long quantity, double price, String account) {
        return "placed " + side + " " + quantity + " " + symbol + " @ " + price + " for " + account;
    }

    @Override
    public String scheduleReport(String name, String fromDate, String toDate, String format) {
        return "scheduled '" + name + "' from " + fromDate + " to " + toDate + " as " + format;
    }

    @Override
    public String searchTrades(String symbol, String side, long minQty, long maxQty) {
        return "searching " + symbol + " " + side + " qty in [" + minQty + ", " + maxQty + "]";
    }

    @Override
    public String transferFunds(String fromAccount, String toAccount, String currency, double amount, String memo) {
        return "transferred " + amount + " " + currency + " from " + fromAccount + " to " + toAccount + ": " + memo;
    }
}
