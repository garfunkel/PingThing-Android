package org.simonallen.pingthing;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by simon on 9/04/17.
 */

public class ServerDetailHistoryAdapter extends RecyclerView.Adapter<ServerDetailHistoryAdapter.ViewHolder>{
	private List<PingResult> mHistory;

	public class ViewHolder extends RecyclerView.ViewHolder {
		public TextView date, pingTime, statusCode, status;

		public ViewHolder(View view) {
			super(view);
			date = (TextView) view.findViewById(R.id.textView_date);
			pingTime = (TextView) view.findViewById(R.id.textView_pingTime);
			statusCode = (TextView) view.findViewById(R.id.textView_statusCode);
			status = (TextView) view.findViewById(R.id.textView_status);
		}
	}

	public ServerDetailHistoryAdapter(List<PingResult> history) {
		mHistory = history;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.server_detail_history_entry, parent, false);

		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		PingResult result = mHistory.get(position);
		holder.date.setText(result.date.toString());
		holder.pingTime.setText(String.valueOf(result.pingTime) + "ms");
		holder.statusCode.setText(result.statusCode.toString().toLowerCase());

		if (result.statusCode == PingStatus.GOOD)
			holder.statusCode.setTextColor(ContextCompat.getColor(holder.statusCode.getContext(), R.color.statusBoxGood));

		else if (result.statusCode == PingStatus.BAD)
			holder.statusCode.setTextColor(ContextCompat.getColor(holder.statusCode.getContext(), R.color.statusBoxBad));

		else
			holder.statusCode.setTextColor(ContextCompat.getColor(holder.statusCode.getContext(), R.color.statusBoxUnknown));

		holder.status.setText(result.status);
	}

	@Override
	public int getItemCount() {
		return mHistory.size();
	}
}
