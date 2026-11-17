package com.example.fuelbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_IMPORT = 501, REQUEST_EXPORT = 502;
    private FuelDb db;
    private SharedPreferences prefs;
    private boolean dark;
    private int bg, panel, primary, secondary, accent, tint, border, field;
    private long selectedVehicle = -1;
    private LinearLayout root;
    private EditText odoInput, litersInput, amountInput, estimateInput;
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle state) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme);
        super.onCreate(state);
        db = new FuelDb(this);
        selectedVehicle = prefs.getLong("selected_vehicle", -1);
        colors(); render();
    }
    private void colors() {
        bg = Color.parseColor(dark ? "#101416" : "#F7F8FA");
        panel = Color.parseColor(dark ? "#1B2225" : "#FFFFFF");
        primary = Color.parseColor(dark ? "#F3F7F7" : "#162B30");
        secondary = Color.parseColor(dark ? "#A2B1B4" : "#64777D");
        accent = Color.parseColor(dark ? "#65DCC7" : "#0D796A");
        tint = Color.parseColor(dark ? "#233D39" : "#E7F6F1");
        border = Color.parseColor(dark ? "#354246" : "#E6ECEE");
        field = Color.parseColor(dark ? "#263034" : "#F4F7F8");
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private int d(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable s = new GradientDrawable(); s.setColor(color); s.setCornerRadius(d(radius));
        if (stroke != 0) s.setStroke(d(1), stroke);
        return s;
    }
    private LinearLayout col() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : d(width), height < 0 ? height : d(height)); }
    private void addSpace(LinearLayout target, int height) { target.addView(new View(this), lp(1, height)); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextColor(color); v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setIncludeFontPadding(false); return v;
    }
    private LinearLayout card(LinearLayout target) {
        LinearLayout l = col(); l.setPadding(d(18), d(18), d(18), d(18)); l.setBackground(shape(panel, 20, border));
        target.addView(l, lp(-1, -2)); return l;
    }
    private Button button(String title, boolean filled) {
        Button v = new Button(this); v.setText(title); v.setAllCaps(false); v.setTextSize(13);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setTextColor(filled ? (dark ? Color.parseColor("#09251E") : Color.WHITE) : accent);
        v.setBackground(shape(filled ? accent : tint, 12, 0));
        v.setPadding(d(12), d(6), d(12), d(6)); v.setMinHeight(d(46)); return v;
    }
    private EditText edit(String hint, String value, boolean numeric) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(16); e.setTextColor(primary); e.setHintTextColor(secondary);
        e.setHint(hint); e.setText(value); e.setSelectAllOnFocus(false);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setPadding(d(14), 0, d(14), 0); e.setBackground(shape(field, 11, 0));
        e.setLayoutParams(lp(-1, 52)); return e;
    }
    private EditText input(LinearLayout target, String label, String hint, String initial) {
        target.addView(text(label, 12, secondary, true)); addSpace(target, 7);
        EditText e = edit(hint, initial, true); target.addView(e); addSpace(target, 14); return e;
    }
    private double number(EditText e, String title, boolean zeroAllowed) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Enter " + title.toLowerCase(Locale.ROOT));
        double val;
        try { val = Double.parseDouble(s); } catch (Exception ex) { throw new IllegalArgumentException("Invalid " + title.toLowerCase(Locale.ROOT)); }
        if (!Double.isFinite(val) || val < 0 || (!zeroAllowed && val == 0)) throw new IllegalArgumentException("Invalid " + title.toLowerCase(Locale.ROOT));
        return val;
    }
    private String n(double value) { return fmt.format(value); }
    private String date(long time) { return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(time)); }
    private void alert(String message) { new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private FuelDb.Vehicle selected(List<FuelDb.Vehicle> vehicles) {
        for (FuelDb.Vehicle v : vehicles) if (v.id == selectedVehicle) return v;
        if (!vehicles.isEmpty()) { selectedVehicle = vehicles.get(0).id; prefs.edit().putLong("selected_vehicle", selectedVehicle).apply(); return vehicles.get(0); }
        selectedVehicle = -1; return null;
    }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        root = col(); root.setPadding(d(18), d(22), d(18), d(36)); scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            root.setPadding(d(18), d(22) + insets.getSystemWindowInsetTop(), d(18), d(36) + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(scroll);
        LinearLayout header = row(); root.addView(header, lp(-1, -2));
        LinearLayout names = col(); header.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        names.addView(text("Fuelbook", 28, primary, true)); addSpace(names, 5);
        names.addView(text("Every fill-up, clearly tracked.", 13, secondary, false));
        Button theme = button(dark ? "☀ Light" : "☾ Dark", false); header.addView(theme, lp(88, 44));
        theme.setOnClickListener(v -> { prefs.edit().putBoolean("dark", !dark).apply(); recreate(); });
        addSpace(root, 26);
        List<FuelDb.Vehicle> vehicles = db.vehicles();
        FuelDb.Vehicle current = selected(vehicles);
        LinearLayout selector = card(root);
        LinearLayout pickerHeader = row(); selector.addView(pickerHeader, lp(-1, -2));
        pickerHeader.addView(text("VEHICLE", 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = button("+ Add", false); pickerHeader.addView(add, lp(82, 41)); add.setOnClickListener(v -> vehicleDialog(null));
        addSpace(selector, 10);
        if (current != null) {
            Spinner spinner = new Spinner(this);
            ArrayAdapter<FuelDb.Vehicle> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicles);
            spinner.setAdapter(adapter); spinner.setBackground(shape(field, 10, 0));
            int index = 0; for (int i = 0; i < vehicles.size(); i++) if (vehicles.get(i).id == current.id) index = i;
            spinner.setSelection(index); selector.addView(spinner, lp(-1, 50));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean initial = true;
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (initial) { initial = false; return; }
                    selectedVehicle = vehicles.get(position).id; prefs.edit().putLong("selected_vehicle", selectedVehicle).apply(); render();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            addSpace(selector, 11);
            LinearLayout tools = row(); selector.addView(tools, lp(-1, -2));
            tools.addView(text("Estimated: " + (current.estimate > 0 ? n(current.estimate) + " km/L" : "not set"), 13, secondary, false), new LinearLayout.LayoutParams(0, -2, 1));
            Button manage = button("Edit vehicle", false); tools.addView(manage, lp(112, 42)); manage.setOnClickListener(v -> vehicleDialog(current));
        } else {
            selector.addView(text("Add a vehicle to start logging fuel.", 14, secondary, false));
        }
        addSpace(root, 18);
        if (current != null) { summary(current); entryForm(current); history(current); }
        addSpace(root, 14); filesCard();
        addSpace(root, 15);
        TextView foot = text("Mileage assumes a full tank at each fill-up.\nDistance since previous fill ÷ litres added now.", 12, secondary, false);
        foot.setGravity(Gravity.CENTER); foot.setLineSpacing(d(3), 1); root.addView(foot);
    }
    private void summary(FuelDb.Vehicle vehicle) {
        List<FuelDb.Fill> list = db.fills(vehicle.id);
        LinearLayout summary = card(root);
        summary.addView(text("MILEAGE AT A GLANCE", 12, secondary, true)); addSpace(summary, 12);
        if (list.size() < 2) {
            summary.addView(text("— km/L", 29, accent, true)); addSpace(summary, 5);
            summary.addView(text(list.isEmpty() ? "Log your first fill-up to get started." : "Add one more fill-up to calculate mileage.", 13, secondary, false));
        } else {
            FuelDb.Fill newer = list.get(0), older = list.get(1);
            double distance = newer.odometer - older.odometer;
            summary.addView(text(n(distance / newer.liters) + " km/L", 31, accent, true)); addSpace(summary, 5);
            summary.addView(text(n(distance) + " km since last fill  ·  " + n(newer.liters) + " L added", 13, secondary, false));
            if (newer.estimate > 0) {
                addSpace(summary, 10);
                double delta = (distance / newer.liters - newer.estimate) / newer.estimate * 100;
                summary.addView(text("Vs estimate: " + (delta >= 0 ? "+" : "") + n(delta) + "%", 13, primary, true));
            }
        }
        addSpace(root, 18);
    }
    private void entryForm(FuelDb.Vehicle vehicle) {
        LinearLayout form = card(root);
        form.addView(text("New fill-up", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Record your fuel purchase and current odometer.", 13, secondary, false)); addSpace(form, 19);
        odoInput = input(form, "ODOMETER (KM)", "e.g. 18540", "");
        litersInput = input(form, "FUEL QUANTITY (LITRES)", "e.g. 25.5", "");
        amountInput = input(form, "AMOUNT SPENT (₹)", "e.g. 2500", "");
        estimateInput = input(form, "ESTIMATED MILEAGE (KM/L) · OPTIONAL", "e.g. 18", vehicle.estimate > 0 ? nPlain(vehicle.estimate) : "");
        TextView info = text("A mileage result appears after the second fill-up for this vehicle.", 12, secondary, false);
        form.addView(info); addSpace(form, 14);
        Button save = button("Save fill-up", true); form.addView(save, lp(-1, 49)); save.setOnClickListener(v -> saveFill(vehicle));
        addSpace(root, 18);
    }
    private String nPlain(double x) { return String.valueOf(x); }
    private void saveFill(FuelDb.Vehicle vehicle) {
        try {
            double odo = number(odoInput, "odometer", true);
            double liters = number(litersInput, "fuel quantity", false);
            double amount = number(amountInput, "amount spent", true);
            String estimateText = estimateInput.getText().toString().trim();
            double estimate = estimateText.isEmpty() ? 0 : number(estimateInput, "estimated mileage", true);
            FuelDb.Fill last = db.newest(vehicle.id);
            if (last != null && odo <= last.odometer) throw new IllegalArgumentException("Odometer must be above the previous reading (" + n(last.odometer) + " km).");
            db.addFill(vehicle.id, System.currentTimeMillis(), odo, liters, amount, estimate);
            // Remember latest estimate for future entries.
            if (estimate != vehicle.estimate) db.updateVehicle(vehicle.id, vehicle.name, estimate);
            toast("Fill-up saved"); render();
        } catch (Exception e) { alert(e.getMessage() == null ? "Could not save fill-up." : e.getMessage()); }
    }
    private void history(FuelDb.Vehicle vehicle) {
        LinearLayout section = col(); root.addView(section, lp(-1, -2));
        LinearLayout heading = row(); section.addView(heading, lp(-1, -2));
        heading.addView(text("Fill-up history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        List<FuelDb.Fill> fills = db.fills(vehicle.id);
        heading.addView(text(fills.size() + " entries", 12, secondary, false)); addSpace(section, 12);
        if (fills.isEmpty()) {
            LinearLayout empty = card(section); empty.addView(text("No fill-ups yet", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Saved entries will appear here.", 13, secondary, false)); return;
        }
        for (int i = 0; i < fills.size(); i++) {
            FuelDb.Fill f = fills.get(i);
            FuelDb.Fill prev = i + 1 < fills.size() ? fills.get(i + 1) : null;
            LinearLayout item = card(section);
            LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
            LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            left.addView(text(date(f.time), 14, primary, true)); addSpace(left, 5);
            left.addView(text(n(f.odometer) + " km on odometer", 12, secondary, false));
            Button delete = button("Delete", false); titleRow.addView(delete, lp(78, 42));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete fill-up?")
                    .setMessage("This cannot be undone. Mileage will be recalculated from the remaining entries.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { db.deleteFill(f.id); render(); }).show());
            addSpace(item, 14);
            LinearLayout nums = row(); item.addView(nums, lp(-1, -2));
            metric(nums, "FUEL", n(f.liters) + " L"); metric(nums, "SPENT", "₹" + n(f.amount));
            metric(nums, "ACTUAL", prev == null ? "—" : n((f.odometer - prev.odometer) / f.liters) + " km/L");
            if (f.estimate > 0) { addSpace(item, 11); item.addView(text("Estimated: " + n(f.estimate) + " km/L", 12, secondary, false)); }
            addSpace(section, 10);
        }
    }
    private void metric(LinearLayout row, String label, String value) {
        LinearLayout block = col(); row.addView(block, new LinearLayout.LayoutParams(0, -2, 1));
        block.addView(text(label, 10, secondary, true)); addSpace(block, 6);
        block.addView(text(value, 14, primary, true));
    }
    private void filesCard() {
        LinearLayout card = card(root);
        card.addView(text("Your data", 20, primary, true)); addSpace(card, 7);
        card.addView(text("Import or export all vehicles and fill-ups as one CSV file. Your data stays on your device.", 13, secondary, false)); addSpace(card, 16);
        LinearLayout actions = row(); card.addView(actions, lp(-1, -2));
        Button importBtn = button("Import CSV", false), exportBtn = button("Export CSV", true);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, d(48), 1); a.setMargins(0, 0, d(7), 0);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, d(48), 1); b.setMargins(d(7), 0, 0, 0);
        actions.addView(importBtn, a); actions.addView(exportBtn, b);
        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/vnd.ms-excel"});
            startActivityForResult(intent, REQUEST_IMPORT);
        });
        exportBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "fuelbook-export.csv");
            startActivityForResult(intent, REQUEST_EXPORT);
        });
    }
    private void vehicleDialog(FuelDb.Vehicle vehicle) {
        LinearLayout content = col(); content.setPadding(d(22), d(10), d(22), 0);
        content.addView(text("VEHICLE NAME", 12, secondary, true)); addSpace(content, 7);
        EditText name = edit("e.g. City / Activa", vehicle == null ? "" : vehicle.name, false);
        content.addView(name); addSpace(content, 16);
        content.addView(text("DEFAULT ESTIMATED MILEAGE (KM/L)", 12, secondary, true)); addSpace(content, 7);
        EditText estimate = edit("Optional", vehicle != null && vehicle.estimate > 0 ? nPlain(vehicle.estimate) : "", true);
        content.addView(estimate);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(vehicle == null ? "Add vehicle" : "Edit vehicle")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null);
        if (vehicle != null) builder.setNeutralButton("Delete", null);
        AlertDialog dialog = builder.create(); dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String label = name.getText().toString().trim();
                    if (label.isEmpty()) throw new IllegalArgumentException("Enter a vehicle name.");
                    double est = estimate.getText().toString().trim().isEmpty() ? 0 : number(estimate, "estimated mileage", true);
                    if (vehicle == null) selectedVehicle = db.addVehicle(label, est);
                    else { db.updateVehicle(vehicle.id, label, est); selectedVehicle = vehicle.id; }
                    prefs.edit().putLong("selected_vehicle", selectedVehicle).apply();
                    dialog.dismiss(); render();
                } catch (Exception e) { alert("Could not save vehicle. Check that its name is unique and mileage is valid."); }
            });
            if (vehicle != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Delete " + vehicle.name + "?")
                        .setMessage("This will permanently delete the vehicle and all its fill-up entries.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (x, y) -> {
                            db.deleteVehicle(vehicle.id); selectedVehicle = -1;
                            prefs.edit().remove("selected_vehicle").apply(); dialog.dismiss(); render();
                        }).show();
            });
        }); dialog.show();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) {
            try (OutputStreamWriter writer = new OutputStreamWriter(getContentResolver().openOutputStream(uri, "wt"), StandardCharsets.UTF_8)) {
                CsvIO.export(db, writer); toast("CSV exported successfully");
            } catch (Exception e) { alert("Export failed: " + e.getMessage()); }
        } else if (requestCode == REQUEST_IMPORT) {
            try (InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8)) {
                CsvIO.Result result = CsvIO.importCsv(db, reader);
                render(); alert("Import complete: " + result.vehicles + " new vehicles, " + result.fills + " new fill-ups, " + result.duplicates + " duplicates skipped.");
            } catch (Exception e) { alert("Import failed (nothing was changed): " + e.getMessage()); }
        }
    }
}
