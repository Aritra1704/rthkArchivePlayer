package com.hei.android.app.rthkArchivePlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
	private static final String ARCHIVE_URL = "http://programme.rthk.hk/channel/radio/index_archive.php";
	
	private List<ProgrammeModel> _programmes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.search_title);

		final LoadProgramListTask loadProgramTask = new LoadProgramListTask(this);
		loadProgramTask.execute();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.search_menu, menu);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
			searchView.setOnQueryTextListener(new OnQueryTextListener() {

				@Override
				public boolean onQueryTextChange(String query) {
					if(query.equals("")) {
						final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(SearchActivity.this, _programmes);
						setListAdapter(adapter);
						return true;
					}

					final Pattern pattern = Pattern.compile(".*" + query + ".*", Pattern.CASE_INSENSITIVE);
					final List<ProgrammeModel> filtered = new ArrayList<ProgrammeModel>();
					for (ProgrammeModel programme : _programmes) {
						final String name = programme.getName();
						final Matcher matcher = pattern.matcher(name);
						if(matcher.matches()) {
							filtered.add(programme);
						}
					}
					
					final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(SearchActivity.this, filtered);
					setListAdapter(adapter);

					return true;
				}

				@Override
				public boolean onQueryTextSubmit(final String query) {
					return false;
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
				final Pattern pattern = Pattern.compile(".*" + query + ".*", Pattern.CASE_INSENSITIVE);
				final List<ProgrammeModel> filtered = new ArrayList<ProgrammeModel>();
				for (ProgrammeModel programme : _programmes) {
					final String name = programme.getName();
					final Matcher matcher = pattern.matcher(name);
					if(matcher.matches()) {
						filtered.add(programme);
					}
				}
				final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(this, filtered);
				setListAdapter(adapter);
			}
		}

		return super.onOptionsItemSelected(item);
	}
	
	public List<ProgrammeModel> getProgrammes() {
		return _programmes;
	}
	
	public void setProgrammes(List<ProgrammeModel> programmes) {
		_programmes = programmes;
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
					model.setStarred(_context, !starred);
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
	
	private static class LoadProgramListTask extends AsyncTask<Void, Void, Map<String, List<ProgrammeModel>>> {
		private static final Map<String, String> CLASS_NAMES;
		static {
			CLASS_NAMES = new LinkedHashMap<String, String>();
			CLASS_NAMES.put("第一台", "radio1");
			CLASS_NAMES.put("第二台", "radio2");
			CLASS_NAMES.put("第三台", "radio3");
			CLASS_NAMES.put("第四台", "radio4");
			CLASS_NAMES.put("第五台", "radio5");
			CLASS_NAMES.put("普通話台", "pth");
		}
		
		private static final Pattern PROGRAM_URL_PATTERN = Pattern.compile("http://programme.rthk.hk/channel/radio/programme.php\\?name=(.*)\\&.*");

		private final SearchActivity _activity;
		private final ProgressDialog _loadingDialog;

		public LoadProgramListTask(final SearchActivity activity) {
			_activity = activity;
			_loadingDialog = ProgressDialog.show(_activity, "載入中", "正在載入節目表，請稍侯...", true);
		}
		
		@Override
		protected Map<String, List<ProgrammeModel>> doInBackground(Void... params) {
			final Map<String, List<ProgrammeModel>> programmes = new LinkedHashMap<String, List<ProgrammeModel>>();
			final Connection connection = Jsoup.connect(ARCHIVE_URL);
			try {
				final Document document = connection.get();
				final Set<Entry<String, String>> classnames = CLASS_NAMES.entrySet();
				for (Entry<String, String> entry : classnames) {
					final String channel = entry.getKey();
					final String classname = entry.getValue();
					final List<ProgrammeModel> models = new LinkedList<ProgrammeModel>();
					final Elements links = document.select("a." + classname);
					for (final Element link : links) {
						
						final String text = link.text();
						final String href = link.attr("href");
						final Matcher matcher = PROGRAM_URL_PATTERN.matcher(href);
						if(matcher.matches()) {
							final String id = matcher.group(1);
							final ProgrammeModel programme = new ProgrammeModel(_activity, text, id);
							models.add(programme);
						}

					}
					programmes.put(channel, models);
				}
			} catch (final IOException e) {
				return null;
			}
			
			return programmes;
		}
		
		@Override
		protected void onPostExecute(Map<String, List<ProgrammeModel>> result) {
			_loadingDialog.dismiss();
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

			List<ProgrammeModel> allProgramesList = new ArrayList<ProgrammeModel>();
			final Set<Entry<String, List<ProgrammeModel>>> channelModel = result.entrySet();
			for (Entry<String, List<ProgrammeModel>> entry : channelModel) {
				final List<ProgrammeModel> programes = entry.getValue();
				allProgramesList.addAll(programes);
			}
			
			_activity.setProgrammes(allProgramesList);
			final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(_activity, allProgramesList);
			_activity.setListAdapter(adapter);
		}
	}

//	TODO: Remove QueryTask
//	private static final String SEARCH_URL = "http://search.rthk.org.hk/search/search_archive_2010.php?archivetype=all&keyword=";
//	private static class QueryTask extends AsyncTask<String, Void, List<ProgrammeModel>> {
//		private final ListActivity _activity;
//
//		public QueryTask(final ListActivity activity) {
//			_activity = activity;
//		}
//
//		@Override
//		protected List<ProgrammeModel> doInBackground(String... params) {
//			final List<ProgrammeModel> programmes = new ArrayList<ProgrammeModel>();
//
//			if(params.length > 0) {
//				final String query;
//				try {
//					query = URLEncoder.encode(params[0], "UTF-8");
//				} catch (UnsupportedEncodingException e1) {
//					return null;
//				}
//				final Connection connection = Jsoup.connect(SEARCH_URL + query);
//				try {
//					final Document document = connection.get();
//					final Elements links = document.select("a");
//					for (final Element link : links) {
//						final String href = link.attr("href");
//						final String text = link.text();
//
//						final ProgrammeModel programme = new ProgrammeModel(text, href);
//						programmes.add(programme);
//					}
//				} catch (final IOException e) {
//					return null;
//				}
//			}
//
//			return programmes;
//		}
//
//		@Override
//		protected void onPostExecute(List<ProgrammeModel> result) {
//			if(result == null) {
//				new AlertDialog.Builder(_activity)
//				.setIcon(R.drawable.alert_dialog_icon)
//				.setTitle("搜尋失敗")
//				.setMessage("請檢查裝置是否連接到互聯網。")
//				.setPositiveButton("確定", new DialogInterface.OnClickListener() {
//
//					@Override
//					public void onClick(final DialogInterface arg0, final int arg1) {
//
//					}
//
//				})
//				.create()
//				.show();
//
//				return;
//			}
//
//			if(result.isEmpty()) {
//				new AlertDialog.Builder(_activity)
//				.setIcon(R.drawable.alert_dialog_icon)
//				.setTitle("搜尋完成")
//				.setMessage("沒有相關的結果。")
//				.setPositiveButton("確定", new DialogInterface.OnClickListener() {
//
//					@Override
//					public void onClick(final DialogInterface arg0, final int arg1) {
//
//					}
//
//				})
//				.create()
//				.show();
//				return;
//			}
//
//			final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(_activity, result);
//			_activity.setListAdapter(adapter);
//		}
//
//	}

}
