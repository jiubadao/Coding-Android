package net.coding.program.task;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;

import net.coding.program.BaseFragmentActivity;
import net.coding.program.Global;
import net.coding.program.R;
import net.coding.program.common.ClickSmallImage;
import net.coding.program.common.DatePickerFragment;
import net.coding.program.common.HtmlContent;
import net.coding.program.common.ImageLoadTool;
import net.coding.program.common.MyImageGetter;
import net.coding.program.common.PhotoOperate;
import net.coding.program.common.StartActivity;
import net.coding.program.common.TextWatcherAt;
import net.coding.program.common.comment.BaseCommentHolder;
import net.coding.program.common.enter.EnterLayout;
import net.coding.program.maopao.item.ContentAreaBase;
import net.coding.program.model.AccountInfo;
import net.coding.program.model.TaskObject;
import net.coding.program.model.UserObject;
import net.coding.program.project.detail.MembersActivity_;
import net.coding.program.project.detail.TaskListFragment;
import net.coding.program.third.EmojiFilter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringArrayRes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

@EActivity(R.layout.activity_task_add)
public class TaskAddActivity extends BaseFragmentActivity implements StartActivity, DatePickerFragment.DateSet {

    @Extra
    TaskObject.SingleTask mSingleTask;

    @Extra
    UserObject mUserOwner;

    @Extra
    TaskJumpParams mJumpParams;

    @ViewById
    ListView listView;

    View mHeadView;
    EditText title;
    ImageView circleIcon;
    TextView name;
    TextView status;
    LinearLayout linearlayout2;
    LinearLayout linearlayout3;
    TextView priority;
    TextView deadline;
    TextView description;
    ViewGroup descriptionLayout;

    TaskObject.TaskDescription descriptionData = new TaskObject.TaskDescription();
    TaskObject.TaskDescription descriptionDataNew = new TaskObject.TaskDescription();

    @StringArrayRes
    String strings_priority[];
    private TextView commentCount;

    TaskParams mNewParam;
    TaskParams mOldParam;

    EnterLayout mEnterLayout;

    final String HOST_COMMENT_ADD = Global.HOST + "/api/task/%s/comment";

    String HOST_DESCRIPTER = Global.HOST + "/api/task/%s/description";

    @AfterViews
    void init() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        initControl();

        if (mJumpParams == null) {
            initData();
        } else { // 跳转过来的，要先取得任务数据
            final String hostTaskDetail = Global.HOST + "/api/user/%s/project/%s/task/%s";
            String url = String.format(hostTaskDetail, mJumpParams.userKey, mJumpParams.projectName, mJumpParams.taskId);
            getNetwork(url, tagTaskDetail);
            showDialogLoading();
        }
    }

    private void initControl() {
        mEnterLayout = new EnterLayout(this, mOnClickSendText, EnterLayout.Type.TextOnly);

        mHeadView = mInflater.inflate(R.layout.activity_task_add_head, null);
        title = (EditText) mHeadView.findViewById(R.id.title);
        circleIcon = (ImageView) mHeadView.findViewById(R.id.circleIcon);
        name = (TextView) mHeadView.findViewById(R.id.name);
        status = (TextView) mHeadView.findViewById(R.id.status);
        priority = (TextView) mHeadView.findViewById(R.id.priority);
        linearlayout2 = (LinearLayout) mHeadView.findViewById(R.id.linearlayout2);
        linearlayout3 = (LinearLayout) mHeadView.findViewById(R.id.linearlayout3);
        deadline = (TextView) mHeadView.findViewById(R.id.deadline);
        descriptionLayout = (ViewGroup) mHeadView.findViewById(R.id.descriptionLayout);
        description = (TextView) mHeadView.findViewById(R.id.description);
        commentCount = (TextView) mHeadView.findViewById(R.id.commentCount);
        listView.addHeaderView(mHeadView);
    }

    private void updateSendButton() {
        if (title.getText().toString().isEmpty()
                || (mNewParam.equals(mOldParam) && !descripChange())) {
            enableSendButton(false);
        } else {
            enableSendButton(true);
        }
    }

    private boolean descripChange() {
        if (mSingleTask.id.isEmpty()) {
            return false;
        } else {
            return descriptionData.markdown.equals(descriptionDataNew.markdown);
        }
    }

    private void enableSendButton(boolean enable) {
        if (mMenuSave == null) {
            return;
        }

        if (enable) {
            mMenuSave.setIcon(R.drawable.ic_menu_ok);
            mMenuSave.setEnabled(true);
        } else {
            mMenuSave.setIcon(R.drawable.ic_menu_ok_unable);
            mMenuSave.setEnabled(false);
        }
    }

    void initData() {
        if (mSingleTask.id.isEmpty()) {
            if (mUserOwner.id.isEmpty()) {
                mSingleTask.owner = AccountInfo.loadAccount(this);
            } else {
                mSingleTask.owner = mUserOwner;
            }
            mSingleTask.owner_id = mSingleTask.owner.id;
            mSingleTask.priority = 1; // 默认优先级是 1：正常处理
        }

        mNewParam = new TaskParams(mSingleTask);
        mOldParam = new TaskParams(mSingleTask);

        mEnterLayout.content.addTextChangedListener(new TextWatcherAt(this, this, RESULT_REQUEST_FOLLOW, mSingleTask.project));

        setHeadData();
        addFoot();
        listView.setAdapter(commentAdpter);
        mStatusAdapter = new StatusBaseAdapter();
        mPriorityAdapter = new PriorityAdapter();

        selectMember();

        if (mSingleTask.id.isEmpty()) {
            status.setText("未完成");
            linearlayout2.setVisibility(View.GONE);
            mEnterLayout.hide();

            findViewById(R.id.layoutListHeadBottom).setVisibility(View.GONE);

        } else {
            title.setText(mSingleTask.content);
            getActionBar().setTitle("编辑任务");

            setStatus();
            setPriority();
            linearlayout2.setVisibility(View.VISIBLE);
        }

        setDeadline();

//        initDescription();

        urlComments = String.format(HOST_FORMAT_TASK_COMMENT, mSingleTask.id);
        getNextPageNetwork(urlComments, HOST_FORMAT_TASK_COMMENT);

    }

    private void initDescription() {
        if (mSingleTask.id.isEmpty()) {
            descriptionLayout.setOnClickListener(onClickCreateDescription);
        } else {
            if (mSingleTask.has_description) {
                description.setText("载入备注中...");
                HOST_DESCRIPTER = String.format(HOST_DESCRIPTER, mSingleTask.id);
                getNetwork(HOST_DESCRIPTER, HOST_DESCRIPTER);
            } else {
                descriptionLayout.setOnClickListener(onClickCreateDescription);
            }
        }
    }

    MenuItem mMenuSave;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.task_add, menu);

        mMenuSave = menu.findItem(R.id.action_save);
        updateSendButton();

        return super.onCreateOptionsMenu(menu);
    }

    private void setHeadData() {
        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mNewParam.content = s.toString();
                updateSendButton();
            }
        });
        title.setText("");

        TextView time = (TextView) mHeadView.findViewById(R.id.time);
        TextView createName = (TextView) mHeadView.findViewById(R.id.createrName);

        View delete = mHeadView.findViewById(R.id.delete);
        if (mSingleTask.id.isEmpty()) {
            delete.setVisibility(View.GONE);
        } else {
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialog("删除任务", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TaskObject.SingleTask task = mSingleTask;
                            String url = String.format(TaskListFragment.hostTaskDelete, task.project.owner_user_name, task.project.name, task.id);
                            deleteNetwork(url, TaskListFragment.hostTaskDelete);
                            showProgressBar(true);
                        }
                    });
                }
            });
        }

        mHeadView.findViewById(R.id.linearlayout1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MembersActivity_
                        .intent(TaskAddActivity.this)
                        .mProjectObjectId(mSingleTask.project_id)
                        .startForResult(RESULT_REQUEST_SELECT_USER);
            }
        });

        linearlayout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popListSelectDialog(
                        "阶段",
                        mStatusAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    mNewParam.status = 1; // "未完成"
                                } else {
                                    mNewParam.status = 2;
                                }

                                setStatus();
                                updateSendButton();
                            }
                        });
            }
        });

        linearlayout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();

                Bundle bundle = new Bundle();
                bundle.putString("date", mNewParam.deadline);
                bundle.putBoolean("clear", true);
                newFragment.setArguments(bundle);


                newFragment.setCancelable(true);
                newFragment.show(getSupportFragmentManager(), "datePicker");
                getSupportFragmentManager().executePendingTransactions();
                dialogTitleLineColor(newFragment.getDialog());
            }
        });

        ViewGroup layout3 = (ViewGroup) mHeadView.findViewById(R.id.layout3);
        layout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popListSelectDialog("优先级",
                        mPriorityAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mNewParam.priority = priorityDrawable.length - 1 - which;
                                setPriority();
                                updateSendButton();
                            }
                        });
            }
        });

        if (mSingleTask.id.isEmpty()) {
            createName.setText(mNewParam.owner.name);
            time.setText(String.format("现在"));
        } else {
            createName.setText(mSingleTask.creator.name);
            time.setText(String.format(Global.dayToNow(mSingleTask.created_at)));
        }
    }

    View listFoot;

    private void addFoot() {
        if (mSingleTask.id.isEmpty()) {
            return;
        }

        listFoot = mInflater.inflate(R.layout.task_comment_empty, null);
        listFoot.setVisibility(View.GONE);
        listView.addFooterView(listFoot);
    }

    final int priorityDrawable[] = new int[]{
            R.drawable.ic_task_priority_0,
            R.drawable.ic_task_priority_1,
            R.drawable.ic_task_priority_2,
            R.drawable.ic_task_priority_3
    };

    private void setStatus() {
        if (mNewParam.status == 1) {
            status.setText("未完成");
        } else {
            status.setText("已完成");
        }
    }

    public void dateSetResult(String date, boolean clear) {
        if (clear) {
            mNewParam.deadline = "";
        } else {
            mNewParam.deadline = date;
        }

        setDeadline();
        updateSendButton();
    }

    private void setPriority() {
        priority.setText(strings_priority[mNewParam.priority]);
    }

    private void setDeadline() {
        String s = mNewParam.deadline;
        if (s.isEmpty()) {
            s = "未指定";
        }

        deadline.setText(s);
    }

    private void setDescription() {
        Global.MessageParse parseData = HtmlContent.parseReplacePhoto(descriptionDataNew.description);
        description.setText(Html.fromHtml(parseData.text, myImageGetter, Global.tagHandler));
    }

    final String HOST_FORMAT_TASK_CONTENT = Global.HOST + "/api/user/%s/project/%s/task/%s";
    final String HOST_TASK_ADD = Global.HOST + "/api%s/task";
    final String HOST_TASK_DEL = Global.HOST + "/api/task/%s";
    final String HOST_FORMAT_TASK_COMMENT = Global.HOST + "/api/task/%s/comments?pageSize=200";

    final String HOST_TASK_UPDATE = Global.HOST + "/api/task/%s/update";
    final String TAG_TASK_UPDATE = "TAG_TASK_UPDATE";

    String urlComments = "";

    @OptionsItem
    void action_save() {
        String content = title.getText().toString();
        if (EmojiFilter.containsEmoji(content)) {
            showMiddleToast("暂不支持发表情");
            return;
        }

        if (mSingleTask.id.isEmpty()) {
            String url = String.format(HOST_TASK_ADD, mSingleTask.project.backend_project_path);
            RequestParams params = new RequestParams();
            params.put("content", content);
            params.put("status", mNewParam.status);
            params.put("priority", mNewParam.priority);
            params.put("owner_id", mNewParam.ownerId);
            params.put("deadline", mNewParam.deadline);
            if (!descriptionDataNew.markdown.isEmpty()) {
                params.put("description", descriptionDataNew.markdown);
            }
            postNetwork(url, params, HOST_TASK_ADD);
            showProgressBar(true);

        } else {
            String url = String.format(HOST_TASK_UPDATE, mSingleTask.id);
            RequestParams params = new RequestParams();
            if (!content.equals(mSingleTask.content)) {
                params.put("content", content);
            }
            if (mNewParam.status != mSingleTask.status) {
                params.put("status", mNewParam.status);
            }
            if (mNewParam.priority != mSingleTask.priority) {
                params.put("priority", mNewParam.priority);
            }
            if (!mNewParam.ownerId.equals(mSingleTask.owner_id)) {
                params.put("owner_id", mNewParam.ownerId);
            }
            if (!mNewParam.deadline.equals(mSingleTask.deadline)) {
                params.put("deadline", mNewParam.deadline);
            }

            String oldData = descriptionData.markdown;
            if (oldData != null && !oldData.equals(descriptionDataNew.markdown)) {
                params.put("description", descriptionDataNew.markdown);
            }

            putNetwork(url, params, TAG_TASK_UPDATE);
            showProgressBar(true);
        }
    }

    void selectMember() {
        name.setText(mNewParam.owner.name);
        ImageLoader.getInstance().displayImage(Global.makeSmallUrl(circleIcon, mNewParam.owner.avatar), circleIcon);
    }

    private void updateCommentCount() {
        int count = mData.size();
        String foramt = "%d 条评论";
        commentCount.setText(String.format(foramt, count));
        if (count == 0) {
//            listFoot.setVisibility(View.VISIBLE);
//            listFoot.setPadding(0, 0, 0, 0);
            listView.addFooterView(listFoot);
        } else {
//            listFoot.setVisibility(View.GONE);
//            listFoot.setPadding(0, -listFoot.getHeight(), 0, 0);
            listView.removeFooterView(listFoot);
        }
    }

    final String hostDeleteComment = Global.HOST + "/api/task/%s/comment/%s";
    final String tagTaskDetail = "tagTaskDetail";

    public static final String RESULT_GLOBARKEY = "RESULT_GLOBARKEY";

    private void closeActivity(String msg) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_GLOBARKEY, mNewParam.owner.global_key);
        setResult(Activity.RESULT_OK, intent);
        showButtomToast(msg);
        finish();
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(HOST_TASK_ADD)) {
            showProgressBar(false);
            if (code == 0) {
                closeActivity("新建任务成功");
            } else {
                showErrorMsg(code, respanse);
            }

        } else if (tag.equals(HOST_FORMAT_TASK_COMMENT)) {
            if (code == 0) {
                JSONArray jsonArray = respanse.getJSONObject("data").getJSONArray("list");
                for (int i = 0; i < jsonArray.length(); ++i) {
                    mData.add(new TaskObject.TaskComment(jsonArray.getJSONObject(i)));
                }
                commentAdpter.notifyDataSetChanged();

                updateCommentCount();

            } else {
                showErrorMsg(code, respanse);
            }

        } else if (tag.equals(HOST_COMMENT_ADD)) {
            if (code == 0) {
                TaskObject.TaskComment item = new TaskObject.TaskComment(respanse.getJSONObject("data"));
                mData.add(0, item);
                mEnterLayout.clearContent();
                mEnterLayout.hideKeyboard();
                mEnterLayout.content.setHint("");
                mEnterLayout.content.setTag(null);

                commentAdpter.notifyDataSetChanged();
                updateCommentCount();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(hostDeleteComment)) {
            if (code == 0) {
                String commentId = (String) data;
                for (int i = 0; i < mData.size(); ++i) {
                    if (mData.get(i).id.equals(commentId)) {
                        mData.remove(i);
                        break;
                    }
                }
                commentAdpter.notifyDataSetChanged();
                updateCommentCount();
                showButtomToast("删除成功");

            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(TAG_TASK_UPDATE)) {
            showProgressBar(false);
            if (code == 0) {
                closeActivity("修改任务成功");
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(TaskListFragment.hostTaskDelete)) {
            showProgressBar(false);
            if (code == 0) {
                closeActivity("删除任务成功");
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(tagTaskDetail)) {
            if (code == 0) {
                mSingleTask = new TaskObject.SingleTask(respanse.getJSONObject("data"));
                initData();
            } else {
                showErrorMsg(code, respanse);
            }
            hideProgressDialog();

        } else if (tag.equals(HOST_DESCRIPTER)) {
            if (code == 0) {
                descriptionData = new TaskObject.TaskDescription(respanse.getJSONObject("data"));
                descriptionDataNew = new TaskObject.TaskDescription(descriptionData);
                setDescription();

                description.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TaskDescriptionActivity_
                                .intent(TaskAddActivity.this)
                                .descriptionData(descriptionDataNew)
                                .taskId(mSingleTask.id)
                                .startForResult(RESULT_REQUEST_DESCRIPTION);
                    }
                });

            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_PREVIEW)) {
            if (code == 0) {
                descriptionDataNew.description = respanse.optString("data", "");
                setDescription();
            } else {
                showButtomToast("发生错误");
            }

            hideProgressDialog();
        }
    }

    private PhotoOperate photoOperate = new PhotoOperate(this);

    public static final int RESULT_REQUEST_SELECT_USER = 3;
    public static final int RESULT_REQUEST_FOLLOW = 1002;

    public static final int RESULT_REQUEST_DESCRIPTION = 4;
    public static final int RESULT_REQUEST_DESCRIPTION_CREATE = 5;


    @OnActivityResult(RESULT_REQUEST_DESCRIPTION)
    void resultDescription(int result, Intent data) {
        if (result == RESULT_OK) {
            updateDescriptionFromResult(data);
        }
    }

    @OnActivityResult(RESULT_REQUEST_DESCRIPTION_CREATE)
    void resultDescriptionCreate(int result, Intent data) {
        if (result == RESULT_OK) {
            updateDescriptionFromResult(data);
        }
    }

    final String HOST_PREVIEW = Global.HOST + "/api/markdown/preview";

    void updateDescriptionFromResult(Intent data) {
        descriptionDataNew.markdown = data.getStringExtra("data");

        RequestParams params = new RequestParams();
        params.put("content", descriptionDataNew.markdown);
        postNetwork(HOST_PREVIEW, params, HOST_PREVIEW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_REQUEST_SELECT_USER) {
            if (resultCode == Activity.RESULT_OK) {
                TaskObject.Members member = (TaskObject.Members) data.getSerializableExtra("members");
                mNewParam.ownerId = member.user.id;
                mNewParam.owner = member.user;
                selectMember();
                updateSendButton();
            }
        } else if (requestCode == RESULT_REQUEST_PHOTO) {
            // 这个版本暂时不支持发图片
            if (resultCode == Activity.RESULT_OK) {
            }

        } else if (requestCode == RESULT_REQUEST_FOLLOW) {
            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra("name");
                mEnterLayout.insertText(name);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OptionsItem(android.R.id.home)
    void close() {
        onBackPressed();
    }

    StatusBaseAdapter mStatusAdapter;

    public class StatusBaseAdapter extends BaseAdapter {

        String[] mData;

        public StatusBaseAdapter() {
            mData = getResources().getStringArray(R.array.task_status);
        }

        @Override
        public int getCount() {
            return mData.length;
        }

        @Override
        public Object getItem(int position) {
            return mData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.activity_task_status_list_item, parent, false);
                holder.mTitle = (TextView) convertView.findViewById(R.id.title);
                holder.mCheck = (ImageView) convertView.findViewById(R.id.check);
                holder.mIcon = convertView.findViewById(R.id.icon);
                holder.mIcon.setVisibility(View.GONE);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.mTitle.setText(mData[position]);
            if (position == getSelectPos())
                holder.mCheck.setVisibility(View.VISIBLE);
            else
                holder.mCheck.setVisibility(View.GONE);
            return convertView;
        }

        private int getSelectPos() {
            if (mNewParam.status == 1) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private void popListSelectDialog(String title, BaseAdapter selectsAdapter, DialogInterface.OnClickListener clickList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TaskAddActivity.this);
        builder.setTitle(title)
                .setAdapter(selectsAdapter, clickList);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialogTitleLineColor(dialog);
    }

    PriorityAdapter mPriorityAdapter;

    class PriorityAdapter extends BaseAdapter {

        final int priorityDrawableInverse[] = new int[]{
                R.drawable.ic_task_priority_3,
                R.drawable.ic_task_priority_2,
                R.drawable.ic_task_priority_1,
                R.drawable.ic_task_priority_0
        };

        String[] mData;

        public PriorityAdapter() {
            mData = getResources().getStringArray(R.array.strings_priority_inverse);
        }

        @Override
        public int getCount() {
            return mData.length;
        }

        @Override
        public Object getItem(int position) {
            return mData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.activity_task_status_list_item, parent, false);
                holder.mTitle = (TextView) convertView.findViewById(R.id.title);
                holder.mCheck = (ImageView) convertView.findViewById(R.id.check);
                holder.mIcon = convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.mTitle.setText(mData[position]);
            holder.mIcon.setBackgroundResource(priorityDrawableInverse[position]);
            if (position == getSelectPos())
                holder.mCheck.setVisibility(View.VISIBLE);
            else
                holder.mCheck.setVisibility(View.GONE);
            return convertView;
        }

        private int getSelectPos() {
            return priorityDrawableInverse.length - 1 - mNewParam.priority;
        }

    }

    static class ViewHolder {
        View mIcon;
        ImageView mCheck;
        TextView mTitle;
    }

    ArrayList<TaskObject.TaskComment> mData = new ArrayList<TaskObject.TaskComment>();

    BaseAdapter commentAdpter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageCommentHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.activity_maopao_detail_item, parent, false);
                holder = new ImageCommentHolder(convertView, mOnClickComment, myImageGetter, getImageLoad(), mOnClickUser, onClickImage);
                convertView.setTag(R.id.layout, holder);

            } else {
                holder = (ImageCommentHolder) convertView.getTag(R.id.layout);
            }

            TaskObject.TaskComment data = mData.get(position);
            holder.setTaskCommentContent(data);

            return convertView;
        }
    };

    private final MyImageGetter myImageGetter = new MyImageGetter(TaskAddActivity.this);

    private final ClickSmallImage onClickImage = new ClickSmallImage(this);

    private View.OnClickListener mOnClickComment = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final TaskObject.TaskComment comment = (TaskObject.TaskComment) v.getTag();
            if (comment.isMy()) {
                showDialog("删除评论", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = String.format(hostDeleteComment, comment.taskId, comment.id);
                        deleteNetwork(url, hostDeleteComment, comment.id);
                    }
                });
            } else {
                mEnterLayout.content.setTag(comment);
                String format = "回复 %s";
                mEnterLayout.content.setHint(String.format(format, comment.owner.name));
                mEnterLayout.popKeyboard();
            }
        }
    };

    private static class ImageCommentHolder extends BaseCommentHolder {

        private ContentAreaBase contentArea;

        public ImageCommentHolder(View convertView, View.OnClickListener onClickComment, Html.ImageGetter imageGetter, ImageLoadTool imageLoadTool, View.OnClickListener clickUser, View.OnClickListener clickImage) {
            super(convertView, onClickComment, imageGetter, imageLoadTool, clickUser);
            contentArea = new ContentAreaBase(convertView, onClickComment, imageGetter);
        }

        public void setTaskCommentContent(TaskObject.TaskComment comment) {
            super.setContent(comment);
            contentArea.setData(comment);
        }
    }

    public static class TaskJumpParams implements Serializable {
        public String userKey;
        public String projectName;
        public String taskId;

        public TaskJumpParams(String user, String project, String task) {
            userKey = user;
            projectName = project;
            taskId = task;
        }
    }

    private static class TaskParams {
        String content = "";
        int status;
        String ownerId = "";
        int priority;
        String deadline = "";

        UserObject owner;

        public TaskParams(TaskObject.SingleTask singleTask) {
            content = singleTask.content;
            status = singleTask.status;
            ownerId = singleTask.owner_id;
            priority = singleTask.priority;
            owner = singleTask.owner;
            deadline = singleTask.deadline;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TaskParams that = (TaskParams) o;

            if (priority != that.priority) return false;
            if (status != that.status) return false;
            if (content != null ? !content.equals(that.content) : that.content != null)
                return false;
            if (deadline != null ? !deadline.equals(that.deadline) : that.deadline != null)
                return false;
            if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
            if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = content != null ? content.hashCode() : 0;
            result = 31 * result + status;
            result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
            result = 31 * result + priority;
            result = 31 * result + (deadline != null ? deadline.hashCode() : 0);
            result = 31 * result + (owner != null ? owner.hashCode() : 0);
            return result;
        }
    }

    private View.OnClickListener onClickCreateDescription = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TaskDescriptionActivity_
                    .intent(TaskAddActivity.this)
                    .taskId("")
                    .descriptionData(descriptionDataNew)
                    .startForResult(RESULT_REQUEST_DESCRIPTION_CREATE);
        }
    };

    // 发评论
    View.OnClickListener mOnClickSendText = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s = mEnterLayout.getContent();

            if (EmojiFilter.containsEmoji(s)) {
                showMiddleToast("暂不支持发表情");
                return;
            }

            Object item = mEnterLayout.content.getTag();
            if (item != null && (item instanceof TaskObject.TaskComment)) {
                TaskObject.TaskComment comment = (TaskObject.TaskComment) item;
                String at = String.format("@%s ", comment.owner.global_key);
                s = at + s;
            }

            RequestParams params = new RequestParams();
            params.put("content", s);
            params.put("extra", "");

            postNetwork(String.format(HOST_COMMENT_ADD, mSingleTask.id), params, HOST_COMMENT_ADD);
        }
    };
}