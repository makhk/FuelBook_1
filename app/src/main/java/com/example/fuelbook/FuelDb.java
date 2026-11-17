package com.example.fuelbook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class FuelDb extends SQLiteOpenHelper {
    public static final class Vehicle {
        public long id;
        public String name;
        public double estimate;
        Vehicle(long id, String name, double estimate) { this.id = id; this.name = name; this.estimate = estimate; }
        @Override public String toString() { return name; }
    }
    public static final class Fill {
        public long id, vehicleId, time;
        public double odometer, liters, amount, estimate;
        public String vehicleName;
        Fill(long id, long vehicleId, String vehicleName, long time, double odometer, double liters, double amount, double estimate) {
            this.id = id; this.vehicleId = vehicleId; this.vehicleName = vehicleName; this.time = time;
            this.odometer = odometer; this.liters = liters; this.amount = amount; this.estimate = estimate;
        }
    }

    public FuelDb(Context context) { super(context, "fuelbook.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, estimate REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE fills (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE, time INTEGER NOT NULL, odometer REAL NOT NULL, liters REAL NOT NULL, amount REAL NOT NULL, estimate REAL NOT NULL DEFAULT 0, UNIQUE(vehicle_id, odometer))");
        db.execSQL("CREATE INDEX fills_vehicle_odo ON fills(vehicle_id, odometer DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { /* Schema v1 */ }
    @Override public void onConfigure(SQLiteDatabase db) { super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true); }

    public List<Vehicle> vehicles() {
        List<Vehicle> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,estimate FROM vehicles ORDER BY name COLLATE NOCASE", null)) {
            while (c.moveToNext()) out.add(new Vehicle(c.getLong(0), c.getString(1), c.getDouble(2)));
        }
        return out;
    }
    public Vehicle vehicleByName(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT id,name,estimate FROM vehicles WHERE name=? COLLATE NOCASE", new String[]{name})) {
            if (c.moveToFirst()) return new Vehicle(c.getLong(0), c.getString(1), c.getDouble(2));
        }
        return null;
    }
    public long addVehicle(String name, double estimate) {
        ContentValues cv = new ContentValues(); cv.put("name", name); cv.put("estimate", estimate);
        return getWritableDatabase().insertOrThrow("vehicles", null, cv);
    }
    public void updateVehicle(long id, String name, double estimate) {
        ContentValues cv = new ContentValues(); cv.put("name", name); cv.put("estimate", estimate);
        getWritableDatabase().update("vehicles", cv, "id=?", new String[]{String.valueOf(id)});
    }
    public void deleteVehicle(long id) { getWritableDatabase().delete("vehicles", "id=?", new String[]{String.valueOf(id)}); }
    public Fill newest(long vehicleId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT f.id,f.vehicle_id,v.name,f.time,f.odometer,f.liters,f.amount,f.estimate FROM fills f JOIN vehicles v ON v.id=f.vehicle_id WHERE f.vehicle_id=? ORDER BY f.odometer DESC LIMIT 1", new String[]{String.valueOf(vehicleId)})) {
            return c.moveToFirst() ? readFill(c) : null;
        }
    }
    public List<Fill> fills(long vehicleId) {
        List<Fill> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT f.id,f.vehicle_id,v.name,f.time,f.odometer,f.liters,f.amount,f.estimate FROM fills f JOIN vehicles v ON v.id=f.vehicle_id WHERE f.vehicle_id=? ORDER BY f.odometer DESC, f.id DESC", new String[]{String.valueOf(vehicleId)})) {
            while (c.moveToNext()) out.add(readFill(c));
        }
        return out;
    }
    public List<Fill> allFills() {
        List<Fill> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT f.id,f.vehicle_id,v.name,f.time,f.odometer,f.liters,f.amount,f.estimate FROM fills f JOIN vehicles v ON v.id=f.vehicle_id ORDER BY v.name COLLATE NOCASE, f.odometer", null)) {
            while (c.moveToNext()) out.add(readFill(c));
        }
        return out;
    }
    private Fill readFill(Cursor c) { return new Fill(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getDouble(4), c.getDouble(5), c.getDouble(6), c.getDouble(7)); }
    public long addFill(long vehicleId, long time, double odo, double liters, double amount, double estimate) {
        ContentValues cv = new ContentValues();
        cv.put("vehicle_id", vehicleId); cv.put("time", time); cv.put("odometer", odo);
        cv.put("liters", liters); cv.put("amount", amount); cv.put("estimate", estimate);
        return getWritableDatabase().insertOrThrow("fills", null, cv);
    }
    public void deleteFill(long id) { getWritableDatabase().delete("fills", "id=?", new String[]{String.valueOf(id)}); }
}
