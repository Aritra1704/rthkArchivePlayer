package com.hei.android.app.rthkArchivePlayer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class ProgrammeActivity extends ActionBarListActivity implements OnScrollListener {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd", Locale.US);
	private static final int LOAD_PADDING = 5;
	
	private ProgrammeModel _programme;
	private volatile boolean _loading = false;
	private volatile int _loadedPage = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		_programme = (ProgrammeModel) intent.getSerializableExtra(getString(R.string.key_programme));
		
		final String name = _programme.getName();
		setTitle(name);

		final EpisodeItemsAdapter adapter = new EpisodeItemsAdapter(ProgrammeActivity.this, new ArrayList<EpisodeModel>(50));
		setListAdapter(adapter);
		
		final LoadEpisodeTask loadEpisodeTask = new LoadEpisodeTask();
		loadEpisodeTask.execute();
		
		getListView().setOnScrollListener(this);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.programme_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
        boolean loadMore = firstVisible + visibleCount + LOAD_PADDING >= totalCount;

        if(loadMore && !_loading) {
        	new LoadEpisodeTask().execute();
        }
		
	}


	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
	}


	private static class EpisodeItemsAdapter extends BaseAdapter {
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
		
		private final Context _context;
		private final List<EpisodeModel> _models;
		private final LayoutInflater _inflater; 

		public EpisodeItemsAdapter(Context context, List<EpisodeModel> models) {
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
			final EpisodeModel model = _models.get(position);
			return model;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			EpisodeItemViewModel viewModel = null; 
			if (convertView == null) {
				convertView = _inflater.inflate(R.layout.episode_item_view, null);

				final View textView = convertView.findViewById(R.id.episode_item_text);
				final TextView dateView = (TextView) convertView.findViewById(R.id.episode_item_date);
				final TextView titleView = (TextView) convertView.findViewById(R.id.episode_item_title);
				final ImageButton downloadButton = (ImageButton) convertView.findViewById(R.id.episode_item_download);
				
				viewModel = new EpisodeItemViewModel(textView, dateView, titleView, downloadButton);
				convertView.setTag(viewModel);
			}else {
				viewModel = (EpisodeItemViewModel) convertView.getTag();
			}

			final EpisodeModel model = _models.get(position);
			final String name = model.getName();
			final Date date = model.getDate();
			final String dateString = DATE_FORMAT.format(date);
			
			final TextView dateView = viewModel.getDateView();
			dateView.setText(dateString);

			final TextView titleView = viewModel.getTitleView();
			titleView.setText(name);
			
			final View textView = viewModel.getTextView();
			textView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final String asxUrl = model.getAsxUrl();

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
					intent.putExtra(_context.getString(R.string.key_episode), model);

					_context.startActivity(intent);
					
				}
			});       
			
			final ImageButton downloadButton = viewModel.getDownloadButton();
			
			downloadButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final Intent intent = new Intent(_context, DownloadActivity.class);
					intent.putExtra(_context.getString(R.string.key_episode), model);
					_context.startActivity(intent);
				}
			});

			return convertView;
		}
		
		public void add(EpisodeModel model) {
			_models.add(model);
		}
		
		class EpisodeItemViewModel {
			private final View _textView;
			private final TextView _dateView;
			private final TextView _titleView;
			private final ImageButton _downloadButton;

			EpisodeItemViewModel(final View textView, final TextView dateView, final TextView titleView, 
					final ImageButton downloadButton) {
				_textView = textView;
				_dateView = dateView;
				_titleView = titleView;
				_downloadButton = downloadButton;
			}

			public View getTextView() {
				return _textView;
			}
			
			public TextView getDateView() {
				return _dateView;
			}
			
			public TextView getTitleView() {
				return _titleView;
			}

			public ImageButton getDownloadButton() {
				return _downloadButton;
			}
		}

	}
	
	private class LoadEpisodeTask extends AsyncTask<Void, EpisodeModel, List<EpisodeModel>> {
		
		private final Pattern TITLE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})(.*)");
		private final ProgressDialog _loadingDialog;
		
		public LoadEpisodeTask() {
			_loading = true;
			if(_loadedPage == 0) {
				_loadingDialog = ProgressDialog.show(ProgrammeActivity.this, "載入中", "正在載入各集資料，請稍侯...", true);
			}
			else {
				_loadingDialog = null;
			}
		}
		
		@Override
		protected List<EpisodeModel> doInBackground(Void... para) {

			final List<EpisodeModel> episodes = new ArrayList<EpisodeModel>();
			
			final String pageUrl = _programme.getPageUrl(1);
			
			final Connection conn = Jsoup.connect(pageUrl);
			try {
				final Document document = conn.get();
				final Elements items = document.select("div.title a");
				for (final Element element : items) {
					final String text = element.text();

					final Matcher matcher = TITLE_PATTERN.matcher(text);
					if(!matcher.matches()) {
						continue;
					}

					final String dateString = matcher.group(1);
					final String title = matcher.group(2).trim();

					final Date date;
					try {
						date = DATE_FORMAT.parse(dateString);
					}catch (final ParseException e) {
						e.printStackTrace();
						continue;
					}

					final EpisodeModel episode = new EpisodeModel(_programme, title, date);
					if(episode.getAsxUrl() != null) {
						publishProgress(episode);
						episodes.add(episode);
					}
					
				}
			} catch (final IOException e) {
				e.printStackTrace();

				
				return null;
			}
			
			return episodes;
		}
		
		@Override
		protected void onProgressUpdate(EpisodeModel... values) {
			final EpisodeItemsAdapter adapter = (EpisodeItemsAdapter) getListAdapter();
			adapter.add(values[0]);
			adapter.notifyDataSetChanged();
		}
		
		@Override
		protected void onPostExecute(List<EpisodeModel> episodes) {
			if(_loadingDialog != null) {
				_loadingDialog.dismiss();
			}
			if(episodes == null) {
				new AlertDialog.Builder(ProgrammeActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle(R.string.alert_internet_fail_title)
				.setMessage(R.string.alert_internet_fail_message)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {

					}

				})
				.create()
				.show();
			}
			else if(!episodes.isEmpty()){
			}
			
			_loadedPage++;
			_loading = false;
		}
		
	}
	
}
