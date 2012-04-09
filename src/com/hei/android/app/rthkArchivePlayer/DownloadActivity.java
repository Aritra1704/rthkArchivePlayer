package com.hei.android.app.rthkArchivePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.AsxModel;
import com.hei.android.app.rthkArchivePlayer.model.AsxModel.AsxEntryModel;
import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.player.asfPlayer.AsfFileOutputStream;
import com.hei.android.app.rthkArchivePlayer.player.mmsPlayer.MMSInputStream;
import com.hei.android.app.widget.actionBar.ActionBarActivity;

public class DownloadActivity extends ActionBarActivity {
	private static final String TAG = "DownloadActivity";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

	private TextView _downloadingText;
	private TextView _infoText;
	private ProgressBar _progressBar;
	private TextView _progressText;
	private DownloadMessageHandler _handler;
	private Timer _downloadingTextTimer;
	private WifiLock _wifiLock;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloader);

		_downloadingText = (TextView) findViewById(R.id.downloadDownloadingText);
		_infoText = (TextView) findViewById(R.id.downloadInfoText);
		_progressBar = (ProgressBar) findViewById(R.id.downloadProgressBar);
		_progressText = (TextView) findViewById(R.id.downloadProgressText);

		_progressBar.setMax(100);

		_handler = new DownloadMessageHandler(this);

		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, DownloadActivity.class.getName());

		scheduleDownloadingTextAnimation();

		final Intent intent = getIntent();
		final EpisodeModel episode = (EpisodeModel) intent.getSerializableExtra(getString(R.string.key_episode));

		final String programmeName = episode.getProgrammeName();
		final String name = episode.getName();
		final Date date = episode.getDate();
		final String dateStr = DATE_FORMAT.format(date);
		setTitle(getString(R.string.title_download) + " - " + programmeName);
		_infoText.setText(programmeName + " " + dateStr + "\n" + name);

		final String asxUrl = episode.getAsxUrl();
		new Downloader(this, asxUrl, 10, _wifiLock, _handler).start();
	}

	private void scheduleDownloadingTextAnimation() {
		_downloadingTextTimer = new Timer("DownloadingTextAnimation");
		_downloadingTextTimer.scheduleAtFixedRate(new TimerTask() {
			private final int MAX_DOT_NUM = 3;
			private int dotNum = 0;


			@Override
			public void run() {
				final StringBuilder downloadingText = new StringBuilder(getString(R.string.download_downloading_text));
				for(int i=0;  i<dotNum; i++) {
					downloadingText.append('.');
				}
				for(int i=dotNum;  i<MAX_DOT_NUM; i++) {
					downloadingText.append(' ');
				}

				_handler.sendDownloadingTextUpdateMessage(downloadingText.toString());

				dotNum = (dotNum + 1) % (MAX_DOT_NUM + 1);

			}
		}, 0, 1000);
	}

	private static class DownloadMessageHandler extends Handler {
		private static enum Type {
			DOWNLOADING_TEXT_UPDATE,
			TOTAL_SIZE_UPDATE,
			PROGRESS_UPDATE,
			DOWNLOAD_SUCCESS,
			DOWNLOAD_FAILED
		}

		private static final String DOWNLOADING_TEXT = "DOWNLOADING_TEXT";
		private static final String TOTAL_SIZE = "TOTAL_SIZE";
		private static final String PROGRESS = "PROGRESS";

		public static Message createDownloadingTextUpdateMessage(final String text) {
			final Message msg = new Message();
			msg.what = Type.DOWNLOADING_TEXT_UPDATE.ordinal();

			final Bundle data = new Bundle();
			data.putString(DOWNLOADING_TEXT, text);
			msg.setData(data);

			return msg;
		}
		public static Message createTotalSizeUpdateMessage(final long size) {
			final Message msg = new Message();
			msg.what = Type.TOTAL_SIZE_UPDATE.ordinal();

			final Bundle data = new Bundle();
			data.putLong(TOTAL_SIZE, size);
			msg.setData(data);

			return msg;
		}

		public static Message createProgressUpdateMessage(final long progress) {
			final Message msg = new Message();
			msg.what = Type.PROGRESS_UPDATE.ordinal();

			final Bundle data = new Bundle();
			data.putLong(PROGRESS, progress);
			msg.setData(data);

			return msg;
		}

		public static Message createDownloadSuccessMessage() {
			final Message msg = new Message();
			msg.what = Type.DOWNLOAD_SUCCESS.ordinal();

			return msg;
		}

		public static Message createDownloadFailedMessage() {
			final Message msg = new Message();
			msg.what = Type.DOWNLOAD_SUCCESS.ordinal();

			return msg;
		}

		private final DownloadActivity _activity;
		private long _progress;
		private long _size;

		public DownloadMessageHandler(final DownloadActivity activity) {
			super();
			_activity = activity;
			_size = -1L;
		}

		@Override
		public void handleMessage(final Message msg) {
			final Type[] types = Type.values();
			final Type msgType = types[msg.what];
			final Bundle data = msg.getData();

			switch (msgType) {
			case DOWNLOADING_TEXT_UPDATE:
				final String downloadingText = data.getString(DOWNLOADING_TEXT);
				_activity._downloadingText.setText(downloadingText);
				break;

			case TOTAL_SIZE_UPDATE:
				final long totalSize = data.getLong(TOTAL_SIZE);
				_size = totalSize;
				break;

			case PROGRESS_UPDATE:
				final long progress = data.getLong(PROGRESS);
				_progress += progress;
				final int currentProgress = (int) (_progress * 100 / _size);
				_activity._progressBar.setProgress(currentProgress);
				_activity._progressText.setText(currentProgress + "%");
				break;

			case DOWNLOAD_SUCCESS:
				_activity._downloadingTextTimer.cancel();
				_activity._downloadingText.setText(_activity.getString(R.string.download_success));
				break;

			case DOWNLOAD_FAILED:
				_activity._downloadingTextTimer.cancel();
				_activity._downloadingText.setText(_activity.getString(R.string.download_failed));
				break;
			}
		}

		public void sendDownloadingTextUpdateMessage(final String text) {
			final Message msg = createDownloadingTextUpdateMessage(text);
			sendMessage(msg);
		}

		public void sendTotalSizeUpdateMessage(final long totalSize) {
			final Message msg = createTotalSizeUpdateMessage(totalSize);
			sendMessage(msg);
		}

		public void sendProgressUpdateMessage(final long progress) {
			final Message msg = createProgressUpdateMessage(progress);
			sendMessage(msg);
		}

		public void sendDownloadSuccessMessage() {
			final Message msg = createDownloadSuccessMessage();
			sendMessage(msg);
		}

		public void sendDownloadFailedMessage() {
			final Message msg = createDownloadFailedMessage();
			sendMessage(msg);
		}
	}

	protected static class Downloader extends Thread {
		private final Context _context;
		private final String _asxUrl;
		private final int _threadNum;
		private final WifiLock _wifiLock;
		private final DownloadMessageHandler _handler;
		private int _threadFinishedCount;

		public Downloader (final Context context, final String asxUrl, final int threadNum, final WifiLock wifiLock, final DownloadMessageHandler handler) {
			super("DownloadActivity.Downloader");
			_context = context;
			_asxUrl = asxUrl;
			_threadNum = threadNum;
			_wifiLock = wifiLock;
			_handler = handler;
		}

		@Override
		public void run() {
			_wifiLock.acquire();
			final AsxModel asx = AsxModel.createModelFromUrl(_asxUrl);
			final List<AsxEntryModel> entries = asx.getEntries();
			final String[] playlist = new String[entries.size()];
			for (int i = 0; i<playlist.length; i++) {
				final AsxEntryModel entry = entries.get(i);
				final String ref = entry.getRef();
				playlist[i] = ref;
			}


			long totalSize = 10L;
			for (final String url : playlist) {
				try {
					final MMSInputStream mmsStream = new MMSInputStream(url);
					totalSize += mmsStream.getSize();
					mmsStream.close();
				} catch (final Exception e) {
					Log.e(TAG, e.getMessage());
					_handler.sendDownloadFailedMessage();
					_wifiLock.release();
					return;
				}
			}
			_handler.sendTotalSizeUpdateMessage(totalSize);

			for (final String url : playlist) {
				_threadFinishedCount = 0;
				try {
					final MMSInputStream mmsStream = new MMSInputStream(url);
					final long size = mmsStream.getSize();

					final List<AsfFileOutputStream> files;
					final String filePath;
					try {
						final String externalStorageState = Environment.getExternalStorageState();
						if(externalStorageState.equals(Environment.MEDIA_REMOVED) ) {
							_handler.sendDownloadFailedMessage();
							_wifiLock.release();
							return;
						}
						final File sdCardRoot = Environment.getExternalStorageDirectory();
						final String sdCardPath = sdCardRoot.getPath();
						final File rthkFolder = new File( sdCardPath + "/RthkArchivePlayer" );

						if(!rthkFolder.exists() ) {
							rthkFolder.mkdirs();
						}

						final String rthkPath = rthkFolder.getPath();
						final String filename = URLEncoder.encode(url);
						final double length = mmsStream.getLength();
						filePath = rthkPath + "/" + filename;
						files = AsfFileOutputStream.createAsfFileOutputStreams(_context,
								url, filePath,length, size, _threadNum);

					} catch (final Exception e) {
						Log.e(TAG, e.getMessage());
						_handler.sendDownloadFailedMessage();
						_wifiLock.release();
						return;
					}

					int streamNum = 0;
					final long stepSize = size / _threadNum;

					long startPos = -1L;
					long readCap = -1L;
					DownloadThread downloadThread = null;

					for (final AsfFileOutputStream file : files) {
						final MMSInputStream stream;
						if(streamNum == 0) {
							stream = mmsStream;
							startPos = 0L;
						}
						else {
							stream = new MMSInputStream(url);
							final long pos = stream.seekByte(stepSize * streamNum);
							readCap = pos - startPos;
							downloadThread.setReadCapacity(readCap);
							downloadThread.start();
							startPos = pos;
						}

						downloadThread = new DownloadThread(stream, file, streamNum);

						streamNum++;

						if(streamNum == _threadNum) {
							downloadThread.setReadCapacity(size * 2);
							downloadThread.start();
						}
					}


					synchronized (this) {
						while (_threadFinishedCount < _threadNum) {
							try {
								wait();
							} catch (final InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					final List<String> pathName = AsfFileOutputStream.getPathName(filePath, _threadNum);
					FileOutputStream baseFile = null;
					for (final String path : pathName) {
						if(baseFile == null) {
							baseFile = new FileOutputStream(filePath, true);
						}
						else {
							try {
								final File file = new File(path);
								final FileInputStream inputStream = new FileInputStream(file);
								final byte[] buffer = new byte[1024];
								int read = 0;
								while ((read = inputStream.read(buffer)) > 0) {
									baseFile.write(buffer, 0, read);
								}
								inputStream.close();
								file.delete();
							} catch (final IOException e) {
								Log.e(TAG, e.getMessage());
							}
						}
					}
					baseFile.close();


				} catch (final IOException e) {
					Log.e(TAG, e.getMessage());
					_handler.sendDownloadFailedMessage();
					_wifiLock.release();
					return;
				}

			}
			_wifiLock.release();
			_handler.sendDownloadSuccessMessage();
		}

		private class DownloadThread extends Thread {
			private final MMSInputStream _stream;
			private final AsfFileOutputStream _file;
			private long _readCap;

			private DownloadThread(final MMSInputStream stream, final AsfFileOutputStream file, final int streamNum) {
				super("DownloadThread" + streamNum);
				_stream = stream;
				_file = file;
				_readCap = -1L;
			}

			public void setReadCapacity(final long capacity) {
				_readCap = capacity;
			}

			@Override
			public void run() {
				final int bufferSize = 1024;
				final byte[] buf = new byte[bufferSize];
				int readlen;
				int readSize = bufferSize;
				long total = 0;
				try {
					while ((readlen = _stream.read(buf, 0, readSize)) != 0) {
						_file.write(buf, 0, readlen);
						_handler.sendProgressUpdateMessage(readlen);
						total += readlen;

						if (total >= _readCap) {
							break;
						}

						final int remain = (int) (_readCap - total);
						if(remain < bufferSize) {
							readSize = remain;
						}
					}
				} catch (final IOException e) {
					e.printStackTrace();
				} finally {
					try {
						_stream.close();
					} catch (final Exception e) {
						e.printStackTrace();
					}
					try {
						_file.close();
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				synchronized (Downloader.this) {
					_threadFinishedCount++;
					Downloader.this.notify();
				}
			}
		}
	}
}
