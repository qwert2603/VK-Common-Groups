package com.alex.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alex.vkmutualgroups.R;
import com.alex.vkmutualgroups.data.DataManager;
import com.alex.vkmutualgroups.fragments.ScrollCallbackableFriendsListFragment;
import com.alex.vkmutualgroups.photo.PhotoManager;
import com.alex.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import static com.alex.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.alex.vkmutualgroups.data.DataManager.FetchingState.notStarted;
import static com.alex.vkmutualgroups.data.DataManager.FriendsSortState.byMutual;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends AppCompatActivity implements ScrollCallbackableFriendsListFragment.Callbacks {

    private static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS, VKScope.MESSAGES };

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private TextView mErrorTextView;

    private SwipeRefreshLayout mRefreshLayout;

    private boolean mIsFetchingErrorHappened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_friends_list);
        setEmptyFragment();

        mDataManager = DataManager.get(this);
        mPhotoManager = PhotoManager.get(this);

        mErrorTextView = (TextView) findViewById(R.id.error_text_view);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);
        mRefreshLayout.setOnRefreshListener(this::refreshData);

        if (VKSdk.isLoggedIn()) {
            if (mDataManager.getFetchingState() == notStarted) {
                loadFromDevice();
            }
        } else {
            VKSdk.login(this, LOGIN_SCOPE);
        }

        updateUI();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                if (mDataManager.getFetchingState() == notStarted) {
                    fetchFromVK();
                    updateUI();
                }
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(LoadingFriendsListActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                VKSdk.login(LoadingFriendsListActivity.this, LOGIN_SCOPE);
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateUI() {
        invalidateOptionsMenu();
        mErrorTextView.setVisibility(View.INVISIBLE);
        if (! VKSdk.isLoggedIn()) {
            setEmptyFragment();
            return;
        }
        if (mIsFetchingErrorHappened) {
            onFetchingErrorUI();
        } else {
            switch (mDataManager.getFetchingState()) {
                case notStarted:
                    setEmptyFragment();
                    break;
                case loadingFriends:
                    break;
                case calculatingMutual:
                    notifyFragmentDataSetChanged();
                    break;
                case finished:
                    notifyFragmentDataSetChanged();
                    break;
            }
        }
    }

    private void onFetchingErrorUI() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if(fragment != null && fragment instanceof ScrollCallbackableFriendsListFragment) {
            ((ScrollCallbackableFriendsListFragment) fragment).notifyDataSetChanged();
            Snackbar.make(mErrorTextView, R.string.error_message, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.refresh, (v) -> refreshData())
                    .show();
        } else {
            mErrorTextView.setVisibility(View.VISIBLE);
        }
    }

    private void notifyFragmentDataSetChanged() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if(fragment != null && fragment instanceof ScrollCallbackableFriendsListFragment) {
            ((ScrollCallbackableFriendsListFragment) fragment).notifyDataSetChanged();
        } else {
            refreshScrollCallbackableFriendsListFragment();
        }
    }

    private void loadFromDevice() {
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));
        mIsFetchingErrorHappened = false;
        mDataManager.loadFromDevice(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshScrollCallbackableFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Snackbar.make(mRefreshLayout, R.string.loading_completed, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
                mRefreshLayout.setRefreshing(false);
                fetchFromVK();
            }
        });
    }

    private void fetchFromVK() {
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));
        mIsFetchingErrorHappened = false;
        mDataManager.fetchFromVK(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshScrollCallbackableFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Snackbar.make(mRefreshLayout, R.string.loading_completed, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                mIsFetchingErrorHappened = true;
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Log.e("AASSDD", e);
            }
        });
    }

    private void refreshData() {
        if (InternetUtils.isInternetConnected(this)) {
            fetchFromVK();
            invalidateOptionsMenu();
        }
        else {
            mRefreshLayout.setRefreshing(false);
            Snackbar.make(mErrorTextView, R.string.no_internet_connection, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.refresh, (v) -> refreshData())
                    .show();
        }
    }

    private void refreshScrollCallbackableFriendsListFragment() {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, ScrollCallbackableFriendsListFragment.newInstance(0))
                .commitAllowingStateLoss();
    }

    private void setEmptyFragment() {
        FragmentManager fm = getFragmentManager();

        // чтобы mRefreshLayout нормально отображался.
        // Просто пустой фрагмент для фона.
        ListFragment fragment = new ListFragment();
        fragment.setListAdapter(new ArrayAdapter<>(this, 0, new String[]{}));
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.loading_friends_list_activity, menu);

        MenuItem sortMenuItem = menu.findItem(R.id.menu_sort);
        sortMenuItem.setChecked(mDataManager.getFriendsSortState() == byMutual);

        MenuItem groupsListMenuItem = menu.findItem(R.id.menu_groups_list);
        if (mDataManager.getFetchingState() != finished) {
            sortMenuItem.setEnabled(false);
            groupsListMenuItem.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                switch (mDataManager.getFriendsSortState()) {
                    case byAlphabet:
                        mDataManager.sortFriendsByMutual();
                        break;
                    case byMutual:
                        mDataManager.sortFriendsByAlphabet();
                        break;
                }
                refreshScrollCallbackableFriendsListFragment();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_groups_list:
                Intent intent = new Intent(this, UserGroupListActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_logout:
                if (VKSdk.isLoggedIn()) {
                    VKSdk.logout();
                    mDataManager.clear();
                    mDataManager.clearDataOnDevice();
                    mPhotoManager.clearPhotosOnDevice();
                    updateUI();
                    VKSdk.login(this, LOGIN_SCOPE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        boolean b = (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);
        mRefreshLayout.setEnabled(b);
    }
}
