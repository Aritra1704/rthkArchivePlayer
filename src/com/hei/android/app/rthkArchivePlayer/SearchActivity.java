package com.hei.android.app.rthkArchivePlayer;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class SearchActivity extends ActionBarListActivity {
	private static final String SEARCH_URL = "http://search.rthk.org.hk/search/search_archive_2010.php?archivetype=all&keyword=";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.title_search);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.search_menu, menu);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
			searchView.setOnQueryTextListener(new OnQueryTextListener() {

				@Override
				public boolean onQueryTextChange(String newText) {
					return false;
				}

				@Override
				public boolean onQueryTextSubmit(final String query) {
					if(query.equals("")) {
						return false;
					}

					new QueryTask(SearchActivity.this).execute(new String[]{query});

					return true;
				}
			});
		}

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == R.id.menu_search) {
			final Intent intent = item.getIntent();
			
			if(intent != null) {
				final String query = intent.getAction();
				new QueryTask(SearchActivity.this).execute(new String[]{query});
			}
		}

		return super.onOptionsItemSelected(item);
	}	

	private static class ProgrammeItemsAdapter extends BaseAdapter {
		private final Context _context;
		private final List<ProgrammeModel> _models;
		private final LayoutInflater _inflater; 

		public ProgrammeItemsAdapter(Context context, List<ProgrammeModel> models) {
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
			ProgrammeItemViewModel viewModel = null; 
			if (convertView == null) {
				convertView = _inflater.inflate(R.layout.programme_item_view, null);

				final TextView textView = (TextView) convertView.findViewById(R.id.programme_item_text);
				final ImageButton starButton = (ImageButton) convertView.findViewById(R.id.programme_item_star);

				viewModel = new ProgrammeItemViewModel(textView, starButton);
				convertView.setTag(viewModel);
			}else {
				viewModel = (ProgrammeItemViewModel) convertView.getTag();
			}

			final ProgrammeModel model = _models.get(position);
			final String name = model.getName();
			final boolean starred = model.isStarred();

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

			final ImageButton starButton = viewModel.getStarButton();

			if(starred) {
				starButton.setImageResource(R.drawable.programme_item_star_filled);
			}
			else {
				starButton.setImageResource(R.drawable.programme_item_star_empty);
			}

			starButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final boolean starred = model.isStarred();
					model.setStarred(!starred);
					if(starred) {
						starButton.setImageResource(R.drawable.programme_item_star_empty);
					}
					else {
						starButton.setImageResource(R.drawable.programme_item_star_filled);
					}

				}
			});

			return convertView;
		}

		class ProgrammeItemViewModel {
			private final TextView _textView;
			private final ImageButton _starButton;

			ProgrammeItemViewModel(final TextView textView, final ImageButton starButton) {
				_textView = textView;
				_starButton = starButton;
			}

			public TextView getTextView() {
				return _textView;
			}

			public ImageButton getStarButton() {
				return _starButton;
			}
		}

	}

	private static class QueryTask extends AsyncTask<String, Void, List<ProgrammeModel>> {
		private final ListActivity _activity;

		public QueryTask(final ListActivity activity) {
			_activity = activity;
		}

		@Override
		protected List<ProgrammeModel> doInBackground(String... params) {
			final List<ProgrammeModel> programmes = new ArrayList<ProgrammeModel>();

			if(params.length > 0) {
				final String query = URLEncoder.encode(params[0]);
				final Connection connection = Jsoup.connect(SEARCH_URL + query);
				try {
					final Document document = connection.get();
					final Elements links = document.select("a");
					for (final Element link : links) {
						final String href = link.attr("href");
						final String text = link.text();

						final ProgrammeModel programme = new ProgrammeModel(text, href);
						programmes.add(programme);
					}
				} catch (final IOException e) {
					return null;
				}
			}

			return programmes;
		}

		@Override
		protected void onPostExecute(List<ProgrammeModel> result) {
			if(result == null) {
				new AlertDialog.Builder(_activity)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("搜尋失敗")
				.setMessage("請檢查裝置是否連接到互聯網。")
				.setPositiveButton("確定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {

					}

				})
				.create()
				.show();

				return;
			}

			if(result.isEmpty()) {
				new AlertDialog.Builder(_activity)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("搜尋完成")
				.setMessage("沒有相關的結果。")
				.setPositiveButton("確定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {

					}

				})
				.create()
				.show();
				return;
			}

			final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(_activity, result);
			_activity.setListAdapter(adapter);
		}

	}

}
