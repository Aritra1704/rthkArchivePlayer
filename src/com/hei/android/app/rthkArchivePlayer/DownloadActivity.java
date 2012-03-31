package com.hei.android.app.rthkArchivePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import android.app.Activity;
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

import com.hei.android.app.rthkArchivePlayer.player.asfPlayer.AsfFileOutputStream;
import com.hei.android.app.rthkArchivePlayer.player.mmsPlayer.MMSInputStream;

public class DownloadActivity extends Activity {
	private static final String TAG = "DownloadActivity";

	private TextView _infoText;
	private ProgressBar _progressBar;
	private TextView _progressText;
	private DownloadMessageHandler _handler;
	private WifiLock _wifiLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloader);

		_infoText = (TextView) findViewById(R.id.downloadText);
		_progressBar = (ProgressBar) findViewById(R.id.downloadProgressBar);
		_progressText = (TextView) findViewById(R.id.downloadProgressText);

		_progressBar.setMax(100);

		_handler = new DownloadMessageHandler(_infoText, _progressBar, _progressText);

		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, DownloadActivity.class.getName());

		final Intent intent = getIntent();
		final String[] playlist = intent.getStringArrayExtra(getString(R.string.key_playlist));
		download(playlist);
	}

	private void download(final String[] playlist) {
		new Downloader(this, playlist, 10, _wifiLock, _handler).start();
	}

	private static class DownloadMessageHandler extends Handler {
		private static enum Type {
			INFO_UPDATE,
			PROGRESS_UPDATE
		}

		private static final String INFO_TRACK_URL = "INFO_TRACK_URL";
		private static final String INFO_TRACK_TOTAL = "INFO_TRACK_TOTAL";
		private static final String INFO_TRACK_NUM = "INFO_TRACK_NUM";
		private static final String INFO_TRACK_SIZE = "INFO_TRACK_SIZE";
		private static final String PROGRESS = "PROGRESS";

		public static Message createInfoUpdateMessage(final String url, final int trackNum, final int trackTotal, final long trackSize) {
			final Message msg = new Message();
			msg.what = Type.INFO_UPDATE.ordinal();

			final Bundle data = new Bundle();
			data.putString(INFO_TRACK_URL, url);
			data.putInt(INFO_TRACK_NUM, trackNum);
			data.putInt(INFO_TRACK_TOTAL, trackTotal);
			data.putLong(INFO_TRACK_SIZE, trackSize);
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

		private final TextView _infoText;
		private final ProgressBar _progressBar;
		private final TextView _progressText;
		private long _progress;
		private long _size;

		public DownloadMessageHandler(final TextView infoText, final ProgressBar progressBar, final TextView progressText) {
			super();
			_infoText = infoText;
			_progressBar = progressBar;
			_progressText = progressText;
		}

		@Override
		public void handleMessage(final Message msg) {
			final Type[] types = Type.values();
			final Type msgType = types[msg.what];
			final Bundle data = msg.getData();

			switch (msgType) {
			case INFO_UPDATE:
				final int trackNum = data.getInt(INFO_TRACK_NUM);
				final int trackTotal = data.getInt(INFO_TRACK_TOTAL);
				final long trackSize = data.getLong(INFO_TRACK_SIZE);
				final String url = data.getString(INFO_TRACK_URL);
				_infoText.setText("Downloading " + url + " (" + trackNum + "/"  + trackTotal + ")");
				_size = trackSize;
				_progress = 0;
				break;

			case PROGRESS_UPDATE:
				final long progress = data.getLong(PROGRESS);
				_progress += progress;
				final int currentProgress = (int) (_progress * 100 / _size);
				_progressBar.setProgress(currentProgress);
				_progressText.setText(currentProgress + "%");
				break;
			}
		}

		public void sendInfoUpdateMessage(final String url, final int trackNum, final int trackTotal, final long trackSize) {
			final Message msg = createInfoUpdateMessage(url, trackNum, trackTotal, trackSize);
			sendMessage(msg);
		}

		public void sendProgressUpdateMessage(final long progress) {
			final Message msg = createProgressUpdateMessage(progress);
			sendMessage(msg);
		}
	}

	protected static class Downloader extends Thread {
		private final Context _context;
		private final String[] _playlist;
		private final int _threadNum;
		private final WifiLock _wifiLock;
		private final DownloadMessageHandler _handler;
		private int _threadFinishedCount;

		public Downloader (Context context, String[] playlist, int threadNum, WifiLock wifiLock, DownloadMessageHandler handler) {
			super("DownloadActivity.Downloader");
			_context = context;
			_playlist = playlist;
			_threadNum = threadNum;
			_wifiLock = wifiLock;
			_handler = handler;
		}

		@Override
		public void run() {
			_wifiLock.acquire();
			for (int i = 0; i<_playlist.length; i++) {
				_threadFinishedCount = 0;
				final String url = _playlist[i];
				try {
					final MMSInputStream mmsStream = new MMSInputStream(url);
					final long size = mmsStream.getSize();

					final List<AsfFileOutputStream> files;
					final String filePath;
					try {
						final String externalStorageState = Environment.getExternalStorageState();
						if(externalStorageState.equals(Environment.MEDIA_REMOVED) ) {
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

					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
						return;
					}

					_handler.sendInfoUpdateMessage(url, i+1, _playlist.length, size);

					int streamNum = 0;
					final long stepSize = size / _threadNum;
					final long[] readCaps = new long[_threadNum];
					final long[] startPos = new long[_threadNum];
					for (int j = 0; j < readCaps.length; j++) {
						readCaps[j] = -1;
					}
					
					for (final AsfFileOutputStream file : files) {
						final MMSInputStream stream;
						if(streamNum == 0) {
							stream = mmsStream;
							startPos[streamNum] = 0;
						}
						else {
							stream = new MMSInputStream(url);
							final long pos = stream.seekByte(stepSize * streamNum);
							startPos[streamNum] = pos;
							readCaps[streamNum - 1] = pos - startPos[streamNum - 1];
						}
						
						final int thisStreamNum = streamNum;

						if(streamNum == _threadNum - 1) {
							readCaps[thisStreamNum] = size * 2;
						}

						new Thread("DownloadTread-" + streamNum) {
							public void run() {
								final int bufferSize = 1024;
								final byte[] buf = new byte[bufferSize];
								int readlen;
								int readSize = bufferSize;
								long total = 0;
								try {
									while ((readlen = stream.read(buf, 0, readSize)) != 0) {
										file.write(buf, 0, readlen);
										_handler.sendProgressUpdateMessage(readlen);
										total += readlen;
										
										final long cap = readCaps[thisStreamNum];

										if(cap > 0) {
											if (total >= cap) {
												break;
											}

											final int remain = (int) (cap - total);
											if(remain < bufferSize) {
												readSize = remain;
											}
										}
									}
								} catch (IOException e) {
									e.printStackTrace();
								} finally {
									try {
										stream.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								
								synchronized (Downloader.this) {
									_threadFinishedCount++;
									Downloader.this.notify();
								}
							};
						}.start();
						streamNum++;
					}
					

					synchronized (this) {
						while (_threadFinishedCount < _threadNum) {
							try {
								wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						final List<String> pathName = AsfFileOutputStream.getPathName(filePath, _threadNum);
						FileOutputStream baseFile = null;
						for (String path : pathName) {
							if(baseFile == null) {
								baseFile = new FileOutputStream(filePath, true); 
							}
							else {
								try {
									final File file = new File(path);
									final FileInputStream inputStream = new FileInputStream(file);
									byte[] buffer = new byte[1024];
									int read = 0;
									while ((read = inputStream.read(buffer)) > 0) {
										baseFile.write(buffer, 0, read);
									}
									inputStream.close();
									file.delete();
								} catch (IOException e) {
									Log.e(TAG, e.getMessage());
								} 
							}
						}
						baseFile.close();
						
					}
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}
				
			}
			_wifiLock.release();
		}
	}
}
