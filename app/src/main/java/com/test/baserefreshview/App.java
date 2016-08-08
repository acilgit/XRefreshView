package com.test.baserefreshview;

import android.app.Application;
import android.support.v7.app.AlertDialog;

import com.alibaba.fastjson.JSONObject;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.xycode.xylibrary.base.BaseActivity;
import com.xycode.xylibrary.okHttp.Header;
import com.xycode.xylibrary.okHttp.OkHttp;
import com.xycode.xylibrary.okHttp.Param;
import com.xycode.xylibrary.utils.TS;
import com.xycode.xylibrary.utils.downloadHelper.DownloadHelper;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by XY on 2016/7/9.
 */
public class App extends Application {

    private static App instance;

    public static App getInstance() {
        if (instance == null) {
            instance = new App();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (instance == null) {
            instance = this;
        } else {
            return;
        }

      /*  TS.init(this, new TS.IToastLayoutSetter() {
            @Override
            public void onToastLayout(View root, Toast toast) {

            }
        });*/
        TS.init(this);
        Fresco.initialize(this);
        OkHttp.init(new OkHttp.IOkInit() {
            @Override
            public int judgeResultWhenFirstReceivedResponse(Call call, Response response, JSONObject json) {
                String resultCode = json.getString("resultCode");
                if ("1".equals(resultCode)) {
                    return OkHttp.RESULT_SUCCESS;
                } else if ("0".equals(resultCode)) {
                    return OkHttp.RESULT_ERROR;
                } else if ("-1".equals(resultCode)) {
                    return OkHttp.RESULT_VERIFY_ERROR;
                }
                return OkHttp.RESULT_OTHER;
            }

            @Override
            public void networkError(Call call, boolean isCanceled) {

            }

            @Override
            public void receivedNetworkErrorCode(Call call, Response response) {
                TS.show(R.string.ts_no_network);
            }

            @Override
            public boolean resultSuccessByJudge(Call call, Response response, JSONObject json, int resultCode) {
                switch (resultCode) {
                    case OkHttp.RESULT_VERIFY_ERROR:

                        return true;
                }
                return false;
            }

            @Override
            public void judgeResultParseResponseFailed(Call call, Response response) {

            }

            @Override
            public Param setDefaultParams(Param defaultParams) {
                defaultParams.put("sectionId", "");
                return defaultParams;
            }

            @Override
            public Header setDefaultHeader(Header defaultHeader) {
                return defaultHeader;
            }

        });
        OkHttp.setMaxTransFileCount(2);

        DownloadHelper.init(this, "现在更新", "暂不更新", "正在下载中","取消", new DownloadHelper.OnShowDownloadDialog() {
            @Override
            public AlertDialog.Builder getConfirmDialogBuilder() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.getForegroundActivity(instance));
                builder.setTitle( "哈哈哈哈").setMessage("来来High！");
                return builder;
            }

            @Override
            public AlertDialog.Builder getProgressDialogBuilder() {
                return null;
            }
        });
        DownloadHelper.getInstance().setOnProgressListener(new DownloadHelper.OnProgressListener() {
            @Override
            public void onFileLength(long length) {

            }

            @Override
            public void onStep(long downloadedLength) {

            }

            @Override
            public void onFinish() {

            }

            @Override
            public void onFailure() {
                TS.show("下载失败");
            }
        });

  /*      XRefresher.setCustomerFooterView(R.layout.layout_load_more, new XAdapter.ICustomerFooter() {
            @Override
            public void bindFooter(XAdapter.CustomHolder holder, int footerState) {
                switch (footerState) {
                    case XAdapter.FOOTER_LOADING:
                        holder.setText(R.id.tvLoading, R.string.app_name);
                        holder.getView(R.id.pbLoadMore).setVisibility();
                        break;
                    default:
                        break;
                }
            }
        });*/
    }
}
