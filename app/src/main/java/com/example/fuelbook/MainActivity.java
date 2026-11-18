package com.example.fuelbook;
​import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
​import androidx.appcompat.app.AppCompatActivity;
​import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
​public class MainActivity extends AppCompatActivity {
​private FuelDb fuelDb;
private TextView tvOverallAverage, tvTotalDistance, tvTotalFuel, tvTotalSpend;
private WebView webViewCharts;
private LinearLayout containerFillups;
private Button btnAddFillup;
​@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_main); // Assumes standard layout
​fuelDb = new FuelDb(this);
​// Bind UI components (Ensure these IDs exist or match your layout)
tvOverallAverage = findViewById(R.id.tvOverallAverage);
tvTotalDistance = findViewById(R.id.tvTotalDistance);
tvTotalFuel = findViewById(R.id.tvTotalFuel);
tvTotalSpend = findViewById(R.id.tvTotalSpend);
webViewCharts = findViewById(R.id.webViewCharts);
containerFillups = findViewById(R.id.containerFillups);
btnAddFillup = findViewById(R.id.btnAddFillup);
​if (webViewCharts != null) {
webViewCharts.getSettings().setJavaScriptEnabled(true);
webViewCharts.setWebViewClient(new WebViewClient());
}
​if (btnAddFillup != null) {
btnAddFillup.setOnClickListener(v -> showAddEditDialog(null));
}
​refreshData();
}
​/
* Refreshes all metrics, UI lists, and graphs automatically upon any data change.
*/
private void refreshData() {
List<FillupModel> fillups = fuelDb.getAllFillups(); // Assuming FuelDb returns a list of models
calculateAndDisplayMetrics(fillups);
renderCharts(fillups);
renderFillupHistoryList(fillups);
}
​private void calculateAndDisplayMetrics(List<FillupModel> fillups) {
if (fillups == null || fillups.size() < 2) {
if (tvOverallAverage != null) tvOverallAverage.setText("0.00 km/L");
if (tvTotalDistance != null) tvTotalDistance.setText("0 km");
if (tvTotalFuel != null) tvTotalFuel.setText("0 L");
if (tvTotalSpend != null) tvTotalSpend.setText("₹0.00");
return;
}
​// Sort chronologically oldest to newest
Collections.sort(fillups, Comparator.comparing(FillupModel::getDate));
​double totalDistance = 0;
double totalFuel = 0;
double totalSpend = 0;
​for (int i = 1; i < fillups.size(); i++) {
FillupModel prev = fillups.get(i - 1);
FillupModel curr = fillups.get(i);
​double distance = curr.getOdometer() - prev.getOdometer();
double fuel = curr.getLiters();
totalSpend += curr.getTotalCost();
​if (distance > 0 && fuel > 0) {
totalDistance += distance;
totalFuel += fuel;
}
}
​// Include initial fill cost if needed
totalSpend += fillups.get(0).getTotalCost();
​double overallAverage = totalFuel > 0 ? (totalDistance / totalFuel) : 0.0;
​if (tvOverallAverage != null)
tvOverallAverage.setText(String.format("%.2f km/L", overallAverage));
if (tvTotalDistance != null)
tvTotalDistance.setText(String.format("%.1f km", totalDistance));
if (tvTotalFuel != null)
tvTotalFuel.setText(String.format("%.2f L", totalFuel));
if (tvTotalSpend != null)
tvTotalSpend.setText(String.format("₹%.2f", totalSpend));
}
​private void renderCharts(List<FillupModel> fillups) {
if (webViewCharts == null || fillups == null || fillups.size() < 2) return;
​Collections.sort(fillups, Comparator.comparing(FillupModel::getDate));
​StringBuilder dates = new StringBuilder("[");
StringBuilder mileages = new StringBuilder("[");
StringBuilder expenses = new StringBuilder("[");
StringBuilder distances = new StringBuilder("[");
StringBuilder fuelConsumptions = new StringBuilder("[");
​for (int i = 1; i < fillups.size(); i++) {
FillupModel prev = fillups.get(i - 1);
FillupModel curr = fillups.get(i);
​double distance = curr.getOdometer() - prev.getOdometer();
double fuel = curr.getLiters();
double mileage = (fuel > 0 && distance > 0) ? (distance / fuel) : 0.0;
​if (i > 1) {
dates.append(",");
mileages.append(",");
expenses.append(",");
distances.append(",");
fuelConsumptions.append(",");
}
​dates.append("'").append(curr.getDate()).append("'");
mileages.append(String.format("%.2f", mileage));
expenses.append(String.format("%.2f", curr.getTotalCost()));
distances.append(String.format("%.1f", distance));
fuelConsumptions.append(String.format("%.2f", fuel));
}
​dates.append("]");
mileages.append("]");
expenses.append("]");
distances.append("]");
fuelConsumptions.append("]");
​// HTML with Chart.js for smooth interactive rendering inside Android WebView
String htmlContent = "<!DOCTYPE html><html><head>" +
"<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>" +
"<style>body { font-family: sans-serif; background: #f8fafc; margin: 0; padding: 10px; }" +
".chart-container { background: white; border-radius: 12px; padding: 12px; margin-bottom: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }" +
"canvas { width: 100% !important; height: 180px !important; }</style>" +
"</head><body>" +
"<div class="chart-container"><h3>Mileage Trend (km/L)</h3><canvas id="mileageChart"></canvas></div>" +
"<div class="chart-container"><h3>Fuel Expenses</h3><canvas id="expenseChart"></canvas></div>" +
"<div class="chart-container"><h3>Distance & Consumption</h3><canvas id="dualChart"></canvas></div>" +
"<script>" +
"const labels = " + dates.toString() + ";" +
"new Chart(document.getElementById('mileageChart'), {" +
"  type: 'line'," +
"  data: { labels: labels, datasets: [{ label: 'km/L', data: " + mileages.toString() + ", borderColor: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.1)', fill: true, tension: 0.2 }] }," +
"  options: { responsive: true, maintainAspectRatio: false }" +
"});" +
"new Chart(document.getElementById('expenseChart'), {" +
"  type: 'bar'," +
"  data: { labels: labels, datasets: [{ label: 'Cost (₹)', data: " + expenses.toString() + ", backgroundColor: '#3b82f6' }] }," +
"  options: { responsive: true, maintainAspectRatio: false }" +
"});" +
"new Chart(document.getElementById('dualChart'), {" +
"  type: 'bar'," +
"  data: { labels: labels, datasets: [" +
"    { label: 'Distance (km)', data: " + distances.toString() + ", backgroundColor: '#6366f1' }," +
"    { label: 'Fuel (L)', data: " + fuelConsumptions.toString() + ", backgroundColor: '#f59e0b', yAxisID: 'y1' }" +
"  ]}," +
"  options: { responsive: true, maintainAspectRatio: false, scales: { y1: { position: 'right', grid: { drawOnChartArea: false } } } }" +
"});" +
"</script></body></html>";
​webViewCharts.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null);
}
​private void renderFillupHistoryList(List<FillupModel> fillups) {
if (containerFillups == null) return;
containerFillups.removeAllViews();
​for (FillupModel fillup : fillups) {
View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, containerFillups, false);
TextView text1 = itemView.findViewById(android.R.id.text1);
TextView text2 = itemView.findViewById(android.R.id.text2);
​text1.setText("Date: " + fillup.getDate() + " | Odo: " + fillup.getOdometer() + " km");
text2.setText("Liters: " + fillup.getLiters() + " L | Cost: ₹" + fillup.getTotalCost());
​itemView.setOnClickListener(v -> showAddEditDialog(fillup));
containerFillups.addView(itemView);
}
}
​private void showAddEditDialog(FillupModel existing) {
// Dialog implementation for adding/editing/deleting fill-ups
// Call fuelDb.addFillup(...), fuelDb.updateFillup(...), or fuelDb.deleteFillup(...)
// Upon success, call refreshData();
}
}
