package com.gdu.demo.views;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;
import android.widget.ImageView;

import com.gdu.demo.R;
import com.gdu.demo.ourgdu.ourGDUCodecManager;

public class ExternalDisplayPresentation extends Presentation {

    private TextureView mExternalTextureView;
    private ourGDUCodecManager codecManager;

    public ExternalDisplayPresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载用于外接屏的布局
        setContentView(R.layout.presentation_layout);

        // 获取布局中的TextureView
        mExternalTextureView = findViewById(R.id.external_texture_view);
        mExternalTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
                Log.d("onSurfaceTextureAvailable: ", "onSurfaceTextureAvailable: ");
                codecManager = new ourGDUCodecManager(mExternalTextureView.getContext(), surface, w, h);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

//    public void setBitMap(Bitmap bitmap){
//        mExternalTextureView.setImageBitmap(bitmap);
//    }

    public ourGDUCodecManager getCodecManager(){
        return codecManager;
    }

}

