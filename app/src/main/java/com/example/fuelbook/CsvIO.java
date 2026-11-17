package com.example.fuelbook;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CsvIO {
    private CsvIO() {}
    public static final String HEADER = "type,vehicle,estimated_km_per_l,epoch_millis,odometer_km,liters,amount";
    private static String field(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
    private static String number(double x) { return String.format(Locale.US, "%.6f", x); }
    public static void export(FuelDb db, Writer destination) throws IOException {
        BufferedWriter w = new BufferedWriter(destination);
        w.write(HEADER); w.newLine();
        for (FuelDb.Vehicle v : db.vehicles()) {
            w.write("vehicle," + field(v.name) + "," + number(v.estimate) + ",,,,"); w.newLine();
        }
        for (FuelDb.Fill f : db.allFills()) {
            w.write("fill," + field(f.vehicleName) + "," + number(f.estimate) + "," + f.time + "," + number(f.odometer) + "," + number(f.liters) + "," + number(f.amount));
            w.newLine();
        }
        w.flush();
    }
    public static final class Result {
        public int vehicles, fills, duplicates;
    }
    private static double parseNumber(String s, boolean allowZero) {
        double d = Double.parseDouble(s.trim());
        if (!Double.isFinite(d) || d < 0 || (!allowZero && d == 0)) throw new IllegalArgumentException("Invalid number: " + s);
        return d;
    }
    public static Result importCsv(FuelDb helper, Reader source) throws IOException {
        List<List<String>> rows = parse(new BufferedReader(source));
        if (rows.isEmpty() || !HEADER.equals(String.join(",", rows.get(0)))) throw new IllegalArgumentException("Invalid CSV header. Import a Fuelbook CSV export.");
        SQLiteDatabase db = helper.getWritableDatabase();
        Result result = new Result();
        db.beginTransaction();
        try {
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row.size() == 1 && row.get(0).trim().isEmpty()) continue;
                if (row.size() != 7) throw new IllegalArgumentException("Row " + (i + 1) + " must have 7 columns");
                String type = row.get(0).trim(), name = row.get(1).trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Missing vehicle in row " + (i + 1));
                double estimate = row.get(2).trim().isEmpty() ? 0 : parseNumber(row.get(2), true);
                FuelDb.Vehicle v = helper.vehicleByName(db, name);
                if (v == null) {
                    ContentValues cv = new ContentValues(); cv.put("name", name); cv.put("estimate", estimate);
                    long id = db.insertOrThrow("vehicles", null, cv);
                    v = new FuelDb.Vehicle(id, name, estimate); result.vehicles++;
                } else if (type.equals("vehicle")) {
                    // Explicit profile rows take precedence over existing mileage estimates.
                    ContentValues cv = new ContentValues(); cv.put("estimate", estimate);
                    db.update("vehicles", cv, "id=?", new String[]{String.valueOf(v.id)});
                }
                if (type.equals("vehicle")) continue;
                if (!type.equals("fill")) throw new IllegalArgumentException("Unknown type in row " + (i + 1));
                long time = Long.parseLong(row.get(3).trim());
                if (time <= 0) throw new IllegalArgumentException("Invalid date in row " + (i + 1));
                double odo = parseNumber(row.get(4), true), liters = parseNumber(row.get(5), false), amount = parseNumber(row.get(6), true);
                ContentValues cv = new ContentValues(); cv.put("vehicle_id", v.id); cv.put("time", time);
                cv.put("odometer", odo); cv.put("liters", liters); cv.put("amount", amount); cv.put("estimate", estimate);
                long inserted = db.insertWithOnConflict("fills", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                if (inserted == -1) result.duplicates++; else result.fills++;
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        return result;
    }
    // CSV reader supports quoted delimiters, escaped quotes, CRLF, and quoted newlines.
    private static List<List<String>> parse(Reader r) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false, atStart = true, justClosed = false;
        int ch;
        while ((ch = r.read()) != -1) {
            char c = (char) ch;
            if (atStart && c == '\ufeff') continue;
            atStart = false;
            if (quoted) {
                if (c == '"') {
                    r.mark(1);
                    int next = r.read();
                    if (next == '"') cell.append('"');
                    else { quoted = false; justClosed = true; if (next != -1) r.reset(); }
                } else cell.append(c);
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(cell.toString()); cell.setLength(0); justClosed = false;
                if (c != ',') {
                    rows.add(row); row = new ArrayList<>();
                    if (c == '\r') { r.mark(1); int next = r.read(); if (next != '\n' && next != -1) r.reset(); }
                }
            } else if (c == '"' && cell.length() == 0 && !justClosed) quoted = true;
            else if (justClosed) throw new IllegalArgumentException("Unexpected character after closing quote");
            else cell.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        if (cell.length() > 0 || !row.isEmpty() || justClosed) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }
}
