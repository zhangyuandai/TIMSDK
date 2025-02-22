package com.tencent.qcloud.tuikit.tuigroup.ui.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.component.activities.ImageSelectActivity;
import com.tencent.qcloud.tuicore.component.fragments.BaseFragment;
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback;
import com.tencent.qcloud.tuicore.util.ScreenUtil;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.tuigroup.R;
import com.tencent.qcloud.tuikit.tuigroup.TUIGroupService;
import com.tencent.qcloud.tuikit.tuigroup.TUIGroupConstants;
import com.tencent.qcloud.tuikit.tuigroup.bean.GroupInfo;
import com.tencent.qcloud.tuikit.tuigroup.presenter.GroupInfoPresenter;
import com.tencent.qcloud.tuikit.tuigroup.ui.view.GroupInfoLayout;
import com.tencent.qcloud.tuikit.tuigroup.ui.interfaces.IGroupMemberListener;

import java.util.ArrayList;
import java.util.List;


public class GroupInfoFragment extends BaseFragment {
    private static final int CHOOSE_AVATAR_REQUEST_CODE = 101;

    private View baseView;
    private GroupInfoLayout groupInfoLayout;

    private String groupId;

    private GroupInfoPresenter groupInfoPresenter = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        baseView = inflater.inflate(R.layout.group_info_fragment, container, false);
        initView();
        return baseView;
    }

    private void initView() {
        Bundle bundle = getArguments();
        if (bundle == null) {
            ToastUtil.toastShortMessage("groupId is empty. bundle is null");
            return;
        }
        groupId = bundle.getString(TUIGroupConstants.Group.GROUP_ID);
        groupInfoLayout = baseView.findViewById(R.id.group_info_layout);
        // 新建 presenter 与 layout 互相绑定
        groupInfoPresenter = new GroupInfoPresenter(groupInfoLayout);
        groupInfoLayout.setGroupInfoPresenter(groupInfoPresenter);
        groupInfoLayout.setOnModifyGroupAvatarListener(new OnModifyGroupAvatarListener() {
            @Override
            public void onModifyGroupAvatar(String originAvatarUrl) {
                ArrayList<String> faceList = new ArrayList<>();
                for (int i = 0; i < TUIGroupConstants.GROUP_FACE_COUNT; i++) {
                    faceList.add(String.format(TUIGroupConstants.GROUP_FACE_URL, (i + 1) + ""));
                }

                Intent intent = new Intent(getContext(), ImageSelectActivity.class);
                intent.putExtra(ImageSelectActivity.TITLE, getResources().getString(R.string.group_choose_avatar));
                intent.putExtra(ImageSelectActivity.SPAN_COUNT, 4);
                intent.putExtra(ImageSelectActivity.PLACEHOLDER, R.drawable.core_default_user_icon_light);
                intent.putExtra(ImageSelectActivity.ITEM_WIDTH, ScreenUtil.dip2px(77));
                intent.putExtra(ImageSelectActivity.ITEM_HEIGHT, ScreenUtil.dip2px(77));
                intent.putExtra(ImageSelectActivity.DATA, faceList);
                intent.putExtra(ImageSelectActivity.SELECTED, originAvatarUrl);
                startActivityForResult(intent, CHOOSE_AVATAR_REQUEST_CODE);
            }
        });
        groupInfoLayout.loadGroupInfo(groupId);
        groupInfoLayout.setRouter(new IGroupMemberListener() {
            @Override
            public void forwardListMember(GroupInfo info) {
                Intent intent = new Intent(getContext(), GroupMemberActivity.class);
                intent.putExtra(TUIGroupConstants.Group.GROUP_INFO, info);
                startActivity(intent);
            }

            @Override
            public void forwardAddMember(GroupInfo info) {
                Bundle param = new Bundle();
                param.putString(TUIGroupConstants.Group.GROUP_ID, info.getId());
                param.putBoolean(TUIGroupConstants.Selection.SELECT_FRIENDS, true);
                TUICore.startActivity(GroupInfoFragment.this, "StartGroupMemberSelectActivity", param, 1);
            }

            @Override
            public void forwardDeleteMember(GroupInfo info) {
                Bundle param = new Bundle();
                param.putString(TUIGroupConstants.Group.GROUP_ID, info.getId());
                param.putBoolean(TUIGroupConstants.Selection.SELECT_FOR_CALL, true);
                TUICore.startActivity(GroupInfoFragment.this, "StartGroupMemberSelectActivity", param, 2);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 3) {
            List<String> friends = (List<String>) data.getSerializableExtra(TUIGroupConstants.Selection.LIST);
            if (requestCode == 1) {
                inviteGroupMembers(friends);
            } else if (requestCode == 2) {
                deleteGroupMembers(friends);
            }
        } else if (requestCode == CHOOSE_AVATAR_REQUEST_CODE && resultCode == ImageSelectActivity.RESULT_CODE_SUCCESS) {
            if (data != null) {
                String avatarUrl = data.getStringExtra(ImageSelectActivity.DATA);
                modifyGroupAvatar(avatarUrl);
            }
        }
    }

    private void modifyGroupAvatar(String avatarUrl) {
        groupInfoLayout.modifyGroupAvatar(avatarUrl);
    }

    private void deleteGroupMembers(List<String> friends) {
        if (friends != null && friends.size() > 0) {
            if (groupInfoPresenter != null) {
                groupInfoPresenter.deleteGroupMembers(groupId, friends, new IUIKitCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> data) {
                        groupInfoPresenter.loadGroupInfo(groupId);
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {

                    }
                });
            }
        }
    }

    private void inviteGroupMembers(List<String> friends) {
        if (friends != null && friends.size() > 0) {
            groupInfoPresenter.inviteGroupMembers(groupId, friends, new IUIKitCallback<Object>() {
                @Override
                public void onSuccess(Object data) {
                    if (data instanceof String) {
                        ToastUtil.toastLongMessage(data.toString());
                    } else {
                        ToastUtil.toastLongMessage(TUIGroupService.getAppContext().getString(R.string.invite_suc));
                    }
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    ToastUtil.toastLongMessage(TUIGroupService.getAppContext().getString(R.string.invite_fail) + errCode + "=" + errMsg);
                }
            });
        }
    }

    public void changeGroupOwner(String newOwnerId) {
        groupInfoPresenter.transferGroupOwner(groupId, newOwnerId, new IUIKitCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                groupInfoLayout.loadGroupInfo(groupId);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {

            }
        });
    }


    public interface OnModifyGroupAvatarListener {
        void onModifyGroupAvatar(String originAvatarUrl);
    }

}
