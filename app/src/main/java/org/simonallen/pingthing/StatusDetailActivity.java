package org.simonallen.pingthing;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class StatusDetailActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getExtras().getString("type").equals(getString(R.string.status_box_tag_type_server))) {
			setContentView(R.layout.activity_status_detail_server);

			((TextView) findViewById(R.id.textView_host)).setText(getIntent().getExtras().getString("host"));

			if (getIntent().getExtras().getBoolean("icmp"))
				((TextView) findViewById(R.id.textView_port)).setText("ICMP");

			else
				((TextView) findViewById(R.id.textView_port)).setText(String.valueOf(getIntent().getExtras().getInt("port")));

		} else {
			setContentView(R.layout.activity_status_detail_website);

			((TextView) findViewById(R.id.textView_url)).setText(getIntent().getExtras().getString("url"));
		}

		PingResult result = (PingResult) getIntent().getExtras().get("result");
		ArrayList<PingResult> resultHistory = (ArrayList<PingResult>) getIntent().getExtras().get("resultHistory");

		((TextView) findViewById(R.id.textView_name)).setText(getIntent().getExtras().getString("name"));

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_detail);
		setTitle(getIntent().getExtras().getString("name"));
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		PieChart uptimeChart = (PieChart) findViewById(R.id.pieChart_uptime);

		List<PieEntry> uptimeData = new ArrayList<>();

		uptimeData.add(new PieEntry(50));
		uptimeData.add(new PieEntry(25));
		uptimeData.add(new PieEntry(25));

		PieDataSet set = new PieDataSet(uptimeData, "");
		set.setDrawValues(false);
		set.setValueLineColor(ContextCompat.getColor(this, R.color.statusBoxGood));
		set.setColors(ContextCompat.getColor(this, R.color.statusBoxGood), ContextCompat.getColor(this, R.color.statusBoxBad), ContextCompat.getColor(this, R.color.statusBoxUnknown));
		PieData data = new PieData(set);
		uptimeChart.setData(data);
		uptimeChart.invalidate();
		uptimeChart.setCenterText("Uptime");
		uptimeChart.setDescription(null);
		uptimeChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.black));
		uptimeChart.getLegend().setEnabled(false);

		((TextView) findViewById(R.id.textView_uptimeChartGoodPercent)).setText(String.format("good (%.2f%%)", 50f));
		((TextView) findViewById(R.id.textView_uptimeChartBadPercent)).setText(String.format("bad (%.2f%%)", 25f));
		((TextView) findViewById(R.id.textView_uptimeChartUnknownPercent)).setText(String.format("unknown (%.2f%%)", 25f));

		RecyclerView historyView = (RecyclerView) findViewById(R.id.recyclerView_history);
		List<PingResult> history = new ArrayList<>();
		ServerDetailHistoryAdapter historyAdapter = new ServerDetailHistoryAdapter(history);
		RecyclerView.LayoutManager historyLayoutManager = new LinearLayoutManager(getApplicationContext());
		historyView.setLayoutManager(historyLayoutManager);
		historyView.setItemAnimator(new DefaultItemAnimator());
		historyView.setAdapter(historyAdapter);

		for (PingResult historyResult : resultHistory) {
			history.add(historyResult);
		}

		historyAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_status_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent();

		switch (item.getItemId()) {
			case R.id.delete:


			default:
				setResult(RESULT_OK, intent);
				finish();
		}

		return super.onOptionsItemSelected(item);
	}
}
