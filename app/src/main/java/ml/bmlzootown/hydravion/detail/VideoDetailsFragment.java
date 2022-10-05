package ml.bmlzootown.hydravion.detail;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.app.DetailsSupportFragmentBackgroundController;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import ml.bmlzootown.hydravion.R;
import ml.bmlzootown.hydravion.browse.MainActivity;
import ml.bmlzootown.hydravion.browse.MainFragment;
import ml.bmlzootown.hydravion.client.HydravionClient;
import ml.bmlzootown.hydravion.client.RequestTask;
import ml.bmlzootown.hydravion.models.Level;
import ml.bmlzootown.hydravion.models.Video;
import ml.bmlzootown.hydravion.models.VideoInfo;
import ml.bmlzootown.hydravion.playback.PlaybackActivity;

public class VideoDetailsFragment extends DetailsSupportFragment {

    private static final String TAG = "VideoDetailsFragment";

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RESUME = 3;
    private static final int ACTION_RES = 2;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private HydravionClient client;

    private Video mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsSupportFragmentBackgroundController mDetailsBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDetailsBackground = new DetailsSupportFragmentBackgroundController(this);
        mSelectedMovie = (Video) getActivity().getIntent().getSerializableExtra(DetailsActivity.Video);

        if (mSelectedMovie != null) {
            //String mSelectedUrl = getActivity().getIntent().getStringExtra("vidURL");
            mPresenterSelector = new ClassPresenterSelector();
            mAdapter = new ArrayObjectAdapter(mPresenterSelector);
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setAdapter(mAdapter);
            setOnItemViewClickedListener(new ItemViewClickedListener());
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        client = HydravionClient.Companion.getInstance(requireContext(), requireActivity().getPreferences(Context.MODE_PRIVATE));
        initializeBackground();
    }

    private void initializeBackground() {
        mDetailsBackground.enableParallax();
        client.getCreatorById(mSelectedMovie.getCreator().getId(), creator -> {
            Glide.with(requireActivity())
                    .asBitmap()
                    .load(new GlideUrl(creator.getCoverImage().getPath(), new LazyHeaders.Builder()
                            .addHeader("User-Agent", "Hydravion (AndroidTV), CFNetwork")
                            .build())
                        )
                    .override(1800, 519)
                    .centerCrop()
                    .error(R.drawable.default_background)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            mDetailsBackground.setCoverBitmap(resource);
                            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            return Unit.INSTANCE;
        });
    }

    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
        row.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_background));
        Glide.with(requireActivity())
                .load(new GlideUrl(mSelectedMovie.getThumbnail().getPath(), new LazyHeaders.Builder()
                        .addHeader("User-Agent", "Hydravion (AndroidTV), CFNetwork")
                        .build())
                )
                .centerCrop()
                .transform(new RoundedCorners(48))
                .error(R.drawable.default_background)
                .into(new CustomTarget<Drawable>() {

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();
        boolean isLive =  mSelectedMovie.getType().equalsIgnoreCase("live");

        // add RESUME first so it is the default
        if (!isLive) {

            if (mSelectedMovie.getVideoInfo().getProgress() > 0) {
                actionAdapter.add(new Action(ACTION_RESUME, getString(R.string.action_resume)));
            }
        }

        actionAdapter.add(new Action(ACTION_PLAY, getString(R.string.play)));

        if (!isLive) {
            actionAdapter.add(new Action(ACTION_RES, getString(R.string.resolutions)));
        }

        row.setActionsAdapter(actionAdapter);

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.default_background));

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);

        detailsPresenter.setOnActionClickedListener(action -> {
            if (action.getId() == ACTION_PLAY) {
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra(DetailsActivity.Video, (Serializable) mSelectedMovie);
                intent.putExtra(DetailsActivity.Resume, false);
                startActivity(intent);
            } else if (action.getId() == ACTION_RESUME) {
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra(DetailsActivity.Video, (Serializable) mSelectedMovie);
                intent.putExtra(DetailsActivity.Resume, true);
                startActivity(intent);
            } else if (action.getId() == ACTION_RES) {
                client.getPost(mSelectedMovie.getGuid(), post -> {
                    String guid = post.getVideoAttachments().get(0).getGuid();
                    if (!guid.isEmpty()) {
                        client.getVideoInfo(guid, videoInfo -> {
                            List<Level> levels = videoInfo.getLevels();
                            List<String> resolutions = new ArrayList<>();
                            for (Level l : levels) {
                                resolutions.add(l.getName());
                            }
                            CharSequence[] res = resolutions.toArray(new CharSequence[resolutions.size()]);

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Resolutions");
                            builder.setItems(res, (dialog, which) -> {
                                        String res1 = resolutions.get(which);

                                        client.getVideo(mSelectedMovie, res1, newVideo -> {
                                            mSelectedMovie.setVidUrl(newVideo.getVidUrl());
                                            Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                                            intent.putExtra(DetailsActivity.Video, (Serializable) mSelectedMovie);
                                            startActivity(intent);
                                            return Unit.INSTANCE;
                                        });
                                    });
                            builder.create().show();
                            return Unit.INSTANCE;
                        });
                    }
                    return Unit.INSTANCE;
                });
            } else {
                Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            if (item instanceof Video) {
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.movie), (Serializable) mSelectedMovie);

                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                itemViewHolder.view.findViewById(R.id.image),
                                DetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }
}
