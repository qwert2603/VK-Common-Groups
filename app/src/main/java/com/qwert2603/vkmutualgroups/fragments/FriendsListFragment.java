package com.qwert2603.vkmutualgroups.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.GroupsListActivity;
import com.qwert2603.vkmutualgroups.behaviors.FloatingActionButtonBehavior;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.ImageViewHolder;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список друзей из DataManager в соответствии с id группы, переданным в {@link #newInstance(int)}.
 */
public class FriendsListFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = "FriendsListFragment";

    private static final String groupIdKey = "groupIdKey";

    private static final int REQUEST_SEND_MESSAGE = 1;

    /**
     * groupId - id группы. В списке будут выведены друзья в этой группе.
     * Если groupId == 0, будут выведены друзья пользователя в текущем порядке сортировки из mDataManager.
     */
    public static FriendsListFragment newInstance(int groupId) {
        FriendsListFragment result = new FriendsListFragment();
        Bundle args = new Bundle();
        args.putInt(groupIdKey, groupId);
        result.setArguments(args);
        return result;
    }

    public interface Callbacks {
        @NonNull
        CoordinatorLayout getCoordinatorLayout();
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    protected ListView mListView;
    private int mListViewScrollState;
    private FriendAdapter mFriendAdapter;

    private VKUsersArray mFriends;
    private int mGroupId;

    protected AbsListView.OnScrollListener mListViewOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mListViewScrollState = scrollState;
            if (mListViewScrollState == SCROLL_STATE_IDLE) {
                // при остановке скроллинга загружаем фото видимых друзей.
                notifyDataSetChanged();
                fetchVisibleFriendsPhoto();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());
        mGroupId = getArguments().getInt(groupIdKey);
        if (mGroupId != 0) {
            VKApiCommunityFull group = mDataManager.getGroupById(mGroupId);
            mFriends = mDataManager.getFriendsInGroup(group);
        }
        else {
            mFriends = mDataManager.getUsersFriends();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                Intent intent = new Intent(getActivity(), GroupsListActivity.class);
                VKApiUserFull friend = (VKApiUserFull) mListView.getAdapter().getItem(position);
                intent.putExtra(GroupsListActivity.EXTRA_FRIEND_ID, friend.id);
                startActivity(intent);
            }
        });
        mListView.setOnScrollListener(mListViewOnScrollListener);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 друга.
                if (mListView.getCheckedItemCount() > 1) {
                    mode.finish();
                } else {
                    mActionedPosition = position;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mDataManager.getFetchingState() != finished) {
                    mode.finish();
                    return false;
                }
                mode.getMenuInflater().inflate(R.menu.friends_list_item_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_message:
                        int friendId = mFriends.get(mActionedPosition).id;
                        SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(friendId);
                        sendMessageDialogFragment.setTargetFragment(FriendsListFragment.this, REQUEST_SEND_MESSAGE);
                        sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        TextView no_friends_text_view = (TextView) view.findViewById(android.R.id.empty);
        no_friends_text_view.setText(mGroupId == 0 ? R.string.no_friends : R.string.no_friends_in_group);

        mListView.setEmptyView(no_friends_text_view);

        mFriendAdapter = new FriendAdapter(mFriends);
        mListView.setAdapter(mFriendAdapter);

        FloatingActionButton actionButton = (FloatingActionButton) view.findViewById(R.id.fragment_list_action_button);
        actionButton.setVisibility(View.INVISIBLE);

        // Прикрепляем actionButton к CoordinatorLayout активности, чтобы actionButton смещался при появлении Snackbar.
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        int margin = (int) getResources().getDimension(R.dimen.floatingActionButtonMargin);
        layoutParams.bottomMargin = layoutParams.rightMargin = margin;
        layoutParams.setBehavior(new FloatingActionButtonBehavior(getActivity(), null));

        ((ViewGroup) actionButton.getParent()).removeView(actionButton);
        CoordinatorLayout coordinatorLayout = ((Callbacks) getActivity()).getCoordinatorLayout();
        coordinatorLayout.addView(actionButton, layoutParams);

        return view;
    }

    private boolean isEverResumed = false;
    @Override
    public void onResume() {
        super.onResume();
        if (! isEverResumed) {
            isEverResumed = true;
            // при первом запуске загружаем фото первых 20 друзей.
            int e = Math.min(20, mFriends.size());
            for (int i = 0; i < e; ++i) {
                if (mPhotoManager.getPhoto(mFriends.get(i).photo_50) == null) {
                    mPhotoManager.fetchPhoto(mFriends.get(i).photo_50, listenerToUpdate);
                }
            }
        }
    }

    /**
     * Загрузить фото для отображаемых друзей и ближайших к отображаемым.
     */
    private void fetchVisibleFriendsPhoto() {
        int padding = 3;
        int b = mListView.getFirstVisiblePosition();
        int e = mListView.getLastVisiblePosition() + 1;
        int pb = Math.max(0, b - padding);
        int pe = Math.min(mFriends.size(), e + padding);
        for (int i = b; i < e; ++i) {
            if (mPhotoManager.getPhoto(mFriends.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mFriends.get(i).photo_50, listenerToUpdate);
            }
        }
        for (int i = e; i < pe; ++i) {
            if (mPhotoManager.getPhoto(mFriends.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mFriends.get(i).photo_50, null);
            }
        }
        for (int i = pb; i < b; ++i) {
            if (mPhotoManager.getPhoto(mFriends.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mFriends.get(i).photo_50, null);
            }
        }
    }

    private Listener<Bitmap> listenerToUpdate = new Listener<Bitmap>() {
        @Override
        public void onCompleted(Bitmap bitmap) {
            if (mListViewScrollState == SCROLL_STATE_IDLE) {
                notifyDataSetChanged();
            }
        }

        @Override
        public void onError(String e) {
            Log.e(TAG, e);
        }
    };

    /**
     * Обновить адаптер ListView.
     */
    public void notifyDataSetChanged() {
        if (mFriendAdapter != null) {
            mFriendAdapter.notifyDataSetChanged();
        }
    }

    private class FriendAdapter extends ArrayAdapter<VKApiUserFull> {
        public FriendAdapter(VKUsersArray users) {
            super(getActivity(),0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mPhotoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
                viewHolder.mTitleTextView = (TextView) convertView.findViewById(R.id.item_title);
                viewHolder.mCommonsTextView = (TextView) convertView.findViewById(R.id.common_count);
                convertView.setTag(viewHolder);
            }

            VKApiUserFull friend = getItem(position);
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.mPosition = position;
            if (mPhotoManager.getPhoto(friend.photo_50) != null) {
                viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(friend.photo_50));
            }
            else {
                viewHolder.mPhotoImageView.setImageBitmap(null);
                //mPhotoManager.setPhotoToImageViewHolder(viewHolder, friend.photo_50);
            }

            viewHolder.mTitleTextView.setText(getString(R.string.friend_name, friend.first_name, friend.last_name));
            if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
                int commons = mDataManager.getGroupsMutualWithFriend(friend).size();
                viewHolder.mCommonsTextView.setText(getString(R.string.mutual, commons));
            }
            else {
                viewHolder.mCommonsTextView.setText("");
            }

            return convertView;
        }
    }

    private static class ViewHolder implements ImageViewHolder {
        int mPosition;
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mCommonsTextView;

        @Override
        public int getPosition() {
            return mPosition;
        }

        @Override
        public ImageView getImageView() {
            return mPhotoImageView;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SEND_MESSAGE && resultCode == Activity.RESULT_OK) {
            if (getView() != null) {
                Snackbar.make(((Callbacks) getActivity()).getCoordinatorLayout(),
                        R.string.message_sent, Snackbar.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
