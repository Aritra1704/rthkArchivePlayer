package com.hei.android.app.rthkArchivePlayer;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class StarredActivity extends ActionBarListActivity {

	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.title_starred);
		
		final List<ProgrammeModel> starredProgrames = ProgrammeModel.getStarredProgrames(this);
		final StarredItemAdapter starredItemAdapter = new StarredItemAdapter(this, starredProgrames);
		setListAdapter(starredItemAdapter);
	}
	 
	private static class StarredItemAdapter extends BaseAdapter {
		private final Context _context;
		private final List<ProgrammeModel> _models;
		private final LayoutInflater _inflater; 
		

		public StarredItemAdapter(Context context, List<ProgrammeModel> models) {
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
		public Object getItem(int position) {
			final ProgrammeModel model = _models.get(position);
			return model;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			StarredItemViewModel viewModel = null; 
			if (convertView == null) {
				convertView = _inflater.inflate(R.layout.starred_item_view, null);

				final TextView textView = (TextView) convertView.findViewById(R.id.starred_item_text);
				final ImageButton starButton = (ImageButton) convertView.findViewById(R.id.starred_item_unstar);

				viewModel = new StarredItemViewModel(textView, starButton);
				convertView.setTag(viewModel);
			}else {
				viewModel = (StarredItemViewModel) convertView.getTag();
			}

			final ProgrammeModel model = _models.get(position);
			final String name = model.getName();

			final TextView textView = viewModel.getTextView();
			textView.setText(name);
			textView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final Intent intent = new Intent(_context, ProgrammeActivity.class);
					final String key = _context.getString(R.string.key_programme);
					intent.putExtra(key, model);
					_context.startActivity(intent);
				}
			});       

			final ImageButton unstarButton = viewModel.getUnstarButton();

			unstarButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(_context)
					.setTitle(R.string.starred_unstar_dialog_header)
					.setMessage(String.format(_context.getString(R.string.starred_unstar_dialog_text), name))
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {
							model.setStarred(_context, false);
							_models.remove(model);
							notifyDataSetChanged();
						}

					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
						
					})
					.show();
				}
			});

			return convertView;
		}

		private static class StarredItemViewModel {
			private final TextView _textView;
			private final ImageButton _unstarButton;

			StarredItemViewModel(final TextView textView, final ImageButton unstarButton) {
				_textView = textView;
				_unstarButton = unstarButton;
			}

			public TextView getTextView() {
				return _textView;
			}

			public ImageButton getUnstarButton() {
				return _unstarButton;
			}
		}
		
	}
}
