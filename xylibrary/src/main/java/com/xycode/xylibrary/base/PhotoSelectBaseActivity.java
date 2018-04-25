package com.xycode.xylibrary.base;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import com.xycode.xylibrary.interfaces.PermissionListener;
import com.xycode.xylibrary.takephoto.app.TakePhoto;
import com.xycode.xylibrary.takephoto.app.TakePhotoActivity;
import com.xycode.xylibrary.takephoto.model.CropOptions;
import com.xycode.xylibrary.takephoto.model.TResult;
import com.xycode.xylibrary.takephoto.model.TakePhotoOptions;
import com.xycode.xylibrary.utils.LogUtil.L;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public abstract class PhotoSelectBaseActivity extends TakePhotoActivity {

    public static final String IMAGES = "IMAGES";
    public static final String PARAM = "PARAM";
    public static final String CROP_OPTIONS = "CropOptions";
    public static final String SELECT_SUCCESS = "SELECT_SUCCESS";

    private PhotoParam param;
    private CropOptions cropOptions;

    public static void startForResult(XyBaseActivity activity, Class activityClass, PhotoParam param) {
        startForResult(activity, activityClass, param, null);
    }

    public static void startForResult(XyBaseActivity activity, Class activityClass, PhotoParam param, CropOptions options) {

        activity.start(activityClass, intent -> {
            if (options == null) {
                intent.putExtra(CROP_OPTIONS, new CropOptions.Builder().create());
            } else {
                intent.putExtra(CROP_OPTIONS, options);
            }
            if (param == null) {
                intent.putExtra(PARAM, new PhotoParam());
            } else {
                intent.putExtra(PARAM, param);
            }
        }, REQUEST_CODE_PHOTO_SELECT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        param = (PhotoParam) getIntent().getSerializableExtra(PARAM);
        cropOptions = (CropOptions) getIntent().getSerializableExtra(CROP_OPTIONS);
    }

    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        if (param.multiSelectLimit > 1) {
            builder.setWithOwnGallery(true);
        }
        builder.setCorrectImage(true);
        takePhoto.setTakePhotoOptions(builder.create());

    }

    /**
     * 请求权限并调用相机
     */
    protected void onCamera() {
                 /* 调用相机 */
        TakePhoto takePhoto = getTakePhoto();

        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Uri imageUri = Uri.fromFile(file);

        takePhoto.onEnableCompress(null, false);
        configTakePhotoOption(takePhoto);
        if (cropOptions.isCrop()) {
            takePhoto.onPickFromCaptureWithCrop(imageUri, cropOptions);
        } else {
            takePhoto.onPickFromCapture(imageUri);
        }
    }

    /**
     * 打开相册
     */
    protected void onAlbum() {
        TakePhoto takePhoto = getTakePhoto();
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Uri imageUri = Uri.fromFile(file);

        configTakePhotoOption(takePhoto);
        if (param.multiSelectLimit > 1) {
            if (cropOptions.isCrop()) {
                takePhoto.onPickMultipleWithCrop(param.multiSelectLimit, cropOptions);
            } else {
                takePhoto.onPickMultiple(param.multiSelectLimit);
            }
            return;
        }
        /*if (rgFrom.getCheckedRadioButtonId() == R.id.rbFile) {
            if (cropOptions.isCrop()) {
                takePhoto.onPickFromDocumentsWithCrop(imageUri, getCropOptions());
            } else {
                takePhoto.onPickFromDocuments();
            }
            return;
        } else {*/
        if (cropOptions.isCrop()) {
            takePhoto.onPickFromGalleryWithCrop(imageUri, cropOptions);
        } else {
            takePhoto.onPickFromGallery();
        }
//        }
    }

    @Override
    protected AlertDialog setLoadingDialog() {
        return null;
    }

    public static class PhotoParam implements Serializable {
        int multiSelectLimit = 1;

        public PhotoParam() {

        }

        public int getMultiSelectLimit() {
            return multiSelectLimit;
        }

        public void setMultiSelectLimit(int multiSelectLimit) {
            this.multiSelectLimit = multiSelectLimit;
        }
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
        Intent intent = new Intent();
        intent.putExtra(SELECT_SUCCESS, true);
        intent.putExtra(IMAGES, result.getImages());
        for (int i = 0; i < result.getImages().size(); i++) {
            L.e("path: " + result.getImages().get(i).getOriginalPath());
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void takeFail(TResult result, String msg) {
        L.i("takeFail:" + msg);
    }

    @Override
    public void takeCancel() {
        L.i("canceled");
    }
}
