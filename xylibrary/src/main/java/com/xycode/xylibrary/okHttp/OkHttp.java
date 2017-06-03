package com.xycode.xylibrary.okHttp;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xycode.xylibrary.base.BaseActivity;
import com.xycode.xylibrary.utils.L;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Created by XY on 2016/7/7.
 */
public class OkHttp {

    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_TYPE_URL_ENCODED = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    public static final MediaType MEDIA_TYPE_MULTI_DATA = MediaType.parse("multipart/form-data; charset=utf-8");

    public static final String FILE = "file";
    public static final byte[] lock = new byte[0];

    public static final int RESULT_ERROR = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_VERIFY_ERROR = -1;
    public static final int RESULT_OTHER = 2;

    public static final int RESULT_PARSE_FAILED = 880;
    public static final int NETWORK_ERROR_CODE = 881;
    public static final int NO_NETWORK = 882;


    private static OkHttpClient client;
    private static IOkInit okInit;
    private static OkHttp.OkOptions okOptions;

    private static OkHttp instance;
    private static Application application;

    public static OkHttp getInstance() {
        if (instance == null) {
            instance = new OkHttp();
        }
        return instance;
    }

    /**
     * init
     */
    public static void init(Application app, IOkInit iOkInit) {
        if (okInit == null) {
            application = app;
            okInit = iOkInit;
        }
    }

    public static void init(Application app, IOkInit iOkInit, OkOptions okOptions) {
        if (okInit == null) {
            application = app;
            okInit = iOkInit;
            OkHttp.okOptions = okOptions;
        }
    }

    public static void setMaxTransFileCount(int max) {
        getClient().dispatcher().setMaxRequestsPerHost(max);
    }

    public static OkHttpClient getClient() {
        synchronized (lock) {
            if (client == null) {
                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .readTimeout(OkOptions.readTimeout, TimeUnit.SECONDS)
                        .connectTimeout(OkOptions.connectTimeout, TimeUnit.SECONDS)
                        .writeTimeout(OkOptions.writeTimeout, TimeUnit.SECONDS);
                if (okOptions != null) {
                    // 添加证书 或处理Builder
                    okOptions.setOkHttpBuilder(builder);
                }
                client = builder.build();
            }
            return client;
        }
    }

    public static RequestBody setFormBody(Param params) {
        return setFormBody(params, true);
    }

    public static RequestBody setFormBody(Param params, boolean addDefaultParams) {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : params.keySet()) {
            builder.add(key, params.get(key));
        }
        if (addDefaultParams) {
            Param defaultParams = okInit.setDefaultParams(new Param());
            for (String key : defaultParams.keySet()) {
                builder.add(key, defaultParams.get(key));
            }
        }
        return builder.build();
    }

    public static Call get(Activity activity, String url, boolean addDefaultHeader, OkResponseListener okResponseListener) {
        return get(activity, url, null, addDefaultHeader, okResponseListener);
    }

    public static Call get(Activity activity, String url, Header header, boolean addDefaultHeader, final OkResponseListener okResponseListener) {
        return postOrGet(activity, url, null, header, addDefaultHeader, okResponseListener);
    }

    public static Call postForm(Activity activity, String url, @NonNull RequestBody body, OkResponseListener okResponseListener) {
        return postForm(activity, url, body, null, true, okResponseListener);
    }

    public static Call postForm(Activity activity, String url, @NonNull RequestBody body, boolean addDefaultHeader, OkResponseListener okResponseListener) {
        return postForm(activity, url, body, null, addDefaultHeader, okResponseListener);
    }

    public static Call postForm(Activity activity, String url, @NonNull RequestBody body, Header header, boolean addDefaultHeader, OkResponseListener okResponseListener) {
        return postOrGet(activity, url, body, header, addDefaultHeader, okResponseListener);
    }

  /*  public static  Call postForm(Activity activity, String url, RequestBody body, Header header, boolean addDefaultHeader, final OkResponseListener okResponseListener) {
        return postOrGet(activity, url, body, header, addDefaultHeader, okResponseListener);
    }*/

    /**
     * @param url
     * @param body               when body isNull will use GET
     * @param header
     * @param addDefaultHeader
     * @param okResponseListener
     * @return
     */
    private static Call postOrGet(final Activity activity, String url, final RequestBody body, final Header header, boolean addDefaultHeader, final OkResponseListener okResponseListener) {
        final Request.Builder builder = new Request.Builder().url(url);

        if (body != null) {
            if (OkOptions.mediaType != null) {
//                body.contentType() =
            }
            builder.post(body);
        } else {
            builder.get();
        }
        if (addDefaultHeader) {
            Header defaultHeader = okInit.setDefaultHeader(new Header());
            for (String key : defaultHeader.keySet()) {
                builder.addHeader(key, defaultHeader.get(key));
            }
        }
        if (header != null) {
            for (String key : header.keySet()) {
                builder.addHeader(key, header.get(key));
            }
        }
        final Request request = builder.build();
        final Call call = getClient().newCall(request);

        new Thread(() -> {
            try {
                final Response response = call.execute();
                L.e("call cancel --> " + call.isCanceled());
                if (response != null) {
                    responseResult(response, call, okResponseListener, activity);
                    response.close();
                } else {
                    responseResultFailure(call, okResponseListener, activity);
                }
            } catch (IOException e) {
                e.printStackTrace();
                responseResultFailure(call, okResponseListener, activity);
            } finally {
            }
        }).start();

        return call;
    }

    /**
     * upload file，you can setMaxTransFileCount() to set max files upload thread pool size
     *
     * @param url
     * @param file
     * @param okResponseListener
     * @param fileProgressListener
     */
    public static Call uploadFile(Activity activity, String url, String fileKey, File file, Param param, final OkResponseListener okResponseListener, OkFileHelper.FileProgressListener fileProgressListener) {
        Map<String, File> files = new HashMap<>();
        files.put(fileKey, file);
        return uploadFiles(activity, url, files, param, okResponseListener, fileProgressListener);
    }

    public static Call uploadFiles(final Activity activity, String url, Map<String, File> files, Param param, final OkResponseListener okResponseListener, OkFileHelper.FileProgressListener fileProgressListener) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if (param != null) {
            for (String key : param.keySet()) {
                builder.addFormDataPart(key, param.get(key));
            }
        }
        for (String key : files.keySet()) {
            builder.addFormDataPart(key, files.get(key).getName(), RequestBody.create(MEDIA_TYPE_MULTI_DATA, files.get(key)));
        }
        RequestBody requestBody = builder.build();
        OkFileHelper.ProgressRequestBody progressRequestBody = new OkFileHelper.ProgressRequestBody(requestBody, fileProgressListener);
        Request request = new Request.Builder()
                .url(url)
                .post(progressRequestBody)
                .build();
        Call call = OkHttp.getClient().newCall(request);
//        final Handler handler = new Handler(Looper.getMainLooper());
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) {
                if (response != null) {
                    responseResult(response, call, okResponseListener, activity);
                    response.close();
                } else {
                    responseResultFailure(call, okResponseListener, activity);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                responseResultFailure(call, okResponseListener, activity);
            }

        });
        return call;
    }


    /**
     * when response success
     *
     * @param response
     * @param call
     * @param okResponseListener
     */
    private static void responseResult(final Response response, final Call call, final OkResponseListener okResponseListener, Activity activity) {
        BaseActivity.dismissLoadingDialogByManualState();
        if (response.isSuccessful()) {
            String responseStr = "";
            try {
                final String strResult = response.body().string();
                responseStr = strResult;
                final JSONObject jsonObject = JSON.parseObject(strResult);
                final int resultCode = okInit.judgeResultWhenFirstReceivedResponse(call, response, jsonObject);
                if (okInit.resultSuccessByJudge(call, response, jsonObject, resultCode)) {
                    L.e(call.request().url().url().toString() + " [resultJudgeFailed] --> " + strResult);
                    BaseActivity.dismissLoadingDialogByManualState();
                    return;
                }
                if (call.isCanceled()) return;
                activity.runOnUiThread(() -> {
                    switch (resultCode) {
                        case RESULT_SUCCESS:
                            L.e(call.request().url().url().toString() + " [Success] --> " + strResult);
                            try {
                                okResponseListener.handleJsonSuccess(call, response, jsonObject);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                        case RESULT_ERROR:
                            L.e(call.request().url().url().toString() + " [Error] --> " + strResult);
                            try {
                                okResponseListener.handleJsonError(call, response, jsonObject);
                                okResponseListener.handleAllFailureSituation(call, resultCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                        case RESULT_VERIFY_ERROR:
                            L.e(call.request().url().url().toString() + " [VerifyError] --> " + strResult);
                            try {
                                okResponseListener.handleJsonVerifyError(call, response, jsonObject);
                                okResponseListener.handleAllFailureSituation(call, resultCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                        default:
                            L.e(call.request().url().url().toString() + " [OtherResultCode] --> " + strResult);
                            try {
                                okResponseListener.handleJsonOther(call, response, jsonObject);
                                okResponseListener.handleAllFailureSituation(call, resultCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                final String parseErrorResult = responseStr;
                L.e(call.request().url().url().toString() + " [JsonParseFailed] --> " + e.getMessage() + "\nResult: " + responseStr);
                okInit.judgeResultParseResponseFailed(call, parseErrorResult, e);
                if (call.isCanceled()) return;
                activity.runOnUiThread(() -> {
                    try {
                        okResponseListener.handleParseError(call, parseErrorResult);
                        okResponseListener.handleAllFailureSituation(call, RESULT_PARSE_FAILED);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });

            }
        } else {
            L.e(call.request().url().url().toString() + " [NetworkErrorCode] --> " + response.code());
            okInit.receivedNetworkErrorCode(call, response);
            if (call.isCanceled()) return;
            activity.runOnUiThread(() -> {
                try {
                    okResponseListener.handleResponseFailure(call, response);
                    okResponseListener.handleAllFailureSituation(call, NETWORK_ERROR_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }
    }

    /**
     * when response failure or call cancel
     *
     * @param call
     * @param okResponseListener
     */
    private static void responseResultFailure(final Call call, final OkResponseListener okResponseListener, Activity activity) {
        okInit.networkError(call, call.isCanceled());
        L.e(call.request().url().url().toString() + " [networkError] --> ");
        BaseActivity.dismissLoadingDialogByManualState();
        if (okResponseListener != null && activity != null) {
            if (call.isCanceled()) return;
            activity.runOnUiThread(() -> {
                try {
                    okResponseListener.handleNoServerNetwork(call, call.isCanceled());
                    okResponseListener.handleAllFailureSituation(call, NO_NETWORK);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }
    }

    public static abstract class OkResponseListener implements IOkResponseListener {

        protected void handleJsonVerifyError(Call call, Response response, JSONObject json) throws Exception {

        }

        protected void handleJsonOther(Call call, Response response, JSONObject json) throws Exception {

        }

        protected void handleParseError(Call call, String responseResult) throws Exception {

        }

        protected void handleNoServerNetwork(Call call, boolean isCanceled) throws Exception {

        }

        protected void handleResponseFailure(Call call, Response response) throws Exception {

        }

        protected void handleAllFailureSituation(Call call, int resultCode) throws Exception {

        }
    }

    interface IOkResponseListener {
        void handleJsonSuccess(Call call, Response response, JSONObject json) throws Exception;

        void handleJsonError(Call call, Response response, JSONObject json) throws Exception;
    }

    public interface IOkInit {
        /**
         * the first time the response got from internet
         * 0：RESULT_ERROR ;
         * 1：RESULT_SUCCESS ;
         * -1：RESULT_VERIFY_ERROR;
         * 2: RESULT_OTHER ;
         * at IOkResponse interface callback
         *
         * @param call
         * @param response
         * @param json
         * @return
         */
        int judgeResultWhenFirstReceivedResponse(Call call, Response response, JSONObject json);

        /**
         * no network or  or call back cancel
         *
         * @param call
         * @param isCanceled
         */
        void networkError(Call call, boolean isCanceled);

        /**
         * after judgeResultWhenFirstReceivedResponse
         * result code not in  [200...300)
         *
         * @param call
         * @param response
         */
        void receivedNetworkErrorCode(Call call, Response response);

        /**
         * after judgeResultWhenFirstReceivedResponse
         * result is SUCCESS
         * returns ---
         * false: go on callbacks
         * true：interrupt callbacks
         *
         * @param call
         * @param response
         * @param json
         * @param resultCode
         * @return
         */
        boolean resultSuccessByJudge(Call call, Response response, JSONObject json, int resultCode);

        /**
         * after judgeResultWhenFirstReceivedResponse
         * when parse JSON failed
         *
         * @param call
         * @param parseErrorResult
         */
        void judgeResultParseResponseFailed(Call call, String parseErrorResult, Exception e);

        /**
         * add defaultParams in param
         * when setFormBody requestBody
         *
         * @param defaultParams
         * @return
         */
        Param setDefaultParams(Param defaultParams);

        /**
         * add defaultHeader in header
         * when new a request
         *
         * @param defaultHeader
         * @return
         */
        Header setDefaultHeader(Header defaultHeader);

    }

    private abstract class XRequestBody extends RequestBody {
        @Override
        public MediaType contentType() {
            return OkOptions.mediaType;
        }
    }


    /**
     * okHttp属性设置
     */
    public static class OkOptions {

        public static long readTimeout = 60;
        public static long connectTimeout = 30;
        public static long writeTimeout = 120;
        public static MediaType mediaType = null;


        public OkOptions(long readTimeout, long connectTimeout, long writeTimeout) {
            OkOptions.readTimeout = readTimeout;
            OkOptions.connectTimeout = connectTimeout;
            OkOptions.writeTimeout = writeTimeout;
        }

        /**
         * 设置 Builder
         * @param builder
         */
        public void setOkHttpBuilder(OkHttpClient.Builder builder) {

        }

        public static void setMediaType(MediaType mediaType) {
            OkOptions.mediaType = mediaType;
        }


    }

}
