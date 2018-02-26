package com.example.testagain;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;

public class MainActivity extends Activity implements OnClickListener,
		OnTouchListener {
	private static String TAG = MainActivity.class.getSimpleName();
	// 语音听写对象
	private SpeechRecognizer mIat;
	// 被监听按压事件的按钮
	private Button txButton;
	// 语音听写UI
	private RecognizerDialog mIatDialog;
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	// 文本框不一定要全局 为了方便我就全局了
	private EditText mResultText;
	// 保存听写参数的对象 参数是jar 里写好的
	private SharedPreferences mSharedPreferences;
	// 引擎类型TYPE_CLOUD为云端（我的理解）
	private String mEngineType = SpeechConstant.TYPE_CLOUD;
	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 默认发音人
	private String voicer = "xiaoyan";

	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		// 注册Appid
		// **************************************重要*************************************
		SpeechUtility.createUtility(MainActivity.this, "appid="
				+ getString(R.string.app_id));
		super.onCreate(savedInstanceState);

		// 规定没有标题
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 设置在哪个页面上
		setContentView(R.layout.activity_main);

		initLayout();
		// 初始化识别无UI识别对象
		// 使用SpeechRecognizer对象，可根据回调消息自定义界面；
		mIat = SpeechRecognizer.createRecognizer(MainActivity.this,
				mInitListener);

		// 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
		// 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
		mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
		mSharedPreferences = getSharedPreferences("com.iflytek.setting",
				Activity.MODE_PRIVATE);
		// 初始化mResultText
		mResultText = ((EditText) findViewById(R.id.iat_text));

		// 语音合成所需要用到的对象
		mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this,
				mTtsInitListener);
	}

	/**
	 * 初始化Layout界面。
	 */
	private void initLayout() {
		txButton = (Button) findViewById(R.id.button2);
		findViewById(R.id.button1).setOnClickListener(MainActivity.this);
		findViewById(R.id.button2).setOnTouchListener(MainActivity.this);
	}

	int ret = 0; // 函数调用返回值 主要是看在识别的过程中有没有出现错误

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		// 开始听写
		// 如何判断一次听写结束：OnResult isLast=true 或者 onError
		case R.id.button1:
			// 移动数据分析，收集开始听写事件，根据哪个控件来判断是否录音
			FlowerCollector.onEvent(MainActivity.this, "button1");
			// 清空显示内容
			mResultText.setText(null);
			mIatResults.clear();
			// 设置参数
			setRecParam();
			// 是否先显示对话框
			// 显示听写对话框
			mIatDialog.setListener(mRecognizerDialogListener);
			mIatDialog.show();
			showTip(getString(R.string.text_begin));
			// 不显示对话框 打开下面需要注释上面
			// ret = mIat.startListening(mRecognizerListener);
			// if (ret != ErrorCode.SUCCESS) {
			// showTip("听写失败,错误码：" + ret);
			// } else {
			// showTip(getString(R.string.text_begin));
			// }
			break;
		}
	}

	/**
	 * 初始化监听器。
	 * 
	 * **************************************重要*************************************
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code);
			}
		}
	};

	/**
	 * 听写监听器。
	 * 
	 * **************************************重要*************************************
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			// Tips：
			// 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
			// 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			Log.d(TAG, results.getResultString());
			printResult(results);

			if (isLast) {
				// TODO 最后的结果
			}
		}

		/**
		 * 音量发生变化
		 */
		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, "返回音频数据：" + data.length);
		}

		/**
		 * 云端出错联系技术人员使用该方法
		 */
		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			/*
			 * if (SpeechEvent.EVENT_SESSION_ID == eventType) { String sid =
			 * obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID); Log.d(TAG,
			 * "session id =" + sid); }
			 */
		}
	};

	/**
	 * 打印出语音转换后的汉字
	 * 
	 * @param results
	 */
	private void printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}

		mResultText.setText(resultBuffer.toString());
		mResultText.setSelection(mResultText.length());
		// 将打印的结果读出来
		readResult();
	}

	/**
	 * 听写UI监听器
	 * 
	 * 
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			printResult(results);
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

	};

	/**
	 * 初始化监听。
	 * 
	 * **************************************重要*************************************
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码：" + code);
			} else {
				// 初始化成功，之后可以调用startSpeaking方法
				// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
				// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

	/**
	 * 合成回调监听。
	 * **************************************重要*************************************
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			// mPercentForBuffering = percent;
			// showTip(String.format(getString(R.string.tts_toast_format),
			// mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			// mPercentForPlaying = percent;
			// showTip(String.format(getString(R.string.tts_toast_format),
			// mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			// if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			// String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			// Log.d(TAG, "session id =" + sid);
			// }
		}
	};

	/**
	 * 读出翻译结果
	 */
	private void readResult() {
		String text = ((EditText) findViewById(R.id.iat_text)).getText()
				.toString();
		;
		setVecParam();
		int code = mTts.startSpeaking(text, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			showTip("语音合成失败,错误码: " + code);
		}
	}

	/**
	 * 弹框提示用户信息
	 * 
	 * @param str
	 */
	private void showTip(final String str) {
		EditText ed = (EditText) findViewById(R.id.iat_text);
		ed.setText(str);
	}

	/**
	 * 听写参数设置
	 * 
	 * **************************************重要*************************************
	 * @return
	 */
	public void setRecParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		String lag = mSharedPreferences.getString("iat_language_preference",
				"mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT, lag);
		}

		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS,
				mSharedPreferences.getString("iat_vadbos_preference", "4000"));

		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS,
				mSharedPreferences.getString("iat_vadeos_preference", "1000"));

		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT,
				mSharedPreferences.getString("iat_punc_preference", "1"));

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
				Environment.getExternalStorageDirectory() + "/msc/iat.wav");
	}

	/**
	 * 合成参数设置
	 * 
	 * **************************************重要*************************************
	 */
	private void setVecParam() {
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置在线合成发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
		// 设置合成语速
		mTts.setParameter(SpeechConstant.SPEED,
				mSharedPreferences.getString("speed_preference", "50"));
		// 设置合成音调
		mTts.setParameter(SpeechConstant.PITCH,
				mSharedPreferences.getString("pitch_preference", "50"));
		// 设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME,
				mSharedPreferences.getString("volume_preference", "50"));
		// 设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE,
				mSharedPreferences.getString("stream_preference", "3"));
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
				Environment.getExternalStorageDirectory() + "/msc/tts.wav");
	}

	
	/*
	 * **************************************一下内容Demo里有我直接Copy的*************************************
	 * */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出时释放连接
		mIat.cancel();
		mIat.destroy();
	}

	@Override
	protected void onResume() {
		// 开放统计 移动数据统计分析
		FlowerCollector.onResume(MainActivity.this);
		FlowerCollector.onPageStart(TAG);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// 开放统计 移动数据统计分析
		FlowerCollector.onPageEnd(TAG);
		FlowerCollector.onPause(MainActivity.this);
		super.onPause();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.button2:
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				txButton.setBackgroundResource(R.drawable.grean);
				// mIat.stopListening();
				mIat.startListening(mRecognizerListener);
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				txButton.setBackgroundResource(R.drawable.huise);
				mRecognizerListener.onEndOfSpeech();
			}
			break;

		default:
			break;
		}
		return false;
	}
}
