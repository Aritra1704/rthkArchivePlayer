package com.hei.android.app.rthkArchivePlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.model.HistoryModel;
import com.hei.android.app.rthkArchivePlayer.repository.HistoryRepository;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class HistoryActivity extends ActionBarListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.history_title);
		
		final List<HistoryModel> history = HistoryRepository.getInstance(this).getHistory(this);
		final HistoryItemsAdapter historyItemsAdapter = new HistoryItemsAdapter(this, history);
		setListAdapter(historyItemsAdapter);
	}
	
	private static class HistoryItemsAdapter extends BaseAdapter {
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
		private static final SimpleDateFormat ACCESS_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);

		private final Context _context;
		private final List<HistoryModel> _models;
		private final LayoutInflater _inflater;

		public HistoryItemsAdapter(final Context context, final List<HistoryModel> models) {
			_context = context;
			_models = models;
			_inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			final int size = _models.size();
			return size;
		}

		@Override
		public Object getItem(final int position) {
			final HistoryModel model = _models.get(position);
			return model;
		}

		@Override
		public long getItemId(final int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			HistoryItemViewModel viewModel = null;
			if (convertView == null) {
				convertView = _inflater.inflate(R.layout.history_item_view, null);

				final View textView = convertView.findViewById(R.id.history_item_text);
				final TextView accessTimeView = (TextView) convertView.findViewById(R.id.history_item_time);
				final TextView nameView = (TextView) convertView.findViewById(R.id.history_item_name);
				final ImageButton deleteButton = (ImageButton) convertView.findViewById(R.id.history_item_delete);

				viewModel = new HistoryItemViewModel(textView, accessTimeView, nameView, deleteButton);
				convertView.setTag(viewModel);
			}else {
				viewModel = (HistoryItemViewModel) convertView.getTag();
			}

			final HistoryModel model = _models.get(position);
			final Date accessTime = model.getAccessTime();
			final TextView accessTimeView = viewModel.getAccessTimeView();
			accessTimeView.setText(ACCESS_TIME_FORMAT.format(accessTime));

			final EpisodeModel episode = model.getEpisode();
			final String programmeName = episode.getProgramme().getName();
			final Date date = episode.getDate();
			final String title = episode.getName();

			final TextView nameView = viewModel.getNameView();
			final String name = programmeName + "(" + DATE_FORMAT.format(date) + ") " + title;
			nameView.setText(name);

			final View textView = viewModel.getTextView();
			textView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {
					final String asxUrl = episode.getAsxUrl();

					if(asxUrl == null) {
						new AlertDialog.Builder(_context)
						.setMessage(R.string.alert_no_asx_url)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface arg0, final int arg1) {

							}

						})
						.show();
						return;
					}

					final Intent intent = new Intent(_context, PlayerActivity.class);
					final Uri uri = Uri.parse(asxUrl);
					intent.setData(uri);
					intent.putExtra(_context.getString(R.string.key_episode), episode);

					_context.startActivity(intent);
				}
			});

			final ImageButton deleteButton = viewModel.getDeleteButton();

			deleteButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {
					new AlertDialog.Builder(_context)
					.setTitle(R.string.history_delete_dialog_header)
					.setMessage(String.format(_context.getString(R.string.history_delete_dialog_text), name))
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {
							HistoryRepository.getInstance(_context).removeHistory(_context, episode);
							_models.remove(model);
							notifyDataSetChanged();
						}

					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog, final int which) {
						}

					})
					.show();
				}
			});

			return convertView;
		}

		private static class HistoryItemViewModel {
			private final View _textView;
			private final TextView _accessTimeView;
			private final TextView _nameView;
			private final ImageButton _starButton;

			private HistoryItemViewModel(final View textView, final TextView accessTimeView,
					final TextView nameView, final ImageButton starButton) {
				_textView = textView;
				_accessTimeView = accessTimeView;
				_nameView = nameView;
				_starButton = starButton;
			}

			public View getTextView() {
				return _textView;
			}

			public TextView getAccessTimeView() {
				return _accessTimeView;
			}

			public TextView getNameView() {
				return _nameView;
			}

			public ImageButton getDeleteButton() {
				return _starButton;
			}
		}

	}

}
