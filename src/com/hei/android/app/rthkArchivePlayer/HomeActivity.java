package com.hei.android.app.rthkArchivePlayer;

import android.os.Bundle;
import android.widget.Button;

import com.hei.android.app.widget.actionBar.ActionBarActivity;

public class HomeActivity extends ActionBarActivity {
	private Button _searchButton;
	private Button _starredButton;
	private Button _downloadsButton;
	private Button _historyButton;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
        initButtons();
    }
	
	private void initButtons() {
		_searchButton = (Button) findViewById(R.id.home_btn_search);
		_starredButton = (Button) findViewById(R.id.home_btn_starred);
		_downloadsButton = (Button) findViewById(R.id.home_btn_downloads);
		_historyButton = (Button) findViewById(R.id.home_btn_history);
	
		_searchButton.setOnClickListener(new StartActivityOnClickListener(SearchActivity.class));
		_starredButton.setOnClickListener(new StartActivityOnClickListener(StarredActivity.class));
		_downloadsButton.setOnClickListener(new StartActivityOnClickListener(null)); //TODO: add starred activity
		_historyButton.setOnClickListener(new StartActivityOnClickListener(HistoryActivity.class));
	}
}
