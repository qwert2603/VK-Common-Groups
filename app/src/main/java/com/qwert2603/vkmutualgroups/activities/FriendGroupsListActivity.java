package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Все группы друга.
 */
public class FriendGroupsListActivity extends GroupsListActivity implements AbstractVkListFragment.Callbacks {

    private VKApiUserFull mFriend;

    private DataManager mDataManager;

    private CoordinatorLayout mCoordinatorLayout;
    private TextView mErrorTextView;
    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCoordinatorLayout = getCoordinatorLayout();
        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        mDataManager = DataManager.get(this);

        mErrorTextView = getErrorTextView();
        mErrorTextView.setText(R.string.loading_failed);
        mErrorTextView.setVisibility(View.INVISIBLE);

        mRefreshLayout = getRefreshLayout();
        mRefreshLayout.setOnRefreshListener(this::fetchFriendGroups);

        getActionButton().setVisibility(View.INVISIBLE);

        setListFragment(null);
        fetchFriendGroups();
    }

    private void fetchFriendGroups() {
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));

        mDataManager.fetchUsersGroups(mFriend.id, new Listener<VKApiCommunityArray>() {
            @Override
            public void onCompleted(VKApiCommunityArray vkApiCommunityFulls) {
                mErrorTextView.setVisibility(View.INVISIBLE);
                setListFragment(GroupsListFragment.newInstance(vkApiCommunityFulls, getString(R.string.no_groups)));
                mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(false));
                mRefreshLayout.setEnabled(true);
                Snackbar.make(mCoordinatorLayout, R.string.groups_list_loaded, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String e) {
                if (! (getListFragment() instanceof AbstractVkListFragment)) {
                    mErrorTextView.setVisibility(View.VISIBLE);
                }
                mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(false));
                mRefreshLayout.setEnabled(true);
                Snackbar.make(mCoordinatorLayout, R.string.loading_failed, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.refresh, (v) -> fetchFriendGroups())
                        .show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_view_groups).setVisible(false);
        return true;
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        boolean b = (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);
        mRefreshLayout.setEnabled(b);
    }

}
