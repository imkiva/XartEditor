package com.imkiva.xart.editor.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.imkiva.xart.ServiceManager;
import com.imkiva.xart.context.IContextManager;
import com.imkiva.xart.editor.api.skin.Skin;
import com.imkiva.xart.editor.common.DocumentProvider;
import com.imkiva.xart.editor.event.SyncModifyEvent;
import com.imkiva.xart.eventbus.api.IEventManager;
import com.imkiva.xarteditor.R;

/**
 * 编辑器的手势控制模块
 */
public class TouchNavigationMethod extends GestureDetector.SimpleOnGestureListener implements ActionMode.Callback {

    private static final int CURSOR_HIDE_DELAYED = 3000;
    private static final int SCROLL_EDGE_SLOP = 10;
    /**
     * 手指误差范围
     */
    private static final int TOUCH_SLOP = 12;

    /**
     * 光标对应的矩形
     */
    private Rect mCursorBloat = new Rect(0, 0, 0, 0);

    /**
     * 编辑器引用
     */
    private XartEditor mXartEditor;

    /**
     * 光标是否被触摸
     */
    private boolean isCursorTouched = false;

    /**
     * 手势识别
     */
    private GestureDetector mGestureDetector;

    private ActionMode mActionMode;
    /**
     * 用于双指缩放
     */
    private double mLastDistance;

    //=========================================
    //             光标控制器
    //=========================================
    private boolean isNearHandle = false;
    private boolean isNearHandleStart = false;
    private boolean isNearHandleEnd = false;
    private Paint handlePaint = new Paint();

    private int restShowHandleTime = 0;
    private boolean needToDrawHandle = false;

    private int sideHandleWidth;
    private int sideHandleHeight;


    private int midHandleWidth;
    private int midHandleHeight;

    private Bitmap leftHandleBitmap = null;
    private Bitmap midHandleBitmap = null;
    private Bitmap rightHandleBitmap = null;

    TouchNavigationMethod(XartEditor xartEditor) {
        mXartEditor = xartEditor;
        mXartEditor.setSelModeListener(active -> {
            if (active) {
                IContextManager contextManager = ServiceManager.get().getService(IContextManager.class);
                mActionMode = ((AppCompatActivity) contextManager.getCurrentActivity())
                        .startSupportActionMode(TouchNavigationMethod.this);
            } else {
                if (mActionMode != null) {
                    mActionMode.finish();
                    mActionMode = null;
                }
            }
        });
        mGestureDetector = new GestureDetector(xartEditor.getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        int coordinateToCharIndex = this.mXartEditor.coordinateToCharIndex(screenToViewX((int) e.getX()), screenToViewY((int) e.getY()));
        if (!this.isNearHandle) {
            if (!this.mXartEditor.inSelectionRange(coordinateToCharIndex)
                    && coordinateToCharIndex >= 0) {
                char charAt;
                this.mXartEditor.moveCursor(coordinateToCharIndex);
                DocumentProvider createDocumentProvider = this.mXartEditor.getDoc();
                int i = coordinateToCharIndex;
                while (i >= 0) {
                    charAt = createDocumentProvider.charAt(i);
                    if ((charAt < 'a' || charAt > 'z') && ((charAt < 'A' || charAt > 'Z') && ((charAt < '0' || charAt > '9') && charAt != '_'))) {
                        break;
                    }
                    i--;
                }
                if (i != coordinateToCharIndex) {
                    ++i;
                }
                while (coordinateToCharIndex >= 0) {
                    charAt = createDocumentProvider.charAt(coordinateToCharIndex);
                    if ((charAt < 'a' || charAt > 'z') && ((charAt < 'A' || charAt > 'Z') && ((charAt < '0' || charAt > '9') && charAt != '_'))) {
                        break;
                    }
                    coordinateToCharIndex++;
                }
                this.mXartEditor.setSelectionRange(i, coordinateToCharIndex - i);
                this.mXartEditor.selectText(true);
                setNeedToDrawHandle(true);
            }
            super.onLongPress(e);
        }
    }


    @Override
    public boolean onDown(MotionEvent motionEvent) {
        int x = screenToViewX((int) motionEvent.getX());
        int y = screenToViewY((int) motionEvent.getY());
        this.isCursorTouched = isNearChar(x, y, this.mXartEditor.getCursorPosition());
        this.isNearHandle = isNearHandle(x, y, this.mXartEditor.getCursorPosition()) && !this.isCursorTouched && this.restShowHandleTime > 0 && !this.mXartEditor.isSelectText();
        this.isNearHandleStart = false;
        this.isNearHandleEnd = false;

        if (this.mXartEditor.isFlingScrolling()) {
            this.mXartEditor.stopFlingScrolling();

        } else if (this.mXartEditor.isSelectText()) {
            if (isNearChar(x, y, this.mXartEditor.getSelectionStart())) {
                this.mXartEditor.focusSelectionStart();
                this.mXartEditor.performHapticFeedback(0);
                this.isCursorTouched = true;

            } else if (isNearChar(x, y, this.mXartEditor.getSelectionEnd())) {
                this.mXartEditor.focusSelectionEnd();
                this.mXartEditor.performHapticFeedback(0);
                this.isCursorTouched = true;

            } else if (isNearHandle(x, y, this.mXartEditor.getSelectionStart(), 1)) {
                this.mXartEditor.focusSelectionStart();
                this.isNearHandleStart = true;

            } else if (isNearHandle(x, y, this.mXartEditor.getSelectionEnd(), 0)) {
                this.mXartEditor.focusSelectionEnd();
                this.isNearHandleEnd = true;
            }
        }

        if (this.isCursorTouched) {
            this.mXartEditor.performHapticFeedback(0);
        }
        return true;
    }

    private boolean onUp(MotionEvent e) {
        isCursorTouched = false;
        this.isNearHandle = false;
        return true;
    }

    private boolean isNearHandle(int x, int y, int charOffset) {
        Rect bounds = this.mXartEditor.getBoundingBox(charOffset);
        return y >= bounds.top + this.mXartEditor.getFontHeight() && y < (bounds.top + this.mXartEditor.getFontHeight()) + this.midHandleHeight && x >= bounds.left - (this.midHandleWidth / 2) && x < bounds.left + (this.midHandleWidth / 2);
    }

    private boolean isNearHandle(int x, int y, int charOffset, int unknownYet) {
        Rect bounds = this.mXartEditor.getBoundingBox(charOffset);
        return y >= bounds.top + this.mXartEditor.getFontHeight() && y < (bounds.top + this.mXartEditor.getFontHeight()) + this.sideHandleHeight && x >= bounds.left - (this.sideHandleWidth * unknownYet) && x < bounds.left + (this.sideHandleWidth * (1 - unknownYet));
    }

    private void drawMidHandle(Canvas canvas) {
        if (this.midHandleBitmap == null) {
            this.midHandleWidth = this.mXartEditor.getAdvance('M') * 4;
            if (this.midHandleBitmap == null) {
                this.midHandleBitmap = BitmapFactory.decodeResource(this.mXartEditor.getResources(), R.drawable.xart_text_select_handle_middle);
            }
            this.midHandleHeight = (this.midHandleWidth * this.midHandleBitmap.getHeight()) / this.midHandleBitmap.getWidth();
            mCursorBloat = new Rect(0, 0, 0, this.midHandleHeight);
            this.midHandleBitmap = Bitmap.createScaledBitmap(this.midHandleBitmap, this.midHandleWidth, this.midHandleHeight, true);
        }
        Rect bounds = this.mXartEditor.getBoundingBox(this.mXartEditor.getCursorPosition());
        canvas.drawBitmap(this.midHandleBitmap, (float) ((bounds.left + this.mXartEditor.getPaddingLeft()) - (this.midHandleWidth / 2)), (float) (bounds.top + this.mXartEditor.getFontHeight()), this.handlePaint);
    }


    private void drawDoubleHandle(Canvas canvas) {
        if (this.leftHandleBitmap == null) {
            this.sideHandleWidth = this.mXartEditor.getAdvance('M') * 4;
            if (this.leftHandleBitmap == null) {
                this.leftHandleBitmap = BitmapFactory.decodeResource(this.mXartEditor.getResources(), R.drawable.xart_text_select_handle_left);
            }
            this.sideHandleHeight = (this.sideHandleWidth * this.leftHandleBitmap.getHeight()) / this.leftHandleBitmap.getWidth();
            this.leftHandleBitmap = Bitmap.createScaledBitmap(this.leftHandleBitmap, this.sideHandleWidth, this.sideHandleHeight, true);
            if (this.rightHandleBitmap == null) {
                this.rightHandleBitmap = BitmapFactory.decodeResource(this.mXartEditor.getResources(), R.drawable.xart_text_select_handle_right);
            }
            this.rightHandleBitmap = Bitmap.createScaledBitmap(this.rightHandleBitmap, this.sideHandleWidth, this.sideHandleHeight, true);
        }
        Rect bounds = this.mXartEditor.getBoundingBox(this.mXartEditor.getSelectionStart());
        canvas.drawBitmap(this.leftHandleBitmap, (float) ((bounds.left + this.mXartEditor.getLineNumberPadding()) - this.sideHandleWidth * 3 / 4), (float) (bounds.top + this.mXartEditor.getFontHeight()), this.handlePaint);
        bounds = this.mXartEditor.getBoundingBox(this.mXartEditor.getSelectionEnd());
        canvas.drawBitmap(this.rightHandleBitmap, (float) ((bounds.left + this.mXartEditor.getLineNumberPadding()) - this.sideHandleWidth / 4), (float) (bounds.top + this.mXartEditor.getFontHeight()), this.handlePaint);
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        float Xdistance = Math.abs(e1.getRawX() - e2.getRawX());
        float Ydistance = Math.abs(e1.getRawY() - e2.getRawY());

        if (Xdistance > Ydistance) {
            distanceY = 0;
        } else {
            distanceX = 0;
        }

        if (this.isCursorTouched || this.isNearHandle || this.isNearHandleStart || this.isNearHandleEnd) {
            dragCursor(e2);
        } else {
            scrollView(distanceX, distanceY);
        }

        if ((e2.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            onUp(e2);
        }
        return true;
    }

    private void dragCursor(MotionEvent motionEvent) {
        boolean z = false;
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (this.isNearHandle) {
            y -= this.mXartEditor.getFontHeight() + (this.midHandleHeight / 2);
        }
        if (this.isNearHandleStart || this.isNearHandleEnd) {
            y -= this.mXartEditor.getFontHeight() + (this.sideHandleHeight / 2);
            x = this.isNearHandleStart ? x + (this.sideHandleWidth / 4) : x - (this.sideHandleWidth / 4);
        }
        if (this.isNearHandle || this.isNearHandleStart || this.isNearHandleEnd) {
            x += (this.mXartEditor.getAdvance('M') + this.mXartEditor.getAdvance('.')) / 4;
        }
        int paddingLeft = (x - this.mXartEditor.getPaddingLeft()) + this.mXartEditor.getLineNumberPadding();
        int paddingTop = y - this.mXartEditor.getPaddingTop();
        if (paddingLeft < SCROLL_EDGE_SLOP) {
            z = this.mXartEditor.autoScrollCursor(XartEditor.ScrollTarget.SCROLL_LEFT);
        } else if (paddingLeft >= (this.mXartEditor.getContentWidth() + this.mXartEditor.getLineNumberPadding()) - SCROLL_EDGE_SLOP) {
            z = this.mXartEditor.autoScrollCursor(XartEditor.ScrollTarget.SCROLL_RIGHT);
        } else if (paddingTop < SCROLL_EDGE_SLOP) {
            z = this.mXartEditor.autoScrollCursor(XartEditor.ScrollTarget.SCROLL_UP);
        } else if (paddingTop >= this.mXartEditor.getContentHeight() - SCROLL_EDGE_SLOP) {
            z = this.mXartEditor.autoScrollCursor(XartEditor.ScrollTarget.SCROLL_DOWN);
        }
        if (!z) {
            y = this.mXartEditor.coordinateToCharIndex(screenToViewX(x), screenToViewY(y));
            if (y >= 0) {
                this.mXartEditor.moveCursor(y);
            }
        }
        if (this.restShowHandleTime > 0) {
            setRestShowHandleTime(CURSOR_HIDE_DELAYED);
        }
        if (this.needToDrawHandle) {
            this.mXartEditor.invalidate();
        }
    }


    private void scrollView(float distanceX, float distanceY) {
        int newX = (int) distanceX + mXartEditor.getScrollX();
        int newY = (int) distanceY + mXartEditor.getScrollY();

        int maxWidth = Math.max(mXartEditor.getMaxScrollX(),
                mXartEditor.getScrollX());
        if (newX > maxWidth) {
            newX = maxWidth;
        } else if (newX < 0) {
            newX = 0;
        }

        int maxHeight = Math.max(mXartEditor.getMaxScrollY(),
                mXartEditor.getScrollY());
        if (newY > maxHeight) {
            newY = maxHeight;
        } else if (newY < 0) {
            newY = 0;
        }
        mXartEditor.scrollTo(newX, newY);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        int x = screenToViewX((int) motionEvent.getX());
        int y = screenToViewY((int) motionEvent.getY());
        int coordToCharIndex = this.mXartEditor.coordinateToCharIndex(x, y);
        if (this.mXartEditor.isSelectText()) {
            int coordToCharIndexStrict = this.mXartEditor.coordinateToCharIndexStrict(x, y);
            if (!(this.mXartEditor.inSelectionRange(coordToCharIndexStrict) || isNearHandle(x, y, this.mXartEditor.getSelectionStart(), 1) || isNearHandle(x, y, this.mXartEditor.getSelectionEnd(), 0))) {
                this.mXartEditor.selectText(false);
                if (coordToCharIndexStrict >= 0) {
                    this.mXartEditor.moveCursor(coordToCharIndex);
                }
            } else {
                mXartEditor.showInputMethod(true);
            }
        } else {
            if (coordToCharIndex >= 0) {
                this.mXartEditor.moveCursor(coordToCharIndex);
            }
            this.mXartEditor.showInputMethod(true);
        }
        if (!this.mXartEditor.isSelectText()) {
            performShowHandleTask();
        }
        return true;
    }

    private void performShowHandleTask() {
        if (this.restShowHandleTime == 0) {
            this.mXartEditor.postDelayed(new Runnable() {
                @Override
                public void run() {
                    restShowHandleTime -= 100;
                    if (restShowHandleTime == 0) {
                        mXartEditor.invalidate();
                    } else {
                        mXartEditor.postDelayed(this, 100);
                    }
                }
            }, 100);
        }
        this.mXartEditor.post(new Runnable() {

            @Override
            public void run() {
                setRestShowHandleTime(CURSOR_HIDE_DELAYED);
            }
        });
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        this.isCursorTouched = true;
        int charOffset = this.mXartEditor.coordinateToCharIndex(screenToViewX((int) motionEvent.getX()), screenToViewY((int) motionEvent.getY()));
        if (charOffset >= 0) {
            if (mXartEditor.isSelectText()) {
                if (mXartEditor.inSelectionRange(charOffset)) {
                    return true;
                }
            }
            mXartEditor.moveCursor(charOffset);
            mXartEditor.selectText(false);
            performShowHandleTask();
        }
        return true;
    }


    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (!isCursorTouched || e.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onDoubleTapEvent(e);
        }
        dragCursor(e);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        float xDist = Math.abs(e1.getRawX() - e2.getRawX());
        float yDist = Math.abs(e1.getRawY() - e2.getRawY());

        //单向滑动,要么上下滑动,要么左右滑动
        if (xDist > yDist) {
            velocityY = 0;
        } else {
            velocityX = 0;
        }

        if (!(this.isCursorTouched || this.isNearHandle || this.isNearHandleStart || this.isNearHandleEnd)) {
            mXartEditor.flingScroll((int) (-velocityX), (int) (-velocityY));
        }
        onUp(e2);
        return true;
    }


    private void setRestShowHandleTime(int restTime) {
        this.restShowHandleTime = restTime;
        this.mXartEditor.invalidate();
    }

    private void setNeedToDrawHandle(boolean needToDrawHandle) {
        this.needToDrawHandle = needToDrawHandle;
        this.mXartEditor.invalidate();
    }


    /**
     * 触摸事件
     *
     * @param motionEvent 事件对象
     */
    public boolean onTouchEvent(MotionEvent motionEvent) {

        onTouchZoom(motionEvent);
        boolean onTouchEvent = this.mGestureDetector.onTouchEvent(motionEvent);

        return (onTouchEvent || (motionEvent.getAction() & MotionEventCompat.ACTION_MASK) != MotionEvent.ACTION_UP) ? onTouchEvent : onUp(motionEvent);
    }

    boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    void onTextDrawComplete(Canvas canvas) {
        if (this.needToDrawHandle) {
            if (this.mXartEditor.isSelectText() && mXartEditor.getSelectionStart() != mXartEditor.getSelectionEnd()) {
                drawDoubleHandle(canvas);
            } else {
                setNeedToDrawHandle(false);
            }
        } else if (this.restShowHandleTime > 0) {
            drawMidHandle(canvas);
        }
    }

    /**
     * 主题切换时回调
     *
     * @param colorScheme 新的主题
     */
    void onColorSchemeChanged(Skin colorScheme) {
    }

    /**
     * 取得光标对应的矩形
     *
     * @return 光标对应的矩形
     */
    Rect getCursorBloat() {
        return mCursorBloat;
    }

    final protected int getPointerId(MotionEvent e) {
        return (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    }

    /**
     * Converts a x-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    private int screenToViewX(int x) {
        return x - mXartEditor.getPaddingLeft() + mXartEditor.getScrollX();
    }


    private int screenToViewY(int y) {
        return y - mXartEditor.getPaddingTop() + mXartEditor.getScrollY();
    }

    private boolean isDragSelect() {
        return false;
    }

    private boolean isNearChar(int x, int y, int charOffset) {
        Rect bounds = mXartEditor.getBoundingBox(charOffset);

        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }


    private boolean onTouchZoom(MotionEvent motionEvent) {
        if (motionEvent.getAction() != 2) {
            this.mLastDistance = 0;
        } else if (motionEvent.getPointerCount() == 2) {//当前有俩手指
            double spacing = getDistance(motionEvent);
            if (this.mLastDistance != 0) {
                this.mXartEditor.setTextSize((float) (this.mXartEditor.getTextSize() * (spacing / this.mLastDistance)));
            }
            this.mLastDistance = spacing;
        }
        return false;
    }


    /**
     * 得到两根手指之间的距离
     *
     * @param event 触摸事件
     */
    private static float getDistance(MotionEvent event) {
        float a = event.getX(1) - event.getX(0);
        float b = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        IEventManager eventManager = ServiceManager.get().getService(IEventManager.class);
        menu.add("paste").setIcon(R.drawable.abc_ic_menu_paste_mtrl_am_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("select all").setIcon(R.drawable.abc_ic_menu_selectall_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (mXartEditor.isSelectText()) {
            menu.add("copy").setIcon(R.drawable.abc_ic_menu_copy_mtrl_am_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add("cut").setIcon(R.drawable.abc_ic_menu_cut_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean finish = true;
        ClipboardManager manager = (ClipboardManager) mXartEditor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (item.getTitle().equals("paste")) {
            if (manager != null && manager.hasPrimaryClip()) {
                ClipData clipData = manager.getPrimaryClip();
                if (clipData.getItemCount() > 0) {
                    mXartEditor.paste(clipData.getItemAt(0).getText().toString());
                }
            }
            IEventManager eventManager = ServiceManager.get().getService(IEventManager.class);
            if (eventManager != null) {
                eventManager.sendEvent(new SyncModifyEvent(true));
            }

        } else if (item.getTitle().equals("copy")) {
            mXartEditor.copy(manager);
            Toast.makeText(mXartEditor.getContext(), "Copy success!", Toast.LENGTH_SHORT).show();

        } else if (item.getTitle().equals("cut")) {
            mXartEditor.cut(manager);

        } else if (item.getTitle().equals("select all")) {
            mXartEditor.selectAll();
            finish = false;
        }

        if (finish) {
            mXartEditor.selectText(false);
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.mActionMode = null;
    }


}
