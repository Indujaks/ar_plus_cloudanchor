/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;

import org.ejml.simple.SimpleMatrix;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.common.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.common.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private static boolean flag=false;
    private boolean installRequested;
    SimpleMatrix CameraPoseinAnchorCS_withoutrotation = new SimpleMatrix(4, 1);
    float X_CameraPoseinAnchorCS_withrotation;
    float Y_CameraPoseinAnchorCS_withrotation;
    float[] cameralocationWS;
    Button button_anchorstatus, button_mapping;
    private ImageView mapView;
    private Bitmap mapImage, blueDot, positionBitmap;
    Canvas canvas;
    private static float X0, Y0, X1, Y1, X2, Y2, left, top;
    private int startX, startY, endX1, endY1, endX2, endY2;
    private static int framecount=0;
    double AnglebetweenAnchorCSandWcs;
    public float k0_x = 0.105f, k0_y = 0.86f, k1_x = 0.90f, k2_y = 0.070f;
    final int mapEndX = 450;
    final int mapEndY = 850;
    float smallest;
    static int resolvecount =0;
    int nearest_anchor = 0;
    private Button resolveButton,delete;
    Camera camera;
    TextView nearestanchor_tv, distance_tv, angle_tv, camera_euler, anchor_euler, pose_camera, pose_anchor;
    private StorageManager storageManager;
    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;
    static  HashMap<String,Anchor.CloudAnchorState> shortcodes_state_hm=new HashMap<String, Anchor.CloudAnchorState>();
    static HashMap<String,Integer> cloudid_shortcode_hm= new HashMap<>();
    static HashMap<String,Integer> Iterator = new HashMap<>();
    static HashMap<String,Boolean> resolvedanchors= new HashMap();
    public ArrayList<Integer> shortcodestobefinalised = new ArrayList<>();

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[]{0f, 0f, 0f, 0f};
    Pose p, p1;
    boolean isButtonClicked = false;
    ImageView mapView_imageView;

    ArrayList<Float> distanceMeters = null;

    // Anchors created from taps used for object placing with a given color.
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;
        private enum AppAnchorState {
            NONE,
            HOSTING,
            HOSTED,
            RESOLVING,
            RESOLVED
        }
        private AppAnchorState appAnchorState = AppAnchorState.NONE;

        public ColoredAnchor(Anchor a, float[] color4f,AppAnchorState state) {
            this.anchor = a;
            this.color = color4f;
            this.appAnchorState = state;
        }

    }

    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        angle_tv = (TextView) findViewById(R.id.angle_tv);
        mapView_imageView = (ImageView) findViewById(R.id.mapImageView);
    /*nearestanchor_tv = (TextView)findViewById(R.id.nearestanchor_tv);
    distance_tv = (TextView)findViewById(R.id.distance_tv);
    angle_tv= (TextView)findViewById(R.id.angle_tv);
    camera_euler = (TextView)findViewById(R.id.Camera_euler);
    anchor_euler = (TextView)findViewById(R.id.Anchor_Euler);
    pose_anchor = (TextView)findViewById(R.id.pose_anchor);
    pose_camera = (TextView)findViewById(R.id.pose_camera);*/
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
        distanceMeters = new ArrayList<Float>();
        button_anchorstatus = (Button) findViewById(R.id.Anchor_Visibility);
        button_mapping = (Button) findViewById(R.id.button_mapping);
        resolveButton =  findViewById (R.id.button2);
        resolveButton.setOnClickListener((view) -> onResolveButtonPress());
        delete = findViewById(R.id.button3);
        delete.setOnClickListener(view -> {
            storageManager.removedata();
        });

        // Set up tap listener.
        tapHelper = new TapHelper(/*context=*/ this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        installRequested = false;

        button_anchorstatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isButtonClicked) {

                    surfaceView.getHolder().setFixedSize(0, 0);
                    mapView_imageView.setVisibility(View.VISIBLE);


                    mapImage = BitmapFactory.decodeResource(getResources(), R.drawable.map);
                    X0 = mapImage.getWidth() * 0.120f;
                    Y0 = mapImage.getHeight() * 0.910f;

                    X1 = mapImage.getWidth() * 0.85f;
                    Y1 = mapImage.getHeight() * 0.890f;

                    X2 = mapImage.getWidth() * 0.120f;
                    Y2 = mapImage.getHeight() * 0.030f;

                    startX = Math.round(X0);
                    startY = Math.round(Y0);

                    endX1 = Math.round(X1);
                    endY1 = Math.round(Y1);

                    endX2 = Math.round(X2);
                    endY2 = Math.round(Y2);

                    blueDot = BitmapFactory.decodeResource(getResources(), R.drawable.direction_marker);
                    blueDot = Bitmap.createScaledBitmap(blueDot, 125, 125, false);
                    positionBitmap = Bitmap.createBitmap(mapImage.getWidth(), mapImage.getHeight(), mapImage.getConfig());
                    canvas = new Canvas(positionBitmap);
                    canvas.drawBitmap(mapImage, new Matrix(), null);
                    float deltaX = Math.abs(50 * (endX1 - startX) / mapEndX);
                    float deltaY = Math.abs(400 * (startY - endY2) / mapEndY);
                    //float temp =  300 * 2685 / 600;

                    canvas.drawBitmap(blueDot, startX , startY , new Paint()); // or try (endY2 + deltaY)
                    mapView_imageView.setImageBitmap(positionBitmap);

                } else {
                    mapView_imageView.setVisibility(View.INVISIBLE);

                }

                if (isButtonClicked)
                    isButtonClicked = false;
                else
                    isButtonClicked = true;
            }


        });
        storageManager = new StorageManager(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Activity", "OnResume is called ");

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);
                Config config = new Config(session);
                config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED); // Add this line.
                session.configure(config);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();

        messageSnackbarHelper.showMessage(this, "Searching for surfaces...");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("Activity", "OnPause() is called ");
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            virtualObject.createOnGlThread(/*context=*/ this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(
                    /*context=*/ this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        Log.d("onDraw", "In On draw frame ()");

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            //Thread.sleep(100);
            MotionEvent tap = tapHelper.poll();
            Frame frame = session.update();

            camera = frame.getCamera();
            framecount++;
            if(framecount>300){
                resolve_anchors_continuously();
                framecount=0;
            }
            if (anchors.size() > 1) {
                SimpleMatrix AnchorMatrix = new SimpleMatrix(4, 4);
                SimpleMatrix InverseanchorMatrix = new SimpleMatrix(4, 4);
                SimpleMatrix cameraposeinWorldCS = new SimpleMatrix(4, 1);

                float[] matrix = new float[16];

                anchors.get(0).anchor.getPose().toMatrix(matrix, 0);
                int index = 0;
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        AnchorMatrix.set(j, i, matrix[index]);
                        index++;
                    }
                }
                InverseanchorMatrix = AnchorMatrix.invert();

                float[] cameraPose = new float[4];

                cameraPose = camera.getPose().getTranslation();
                //Log.d("CameraPoseinFloat",cameraPose[0]+" "+cameraPose[1]+" "+cameraPose[2]);
                cameralocationWS = new float[]{cameraPose[0], cameraPose[1], cameraPose[2], 1.0f};
                Log.d("CameraPoseinFloat1", cameralocationWS[0] + " " + cameralocationWS[1] + " " + cameralocationWS[2]);

                for (int i = 0; i < 4; i++) {
                    cameraposeinWorldCS.set(i, 0, cameralocationWS[i]);
                }
                CameraPoseinAnchorCS_withoutrotation = InverseanchorMatrix.mult(cameraposeinWorldCS);
                float temp_X = (float) CameraPoseinAnchorCS_withoutrotation.get(0, 0);
                float temp_Z = (float) CameraPoseinAnchorCS_withoutrotation.get(2, 0);
                X_CameraPoseinAnchorCS_withrotation = (float) (Math.cos(AnglebetweenAnchorCSandWcs) * temp_X) +
                        (float) (Math.sin(AnglebetweenAnchorCSandWcs) * temp_Z);
                Y_CameraPoseinAnchorCS_withrotation = (float) (-(Math.sin(AnglebetweenAnchorCSandWcs) * temp_X) +
                        (float) (Math.cos(AnglebetweenAnchorCSandWcs) * temp_Z));
            }

            // Handle one tap per frame.
            handleTap(frame, camera);

            // Draw background.
            backgroundRenderer.draw(frame);

            Thread one = new Thread() {
                public void run() {
                    try {
                        float[] matrix = new float[16];
                        float[] matrix1 = new float[16];

                        anchors.get(0).anchor.getPose().toMatrix(matrix, 0);

                        camera.getPose().toMatrix(matrix1, 0);

                        Log.d("TestMatrixAnchor", Arrays.toString(matrix));
                        Log.d("TestMatrixCamera", Float.toString(matrix1[12]) + " " + Float.toString(matrix1[13]) + " " + Float.toString(matrix1[14]) + " " + Float.toString(matrix1[15]));

                        distanceMeters.clear();
                        for (int i = 0; i < anchors.size(); i++) {
                            p = anchors.get(i).anchor.getPose();
                            p1 = camera.getPose();
                            float diffx = p.tx() - p1.tx();
                            float diffy = p.ty() - p1.ty();
                            float diffz = p.tz() - p1.tz();
                            distanceMeters.add((float) Math.sqrt(diffx * diffx + diffy * diffy + diffz * diffz));
                        }

                        smallest = distanceMeters.get(0);
                        nearest_anchor = 0;
                        for (int i = 0; i < distanceMeters.size(); i++) {
                            if (distanceMeters.get(i) < smallest) {
                                smallest = distanceMeters.get(i);
                                nearest_anchor = i;
                                Log.d("AIKMRSV", "Nearest Anchor " + i);
                            }
                        }

                        Log.d("AIKMRSV", "Nearest Anchor is at :" + smallest + " meters" + " Anchor id is " + nearest_anchor);
                        p = anchors.get(nearest_anchor).anchor.getPose();

                        HelloArActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                String Direction = "right";
                                int tracking_count = 0;
                                for (int i = 0; i < anchors.size(); i++) {
                                    if (anchors.get(i).anchor.getTrackingState() == TrackingState.TRACKING)
                                        tracking_count++;
                                }

                                button_anchorstatus.setText(Integer.toString(tracking_count));
                                if (tracking_count == anchors.size())
                                    button_anchorstatus.setBackgroundColor(Color.GREEN);
                                else if (tracking_count == 0)
                                    button_anchorstatus.setBackgroundColor(Color.RED);
                                else
                                    button_anchorstatus.setBackgroundColor(Color.YELLOW);

                                DecimalFormat df = new DecimalFormat("#.##");
                                angle_tv.setText("Camera in A1CS :" + Math.round(X_CameraPoseinAnchorCS_withrotation * 100) + " _ " + Math.round((float) CameraPoseinAnchorCS_withoutrotation.get(1, 0) * 100) + " _ " + Math.round(Y_CameraPoseinAnchorCS_withrotation * 100) + " Distance :" + (float) Math.round(Math.sqrt(Math.round(X_CameraPoseinAnchorCS_withrotation * 100) * Math.round(X_CameraPoseinAnchorCS_withrotation * 100) + Math.round(CameraPoseinAnchorCS_withoutrotation.get(1, 0) * 100) * Math.round(CameraPoseinAnchorCS_withoutrotation.get(1, 0) * 100) + Math.round(Y_CameraPoseinAnchorCS_withrotation * 100) * Math.round(Y_CameraPoseinAnchorCS_withrotation * 100))));
               /* if (isButtonClicked)
                {
                    float deltaX = Math.abs(X_CameraPoseinAnchorCS_withrotation * (endX1 - startX) / mapEndX);
                    float deltaY = Math.abs(Y_CameraPoseinAnchorCS_withrotation * (startY - endY2) / mapEndY);
                    left = startX + deltaX;
                    top = startY - deltaY;  // or try (endY2 + deltaY)

                    canvas.drawBitmap(mapImage, new Matrix(), null);
                    canvas.drawBitmap(blueDot, left, top, new Paint());
                }*/
                                if (isButtonClicked) {
                                    drawOnMap(Math.round(X_CameraPoseinAnchorCS_withrotation * 100), Math.round(Y_CameraPoseinAnchorCS_withrotation * 100));
                                }
                            }


                        });

                    } catch (Exception v) {
                        v.printStackTrace();
                    }
                }
            };

            if (anchors.size() > 1) {
                one.start();

            }

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (messageSnackbarHelper.isShowing()) {
                for (Plane plane : session.getAllTrackables(Plane.class)) {
                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                        messageSnackbarHelper.hide(this);
                        break;
                    }
                }
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (ColoredAnchor coloredAnchor : anchors) {
                if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

                // Update and draw the model and its shadow.
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);

                // coloredAnchor.anchor.notify();
                Log.d("Test", "Test");
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }
    public void onResolveButtonPress() {
        flag = true;

        Log.e(TAG,"indu onresolvebutonpress");
        ArrayList<String> data = storageManager.getAllShortCodes(anchorcodes -> {
            ArrayList<String> code = anchorcodes;
            Log.e(TAG, "indu code" + code);
            for (String shorty : code) {
                int shortcode = Integer.parseInt(shorty);

                storageManager.getCloudAnchorId(shortcode, cloudAnchorId -> {
                    if (cloudAnchorId != null) {
                       // localanchorcount++;
                        Log.e(TAG, "indu string " + shortcode + cloudAnchorId);
                        Anchor resolvedAnchor = session.resolveCloudAnchor(cloudAnchorId);
                        float[] objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};
                        // if(resolvedAnchor.getCloudAnchorState() == Anchor.CloudAnchorState.SUCCESS){
                        Log.e(TAG, "indu cloudanchor state " + resolvedAnchor.getCloudAnchorState() + resolvedAnchor.getCloudAnchorId());
                        // if(!codes_hm.containsKey(cloudAnchorId)){
                        shortcodes_state_hm.put(cloudAnchorId, resolvedAnchor.getCloudAnchorState());
                        cloudid_shortcode_hm.put(cloudAnchorId, shortcode);

                        if (!shortcodestobefinalised.contains(shortcode)){
                            shortcodestobefinalised.add(shortcode);
                        }
                        //}else{
                        //  codes_hm.put(shorty,0);
                        //


                            anchors.add(new ColoredAnchor(resolvedAnchor, objColor, ColoredAnchor.AppAnchorState.RESOLVING));
                            Iterator.put(cloudAnchorId, 1);
                        Toast.makeText(getApplicationContext(), "Now resolving anchor..", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void resolve_anchors_continuously() {
        Log.e(TAG,"indu resolving _anchors");

        if(flag) {
            Log.e(TAG,"indu flag is true");
            updateanchorstate();
            Log.e(TAG,"indu shortcodestobefinalised"+shortcodestobefinalised);
            for(int shortcode : shortcodestobefinalised){
                storageManager.getCloudAnchorId(shortcode,cloudAnchorId -> {
                    if(cloudAnchorId!=null) {
                        Log.e(TAG,"indu shorty "+shortcode+cloudAnchorId);
                        Log.e(TAG,"indu cloud id"+cloudAnchorId+" \t state"+shortcodes_state_hm.get(cloudAnchorId));
                        if(shortcodes_state_hm.containsKey(cloudAnchorId)&& !shortcodes_state_hm.get(cloudAnchorId).equals( Anchor.CloudAnchorState.SUCCESS)  ) {
                            Anchor resolvedAnchor = session.resolveCloudAnchor(cloudAnchorId);
                            float[] objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};
                              if(Iterator.get(cloudAnchorId)!=1) {
                                  Log.e(TAG,"indu iterator value is not 1");
                                  Iterator.put(cloudAnchorId,1);
                                  anchors.add(new ColoredAnchor(resolvedAnchor, objColor, ColoredAnchor.AppAnchorState.RESOLVING));
                              }
                            Toast.makeText(getApplicationContext(), "Now resolving anchor..", Toast.LENGTH_SHORT).show();
                        }
                    }
                });}
               // checkTrackingresolve();
        }


    }
    private void updateanchorstate() {
        Log.e(TAG,"indu updateanchorstate");
        for(HashMap.Entry<String,Anchor.CloudAnchorState> hmentry:shortcodes_state_hm.entrySet()){

            for(ColoredAnchor canchor:anchors){
                String id=canchor.anchor.getCloudAnchorId();
                Anchor.CloudAnchorState state=canchor.anchor.getCloudAnchorState();
                if(id.equals(hmentry.getKey())){
                    Log.e(TAG,"indu setting anchor status" +state);
                    shortcodes_state_hm.put(hmentry.getKey(),state);

                    if(state== Anchor.CloudAnchorState.SUCCESS) {
                        canchor.appAnchorState= ColoredAnchor.AppAnchorState.RESOLVED;
                        Log.e(TAG,"indu cloudid_shortcode_hm"+cloudid_shortcode_hm);
                        if(cloudid_shortcode_hm.containsKey(id)){
                            Log.e(TAG,"indu getshortcodes");
                            shortcodestobefinalised.remove(cloudid_shortcode_hm.get(id));
                        }
                    }
                }
                else{
                    // if(state==Anchor.CloudAnchorState.ERROR_CLOUD_ID_NOT_FOUND || state==Anchor.CloudAnchorState.ERROR_HOSTING_DATASET_PROCESSING_FAILED ||state==Anchor.CloudAnchorState.ERROR_RESOLVING_LOCALIZATION_NO_MATCH )
                    Iterator.put(id,0);
                    Log.e(TAG,"indu updateanchorstate shortcode and count"+id+"0");
                }
            }

        }
    }
    private void checkTrackingresolve(){

        for (int i = 0; i < anchors.size(); i++) {
            if (anchors.get(i).anchor.getTrackingState() != TrackingState.TRACKING){
                resolvedanchors.put(anchors.get(i).anchor.getCloudAnchorId(),false);
                Log.e(TAG,"indu tracking state lost");
                resolvecount--;
            }
        }
    }
    private void drawOnMap(int x, int y)
    {

    float deltaX = Math.abs(x * (endX1 - startX) / mapEndX);
    float deltaY = Math.abs(y * (startY - endY2) / mapEndY);
    left =startX +deltaX;
    top =startY -deltaY;  // or try (endY2 + deltaY)

        canvas.drawBitmap(mapImage,new

    Matrix(), null);
        canvas.drawBitmap(blueDot,left,top,new

    Paint());
        mapView_imageView.setImageBitmap(positionBitmap);
}

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      for (HitResult hit : frame.hitTest(tap)) {
        // Check if any plane was hit, and if it was hit inside the plane polygon
        Trackable trackable = hit.getTrackable();
        // Creates an anchor if a plane or an oriented point was hit.
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
          // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
          // Cap the number of objects created. This avoids overloading both the
          // rendering system and ARCore.
          if (anchors.size() >= 20) {
            anchors.get(0).anchor.detach();
            anchors.remove(0);
          }

          // Assign a color to the object for rendering based on the trackable type
          // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
          // for AR_TRACKABLE_PLANE, it's green color.
          float[] objColor;
          if (trackable instanceof Point) {
            objColor = new float[] {66.0f, 133.0f, 244.0f, 255.0f};
          } else if (trackable instanceof Plane) {
            objColor = new float[] {139.0f, 195.0f, 74.0f, 255.0f};
          } else {
            objColor = DEFAULT_COLOR;
          }

          // Adding an Anchor tells ARCore that it should track this position in
          // space. This anchor is created on the Plane to place the 3D model
          // in the correct position relative both to the world and to the plane.
          hit.getHitPose().compose(Pose.makeTranslation(hit.getHitPose().getTranslation()));
          Pose p = hit.getHitPose().extractTranslation();

          Anchor anc = session.hostCloudAnchor(hit.createAnchor());
          anchors.add(new ColoredAnchor(anc, objColor,ColoredAnchor.AppAnchorState.HOSTING));

          if(anchors.size()>1)
          {
            Log.d("TryKau2",anchors.get(1).anchor.getPose().toString());
            float[] anchor1_temp = anchors.get(0).anchor.getPose().getTranslation();
            float[] anchor2_temp = anchors.get(1).anchor.getPose().getTranslation();
            AnglebetweenAnchorCSandWcs = Math.atan((anchor2_temp[2]-anchor1_temp[2])/(anchor2_temp[0]-anchor1_temp[0]));
            Log.d("Angle",Double.toString(Math.toDegrees(AnglebetweenAnchorCSandWcs)));
          }
          break;
        }
      }
    }
  }
}

