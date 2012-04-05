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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.widget.actionBar.ActionBarListActivity;

public class SearchActivity extends ActionBarListActivity {
	private static final String SEARCH_URL = "http://search.rthk.org.hk/search/search_archive_2010.php?archivetype=all&keyword=";

	private AlertDialog _searchDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final LayoutInflater factory = LayoutInflater.from(this);
		final View dialogView = factory.inflate(R.layout.searh_dialog, null);
		_searchDialog = new AlertDialog.Builder(this)
		.setTitle("搜尋")
		.setView(dialogView)
		.setPositiveButton("搜尋", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				final EditText editText = (EditText) dialogView.findViewById(R.id.search_field);
				final String keywords = editText.getText().toString();

				if(keywords.equals("")) {
					return;
				}

				final List<ProgrammeModel> programmes = new ArrayList<ProgrammeModel>();
				final String query = URLEncoder.encode(keywords);
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
					e.printStackTrace();

					new AlertDialog.Builder(SearchActivity.this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle("搜尋失敗")
					.setMessage("請檢查裝置是否連接到互聯網。")
					.setPositiveButton("確定", new OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {

						}

					})
					.create()
					.show();
				}

				final ProgrammeItemsAdapter adapter = new ProgrammeItemsAdapter(SearchActivity.this, programmes);
				setListAdapter(adapter);
			}

		})
		.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {

				/* User clicked cancel so do some stuff */
			}
		})
		.create();

		_searchDialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.search_menu, menu);

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == R.id.menu_search) {
			_searchDialog.show();
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
				final ImageView starImageView = (ImageView) convertView.findViewById(R.id.programme_item_star);
				
				viewModel = new ProgrammeItemViewModel(textView, starImageView);
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
			
			final ImageView starImageView = viewModel.getStarImageView();
	    	
			if(starred) {
	    		starImageView.setImageResource(R.drawable.programme_item_star_filled);
	    	}
	    	else {
	    		starImageView.setImageResource(R.drawable.programme_item_star_empty);
	    	}
			
			starImageView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final boolean starred = model.isStarred();
					model.setStarred(!starred);
					if(starred) {
			    		starImageView.setImageResource(R.drawable.programme_item_star_empty);
			    	}
			    	else {
			    		starImageView.setImageResource(R.drawable.programme_item_star_filled);
			    	}
					
				}
			});

			return convertView;
		}

		class ProgrammeItemViewModel {
			private final TextView _textView;
			private final ImageView _starImageView;

			ProgrammeItemViewModel(final TextView textView, final ImageView starImageView) {
				_textView = textView;
				_starImageView = starImageView;
			}

			public TextView getTextView() {
				return _textView;
			}

			public ImageView getStarImageView() {
				return _starImageView;
			}
		}

	}

}
