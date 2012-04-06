package com.hei.android.app.rthkArchivePlayer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import android.widget.Toast;

import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class ProgrammeActivity extends ActionBarListActivity {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final ProgrammeModel programme = (ProgrammeModel) intent.getSerializableExtra(getString(R.string.key_programme));

		final List<EpisodeModel> episodes = new ArrayList<EpisodeModel>();

		final String pageUrl = programme.getPageUrl();
		final Connection conn = Jsoup.connect(pageUrl);
		try {
			final Document document = conn.get();
			final Elements items = document.select("div.title a");
			for (final Element element : items) {
				final String href = element.attr("href");
				final String text = element.text();

				final int indexOfSpace = text.indexOf(' ');
				if(indexOfSpace == -1) {
					continue;
				}

				final String dateString = text.substring(0, indexOfSpace);
				final String title = text.substring(indexOfSpace + 1);

				final Date date;
				try {
					date = DATE_FORMAT.parse(dateString);
				}catch (final ParseException e) {
					e.printStackTrace();
					continue;
				}

				final EpisodeModel episode = new EpisodeModel(title, "http://programme.rthk.org.hk/channel/radio/" + href, date);
				episodes.add(episode);
				
			}
		} catch (final IOException e) {
			e.printStackTrace();

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
		
		final EpisodeItemsAdapter adapter = new EpisodeItemsAdapter(this, episodes);
		setListAdapter(adapter);

	}
	


	private static class EpisodeItemsAdapter extends BaseAdapter {
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
		
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
				
				final TextView textView = (TextView) convertView.findViewById(R.id.episode_item_text);
				final ImageButton downloadButton = (ImageButton) convertView.findViewById(R.id.episode_item_download);
				
				viewModel = new EpisodeItemViewModel(textView, downloadButton);
				convertView.setTag(viewModel);
			}else {
				viewModel = (EpisodeItemViewModel) convertView.getTag();
			}

			final EpisodeModel model = _models.get(position);
			final String name = model.getName();
			final Date date = model.getDate();
			final String dateString = DATE_FORMAT.format(date);
			
			final TextView textView = viewModel.getTextView();
			textView.setText(dateString + ": " + name);
			textView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final String asxUrl = model.getAsxUrl();

					if(asxUrl == null) {
						new AlertDialog.Builder(_context)
						.setMessage("網上直播完畢稍後提供節目重溫。")
						.setPositiveButton("確定", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface arg0, final int arg1) {

							}

						})
						.show();
						return;
					}

					final Intent intent = new Intent(_context, AsxActivity.class);
					final Uri uri = Uri.parse(asxUrl);
					intent.setData(uri);

					_context.startActivity(intent);
					
				}
			});       
			
			final ImageButton downloadButton = viewModel.getDownloadButton();
			
			downloadButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Toast.makeText(_context, model.getName(), Toast.LENGTH_SHORT);
				}
			});

			return convertView;
		}

		class EpisodeItemViewModel {
			private final TextView _textView;
			private final ImageButton _downloadButton;

			EpisodeItemViewModel(final TextView textView, final ImageButton downloadButton) {
				_textView = textView;
				_downloadButton = downloadButton;
			}

			public TextView getTextView() {
				return _textView;
			}

			public ImageButton getDownloadButton() {
				return _downloadButton;
			}
		}

	}

	
}
