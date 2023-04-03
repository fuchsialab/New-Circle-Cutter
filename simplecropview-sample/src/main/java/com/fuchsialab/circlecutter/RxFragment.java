package com.fuchsialab.circlecutter;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.util.Logger;
import com.isseiaoki.simplecropview.util.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import io.reactivex.CompletableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

public class RxFragment extends Fragment {

  FragmentActivity activity;
  private static final int REQUEST_PICK_IMAGE = 10011;
  private static final int REQUEST_SAF_PICK_IMAGE = 10012;
  private static final String PROGRESS_DIALOG = "ProgressDialog";
  private static final String KEY_FRAME_RECT = "FrameRect";
  private static final String KEY_SOURCE_URI = "SourceUri";

  // Views ///////////////////////////////////////////////////////////////////////////////////////
  private CropImageView mCropView;
  private CompositeDisposable mDisposable = new CompositeDisposable();
  private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.PNG;
  private RectF mFrameRect = null;
  private Uri mSourceUri = null;

  // Note: only the system can call this constructor by reflection.
  public RxFragment() {
  }

  public static RxFragment newInstance() {
    RxFragment fragment = new RxFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_basic, null, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // bind Views
    bindViews(view);

    activity = getActivity();


    MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {
        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
        for (String adapterClass : statusMap.keySet()) {
          AdapterStatus status = statusMap.get(adapterClass);
          Log.d("MyApp", String.format(
                  "Adapter name: %s, Description: %s, Latency: %d",
                  adapterClass, status.getDescription(), status.getLatency()));
        }

        // Start loading ads here...
      }
    });


    if (savedInstanceState != null) {
      // restore data
      mFrameRect = savedInstanceState.getParcelable(KEY_FRAME_RECT);
      mSourceUri = savedInstanceState.getParcelable(KEY_SOURCE_URI);
    }

    if (mSourceUri == null) {
      // default data
      mSourceUri = getUriFromDrawableResId(getContext(), R.drawable.ironman);
    }
    // load image
    mDisposable.add(loadImage(mSourceUri));
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // save data
    outState.putParcelable(KEY_FRAME_RECT, mCropView.getActualCropRect());
    outState.putParcelable(KEY_SOURCE_URI, mCropView.getSourceUri());
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mDisposable.dispose();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent result) {
    super.onActivityResult(requestCode, resultCode, result);
    if (resultCode == Activity.RESULT_OK) {
      // reset frame rect
      mFrameRect = null;
      switch (requestCode) {
        case REQUEST_PICK_IMAGE:
          mDisposable.add(loadImage(result.getData()));
          break;
        case REQUEST_SAF_PICK_IMAGE:
          mDisposable.add(loadImage(Utils.ensureUriPermission(getContext(), result)));
          break;
      }
    }
  }

  private Disposable loadImage(final Uri uri) {


    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

      mSourceUri = uri;

      return new RxPermissions(getActivity()).request(Manifest.permission.READ_MEDIA_IMAGES)
              .filter(new Predicate<Boolean>() {
                @Override
                public boolean test(@io.reactivex.annotations.NonNull Boolean granted)
                        throws Exception {
                  return granted;
                }
              })
              .flatMapCompletable(new Function<Boolean, CompletableSource>() {
                @Override
                public CompletableSource apply(@io.reactivex.annotations.NonNull Boolean aBoolean)
                        throws Exception {
                  return mCropView.load(uri)
                          .useThumbnail(true)
                          .initialFrameRect(mFrameRect)
                          .executeAsCompletable();
                }
              })
              .subscribeOn(Schedulers.newThread())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Action() {
                @Override
                public void run() throws Exception {
                }
              }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) throws Exception {
                }
              });


    }else {

      mSourceUri = uri;

      return new RxPermissions(getActivity()).request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              .filter(new Predicate<Boolean>() {
                @Override
                public boolean test(@io.reactivex.annotations.NonNull Boolean granted)
                        throws Exception {
                  return granted;
                }
              })
              .flatMapCompletable(new Function<Boolean, CompletableSource>() {
                @Override
                public CompletableSource apply(@io.reactivex.annotations.NonNull Boolean aBoolean)
                        throws Exception {
                  return mCropView.load(uri)
                          .useThumbnail(true)
                          .initialFrameRect(mFrameRect)
                          .executeAsCompletable();
                }
              })
              .subscribeOn(Schedulers.newThread())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Action() {
                @Override
                public void run() throws Exception {
                }
              }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) throws Exception {
                }
              });

    }
  }

  private Disposable cropImage() {
    return mCropView.crop(mSourceUri)
        .executeAsSingle()
        .flatMap(new Function<Bitmap, SingleSource<Uri>>() {
          @Override public SingleSource<Uri> apply(@io.reactivex.annotations.NonNull Bitmap bitmap)
              throws Exception {
            return mCropView.save(bitmap)
                .compressFormat(mCompressFormat)
                .executeAsSingle(createSaveUri());
          }
        })
        .doOnSubscribe(new Consumer<Disposable>() {
          @Override public void accept(@io.reactivex.annotations.NonNull Disposable disposable)
              throws Exception {
            showProgress();
          }
        })
        .doFinally(new Action() {
          @Override public void run() throws Exception {
            dismissProgress();
          }
        })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<Uri>() {
          @Override public void accept(@io.reactivex.annotations.NonNull Uri uri) throws Exception {
            ((RxActivity) getActivity()).startResultActivity(uri);
          }
        }, new Consumer<Throwable>() {
          @Override public void accept(@io.reactivex.annotations.NonNull Throwable throwable)
              throws Exception {
          }
        });
  }

  // Bind views //////////////////////////////////////////////////////////////////////////////////

  private void bindViews(View view) {
    mCropView = (CropImageView) view.findViewById(R.id.cropImageView);
    view.findViewById(R.id.buttonDone).setOnClickListener(btnListener);
    view.findViewById(R.id.buttonPickImage).setOnClickListener(btnListener);
    view.findViewById(R.id.buttonRotateLeft).setOnClickListener(btnListener);
    view.findViewById(R.id.buttonRotateRight).setOnClickListener(btnListener);
    view.findViewById(R.id.buttonCircle).setOnClickListener(btnListener);
    view.findViewById(R.id.button1_1).setOnClickListener(btnListener);
    view.findViewById(R.id.buttonFree).setOnClickListener(btnListener);
  }

  public void pickImage() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
          REQUEST_PICK_IMAGE);
    } else {

      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("image/*");
      startActivityForResult(intent, REQUEST_SAF_PICK_IMAGE);
    }
  }

  public void showProgress() {
    ProgressDialogFragment f = ProgressDialogFragment.getInstance();
    getFragmentManager().beginTransaction().add(f, PROGRESS_DIALOG).commitAllowingStateLoss();
  }

  public void dismissProgress() {
    if (!isResumed()) return;
    androidx.fragment.app.FragmentManager manager = getFragmentManager();
    if (manager == null) return;
    ProgressDialogFragment f = (ProgressDialogFragment) manager.findFragmentByTag(PROGRESS_DIALOG);
    if (f != null) {
      getFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
    }
  }

  public Uri createSaveUri() {
    return createNewUri(getContext(), mCompressFormat);
  }

  public static String getDirPath() {
    String dirPath = "";
    File imageDir = null;
    File extStorageDir = Environment.getExternalStorageDirectory();
    if (extStorageDir.canWrite()) {
      imageDir = new File(extStorageDir.getPath() + "/Circle Cutter");
    }
    if (imageDir != null) {
      if (!imageDir.exists()) {
        imageDir.mkdirs();
      }
      if (imageDir.canWrite()) {
        dirPath = imageDir.getPath();
      }
    }
    return dirPath;
  }

  public static Uri getUriFromDrawableResId(Context context, int drawableResId) {
    StringBuilder builder = new StringBuilder().append(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .append("://")
        .append(context.getResources().getResourcePackageName(drawableResId))
        .append("/")
        .append(context.getResources().getResourceTypeName(drawableResId))
        .append("/")
        .append(context.getResources().getResourceEntryName(drawableResId));
    return Uri.parse(builder.toString());
  }

  public static Uri createNewUri(Context context, Bitmap.CompressFormat format) {
    long currentTimeMillis = System.currentTimeMillis();
    Date today = new Date(currentTimeMillis);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String title = dateFormat.format(today);
    String dirPath = getDirPath();
    String fileName = "scv" + title +"." + getMimeType(format);
    String path = dirPath + "/" + fileName;
    File file = new File(path);
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.TITLE, title);
    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + getMimeType(format));
    values.put(MediaStore.Images.Media.DATA, path);
    long time = currentTimeMillis / 1000;
    values.put(MediaStore.MediaColumns.DATE_ADDED, time);
    values.put(MediaStore.MediaColumns.DATE_MODIFIED, time);
    if (file.exists()) {
      values.put(MediaStore.Images.Media.SIZE, file.length());
    }

    ContentResolver resolver = context.getContentResolver();
    Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    Logger.i("SaveUri = " + uri);
    return uri;
  }

  public static String getMimeType(Bitmap.CompressFormat format) {
    switch (format) {
      case JPEG:
      case PNG:
        return "png";
    }
    return "png";
  }

  // Handle button event /////////////////////////////////////////////////////////////////////////

  private final View.OnClickListener btnListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      switch (v.getId()) {
        case R.id.buttonDone:
          mDisposable.add(cropImage());

          break;
        case R.id.button1_1:
          mCropView.setCropMode(CropImageView.CropMode.SQUARE);
          break;
        case R.id.buttonFree:
          mCropView.setCropMode(CropImageView.CropMode.FREE);
          break;
        case R.id.buttonCircle:
          mCropView.setCropMode(CropImageView.CropMode.CIRCLE);
          break;
        case R.id.buttonRotateLeft:
          mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
          break;
        case R.id.buttonRotateRight:
          mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
          break;
        case R.id.buttonPickImage:
          pickImage();
          break;
      }
    }
  };


}
