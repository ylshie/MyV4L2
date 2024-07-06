package com.ylshie.myv4l2;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jiangdg.uvc.IFrameCallback;
import com.jiangdg.uvc.UVCCamera;
import com.jiangdg.usb.*;

import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    protected void showMessage(String title, String message) {
        Context context = this;
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
    UVCCamera mUVCCamera = new UVCCamera();
    USBMonitor mUsbMonitor = null;
    Handler mMainHandler = new Handler(Looper.getMainLooper());

    protected boolean isUsbCamera(UsbDevice device) {
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) {
            return true;
        } else if (device.getDeviceClass() == UsbConstants.USB_CLASS_MISC) {
            boolean isVideo = false;
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                int cls = device.getInterface(i).getInterfaceClass();
                if (cls == UsbConstants.USB_CLASS_VIDEO) {
                    isVideo = true;
                    break;
                }
            }
            return isVideo;
        }
        else {
            return false;
        }
    }
    final String TAG = "FUCK";
    boolean mGranted = false;
    boolean  mOpen = false;
    USBMonitor.UsbControlBlock mCtlBlock = null;
    protected void openCamera(String reason) {
        /*
        if (! mGranted) {
            Log.d(TAG, "openCamera failed from " + reason + ": not granted");
            return;
        }
        */
        if (mOpen) return;
        if (mCtlBlock == null) {
            Log.d(TAG, "openCamera failed from " + reason + " no CTRL Block");
            return;
        }
        Log.d(TAG, "openCamera success from " + reason);

        SurfaceView viewFinder = findViewById(R.id.view_finder);

        mUVCCamera.open(mCtlBlock);
        List<com.jiangdg.utils.Size> list = mUVCCamera.getSupportedSizeList();
        //list.get(0);
        mUVCCamera.setFrameCallback(new IFrameCallback() {
            @Override
            public void onFrame(ByteBuffer frame) {
                Log.d(TAG, "---- frame ------");
            }
        }, UVCCamera.PIXEL_FORMAT_YUV420SP);
        mUVCCamera.setPreviewSize(list.get(0).width, list.get(0).height);
        mUVCCamera.setPreviewDisplay(viewFinder.getHolder());
        mUVCCamera.startPreview();
        //mUVCCamera.updateCameraParams();
        Log.d(TAG,"==================== OPEN ================");
        mOpen = true;
        //showMessage("XXX", "Open Camera");
    }
    protected USBMonitor createMonitor() {
        return new USBMonitor(this, new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(UsbDevice device) {
                boolean bCamera = isUsbCamera(device);
                String C = bCamera ? "_V": "_N";
                //showMessage("V4L2", "Attach " + device.getDeviceName() + C);
                if (bCamera) mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mGranted = mUsbMonitor.requestPermission(device);
                        /*
                        if (mGranted) {
                            Log.d(TAG, "request Permission granted");
                            openCamera("request permission");
                        } else {
                            Log.d(TAG, "request Permission denied");
                        }
                        */
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                openCamera("request permission");
                            }
                        }, 30000);
                    }
                });
            }

            @Override
            public void onDetach(UsbDevice device) {
                String C = isUsbCamera(device)? "_V": "_N";
                //showMessage("V4L2", "Detach " + device.getDeviceName() + C);
            }

            @Override
            public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                String C = isUsbCamera(device)? "_V": "_N";
                //showMessage("V4L2", "Connect " + device.getDeviceName() + C);
                mCtlBlock = ctrlBlock;
                /*
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        openCamera("onConnect");
                    }
                });
                */
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                String C = isUsbCamera(device)? "_V": "_N";
                //showMessage("V4L2", "Disconnect " + device.getDeviceName() + C);
            }

            @Override
            public void onCancel(UsbDevice device) {
                String C = isUsbCamera(device)? "_V": "_N";
                //showMessage("V4L2", "Cancel " + device.getDeviceName() + C);
            }
        });
    }
    boolean mRegistered = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mUsbMonitor = createMonitor();
        mUsbMonitor.register();
    }
}