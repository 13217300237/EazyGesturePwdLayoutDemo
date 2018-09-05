package com.example.gesture_password_study.gesture_pwd;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gesture_password_study.R;
import com.example.gesture_password_study.gesture_pwd.base.GestureBaseActivity;
import com.example.gesture_password_study.gesture_pwd.custom.EasyGestureLockLayout;
import com.example.gesture_password_study.util.CacheUtils;

import java.util.List;

/**
 * 手势密码 重新设置；
 * 首先校验原密码，在校验通过之后，重新设置手势；
 * 如果5次校验原密码都失败了，则弹窗提示；
 */
public class GesturePwdResetActivity extends GestureBaseActivity {

    EasyGestureLockLayout layout_small;
    TextView tv_go;
    TextView tv_redraw;
    EasyGestureLockLayout layout_parent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_gesture_pwd_reset);
        initView();
        initLayoutView();
    }

    private void initView() {
        tv_go = findViewById(R.id.tv_go);
        layout_parent = findViewById(R.id.layout_parent);
        layout_small = findViewById(R.id.layout_small);
        tv_redraw = findViewById(R.id.tv_redraw);
    }

    protected void initLayoutView() {
        EasyGestureLockLayout.GestureEventCallbackAdapter adapter = new EasyGestureLockLayout.GestureEventCallbackAdapter() {

            @Override
            public void onSwipeFinish(List<Integer> pwd) {
                layout_small.refreshPwdKeyboard(pwd);//通知另一个小密码盘，将密码点展示出来，但是不展示轨迹线
                if (layout_parent.getCurrentMode() == EasyGestureLockLayout.STATUS_RESET)
                    tv_redraw.setVisibility(View.VISIBLE);
                else
                    tv_redraw.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onResetFinish(List<Integer> pwd) {
                super.onResetFinish(pwd);
                savePwd(showPwd("showGesturePwdInt", pwd));//保存密码到本地
                Toast.makeText(GesturePwdResetActivity.this, "密码已保存", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCheckFinish(boolean succeedOrFailed) {
                if (succeedOrFailed) {//如果解锁成功，则切换到set模式
                    layout_parent.switchToResetMode();
                    tv_go.setText("请输入新的手势密码");
                } else {
                    Toast.makeText(GesturePwdResetActivity.this, "手势密码验证失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSwipeMore() {
                //执行动画
                animate(tv_go);
            }

            @Override
            public void onToast(String s, int textColor) {
                tv_go.setText(s);
                if (textColor != 0)
                    tv_go.setTextColor(textColor);
                if (textColor == 0xffFF3232) {
                    animate(tv_go);
                }
            }
        };
        layout_parent.setGestureFinishedCallback(adapter);

        if (!TextUtils.isEmpty(CacheUtils.readFile(this, CacheUtils.get_CACHE_GESTURE_PWD_INT()))) {//检查当前是否已经设置了密码
            // 如果已经设置，则先进入check模式
            layout_parent.switchToCheckMode(parsePwdStr(getPwd()), 5);//校验密码
        } else {//如果没有设置，则进入setting模式
            layout_parent.switchToResetMode();
            tv_go.setText(EasyGestureLockLayout.ToastStrHolder.forYourSafetyStr);
            tv_go.setTextColor(EasyGestureLockLayout.ColorHolder.COLOR_YELLOW);
        }

        tv_redraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_parent.initCurrentTimes();
                tv_redraw.setVisibility(View.INVISIBLE);
                layout_small.refreshPwdKeyboard(null);
                tv_go.setText("请重新绘制");
            }
        });
    }


    /**
     * 文案的左右摇动动画;重写，因为效果和基类的有一个差别
     */
    @Override
    protected void animate(View tv_go) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(tv_go, "translationX", -20, 20, -20, 0);
        objectAnimator.setDuration(300);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {//重写之后，增加了这个lis
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                layout_small.refreshPwdKeyboard(null);
            }
        });
        objectAnimator.start();
    }

}
